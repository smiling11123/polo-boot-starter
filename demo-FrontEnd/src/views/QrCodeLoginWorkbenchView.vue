<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type {ActivityLog, RequestOptions, ApiResponse, LoginResponse, QrCheckResult} from "@/interface"


const STORAGE_KEY = 'polo-qrcode-workbench-settings'
const defaultBaseUrl =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ??
  (typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8080')

function loadPersistedSettings() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as Record<string, unknown>
  } catch {
    return {}
  }
}

function normalizeBaseUrl(baseUrl: string) {
  return (baseUrl || defaultBaseUrl).trim().replace(/\/+$/, '')
}

const persistedSettings = loadPersistedSettings()

const settings = reactive({
  baseUrl: String(persistedSettings.baseUrl ?? defaultBaseUrl),
  pollIntervalMs: Number(persistedSettings.pollIntervalMs ?? 2000),
})

const mobileAuth = reactive({
  username: String(persistedSettings.username ?? 'admin'),
  password: String(persistedSettings.password ?? '123456'),
  token: String(persistedSettings.token ?? ''),
  currentUser: '',
})

const pcState = reactive({
  uuid: '',
  status: 'IDLE',
  nickname: '',
  avatar: '',
  accessToken: '',
  refreshToken: '',
  error: '',
  polling: false,
  lastCheckAt: '',
})

const mobileState = reactive({
  uuidInput: '',
  loginResponse: null as LoginResponse | null,
  confirmResponse: null as LoginResponse | null,
  error: '',
})

const logs = ref<ActivityLog[]>([])
const pollTimer = ref<number | null>(null)

const statusLabelMap: Record<string, string> = {
  IDLE: '未生成',
  WAITING_SCAN: '等待扫码',
  SCANNED: '已扫码，待确认',
  CONFIRMED: '已确认登录',
  EXPIRED: '二维码已过期',
}

const hasMobileToken = computed(() => Boolean(mobileAuth.token.trim()))
const hasActiveQr = computed(() => Boolean(pcState.uuid))
const pollStatusText = computed(() => (pcState.polling ? '轮询中' : '未轮询'))
const qrDisplayText = computed(() => pcState.uuid || '点击“生成二维码”后显示 uuid')
const resolvedStatusLabel = computed(() => statusLabelMap[pcState.status] || pcState.status || '未生成')

function addLog(message: string, level: ActivityLog['level'] = 'info') {
  logs.value.unshift({
    id: Date.now() + Math.floor(Math.random() * 1000),
    time: new Date().toLocaleTimeString(),
    level,
    message,
  })
  if (logs.value.length > 80) {
    logs.value.length = 80
  }
}

watch(
  [
    () => settings.baseUrl,
    () => settings.pollIntervalMs,
    () => mobileAuth.username,
    () => mobileAuth.password,
    () => mobileAuth.token,
  ],
  () => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        baseUrl: settings.baseUrl,
        pollIntervalMs: settings.pollIntervalMs,
        username: mobileAuth.username,
        password: mobileAuth.password,
        token: mobileAuth.token,
      }),
    )
  },
)

function buildUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizeBaseUrl(settings.baseUrl)}${normalizedPath}`
}

async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers ?? {})
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }

  const bearerToken = options.bearerToken ?? mobileAuth.token.trim()
  if (options.auth !== false && bearerToken) {
    headers.set('Authorization', bearerToken)
  }

  if (options.body instanceof URLSearchParams && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/x-www-form-urlencoded;charset=UTF-8')
  }

  const response = await fetch(buildUrl(path), { ...options, headers })
  const text = await response.text()
  let payload: ApiResponse<T> | null = null

  try {
    payload = text ? (JSON.parse(text) as ApiResponse<T>) : null
  } catch {
    throw new Error(text || `请求失败 (HTTP ${response.status})`)
  }

  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.msg || `请求失败 (HTTP ${response.status})`)
  }
  return payload.data
}

function clearPcResult(keepUuid = true) {
  const previousUuid = keepUuid ? pcState.uuid : ''
  pcState.uuid = previousUuid
  pcState.status = previousUuid ? pcState.status : 'IDLE'
  pcState.nickname = ''
  pcState.avatar = ''
  pcState.accessToken = ''
  pcState.refreshToken = ''
  pcState.error = ''
}

function stopPolling() {
  if (pollTimer.value !== null) {
    window.clearInterval(pollTimer.value)
    pollTimer.value = null
  }
  pcState.polling = false
}

function startPolling() {
  stopPolling()
  if (!pcState.uuid) {
    return
  }
  pcState.polling = true
  const interval = Math.max(1000, settings.pollIntervalMs || 2000)
  pollTimer.value = window.setInterval(() => {
    void checkStatus(true)
  }, interval)
}

async function generateQrCode() {
  try {
    clearPcResult(false)
    const uuid = await apiRequest<string>('/auth/qrcode/generate', {
      method: 'GET',
      auth: false,
    })
    pcState.uuid = uuid
    pcState.status = 'WAITING_SCAN'
    mobileState.uuidInput = uuid
    addLog(`已生成二维码会话：${uuid}`)
    await checkStatus(true)
    startPolling()
  } catch (error) {
    pcState.error = toErrorMessage(error)
    addLog(`生成二维码失败：${pcState.error}`, 'error')
  }
}

async function checkStatus(silent = false) {
  if (!pcState.uuid) {
    if (!silent) {
      addLog('请先生成二维码或填写 uuid', 'warn')
    }
    return
  }

  try {
    const result = await apiRequest<QrCheckResult>(`/auth/qrcode/check?uuid=${encodeURIComponent(pcState.uuid)}`, {
      method: 'GET',
      auth: false,
    })
    pcState.status = result.status || 'IDLE'
    pcState.nickname = result.nickname || ''
    pcState.avatar = result.avatar || ''
    pcState.accessToken = result.tokenPair?.accessToken || result.token || ''
    pcState.refreshToken = result.tokenPair?.refreshToken || ''
    pcState.lastCheckAt = new Date().toLocaleTimeString()
    pcState.error = ''

    if (!silent) {
      addLog(`二维码状态更新：${resolvedStatusLabel.value}`)
    }

    if (pcState.status === 'CONFIRMED' || pcState.status === 'EXPIRED') {
      stopPolling()
      addLog(`二维码流程结束：${resolvedStatusLabel.value}`, pcState.status === 'CONFIRMED' ? 'info' : 'warn')
    }
  } catch (error) {
    pcState.error = toErrorMessage(error)
    if (!silent) {
      addLog(`查询二维码状态失败：${pcState.error}`, 'error')
    }
  }
}

async function loginMobile() {
  try {
    const response = await apiRequest<LoginResponse>('/auth/login', {
      method: 'POST',
      auth: false,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: mobileAuth.username,
        password: mobileAuth.password,
        device: {
          deviceId: 'qrcode-mobile-simulator',
          deviceType: 'APP',
          deviceName: 'QrCode Mobile Simulator',
        },
      }),
    })
    const tokenType = response.tokenPair.tokenType || 'Bearer'
    mobileAuth.token = `${tokenType} ${response.tokenPair.accessToken}`
    mobileAuth.currentUser = `${response.username} (${response.role})`
    mobileState.loginResponse = response
    addLog(`移动端模拟登录成功：${mobileAuth.currentUser}`)
  } catch (error) {
    mobileState.error = toErrorMessage(error)
    addLog(`移动端登录失败：${mobileState.error}`, 'error')
  }
}

function clearMobileToken() {
  mobileAuth.token = ''
  mobileAuth.currentUser = ''
  mobileState.loginResponse = null
  addLog('已清空移动端 Bearer Token', 'warn')
}

async function scanQrCode() {
  const uuid = mobileState.uuidInput.trim() || pcState.uuid
  if (!uuid) {
    addLog('请先生成二维码或填写 uuid', 'warn')
    return
  }
  if (!hasMobileToken.value) {
    addLog('扫码前请先完成移动端登录', 'warn')
    return
  }

  try {
    await apiRequest<null>('/auth/qrcode/scan', {
      method: 'POST',
      body: new URLSearchParams({ uuid }),
    })
    addLog(`已模拟扫码：${uuid}`)
    if (uuid === pcState.uuid) {
      await checkStatus(true)
    }
  } catch (error) {
    mobileState.error = toErrorMessage(error)
    addLog(`扫码失败：${mobileState.error}`, 'error')
  }
}

async function confirmQrCode() {
  const uuid = mobileState.uuidInput.trim() || pcState.uuid
  if (!uuid) {
    addLog('请先生成二维码或填写 uuid', 'warn')
    return
  }
  if (!hasMobileToken.value) {
    addLog('确认登录前请先完成移动端登录', 'warn')
    return
  }

  try {
    const response = await apiRequest<LoginResponse>('/auth/qrcode/confirm', {
      method: 'POST',
      body: new URLSearchParams({ uuid }),
    })
    mobileState.confirmResponse = response
    addLog(`已确认二维码登录：${uuid}`)
    if (uuid === pcState.uuid) {
      await checkStatus(true)
    }
  } catch (error) {
    mobileState.error = toErrorMessage(error)
    addLog(`确认登录失败：${mobileState.error}`, 'error')
  }
}

async function copyUuid() {
  if (!pcState.uuid) {
    addLog('当前没有可复制的 uuid', 'warn')
    return
  }
  try {
    await navigator.clipboard.writeText(pcState.uuid)
    addLog('已复制当前二维码 uuid')
  } catch (error) {
    addLog(`复制失败：${toErrorMessage(error)}`, 'error')
  }
}

function fillMobileUuid() {
  if (!pcState.uuid) {
    addLog('当前没有生成中的二维码', 'warn')
    return
  }
  mobileState.uuidInput = pcState.uuid
  addLog('已将当前二维码 uuid 填充到移动端区域')
}

function toErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}

function formatJson(value: unknown) {
  return JSON.stringify(value, null, 2)
}

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="workbench">
    <header class="hero">
      <h1>二维码登录测试页</h1>
      <p>模拟 PC 端生成二维码并轮询状态，再用“移动端模拟器”完成扫码与确认登录，直接对接 demo 的二维码登录接口。</p>
    </header>

    <section class="card settings-card">
      <div class="section-title">
        <h2>基础设置</h2>
        <span class="status-chip" :data-state="pcState.polling ? 'active' : 'idle'">{{ pollStatusText }}</span>
      </div>
      <div class="form-grid">
        <!-- <label>
          <span>后端地址</span>
          <input v-model.trim="settings.baseUrl" type="text" placeholder="http://localhost:8080" />
        </label> -->
        <label>
          <span>轮询间隔（ms）</span>
          <input v-model.number="settings.pollIntervalMs" type="number" min="1000" step="500" />
        </label>
      </div>
    </section>

    <div class="content-grid">
      <section class="card">
        <div class="section-title">
          <h2>PC 端二维码面板</h2>
          <span class="status-chip" :data-state="pcState.status">{{ resolvedStatusLabel }}</span>
        </div>

        <div class="actions">
          <button type="button" @click="generateQrCode">生成二维码</button>
          <button type="button" class="secondary" :disabled="!hasActiveQr" @click="checkStatus()">立即查询状态</button>
          <button type="button" class="secondary" :disabled="!hasActiveQr" @click="copyUuid">复制 uuid</button>
        </div>

        <div class="qr-card" :data-state="pcState.status">
          <div class="qr-matrix" aria-hidden="true">
            <span v-for="index in 64" :key="index" :class="{ dark: (index * 7 + qrDisplayText.length) % 5 < 2 }"></span>
          </div>
          <div class="qr-meta">
            <strong>二维码 payload</strong>
            <code>{{ qrDisplayText }}</code>
            <span>demo 里的二维码核心内容就是这个 uuid，本页用它做测试，不依赖外部二维码图片服务。</span>
          </div>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <span>uuid</span>
            <strong>{{ pcState.uuid || '-' }}</strong>
          </div>
          <div class="stat-item">
            <span>状态</span>
            <strong>{{ resolvedStatusLabel }}</strong>
          </div>
          <div class="stat-item">
            <span>扫码昵称</span>
            <strong>{{ pcState.nickname || '-' }}</strong>
          </div>
          <div class="stat-item">
            <span>最后查询时间</span>
            <strong>{{ pcState.lastCheckAt || '-' }}</strong>
          </div>
        </div>

        <div v-if="pcState.avatar" class="avatar-row">
          <img :src="pcState.avatar" alt="扫码用户头像" />
          <span>{{ pcState.nickname || '扫码用户' }}</span>
        </div>

        <div class="detail-box">
          <h3>PC 端登录结果</h3>
          <p v-if="!pcState.accessToken">等待确认登录后，这里会显示 PC 端拿到的 token。</p>
          <pre v-else>{{ formatJson({ accessToken: pcState.accessToken, refreshToken: pcState.refreshToken || null }) }}</pre>
        </div>
      </section>

      <section class="card">
        <div class="section-title">
          <h2>移动端模拟器</h2>
          <span class="status-chip" :data-state="hasMobileToken ? 'active' : 'idle'">{{ hasMobileToken ? '已登录' : '未登录' }}</span>
        </div>

        <div class="form-grid">
          <label>
            <span>测试账号</span>
            <input v-model.trim="mobileAuth.username" type="text" placeholder="admin" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="mobileAuth.password" type="password" placeholder="123456" />
          </label>
          <label class="full-width">
            <span>Bearer Token</span>
            <textarea v-model.trim="mobileAuth.token" rows="3" placeholder="可手动粘贴，也可先点“移动端登录”"></textarea>
          </label>
          <label class="full-width">
            <span>要扫码的 uuid</span>
            <input v-model.trim="mobileState.uuidInput" type="text" placeholder="可手填，或点击“填充当前二维码 uuid”" />
          </label>
        </div>

        <div class="actions">
          <button type="button" @click="loginMobile">移动端登录</button>
          <button type="button" class="secondary" :disabled="!hasActiveQr" @click="fillMobileUuid">填充当前二维码 uuid</button>
          <button type="button" class="secondary" :disabled="!hasMobileToken" @click="scanQrCode">模拟扫码</button>
          <button type="button" class="secondary" :disabled="!hasMobileToken" @click="confirmQrCode">确认登录</button>
          <button type="button" class="danger" @click="clearMobileToken">清空 Token</button>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <span>当前移动端用户</span>
            <strong>{{ mobileAuth.currentUser || '-' }}</strong>
          </div>
          <div class="stat-item">
            <span>扫码 uuid</span>
            <strong>{{ mobileState.uuidInput || '-' }}</strong>
          </div>
        </div>

        <div class="detail-box">
          <h3>移动端登录响应</h3>
          <p v-if="!mobileState.loginResponse">先点“移动端登录”，拿到 Bearer Token 后再扫码和确认。</p>
          <pre v-else>{{ formatJson(mobileState.loginResponse) }}</pre>
        </div>

        <div class="detail-box">
          <h3>确认登录响应</h3>
          <p v-if="!mobileState.confirmResponse">确认成功后，这里会展示服务端返回的 PC 端登录结果。</p>
          <pre v-else>{{ formatJson(mobileState.confirmResponse) }}</pre>
        </div>
      </section>
    </div>

    <section class="card">
      <div class="section-title">
        <h2>操作日志</h2>
        <span>{{ logs.length }} 条</span>
      </div>
      <ul class="log-list">
        <li v-for="item in logs" :key="item.id" :data-level="item.level">
          <time>{{ item.time }}</time>
          <span>{{ item.message }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.workbench {
  max-width: 1360px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  color: #12253f;
}

.hero {
  margin-bottom: 24px;
}

.hero h1 {
  margin: 0 0 10px;
  font-size: 34px;
}

.hero p {
  margin: 0;
  color: #5a6d88;
  line-height: 1.7;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(16, 35, 61, 0.08);
  border-radius: 22px;
  padding: 24px;
  box-shadow: 0 24px 70px rgba(23, 37, 84, 0.08);
  margin-bottom: 20px;
}

.settings-card {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.section-title h2 {
  margin: 0;
  font-size: 22px;
}

.status-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(71, 85, 105, 0.12);
  color: #334155;
  font-size: 13px;
}

.status-chip[data-state='WAITING_SCAN'],
.status-chip[data-state='active'] {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.status-chip[data-state='SCANNED'] {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.status-chip[data-state='CONFIRMED'] {
  background: rgba(22, 163, 74, 0.14);
  color: #15803d;
}

.status-chip[data-state='EXPIRED'] {
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.full-width {
  grid-column: 1 / -1;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label span {
  font-size: 13px;
  color: #54657f;
}

input,
textarea {
  border: 1px solid rgba(148, 163, 184, 0.46);
  border-radius: 14px;
  padding: 12px 14px;
  font: inherit;
  color: #10233d;
  background: #fff;
}

textarea {
  resize: vertical;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

button {
  border: none;
  border-radius: 14px;
  padding: 12px 18px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  background: #1d4ed8;
  color: #fff;
}

button.secondary {
  background: #e2e8f0;
  color: #0f172a;
}

button.danger {
  background: #dc2626;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.qr-card {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  align-items: center;
  padding: 18px;
  margin-top: 18px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.03), rgba(37, 99, 235, 0.08)),
    #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.qr-matrix {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 5px;
  padding: 14px;
  border-radius: 18px;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.qr-matrix span {
  aspect-ratio: 1;
  border-radius: 4px;
  background: #dbe4f0;
}

.qr-matrix span.dark {
  background: #111827;
}

.qr-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.qr-meta strong {
  font-size: 18px;
}

.qr-meta code {
  display: block;
  padding: 12px 14px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
  word-break: break-all;
}

.qr-meta span {
  color: #586b86;
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.stat-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.stat-item span {
  display: block;
  color: #60748d;
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-item strong {
  display: block;
  word-break: break-all;
}

.avatar-row {
  margin-top: 18px;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
}

.avatar-row img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.detail-box {
  margin-top: 18px;
}

.detail-box h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.detail-box p {
  margin: 0;
  color: #5f6f87;
  line-height: 1.6;
}

.detail-box pre {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  background: #0f172a;
  color: #e2e8f0;
  overflow: auto;
  font-size: 13px;
  line-height: 1.6;
}

.log-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-list li {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.log-list li[data-level='warn'] {
  background: #fff7ed;
}

.log-list li[data-level='error'] {
  background: #fef2f2;
}

.log-list time {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1040px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .workbench {
    padding: 24px 16px 40px;
  }

  .form-grid,
  .stats-grid,
  .qr-card {
    grid-template-columns: 1fr;
  }

  .log-list li {
    grid-template-columns: 1fr;
  }
}
</style>

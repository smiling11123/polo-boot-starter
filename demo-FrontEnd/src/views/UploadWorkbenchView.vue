<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type {UploadResult, ActivityLog, RequestOptions, ApiResponse, LoginResponse, ChunkUploadState,  } from "@/interface"

type SaveHandle = any

const STORAGE_KEY = 'polo-upload-workbench-settings'

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

function createUploadTaskState() {
  return {
    status: 'idle',
    uploadId: '',
    totalChunks: 0,
    totalSize: 0,
    uploadedChunks: [] as number[],
    uploadedCount: 0,
    uploadedBytes: 0,
    progress: 0,
    currentChunkIndex: -1,
    currentChunkProgress: 0,
    readyToComplete: false,
    error: '',
    result: null as UploadResult | null,
    shouldPause: false,
    shouldCancel: false,
  }
}

function createDownloadTaskState() {
  return {
    status: 'idle',
    totalSize: 0,
    downloadedBytes: 0,
    progress: 0,
    fileName: '',
    error: '',
    shouldPause: false,
    shouldCancel: false,
  }
}

const persistedSettings = loadPersistedSettings()

const auth = reactive({
  baseUrl: String(persistedSettings.baseUrl ?? defaultBaseUrl),
  username: String(persistedSettings.username ?? 'admin'),
  password: String(persistedSettings.password ?? '123456'),
  token: String(persistedSettings.token ?? ''),
})

const uploadConfig = reactive({
  chunkSizeMb: Number(persistedSettings.uploadChunkSizeMb ?? 2),
  pathPrefix: String(persistedSettings.pathPrefix ?? 'workbench'),
})

const downloadConfig = reactive({
  filepath: '',
  storageType: String(persistedSettings.storageType ?? ''),
  rangeSizeMb: Number(persistedSettings.downloadChunkSizeMb ?? 2),
})

const selectedFile = ref<File | null>(null)
const uploadTask = reactive(createUploadTaskState())
const downloadTask = reactive(createDownloadTaskState())
const logs = ref<ActivityLog[]>([])
const activeUploadXhr = ref<XMLHttpRequest | null>(null)
const activeDownloadController = ref<AbortController | null>(null)
const downloadFileHandle = ref<SaveHandle | null>(null)
const activeDownloadWritable = ref<any | null>(null)

const uploadChunkSizeBytes = computed(() => Math.max(256 * 1024, Math.floor(uploadConfig.chunkSizeMb * 1024 * 1024)))
const downloadChunkSizeBytes = computed(() => Math.max(256 * 1024, Math.floor(downloadConfig.rangeSizeMb * 1024 * 1024)))
const supportsResumableDownload = computed(() => typeof (window as any).showSaveFilePicker === 'function')

const canStartUpload = computed(() => Boolean(selectedFile.value) && uploadTask.status !== 'uploading')
const canPauseUpload = computed(() => uploadTask.status === 'uploading')
const canResumeUpload = computed(() => Boolean(selectedFile.value) && ['paused', 'error'].includes(uploadTask.status))
const canCancelUpload = computed(() => ['uploading', 'paused', 'error'].includes(uploadTask.status))

const canStartDownload = computed(() => Boolean(downloadConfig.filepath) && downloadTask.status !== 'downloading')
const canPauseDownload = computed(() => downloadTask.status === 'downloading')
const canResumeDownload = computed(() => downloadTask.status === 'paused')
const canCancelDownload = computed(() => ['downloading', 'paused', 'error'].includes(downloadTask.status))

function addLog(message: string, level: ActivityLog['level'] = 'info') {
  logs.value.unshift({
    id: Date.now() + Math.floor(Math.random() * 1000),
    time: new Date().toLocaleTimeString(),
    level,
    message,
  })
  if (logs.value.length > 60) {
    logs.value.length = 60
  }
}

function persistSettings() {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      baseUrl: auth.baseUrl,
      username: auth.username,
      password: auth.password,
      token: auth.token,
      uploadChunkSizeMb: uploadConfig.chunkSizeMb,
      pathPrefix: uploadConfig.pathPrefix,
      storageType: downloadConfig.storageType,
      downloadChunkSizeMb: downloadConfig.rangeSizeMb,
    }),
  )
}

watch(
  [
    () => auth.baseUrl,
    () => auth.username,
    () => auth.password,
    () => auth.token,
    () => uploadConfig.chunkSizeMb,
    () => uploadConfig.pathPrefix,
    () => downloadConfig.storageType,
    () => downloadConfig.rangeSizeMb,
  ],
  persistSettings,
)

function normalizeBaseUrl(baseUrl: string) {
  return (baseUrl || defaultBaseUrl).trim().replace(/\/+$/, '')
}

function buildUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizeBaseUrl(auth.baseUrl)}${normalizedPath}`
}

async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers ?? {})
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  if (options.auth !== false && auth.token.trim()) {
    headers.set('Authorization', auth.token.trim())
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

function resetUploadTask(keepResult = false) {
  const previousResult = keepResult ? uploadTask.result : null
  Object.assign(uploadTask, createUploadTaskState())
  uploadTask.result = previousResult
}

function resetDownloadTask(keepSource = true, keepHandle = false, keepFileName = false) {
  const previousPath = keepSource ? downloadConfig.filepath : ''
  const previousStorageType = keepSource ? downloadConfig.storageType : ''
  const previousFileName = keepFileName ? downloadTask.fileName : ''
  Object.assign(downloadTask, createDownloadTaskState())
  downloadConfig.filepath = previousPath
  downloadConfig.storageType = previousStorageType
  downloadTask.fileName = previousFileName
  if (!keepHandle) {
    downloadFileHandle.value = null
  }
}

function onSelectFile(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] ?? null
  resetUploadTask()
  if (selectedFile.value) {
    addLog(`已选择文件：${selectedFile.value.name}`)
  }
}

function sortChunkList(chunks: number[]) {
  return [...new Set(chunks)].sort((left, right) => left - right)
}

function calculateUploadedBytes(chunkIndexes: number[]) {
  const file = selectedFile.value
  if (!file || chunkIndexes.length === 0) {
    return 0
  }
  return sortChunkList(chunkIndexes).reduce((total, chunkIndex) => {
    const start = chunkIndex * uploadChunkSizeBytes.value
    const end = Math.min(file.size, start + uploadChunkSizeBytes.value)
    return total + Math.max(0, end - start)
  }, 0)
}

function applyChunkState(state: ChunkUploadState) {
  const uploadedChunks = sortChunkList(state.uploadedChunks ?? [])
  uploadTask.uploadId = state.uploadId
  uploadTask.totalChunks = state.totalChunks
  uploadTask.totalSize = state.totalSize
  uploadTask.uploadedChunks = uploadedChunks
  uploadTask.uploadedCount = state.uploadedCount ?? uploadedChunks.length
  uploadTask.uploadedBytes = calculateUploadedBytes(uploadedChunks)
  uploadTask.progress = state.totalSize ? Number(((uploadTask.uploadedBytes / state.totalSize) * 100).toFixed(2)) : 0
  uploadTask.readyToComplete = state.readyToComplete
}

function formatBytes(value: number) {
  if (!value) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let current = value
  let unitIndex = 0
  while (current >= 1024 && unitIndex < units.length - 1) {
    current /= 1024
    unitIndex += 1
  }
  return `${current.toFixed(current >= 10 || unitIndex === 0 ? 0 : 2)} ${units[unitIndex]}`
}

function formatJson(value: unknown) {
  return JSON.stringify(value, null, 2)
}

function formatChunkIndexes(chunks: number[]) {
  return chunks.length ? chunks.join(', ') : '-'
}

function extractFileName(filepath: string) {
  if (!filepath) {
    return ''
  }
  const segments = filepath.split('/')
  return decodeURIComponent(segments[segments.length - 1] || filepath)
}

function parseJson<T>(value: string) {
  try {
    return JSON.parse(value) as T
  } catch {
    return null
  }
}

function toErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}

async function login() {
  try {
    const response = await apiRequest<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        username: auth.username,
        password: auth.password,
        device: {
          deviceId: 'upload-workbench',
          deviceType: 'PC',
          deviceName: 'Vue Upload Workbench',
        },
      }),
      headers: {
        'Content-Type': 'application/json',
      },
      auth: false,
    })
    const tokenType = response.tokenPair.tokenType || 'Bearer'
    auth.token = `${tokenType} ${response.tokenPair.accessToken}`
    addLog(`登录成功，当前用户：${response.username}`)
  } catch (error) {
    addLog(`登录失败：${toErrorMessage(error)}`, 'error')
  }
}

function clearToken() {
  auth.token = ''
  addLog('已清空 Bearer Token', 'warn')
}

function findNextChunkIndex() {
  const uploaded = new Set(uploadTask.uploadedChunks)
  for (let chunkIndex = 0; chunkIndex < uploadTask.totalChunks; chunkIndex += 1) {
    if (!uploaded.has(chunkIndex)) {
      return chunkIndex
    }
  }
  return -1
}

async function startUpload() {
  if (!selectedFile.value) {
    addLog('请先选择需要上传的文件', 'warn')
    return
  }
  if (!auth.token.trim()) {
    addLog('分片上传接口需要 Bearer Token，请先登录或手动填写 Token', 'warn')
    return
  }

  resetUploadTask()

  try {
    const state = await apiRequest<ChunkUploadState>('/files/chunk/init', {
      method: 'POST',
      body: new URLSearchParams({
        fieldName: 'file',
        originalFilename: selectedFile.value.name,
        contentType: selectedFile.value.type || 'application/octet-stream',
        totalSize: String(selectedFile.value.size),
        totalChunks: String(Math.max(1, Math.ceil(selectedFile.value.size / uploadChunkSizeBytes.value))),
      }),
    })
    applyChunkState(state)
    uploadTask.status = 'uploading'
    addLog(`创建上传会话成功，uploadId=${state.uploadId}`)
    await runUploadLoop()
  } catch (error) {
    uploadTask.status = 'error'
    uploadTask.error = toErrorMessage(error)
    addLog(`初始化分片上传失败：${uploadTask.error}`, 'error')
  }
}

async function syncUploadStatus() {
  if (!uploadTask.uploadId) {
    return
  }
  const state = await apiRequest<ChunkUploadState>(`/files/chunk/status?uploadId=${encodeURIComponent(uploadTask.uploadId)}`)
  applyChunkState(state)
}

async function resumeUpload() {
  if (!selectedFile.value) {
    addLog('继续上传前请重新选择同一个文件', 'warn')
    return
  }
  if (!uploadTask.uploadId) {
    await startUpload()
    return
  }

  try {
    uploadTask.shouldPause = false
    uploadTask.shouldCancel = false
    uploadTask.error = ''
    await syncUploadStatus()
    if (uploadTask.readyToComplete || uploadTask.uploadedCount >= uploadTask.totalChunks) {
      uploadTask.status = 'uploading'
      await completeUpload()
      return
    }
    uploadTask.status = 'uploading'
    addLog('已恢复上传，会自动跳过服务端已存在的分片')
    await runUploadLoop()
  } catch (error) {
    uploadTask.status = 'error'
    uploadTask.error = toErrorMessage(error)
    addLog(`恢复上传失败：${uploadTask.error}`, 'error')
  }
}

function pauseUpload() {
  if (uploadTask.status !== 'uploading') {
    return
  }
  uploadTask.shouldPause = true
  activeUploadXhr.value?.abort()
}

async function cancelUpload() {
  uploadTask.shouldCancel = true
  uploadTask.shouldPause = false
  activeUploadXhr.value?.abort()

  if (!uploadTask.uploadId) {
    resetUploadTask()
    return
  }

  try {
    await apiRequest<boolean>(`/files/chunk/abort?uploadId=${encodeURIComponent(uploadTask.uploadId)}`, {
      method: 'DELETE',
    })
    addLog('已取消当前上传会话', 'warn')
  } catch (error) {
    addLog(`取消上传会话失败：${toErrorMessage(error)}`, 'error')
  } finally {
    resetUploadTask()
  }
}

async function runUploadLoop() {
  try {
    while (!uploadTask.shouldPause && !uploadTask.shouldCancel) {
      const nextChunkIndex = findNextChunkIndex()
      if (nextChunkIndex < 0) {
        break
      }
      const state = await uploadSingleChunk(nextChunkIndex)
      applyChunkState(state)
      addLog(`分片 ${nextChunkIndex + 1}/${uploadTask.totalChunks} 上传完成`)
    }

    if (uploadTask.shouldCancel) {
      return
    }
    if (uploadTask.shouldPause) {
      uploadTask.status = 'paused'
      addLog('上传已暂停')
      return
    }

    await completeUpload()
  } catch (error: any) {
    if (error?.code === 'UPLOAD_ABORTED') {
      if (uploadTask.shouldPause) {
        uploadTask.status = 'paused'
        addLog('上传已暂停')
      }
      return
    }
    uploadTask.status = 'error'
    uploadTask.error = toErrorMessage(error)
    addLog(`上传失败：${uploadTask.error}`, 'error')
  } finally {
    activeUploadXhr.value = null
    if (uploadTask.status !== 'paused') {
      uploadTask.shouldPause = false
    }
  }
}

function uploadSingleChunk(chunkIndex: number) {
  const file = selectedFile.value
  if (!file || !uploadTask.uploadId) {
    return Promise.reject(new Error('上传状态无效，请重新开始上传'))
  }

  const start = chunkIndex * uploadChunkSizeBytes.value
  const end = Math.min(file.size, start + uploadChunkSizeBytes.value)
  const blob = file.slice(start, end)
  const totalUploadedBeforeCurrentChunk = uploadTask.uploadedBytes

  return new Promise<ChunkUploadState>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    activeUploadXhr.value = xhr

    xhr.open('POST', buildUrl('/files/chunk/part'))
    if (auth.token.trim()) {
      xhr.setRequestHeader('Authorization', auth.token.trim())
    }
    xhr.responseType = 'json'

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) {
        return
      }
      uploadTask.currentChunkIndex = chunkIndex
      uploadTask.currentChunkProgress = Number(((event.loaded / event.total) * 100).toFixed(2))
      const uploadedBytes = totalUploadedBeforeCurrentChunk + event.loaded
      uploadTask.progress = uploadTask.totalSize ? Number(((uploadedBytes / uploadTask.totalSize) * 100).toFixed(2)) : 0
    }

    xhr.onerror = () => {
      activeUploadXhr.value = null
      reject(new Error('上传分片失败，请检查网络连接'))
    }

    xhr.onabort = () => {
      activeUploadXhr.value = null
      reject({
        code: 'UPLOAD_ABORTED',
        message: '上传已中断',
      })
    }

    xhr.onload = () => {
      activeUploadXhr.value = null
      const payload =
        (xhr.response as ApiResponse<ChunkUploadState> | null) ??
        parseJson<ApiResponse<ChunkUploadState>>(xhr.responseText)
      if (xhr.status >= 200 && xhr.status < 300 && payload?.code === 200) {
        resolve(payload.data)
        return
      }
      reject(new Error(payload?.msg || `上传分片失败 (HTTP ${xhr.status})`))
    }

    const formData = new FormData()
    formData.append('uploadId', uploadTask.uploadId)
    formData.append('chunkIndex', String(chunkIndex))
    formData.append('file', blob, file.name)
    xhr.send(formData)
  })
}

async function completeUpload() {
  if (!uploadTask.uploadId) {
    return
  }

  const params = new URLSearchParams({
    uploadId: uploadTask.uploadId,
  })
  if (uploadConfig.pathPrefix.trim()) {
    params.set('pathPrefix', uploadConfig.pathPrefix.trim())
  }

  const result = await apiRequest<UploadResult>('/files/chunk/complete', {
    method: 'POST',
    body: params,
  })

  uploadTask.result = result
  uploadTask.status = 'completed'
  uploadTask.progress = 100
  uploadTask.currentChunkProgress = 100
  if (result.filepath) {
    downloadConfig.filepath = result.filepath
  }
  if (result.storageType) {
    downloadConfig.storageType = result.storageType
  }
  if (result.originalFilename || result.filename) {
    downloadTask.fileName = result.originalFilename || result.filename || ''
  }
  addLog(`上传完成，文件路径：${result.filepath}`)
}

function buildDownloadUrl() {
  const url = new URL(buildUrl('/files/download'))
  url.searchParams.set('filepath', downloadConfig.filepath)
  if (downloadConfig.storageType.trim()) {
    url.searchParams.set('storageType', downloadConfig.storageType.trim())
  }
  return url.toString()
}

async function startDownload() {
  if (!downloadConfig.filepath.trim()) {
    addLog('请先填写需要下载的 filepath', 'warn')
    return
  }
  if (!supportsResumableDownload.value) {
    addLog('当前浏览器不支持可暂停/继续下载，请使用 Chrome 或 Edge', 'error')
    return
  }

  resetDownloadTask(true, false, true)

  try {
    downloadFileHandle.value = null
    const suggestedName = downloadTask.fileName || extractFileName(downloadConfig.filepath) || 'download.bin'
    downloadFileHandle.value = await (window as any).showSaveFilePicker({
      suggestedName,
    })
    downloadTask.status = 'downloading'
    addLog(`开始下载：${downloadConfig.filepath}`)
    await runDownloadLoop(false)
  } catch (error: any) {
    if (error?.name === 'AbortError') {
      addLog('已取消选择下载保存位置', 'warn')
      return
    }
    downloadTask.status = 'error'
    downloadTask.error = toErrorMessage(error)
    addLog(`开始下载失败：${downloadTask.error}`, 'error')
  }
}

function pauseDownload() {
  if (downloadTask.status !== 'downloading') {
    return
  }
  downloadTask.shouldPause = true
  activeDownloadController.value?.abort()
}

async function resumeDownload() {
  if (!downloadConfig.filepath.trim()) {
    addLog('请先填写需要下载的 filepath', 'warn')
    return
  }
  if (!supportsResumableDownload.value) {
    addLog('当前浏览器不支持可暂停/继续下载，请使用 Chrome 或 Edge', 'error')
    return
  }
  if (!downloadFileHandle.value) {
    addLog('未找到可恢复的下载目标，请重新开始下载', 'warn')
    return
  }

  downloadTask.error = ''
  downloadTask.shouldPause = false
  downloadTask.shouldCancel = false
  downloadTask.status = 'downloading'
  addLog('继续下载中')
  await runDownloadLoop(true)
}

async function cancelDownload() {
  downloadTask.shouldCancel = true
  downloadTask.shouldPause = false
  activeDownloadController.value?.abort()

  if (!activeDownloadController.value) {
    await discardPartialDownload()
    resetDownloadTask(true, false, true)
    addLog('下载已取消', 'warn')
  }
}

async function runDownloadLoop(keepExistingData: boolean) {
  let writable: any = null
  let aborted = false
  try {
    if (!downloadFileHandle.value) {
      throw new Error('缺少下载文件句柄，请重新开始下载')
    }
    let position = keepExistingData ? downloadTask.downloadedBytes : 0

    while (!downloadTask.shouldPause && !downloadTask.shouldCancel) {
      if (downloadTask.totalSize > 0 && position >= downloadTask.totalSize) {
        break
      }

      const start = position
      const end =
        downloadTask.totalSize > 0
          ? Math.min(start + downloadChunkSizeBytes.value - 1, downloadTask.totalSize - 1)
          : start + downloadChunkSizeBytes.value - 1

      const controller = new AbortController()
      activeDownloadController.value = controller

      const response = await fetch(buildDownloadUrl(), {
        method: 'GET',
        headers: {
          Range: `bytes=${start}-${end}`,
        },
        signal: controller.signal,
      })

      if (!(response.status === 206 || (response.ok && start === 0))) {
        throw new Error(`下载失败 (HTTP ${response.status})`)
      }

      if (!downloadTask.totalSize) {
        downloadTask.totalSize = resolveTotalSize(response)
      }

      const fileName = resolveFileName(response)
      if (fileName) {
        downloadTask.fileName = fileName
      }

      if (!writable) {
        writable = await downloadFileHandle.value.createWritable({ keepExistingData })
        activeDownloadWritable.value = writable
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('当前浏览器不支持流式读取下载响应')
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          break
        }
        await writable.write({
          type: 'write',
          position,
          data: value,
        })
        position += value.byteLength
        downloadTask.downloadedBytes = position
        if (downloadTask.totalSize > 0) {
          downloadTask.progress = Number(((position / downloadTask.totalSize) * 100).toFixed(2))
        }
      }

      if (downloadTask.totalSize > 0 && position >= downloadTask.totalSize) {
        break
      }
    }

    if (writable) {
      await writable.close()
      writable = null
      activeDownloadWritable.value = null
    }

    if (downloadTask.shouldCancel) {
      await discardPartialDownload()
      resetDownloadTask(true, false, true)
      addLog('下载已取消', 'warn')
      return
    }

    if (downloadTask.shouldPause) {
      downloadTask.status = 'paused'
      addLog('下载已暂停')
      return
    }

    downloadTask.status = 'completed'
    downloadTask.progress = 100
    addLog('下载完成')
    } catch (error: any) {
      if (error?.name === 'AbortError') {
        if (downloadTask.shouldCancel) {
          if (writable) {
            try {
              if (typeof writable.abort === 'function') {
                await writable.abort()
                aborted = true
              } else {
                await writable.close()
              }
            } catch {
              // ignore
            }
            writable = null
            activeDownloadWritable.value = null
          }
          if (!aborted) {
            await discardPartialDownload()
          }
          resetDownloadTask(true, false, true)
          addLog('下载已取消', 'warn')
          return
        }
        if (downloadTask.shouldPause) {
        if (writable) {
          try {
            await writable.close()
            } catch {
              // ignore
            }
          }
          writable = null
          activeDownloadWritable.value = null
          downloadTask.status = 'paused'
          addLog('下载已暂停')
          return
        }
      }

    downloadTask.status = 'error'
    downloadTask.error = toErrorMessage(error)
    addLog(`下载失败：${downloadTask.error}`, 'error')
  } finally {
    activeDownloadController.value = null
    if (writable) {
      try {
        if (downloadTask.shouldCancel && typeof writable.abort === 'function') {
          await writable.abort()
        } else {
          await writable.close()
        }
      } catch {
        // ignore
      }
    }
    activeDownloadWritable.value = null
    if (downloadTask.status !== 'paused') {
      downloadTask.shouldPause = false
    }
  }
}

async function discardPartialDownload() {
  if (activeDownloadWritable.value && typeof activeDownloadWritable.value.abort === 'function') {
    try {
      await activeDownloadWritable.value.abort()
      return
    } catch {
      // ignore and fallback
    } finally {
      activeDownloadWritable.value = null
    }
  }
  if (!downloadFileHandle.value) {
    return
  }
  const remover = (downloadFileHandle.value as any).remove
  if (typeof remover === 'function') {
    try {
      await remover.call(downloadFileHandle.value)
      return
    } catch {
      // ignore and fallback
    }
  }
}

function useUploadResultForDownload() {
  if (!uploadTask.result) {
    return
  }
  downloadConfig.filepath = uploadTask.result.filepath
  downloadConfig.storageType = uploadTask.result.storageType || ''
  downloadTask.fileName = uploadTask.result.originalFilename || uploadTask.result.filename || ''
  addLog('已将最新上传结果填充到下载区域')
}

function resolveTotalSize(response: Response) {
  const contentRange = response.headers.get('Content-Range')
  if (contentRange) {
    const match = contentRange.match(/bytes\s+\d+-\d+\/(\d+)/i)
    if (match) {
      return Number(match[1])
    }
  }
  const contentLength = response.headers.get('Content-Length')
  return contentLength ? Number(contentLength) : 0
}

function resolveFileName(response: Response) {
  const disposition = response.headers.get('Content-Disposition')
  if (!disposition) {
    return ''
  }
  const encodedMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] ?? ''
}

onBeforeUnmount(() => {
  activeUploadXhr.value?.abort()
  activeDownloadController.value?.abort()
})
</script>

<template>
  <div class="workbench">
    <header class="hero">
      <h1>文件上传与下载工作台</h1>
      <p>对接 demo 的 UploadController，支持分片上传、暂停/继续/取消上传，以及基于 Range 的暂停/继续/取消下载。</p>
    </header>

    <section class="card">
      <div class="section-title">
        <h2>连接与认证</h2>
      </div>
      <div class="grid two-columns">
        <!-- <label>
          <span>后端地址</span>
          <input v-model.trim="auth.baseUrl" type="text" placeholder="http://localhost:8080" />
        </label> -->
        <label>
          <span>登录账号</span>
          <input v-model.trim="auth.username" type="text" placeholder="admin" />
        </label>
        <label>
          <span>登录密码</span>
          <input v-model="auth.password" type="password" placeholder="123456" />
        </label>
        <label>
          <span>Bearer Token</span>
          <textarea v-model.trim="auth.token" rows="3" placeholder="先登录，或直接粘贴 Bearer Token"></textarea>
        </label>
      </div>
      <div class="actions">
        <button type="button" @click="login">一键登录获取 Token</button>
        <button type="button" class="secondary" @click="clearToken">清空 Token</button>
      </div>
    </section>

    <section class="card">
      <div class="section-title">
        <h2>分片上传</h2>
        <span class="status" :data-state="uploadTask.status">{{ uploadTask.status }}</span>
      </div>
      <div class="grid two-columns">
        <label class="full-width">
          <span>选择文件</span>
          <input type="file" @change="onSelectFile" />
        </label>
        <label>
          <span>分片大小（MB）</span>
          <input v-model.number="uploadConfig.chunkSizeMb" type="number" min="1" step="1" />
        </label>
        <label>
          <span>完成上传后的 pathPrefix</span>
          <input v-model.trim="uploadConfig.pathPrefix" type="text" placeholder="workbench" />
        </label>
      </div>
      <div class="actions">
        <button type="button" :disabled="!canStartUpload" @click="startUpload">开始上传</button>
        <button type="button" class="secondary" :disabled="!canPauseUpload" @click="pauseUpload">暂停上传</button>
        <button type="button" class="secondary" :disabled="!canResumeUpload" @click="resumeUpload">继续上传</button>
        <button type="button" class="danger" :disabled="!canCancelUpload" @click="cancelUpload">取消上传</button>
      </div>

      <div class="grid stats-grid">
        <div class="stat-item">
          <span>文件名</span>
          <strong>{{ selectedFile?.name || '-' }}</strong>
        </div>
        <div class="stat-item">
          <span>uploadId</span>
          <strong>{{ uploadTask.uploadId || '-' }}</strong>
        </div>
        <div class="stat-item">
          <span>已上传分片</span>
          <strong>{{ uploadTask.uploadedCount }}/{{ uploadTask.totalChunks || 0 }}</strong>
        </div>
        <div class="stat-item">
          <span>已上传大小</span>
          <strong>{{ formatBytes(uploadTask.uploadedBytes) }} / {{ formatBytes(uploadTask.totalSize) }}</strong>
        </div>
      </div>

      <div class="progress-block">
        <div class="progress-line">
          <span>总进度</span>
          <strong>{{ uploadTask.progress.toFixed(2) }}%</strong>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${uploadTask.progress}%` }"></div>
        </div>
        <div class="progress-line small">
          <span>当前分片：{{ uploadTask.currentChunkIndex >= 0 ? uploadTask.currentChunkIndex + 1 : '-' }}</span>
          <strong>{{ uploadTask.currentChunkProgress.toFixed(2) }}%</strong>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-item">
          <h3>已上传分片索引</h3>
          <p>{{ formatChunkIndexes(uploadTask.uploadedChunks) }}</p>
        </div>
        <div class="detail-item">
          <h3>错误信息</h3>
          <p>{{ uploadTask.error || '-' }}</p>
        </div>
      </div>

      <div v-if="uploadTask.result" class="result-panel">
        <div class="section-title compact">
          <h3>上传结果</h3>
          <button type="button" class="secondary" @click="useUploadResultForDownload">填充到下载区域</button>
        </div>
        <pre>{{ formatJson(uploadTask.result) }}</pre>
      </div>
    </section>

    <section class="card">
      <div class="section-title">
        <h2>分片下载</h2>
        <span class="status" :data-state="downloadTask.status">{{ downloadTask.status }}</span>
      </div>
      <div class="grid two-columns">
        <label class="full-width">
          <span>filepath</span>
          <input v-model.trim="downloadConfig.filepath" type="text" placeholder="例如：manual/demo.png" />
        </label>
        <label>
          <span>storageType</span>
          <input v-model.trim="downloadConfig.storageType" type="text" placeholder="minio / oss / cos / local" />
        </label>
        <label>
          <span>下载分片大小（MB）</span>
          <input v-model.number="downloadConfig.rangeSizeMb" type="number" min="1" step="1" />
        </label>
      </div>
      <div class="actions">
        <button type="button" :disabled="!canStartDownload" @click="startDownload">开始下载</button>
        <button type="button" class="secondary" :disabled="!canPauseDownload" @click="pauseDownload">暂停下载</button>
        <button type="button" class="secondary" :disabled="!canResumeDownload" @click="resumeDownload">继续下载</button>
        <button type="button" class="danger" :disabled="!canCancelDownload" @click="cancelDownload">取消下载</button>
      </div>

      <p class="hint" :class="{ warning: !supportsResumableDownload }">
        <template v-if="supportsResumableDownload">
          下载恢复依赖 File System Access API，推荐使用 Chrome 或 Edge。
        </template>
        <template v-else>
          当前浏览器不支持可暂停/继续下载，请使用 Chrome 或 Edge。
        </template>
      </p>

      <div class="grid stats-grid">
        <div class="stat-item">
          <span>保存文件名</span>
          <strong>{{ downloadTask.fileName || extractFileName(downloadConfig.filepath) || '-' }}</strong>
        </div>
        <div class="stat-item">
          <span>已下载大小</span>
          <strong>{{ formatBytes(downloadTask.downloadedBytes) }}</strong>
        </div>
        <div class="stat-item">
          <span>总大小</span>
          <strong>{{ formatBytes(downloadTask.totalSize) }}</strong>
        </div>
        <div class="stat-item">
          <span>filepath</span>
          <strong>{{ downloadConfig.filepath || '-' }}</strong>
        </div>
      </div>

      <div class="progress-block">
        <div class="progress-line">
          <span>下载进度</span>
          <strong>{{ downloadTask.progress.toFixed(2) }}%</strong>
        </div>
        <div class="progress-track">
          <div class="progress-fill alt" :style="{ width: `${downloadTask.progress}%` }"></div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-item">
          <h3>错误信息</h3>
          <p>{{ downloadTask.error || '-' }}</p>
        </div>
        <div class="detail-item">
          <h3>下载说明</h3>
          <p>开始下载时会弹出系统保存对话框；暂停后继续会写回同一个目标文件。</p>
        </div>
      </div>
    </section>

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

<style scoped src="./upload-workbench.css"></style>

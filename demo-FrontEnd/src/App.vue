<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand">
        <strong>polo-boot demo-FrontEnd</strong>
        <span>测试工作台</span>
      </div>
      <nav class="nav">
        <RouterLink to="/upload" class="nav-link">文件上传下载</RouterLink>
        <RouterLink to="/qrcode-login" class="nav-link">二维码登录</RouterLink>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(53, 124, 255, 0.14), transparent 28%),
    linear-gradient(180deg, #f7f9fc 0%, #eef3fb 100%);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #10233d;
}

.brand span {
  color: #586b86;
  font-size: 13px;
}

.nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.nav-link {
  padding: 10px 16px;
  border-radius: 999px;
  text-decoration: none;
  color: #1e3658;
  background: rgba(29, 78, 216, 0.08);
  transition: 0.2s ease;
}

.nav-link.router-link-active {
  background: #1d4ed8;
  color: #fff;
}

.main {
  padding: 0 0 32px;
}

@media (max-width: 720px) {
  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

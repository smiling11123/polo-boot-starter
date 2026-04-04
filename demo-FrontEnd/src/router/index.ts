import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/upload',
    },
    {
      path: '/upload',
      name: 'upload-workbench',
      component: () => import('@/views/UploadWorkbenchView.vue'),
    },
    {
      path: '/qrcode-login',
      name: 'qrcode-login-workbench',
      component: () => import('@/views/QrCodeLoginWorkbenchView.vue'),
    },
  ],
})

export default router

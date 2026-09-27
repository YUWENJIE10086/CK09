<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const loginForm = ref({
  username: '',
  password: '',
  code: '',
  uuid: '',
})

// 记住密码
const rememberMe = ref(false)

// 加载状态
const loading = ref(false)

// 验证码图片base64
const captchaImg = ref('')

/** 生成随机验证码字符串 */
function generateCaptchaText(len = 4): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let result = ''
  for (let i = 0; i < len; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

/** 当前验证码答案 */
let captchaAnswer = ''

/** 用Canvas绘制验证码 */
function generateCaptcha(): void {
  const canvas = document.createElement('canvas')
  canvas.width = 120
  canvas.height = 40
  const ctx = canvas.getContext('2d')!

  // 背景
  ctx.fillStyle = '#e8eaf0'
  ctx.fillRect(0, 0, 120, 40)

  // 干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(${Math.random() * 180}, ${Math.random() * 180}, ${Math.random() * 180}, 0.5)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * 120, Math.random() * 40)
    ctx.lineTo(Math.random() * 120, Math.random() * 40)
    ctx.stroke()
  }

  // 干扰点
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = `rgba(${Math.random() * 200}, ${Math.random() * 200}, ${Math.random() * 200}, 0.6)`
    ctx.beginPath()
    ctx.arc(Math.random() * 120, Math.random() * 40, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  // 绘制验证码文字
  captchaAnswer = generateCaptchaText(4)
  const colors = ['#0D5D46', '#2ECC71', '#E74C3C', '#F39C12']
  for (let i = 0; i < captchaAnswer.length; i++) {
    ctx.font = `bold ${20 + Math.random() * 6}px Arial`
    ctx.fillStyle = colors[i % colors.length]
    ctx.save()
    ctx.translate(22 + i * 24, 28)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(captchaAnswer[i], 0, 0)
    ctx.restore()
  }

  captchaImg.value = canvas.toDataURL('base64')
  loginForm.value.uuid = 'captcha-' + Date.now()
}

/** 登录处理 */
async function handleLogin(): Promise<void> {
  // 校验
  if (!loginForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (!loginForm.value.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  // 验证码校验（忽略大小写）
  if (loginForm.value.code.toLowerCase() !== captchaAnswer.toLowerCase()) {
    ElMessage.error('验证码错误')
    generateCaptcha()
    return
  }

  loading.value = true
  try {
    await userStore.login({
      username: loginForm.value.username,
      password: loginForm.value.password,
      code: loginForm.value.code,
      uuid: loginForm.value.uuid,
    })
    // 记住密码
    if (rememberMe.value) {
      localStorage.setItem('remembered_username', loginForm.value.username)
      localStorage.setItem('remembered_password', loginForm.value.password)
    } else {
      localStorage.removeItem('remembered_username')
      localStorage.removeItem('remembered_password')
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败')
    generateCaptcha()
  } finally {
    loading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  generateCaptcha()
  // 恢复记住的账号密码
  const savedUsername = localStorage.getItem('remembered_username')
  const savedPassword = localStorage.getItem('remembered_password')
  if (savedUsername) {
    loginForm.value.username = savedUsername
    rememberMe.value = true
  }
  if (savedPassword) {
    loginForm.value.password = savedPassword
  }
})
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 顶部渐变色条装饰 -->
      <div class="login-card-accent"></div>

      <!-- 顶部标题区 -->
      <div class="login-header">
        <div class="login-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="6" y="14" width="36" height="28" rx="3" stroke="#ffffff" stroke-width="2.5" fill="none"/>
            <path d="M14 14V10a10 10 0 0120 0v4" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" fill="none"/>
            <circle cx="24" cy="27" r="4" fill="#ffffff"/>
            <path d="M24 31v4" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round"/>
            <path d="M6 22h36" stroke="#ffffff" stroke-width="1.5" opacity="0.3"/>
          </svg>
        </div>
        <h1 class="login-title">烤房管理平台</h1>
        <p class="login-subtitle">Barn Management Platform</p>
      </div>

      <!-- 表单区 -->
      <el-form class="login-form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <div class="captcha-row">
            <el-input
              v-model="loginForm.code"
              placeholder="请输入验证码"
              size="large"
              :prefix-icon="Key"
              clearable
              class="captcha-input"
            />
            <div class="captcha-img" @click="generateCaptcha">
              <img :src="captchaImg" alt="验证码" />
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="rememberMe" label="记住密码" />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部版权 -->
      <div class="login-footer">
        © 2026 烤房管理平台
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 全屏毛玻璃登录页面 ==================== */
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0A3D38 0%, #0D5D46 40%, #1A6B4F 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆 */
.login-bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: #ffffff;
}

.circle-1 {
  width: clamp(400px, 50vw, 700px);
  height: clamp(400px, 50vw, 700px);
  top: -150px;
  right: -120px;
  animation: float1 8s ease-in-out infinite;
}

.circle-2 {
  width: clamp(250px, 30vw, 420px);
  height: clamp(250px, 30vw, 420px);
  bottom: -100px;
  left: -80px;
  animation: float2 10s ease-in-out infinite;
}

.circle-3 {
  width: clamp(180px, 22vw, 280px);
  height: clamp(180px, 22vw, 280px);
  top: 50%;
  left: 10%;
  animation: float3 12s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-30px, 20px); }
}
@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -30px); }
}
@keyframes float3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(15px, 15px); }
}

/* 全屏毛玻璃登录卡片 */
.login-card {
  width: clamp(340px, 42vw, 460px);
  padding: clamp(32px, 4vw, 48px) clamp(28px, 3.5vw, 42px) clamp(24px, 3vw, 36px);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.88) 0%, rgba(255, 255, 255, 0.78) 100%);
  backdrop-filter: blur(24px) saturate(1.5);
  -webkit-backdrop-filter: blur(24px) saturate(1.5);
  border-radius: 8px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.15),
    0 24px 80px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  z-index: 1;
  animation: cardEnter 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

/* 顶部渐变色条 */
.login-card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #0A4D3E, #0D5D46, #1A6B4F, #2E8B6A);
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 标题区 */
.login-header {
  text-align: center;
  margin-bottom: clamp(24px, 3vw, 38px);
}

.login-icon {
  width: clamp(52px, 6.5vw, 72px);
  height: clamp(52px, 6.5vw, 72px);
  margin: 0 auto clamp(14px, 1.8vw, 20px);
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 6px 20px rgba(10, 77, 62, 0.4),
    0 2px 4px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.login-icon svg {
  width: clamp(26px, 3.2vw, 36px);
  height: clamp(26px, 3.2vw, 36px);
}

.login-title {
  font-size: clamp(18px, 2.2vw, 26px);
  font-weight: 700;
  color: #0A4D3E;
  margin: 0 0 6px;
  letter-spacing: clamp(2px, 0.3vw, 4px);
}

.login-subtitle {
  font-size: clamp(10px, 1.2vw, 13px);
  color: #909399;
  margin: 0;
  letter-spacing: 0.5px;
}

/* 表单 */
.login-form {
  margin-bottom: clamp(12px, 1.5vw, 18px);
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow:
    0 0 0 1px rgba(10, 77, 62, 0.15) inset,
    0 2px 4px rgba(10, 77, 62, 0.05);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    0 0 0 1px #0A4D3E inset,
    0 4px 8px rgba(10, 77, 62, 0.08);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #0A4D3E inset,
    0 0 0 4px rgba(10, 77, 62, 0.15) !important,
    0 4px 8px rgba(10, 77, 62, 0.1);
  background: rgba(255, 255, 255, 0.9);
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: clamp(10px, 1.2vw, 14px);
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: clamp(100px, 12vw, 130px);
  height: clamp(36px, 4.5vw, 44px);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(10, 77, 62, 0.15);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 4px rgba(10, 77, 62, 0.05);
}

.captcha-img:hover {
  border-color: #0A4D3E;
  box-shadow: 0 0 0 3px rgba(10, 77, 62, 0.12);
  transform: scale(1.02);
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 记住密码 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* 登录按钮 - 轻拟态 */
.login-btn {
  width: 100%;
  height: clamp(42px, 5.2vw, 50px);
  border-radius: 8px;
  font-size: clamp(15px, 1.8vw, 18px);
  font-weight: 600;
  letter-spacing: clamp(3px, 0.4vw, 5px);
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.35),
    0 8px 24px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.login-btn:hover {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  box-shadow:
    0 6px 20px rgba(10, 77, 62, 0.4),
    0 12px 32px rgba(10, 77, 62, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow:
    0 2px 6px rgba(10, 77, 62, 0.3),
    inset 0 1px 3px rgba(0, 0, 0, 0.15);
}

/* 底部版权 */
.login-footer {
  text-align: center;
  font-size: clamp(11px, 1.3vw, 13px);
  color: rgba(255, 255, 255, 0.6);
  padding-top: clamp(10px, 1.2vw, 14px);
  border-top: 1px solid rgba(10, 77, 62, 0.15);
}

/* 响应式 */
@media (max-width: 480px) {
  .login-card {
    width: 94vw;
    padding: 28px 18px 20px;
  }

  .login-title {
    font-size: 17px;
    letter-spacing: 2px;
  }
}
</style>

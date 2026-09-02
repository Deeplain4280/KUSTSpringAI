<template>
  <div class="login-container">
    <div class="glass-panel">
      <h3 class="title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        <span>邮箱登录</span>
      </h3>

      <el-form :model="account" :rules="rules" ref="formRef" class="login-form">
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="account.email"
            placeholder="请输入邮箱地址"
            clearable
            @input="validateEmail"
          />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-input-wrapper">
            <el-input
              v-model="account.code"
              placeholder="请输入6位验证码"
              maxlength="6"
              clearable
            />
            <el-button
              type="primary"
              @click="sendCode"
              :disabled="isSending"
              class="send-code-btn"
              :class="{ 'counting': isSending }"
            >
              {{ isSending ? `(${countdown}s)` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="CodeLogin"
            :loading="isLoading"
            :disabled="!canLogin"
            class="login-btn"
            style="width: 100%"
          >
            立即登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useRouter } from 'vue-router'
import axios from 'axios'


const router = useRouter()
const formRef = ref<FormInstance>()
const isLoading = ref(false)

// 定义响应式数据
const account = reactive({
  email: "",
  code: "",
})

const isSending = ref(false)
const countdown = ref(60)
let timer: number | null = null

// 表单验证规则
const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ] as FormItemRule[],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码必须为6位', trigger: 'blur' }
  ] as FormItemRule[]
}

// 计算属性：是否可以登录
const canLogin = computed(() => {
  return account.email && account.code && account.code.length === 6
})

// 验证邮箱格式
const validateEmail = () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(account.email)
}

// 发送验证码
const sendCode = async () => {
  if (!account.email) {
    ElMessage.warning('请输入邮箱地址')
    return
  }

  if (!validateEmail()) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  // 倒计时逻辑
  isSending.value = true
  countdown.value = 60

  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (timer !== null) {
        window.clearInterval(timer)
        timer = null
      }
      isSending.value = false
    }
  }, 1000)

  try {
    const response = await axios({
      url: "http://127.0.0.1:8080/api/auth/send-code",
      headers: { "Content-Type": "application/json" },
      method: "POST",
      data: { identity: account.email },
    })
    console.log(response)
    if (response.data.code === 200) {
      ElMessage.success(response.data.data)
    } else {
      ElMessage.warning(response.data.data)
    }
  } catch (error) {
    ElMessage.error("服务器错误！请稍候重试！")
  }
}

// 验证码登录
const CodeLogin = async () => {
  if (!formRef.value) return

  // 表单验证
  await formRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true

      try {
        const response = await axios({
          url: "http://127.0.0.1:8080/api/auth/verify",
          headers: { "Content-Type": "application/json" },
          method: "POST",
          data: account,
        })
        console.log(response)
        if (response.data.code === 200) {
          ElMessage.success(response.data.data)
          // 对对象中邮箱做序列化
          localStorage.setItem("userEmail", account.email);
          setTimeout(() => {
            router.push("/index")
          }, 1000)
        } else {
          ElMessage.warning(response.data.message)
        }
      } catch (error) {
        ElMessage.error("服务器错误！请稍候重试！")
        console.error('登录失败:', error)
      } finally {
        isLoading.value = false
      }
    }
  })
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style lang="scss" scoped>
/* ============ Retro-Futurism · 登录终端 ============ */
$font-mono: 'JetBrains Mono', 'Share Tech Mono', ui-monospace, SFMono-Regular, Menlo, Consolas,
  'Microsoft YaHei', 'PingFang SC', 'Noto Sans SC', sans-serif;

.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  position: relative;
  overflow: hidden;

  /* 背景：深空渐变 + 复古太阳辉光 */
  background:
    radial-gradient(1400px 560px at 50% 122%, rgba(255, 45, 146, 0.28) 0%, rgba(255, 45, 146, 0.09) 40%, transparent 62%),
    radial-gradient(1000px 440px at 50% 124%, rgba(255, 200, 60, 0.18) 0%, transparent 55%),
    linear-gradient(180deg, #05060f 0%, #0a0d1e 55%, #170b28 100%);
  font-family: $font-mono;

  /* 底部透视霓虹网格 */
  &::before {
    content: '';
    position: absolute;
    left: -12%;
    right: -12%;
    bottom: -3%;
    height: 46%;
    background-image:
      linear-gradient(rgba(0, 212, 255, 0.20) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 212, 255, 0.20) 1px, transparent 1px);
    background-size: 44px 44px;
    transform: perspective(460px) rotateX(58deg);
    transform-origin: bottom center;
    mask-image: linear-gradient(to top, black 10%, transparent 82%);
    -webkit-mask-image: linear-gradient(to top, black 10%, transparent 82%);
    pointer-events: none;
    z-index: 0;
  }

  /* CRT 扫描线 */
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    z-index: 5;
    background: repeating-linear-gradient(to bottom, rgba(255, 255, 255, 0.028) 0 1px, transparent 1px 3px);
  }

  .glass-panel {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 450px;
    padding: 40px;
    background: rgba(9, 11, 24, 0.76);
    border: 1px solid rgba(0, 212, 255, 0.38);
    border-radius: 6px;
    box-shadow:
      0 0 0 1px rgba(255, 45, 146, 0.10),
      0 0 46px rgba(0, 212, 255, 0.20),
      inset 0 0 70px rgba(0, 212, 255, 0.045);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);

    /* HUD 边角括号 */
    &::before,
    &::after {
      content: '';
      position: absolute;
      width: 16px;
      height: 16px;
      pointer-events: none;
    }
    &::before {
      top: 7px;
      left: 7px;
      border-top: 2px solid #00d4ff;
      border-left: 2px solid #00d4ff;
      opacity: 0.9;
    }
    &::after {
      bottom: 7px;
      right: 7px;
      border-bottom: 2px solid #ff2d92;
      border-right: 2px solid #ff2d92;
      opacity: 0.9;
    }

    .title {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      color: #e6f6ff;
      text-align: center;
      margin-bottom: 30px;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: 0.10em;
      text-shadow: 0 0 16px rgba(0, 212, 255, 0.4);

      svg {
        color: #00d4ff;
        flex-shrink: 0;
      }
    }

    .login-form {
      :deep(.el-form-item) {
        margin-bottom: 25px;

        .el-form-item__label {
          color: #9fd7ff;
          font-weight: 500;
          font-size: 13px;
          letter-spacing: 0.04em;
        }

        .el-form-item__content {
          .el-input__wrapper {
            background: rgba(6, 8, 18, 0.55);
            border: 1px solid rgba(0, 212, 255, 0.28);
            border-radius: 4px;
            box-shadow: none;
            transition: all 0.2s;

            &:hover {
              border-color: rgba(0, 212, 255, 0.55);
              box-shadow: 0 0 10px rgba(0, 212, 255, 0.18);
            }

            &.is-focus {
              border-color: #00d4ff;
              box-shadow: 0 0 0 1px rgba(0, 212, 255, 0.4), 0 0 18px rgba(0, 212, 255, 0.25);
            }

            .el-input__inner {
              color: #e6f6ff;
              font-size: 15px;
              font-family: $font-mono;
              caret-color: #00d4ff;
            }

            .el-input__inner::placeholder {
              color: rgba(147, 164, 200, 0.55);
            }

            .el-input__prefix,
            .el-input__suffix {
              .el-input__icon {
                color: #7fe9ff;
              }
            }
          }
        }
      }

      .code-input-wrapper {
        display: flex;
        gap: 10px;

        .el-input {
          flex: 1;
        }

        .send-code-btn {
          flex-shrink: 0;
          height: 40px;
          background: rgba(0, 212, 255, 0.06);
          border: 1px solid rgba(0, 212, 255, 0.45);
          color: #7fe9ff;
          border-radius: 4px;
          font-family: $font-mono;
          font-weight: 500;
          font-size: 13px;
          letter-spacing: 0.04em;
          transition: all 0.2s;

          &:hover:not(:disabled) {
            background: rgba(0, 212, 255, 0.14);
            border-color: #00d4ff;
            color: #c9f6ff;
            box-shadow: 0 0 14px rgba(0, 212, 255, 0.3);
            cursor: pointer;
          }

          &:disabled {
            opacity: 0.55;
            cursor: not-allowed;
          }

          &.counting {
            border-color: #ff2d92;
            color: #ffb3d6;
            background: rgba(255, 45, 146, 0.10);
            box-shadow: 0 0 12px rgba(255, 45, 146, 0.28);
            animation: pulse 1.5s infinite alternate;
          }
        }
      }

      .login-btn {
        background: linear-gradient(135deg, #00d4ff, #0072ff);
        border: none;
        color: #02121c;
        font-family: $font-mono;
        font-weight: 700;
        font-size: 15px;
        letter-spacing: 0.12em;
        border-radius: 4px;
        height: 48px;
        box-shadow: 0 0 22px rgba(0, 212, 255, 0.4);
        transition: all 0.2s;

        &:hover:not(:disabled) {
          background: linear-gradient(135deg, #33ddff, #2f8bff);
          transform: translateY(-1px);
          box-shadow: 0 0 30px rgba(0, 212, 255, 0.55);
        }

        &:disabled {
          opacity: 0.4;
          cursor: not-allowed;
          transform: none;
          box-shadow: none;
        }
      }
    }
  }
}

@keyframes pulse {
  from {
    box-shadow: 0 0 8px rgba(255, 45, 146, 0.2);
    opacity: 0.85;
  }
  to {
    box-shadow: 0 0 16px rgba(255, 45, 146, 0.5);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .login-container {
    .glass-panel {
      padding: 30px 20px;
      max-width: 100%;

      .title {
        font-size: 21px;
        letter-spacing: 0.06em;
      }

      .code-input-wrapper {
        flex-direction: column;

        .send-code-btn {
          width: 100%;
        }
      }
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-container .login-form .send-code-btn.counting {
    animation: none;
  }
  * {
    transition-duration: 0.01ms !important;
  }
}

:deep(.el-message) {
  background: rgba(9, 11, 24, 0.94);
  border: 1px solid rgba(0, 212, 255, 0.4);
  color: #dff6ff;
  border-radius: 4px;
  box-shadow: 0 0 22px rgba(0, 212, 255, 0.25);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  font-family: $font-mono;
}

:deep(.el-message .el-message__content) {
  color: inherit;
}
</style>

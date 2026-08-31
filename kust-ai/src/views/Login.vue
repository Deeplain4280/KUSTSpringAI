<template>
  <div class="login-container">
    <div class="glass-panel">
      <h3 class="title">🔐 邮箱登录</h3>

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

.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;

  /* 背景图片设置 */
  background-image: url('../assets/1.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;

  /* 暗色遮罩层，让文字更清晰 */
  position: relative;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4); /* 半透明黑色遮罩 */
    z-index: -1;
  }

  .glass-panel {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    padding: 40px;
    width: 100%;
    max-width: 450px;
    box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
    position: relative;
    z-index: 1;

    .title {
      color: #fff;
      text-align: center;
      margin-bottom: 30px;
      font-size: 28px;
      font-weight: 600;
    }

    .login-form {
      :deep(.el-form-item) {
        margin-bottom: 25px;

        .el-form-item__label {
          color: #fff;
          font-weight: 500;
          font-size: 15px;
        }

        .el-form-item__content {
          .el-input__wrapper {
            background: rgba(255, 255, 255, 0.15);
            border: 1px solid rgba(255, 255, 255, 0.3);
            border-radius: 10px;
            box-shadow: none;

            &:hover {
              box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.5);
            }

            &:focus-within {
              box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.6);
            }

            .el-input__inner {
              color: #fff;
              font-size: 15px;
            }

            .el-input__prefix,
            .el-input__suffix {
              .el-input__icon {
                color: rgba(255, 255, 255, 0.7);
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
          background: rgba(255, 255, 255, 0.2);
          border: 1px solid rgba(255, 255, 255, 0.3);
          color: #fff;
          border-radius: 10px;
          font-weight: 500;
          transition: all 0.3s ease;

          &:hover {
            background: rgba(255, 255, 255, 0.3);
            border-color: rgba(255, 255, 255, 0.5);
            cursor: pointer;
          }

          &:disabled {
            background: rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.2);
            color: rgba(255, 255, 255, 0.7);
            cursor: not-allowed;
            opacity: 0.8;
          }


          &.counting {
            background: rgba(255, 255, 255, 0.15);
            border-color: rgba(255, 255, 255, 0.4);
            color: rgba(255, 255, 255, 0.9);
            box-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
            animation: pulse 1.5s infinite alternate;
          }
        }
      }

      .login-btn {
        background: rgba(255, 255, 255, 0.9);
        border: none;
        color: #667eea;
        font-weight: 600;
        font-size: 16px;
        border-radius: 12px;
        height: 48px;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);

        &:hover {
          background: rgba(255, 255, 255, 1);
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
        }

        &:disabled {
          background: rgba(255, 255, 255, 0.3);
          color: rgba(255, 255, 255, 0.5);
          cursor: not-allowed;
          transform: none;
        }
      }
    }
  }
}


@keyframes pulse {
  from {
    box-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
    opacity: 0.8;
  }
  to {
    box-shadow: 0 0 12px rgba(255, 255, 255, 0.5);
    opacity: 1;
  }
}


@media (max-width: 768px) {
  .login-container {
    .glass-panel {
      padding: 30px 20px;
      max-width: 100%;

      .title {
        font-size: 24px;
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

:deep(.el-message) {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}
</style>

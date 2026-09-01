// src/assets/ts/sse.ts
import router from '../../router'
import { ElMessage } from 'element-plus'
import type { Ref } from 'vue'

interface Message {
  role: 'user' | 'ai'
  content: string
}

/* ==================== 通用对话 SSE ==================== */

interface StreamChatOptions {
  messages: Message[]
  question: string
  typingRef: Ref<boolean>
  scrollFn?: () => void
  sessionId?: string
  url?: string
  onComplete?: () => void
  onError?: () => void
}

let currentES: EventSource | null = null

export function stopStream(): void {
  if (currentES) {
    currentES.close()
    currentES = null
  }
}

export function streamChat(options: StreamChatOptions): void {
  const { messages, question, typingRef, scrollFn, sessionId, url, onComplete, onError } = options

  const sid = localStorage.getItem('userEmail')
  if (!sid) {
    ElMessage.warning('请登录后使用')
    setTimeout(() => router.push('/'), 1000)
    return
  }

  stopStream()

  const params = new URLSearchParams({
    userInput: question,
    sessionId: sessionId || '',
  })
  const fullUrl = `${url || '/api/chat/stream/memory'}?${params.toString()}`

  typingRef.value = true

  let aiMsgIndex = -1
  let hasReceivedData = false

  const es = new EventSource(fullUrl)
  currentES = es

  es.onmessage = (event: MessageEvent) => {
    if (event.data === '[DONE]') {
      cleanup()
      onComplete?.()
      return
    }
    //  跳过空心跳 chunk（零内容，避免触发无意义的重渲染）
    if (event.data === '') return

    if (!hasReceivedData) {
      hasReceivedData = true
      messages.push({ role: 'ai', content: '' })
      aiMsgIndex = messages.length - 1
      typingRef.value = false
    }
    messages[aiMsgIndex].content += event.data
    scrollFn?.()
  }

  es.onerror = () => {
    if (hasReceivedData) {
      cleanup()
      onComplete?.()
      return
    }
    messages.push({ role: 'ai', content: '⚠️ AI 响应失败，请重试' })
    cleanup()
    onError?.()
  }

  function cleanup(): void {
    es.close()
    if (currentES === es) currentES = null
    typingRef.value = false
  }
}

/* ==================== 邮件智能体 SSE ==================== */

interface EmailStreamOptions {
  question: string
  sessionId: string
  messages: Message[]
  typingRef: Ref<boolean>
  scrollFn?: () => void
  onComplete?: () => void
  onError?: () => void
}

let emailES: EventSource | null = null

export function stopEmailStream(): void {
  if (emailES) {
    emailES.close()
    emailES = null
  }
}

export function streamEmailChat(options: EmailStreamOptions): void {
  const { question, sessionId, messages, typingRef, scrollFn, onComplete, onError } = options

  stopEmailStream()

  const params = new URLSearchParams({
    userInput: question,
    sessionId: sessionId || 'default',
  })
  const fullUrl = `/api/email-agent/send?${params.toString()}`

  typingRef.value = true

  let aiMsgIndex = -1
  let hasReceivedData = false

  const es = new EventSource(fullUrl)
  emailES = es

  es.onmessage = (event: MessageEvent) => {
    if (event.data === '[DONE]') {
      cleanup()
      onComplete?.()
      return
    }
    //  跳过空心跳 chunk（零内容，避免触发无意义的重渲染）
    if (event.data === '') return

    if (!hasReceivedData) {
      hasReceivedData = true
      messages.push({ role: 'ai', content: '' })
      aiMsgIndex = messages.length - 1
      typingRef.value = false
    }
    messages[aiMsgIndex].content += event.data
    scrollFn?.()
  }

  es.onerror = () => {
    if (hasReceivedData) {
      cleanup()
      onComplete?.()
      return
    }
    messages.push({ role: 'ai', content: '⚠️ 邮件助手连接失败，请重试' })
    cleanup()
    onError?.()
  }

  function cleanup(): void {
    es.close()
    if (emailES === es) emailES = null
    typingRef.value = false
  }
}

/* ==================== 统一停止所有流 ==================== */

export function stopAllStreams(): void {
  stopStream()
  stopEmailStream()
}
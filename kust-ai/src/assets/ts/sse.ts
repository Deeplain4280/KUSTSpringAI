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
  const fullUrl = `${url || '/api/qwen/stream/memory'}?${params.toString()}`

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
  const fullUrl = `/api/email/send?${params.toString()}`

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

/* ==================== 图片创作智能体 SSE ==================== */

interface MediaStreamOptions {
  question: string
  sessionId: string
  messages: Message[]
  typingRef: Ref<boolean>
  scrollFn?: () => void
  onComplete?: () => void
  onError?: () => void
}

let mediaES: EventSource | null = null

export function stopMediaStream(): void {
  if (mediaES) {
    mediaES.close()
    mediaES = null
  }
}

export function streamMediaChat(options: MediaStreamOptions): void {
  const { question, sessionId, messages, typingRef, scrollFn, onComplete, onError } = options

  stopMediaStream()

  const params = new URLSearchParams({
    userInput: question,
    sessionId: sessionId || 'default',
  })
  const fullUrl = `/api/media-agent/chat?${params.toString()}`

  typingRef.value = true

  let aiMsgIndex = -1
  let hasReceivedData = false

  const es = new EventSource(fullUrl)
  mediaES = es

  es.onmessage = (event: MessageEvent) => {
    if (event.data === '[DONE]') {
      cleanup()
      onComplete?.()
      return
    }
    // 跳过空心跳 chunk
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
    messages.push({ role: 'ai', content: '⚠️ 图片创作助手连接失败，请重试' })
    cleanup()
    onError?.()
  }

  function cleanup(): void {
    es.close()
    if (mediaES === es) mediaES = null
    typingRef.value = false
  }
}

/* ==================== 识图反推 Agent SSE（POST multipart，fetch 流式） ==================== */

interface ImageStreamOptions {
  file: File | null
  question: string
  sessionId: string
  messages: Message[]
  typingRef: Ref<boolean>
  scrollFn?: () => void
  onComplete?: () => void
  onError?: () => void
}

let imageAbort: AbortController | null = null

export function stopImageStream(): void {
  if (imageAbort) {
    imageAbort.abort()
    imageAbort = null
  }
}

/**
 * 识图反推接口是 POST multipart，EventSource 只支持 GET，
 * 所以用 fetch + ReadableStream 手动解析 text/event-stream（每个事件以空行分隔，内容行以 data: 开头）。
 */
export function streamImageChat(options: ImageStreamOptions): void {
  const { file, question, sessionId, messages, typingRef, scrollFn, onComplete, onError } = options

  const sid = localStorage.getItem('userEmail')
  if (!sid) {
    ElMessage.warning('请登录后使用')
    setTimeout(() => router.push('/'), 1000)
    return
  }

  stopImageStream()

  const form = new FormData()
  if (file) form.append('file', file)
  form.append('userInput', question)
  form.append('sessionId', sessionId || 'default')

  typingRef.value = true

  const abort = new AbortController()
  imageAbort = abort

  let aiMsgIndex = -1
  let hasReceivedData = false

  run()

  async function run(): Promise<void> {
    try {
      const resp = await fetch('/api/image-agent/chat', { method: 'POST', body: form, signal: abort.signal })
      if (!resp.ok || !resp.body) throw new Error(`HTTP ${resp.status}`)

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // SSE 事件以空行分隔
        let sep: number
        while ((sep = buffer.indexOf('\n\n')) >= 0) {
          const raw = buffer.slice(0, sep)
          buffer = buffer.slice(sep + 2)
          handleEvent(raw)
        }
      }
      handleEvent(buffer) // 处理尾部残余
    } catch (e) {
      if ((e as Error)?.name === 'AbortError') return
      logError(e)
      if (!hasReceivedData) {
        messages.push({ role: 'ai', content: '⚠️ 识图助手连接失败，请重试' })
        onError?.()
      }
    } finally {
      if (imageAbort === abort) imageAbort = null
      typingRef.value = false
      onComplete?.()
    }
  }

  function handleEvent(rawEvent: string): void {
    if (!rawEvent) return
    const lines = rawEvent.split('\n').filter((l) => l.startsWith('data:'))
    if (lines.length === 0) return
    const data = lines
      .map((l) => l.slice(5).replace(/^ /, ''))
      .join('\n')
    if (!data || data === '[DONE]') return

    if (!hasReceivedData) {
      hasReceivedData = true
      messages.push({ role: 'ai', content: '' })
      aiMsgIndex = messages.length - 1
      typingRef.value = false
    }
    messages[aiMsgIndex].content += data
    scrollFn?.()
  }

  function logError(e: unknown): void {
    // 与其它 Agent 一致：错误信息直接作为文本回显（如 "[识别失败] ..."），无需再告警
    console.error('[streamImageChat]', e)
  }
}

/* ==================== 统一停止所有流 ==================== */

export function stopAllStreams(): void {
  stopStream()
  stopEmailStream()
  stopMediaStream()
  stopImageStream()
}

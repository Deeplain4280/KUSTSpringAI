// src/utils/markdown.ts
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/atom-one-dark.css'

/* ---------- 1. markdown-it 初始化（GFM + breaks，符合聊天习惯） ---------- */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,   // ✅ 单换行 → <br>，诗歌每行独立显示全靠它
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
        }</code></pre>`
      } catch (_) { /* fallthrough */ }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  },
})

/* ---------- 2. 行内数学公式 $...$ ---------- */
function mathInline(state: any) {
  const src = state.src
  const pos = state.pos
  if (src[pos] !== '$') return false
  // 避免 $$ 被当成两个行内公式
  if (src[pos + 1] === '$') return false
  const end = src.indexOf('$', pos + 1)
  if (end < 0 || end === pos + 1) return false
  try {
    const html = katex.renderToString(src.slice(pos + 1, end), { throwOnError: false })
    const token = state.push('html_inline', '', 0)
    token.content = html
    state.pos = end + 1
    return true
  } catch (_) {
    return false
  }
}
md.inline.ruler.before('escape', 'math_inline', mathInline)

/* ---------- 3. 块级数学公式 $$...$$ ---------- */
md.block.ruler.before('fence', 'math_block', (state, startLine, endLine, silent) => {
  const startPos = state.bMarks[startLine] + state.tShift[startLine]
  if (state.src.slice(startPos, startPos + 2) !== '$$') return false
  if (silent) return true
  let nextLine = startLine + 1
  while (nextLine < endLine) {
    const p = state.bMarks[nextLine] + state.tShift[nextLine]
    if (state.src.slice(p, p + 2) === '$$') break
    nextLine++
  }
  const content = state.getLines(startLine + 1, nextLine, state.tShift[startLine], false)
  try {
    const token = state.push('html_block', '', 0)
    token.content = `<div class="math-block">${katex.renderToString(content.trim(), {
      throwOnError: false,
      displayMode: true,
    })}</div>`
  } catch (_) { /* ignore */ }
  state.line = nextLine + 1
  return true
})

/* ---------- 4. 流式：自动补全未闭合代码块 ---------- */
function fixUnclosedFence(text: string): string {
  const fenceCount = (text.match(/^```/gm) || []).length
  return fenceCount % 2 === 1 ? text + '\n```' : text
}

/* ---------- ✅ 5. 空白规范化（只压缩连续空行，单换行原样保留交给 breaks） ---------- */
function normalizeWhitespace(text: string): string {
  const lines = text.replace(/\r\n?/g, '\n').split('\n')
  const out: string[] = []
  let inCode = false   // 代码块状态（兼容流式期间未闭合的代码块）
  let blank = 0        // 连续空行计数

  for (const line of lines) {
    // 围栏行：切换状态，原样保留
    if (/^ {0,3}(```|~~~)/.test(line)) {
      inCode = !inCode
      blank = 0
      out.push(line)
      continue
    }
    // 代码块内部：原样保留（保护代码格式）
    if (inCode) {
      out.push(line)
      continue
    }
    // ✅ 非代码区：只统计空行，不去行尾空格、不动单换行
    if (line.trim() === '') {
      blank++
      if (blank > 1) continue   // 连续空行最多保留 1 个（= 段落分隔）
    } else {
      blank = 0
    }
    out.push(line)              // ✅ 原样保留
  }

  // 去掉开头空行
  while (out.length && out[0].trim() === '') out.shift()
  return out.join('\n')
}

/* ---------- 6. 安全渲染 ---------- */
export function renderMarkdown(text: string, streaming = false): string {
  if (!text) return ''
  let safeText = normalizeWhitespace(text)          // ✅ 先清洗多余空行
  if (streaming) safeText = fixUnclosedFence(safeText)
  const rawHtml = md.render(safeText)
  return DOMPurify.sanitize(rawHtml, {
    ADD_TAGS: [
      'math', 'semantics', 'mrow', 'mi', 'mo', 'mn', 'msup', 'msub',
      'mfrac', 'msqrt', 'mover', 'munder', 'span', 'annotation',
    ],
    ADD_ATTR: ['class', 'style', 'aria-hidden', 'xmlns', 'encoding'],
  })
}

/* ---------- 7. Mermaid 异步渲染 ---------- */
let mermaidReady = false
export async function renderMermaidBlocks(container: HTMLElement | null) {
  if (!container) return
  const blocks = container.querySelectorAll<HTMLElement>('code.language-mermaid')
  if (blocks.length === 0) return
  if (!mermaidReady) {
    const mermaid = (await import('mermaid')).default
    mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'strict' })
    ;(window as any).__mermaid = mermaid
    mermaidReady = true
  }
  const mermaid = (window as any).__mermaid
  for (const block of Array.from(blocks)) {
    const pre = block.parentElement
    if (!pre || (pre as HTMLElement).dataset.mermaidRendered) continue
    try {
      const id = `mermaid-${Date.now()}-${Math.random().toString(36).slice(2)}`
      const { svg } = await mermaid.render(id, block.textContent || '')
      const wrapper = document.createElement('div')
      wrapper.className = 'mermaid-render'
      wrapper.innerHTML = svg
      pre.replaceWith(wrapper)
      ;(pre as HTMLElement).dataset.mermaidRendered = '1'
    } catch (e) {
      console.warn('mermaid 渲染失败', e)
    }
  }
}
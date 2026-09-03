<template>
  <div class="app-container" :class="{ dark: settings.darkMode }">
    <!-- ========== 左侧边栏 ========== -->
    <aside class="sidebar" :class="{ collapsed: !isSidebarOpen }">

      <!-- 顶部 Logo 栏 -->
      <div class="sidebar-top-bar">
        <span class="logo-text" v-show="isSidebarOpen">Zcode</span>
        <div class="top-actions">
          <button class="icon-btn" title="搜索" @click="openSearch">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </button>
          <button class="icon-btn" :title="isSidebarOpen ? '收起侧边栏' : '展开侧边栏'" @click="toggleSidebar">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
          </button>
        </div>
      </div>

      <!-- 新建对话区域 -->
      <div class="new-chat-section" v-if="isSidebarOpen">
        <button class="new-chat-btn-large" @click="newChat">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
          新建对话
        </button>
        <button class="icon-btn-square" title="临时对话" @click="openTemp">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </button>
      </div>
      <div class="new-chat-collapsed" v-else>
        <button class="icon-btn-square" title="新建对话" @click="newChat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>

      <!-- 菜单 -->
      <div class="sidebar-menu" v-if="isSidebarOpen">
        <button class="menu-item" @click="showToast('「云空间」功能模拟中')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          云空间
        </button>
        <button class="menu-item" @click="showToast('「千问创作」功能模拟中')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
          千问创作
          <span class="badge">Wan 3.0</span>
        </button>

        <!-- ✅ 邮件智能体入口 -->
        <button class="menu-item" :class="{ active: agentMode === 'email' }" @click="switchAgentMode('email')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
          邮件助手
          <span class="badge badge-email">Agent</span>
        </button>

        <div class="section-title">对话分组</div>
        <button class="menu-item text-gray" @click="showToast('已创建新分组（模拟）')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新分组
        </button>

        <div class="section-title">定时任务</div>
        <button class="menu-item text-gray" @click="showToast('已创建新定时任务（模拟）')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新定时任务
        </button>

        <div class="section-title">最近对话</div>
        <div v-if="conversations.length === 0" class="empty-state">暂无对话</div>
        <div v-for="c in conversations" :key="c.id" class="chat-item-wrap">
          <button class="menu-item text-gray" :class="{ active: c.id === activeId }" @click="openConversation(c.id)">
            <span class="chat-title">{{ c.title }}</span>
          </button>
          <button class="delete-btn" title="删除对话" @click.stop="deleteConversation(c.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>

      <!-- 底部用户区 -->
      <div class="sidebar-footer" v-if="isSidebarOpen" v-click-outside="() => userMenuOpen = false">
        <div class="dropdown-menu user-menu" v-if="userMenuOpen">
          <button class="dropdown-item" @click="openSettings">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            <span>设置</span>
          </button>
          <button class="dropdown-item" @click="settings.darkMode = !settings.darkMode">
            <svg v-if="settings.darkMode" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
            <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            <span>{{ settings.darkMode ? '浅色模式' : '深色模式' }}</span>
          </button>
          <button class="dropdown-item danger" @click="logout">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            <span>退出登录</span>
          </button>
        </div>
        <button class="user-profile" @click="userMenuOpen = !userMenuOpen">
          <div class="avatar"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="User" /></div>
          <span class="username">{{ userEmail }}</span>
        </button>
      </div>
    </aside>

    <!-- ========== 主内容区 ========== -->
    <main class="main-content">
      <!-- 对话头部 -->
      <header class="chat-header" v-if="activeConversation">
        <div class="chat-title-wrap">
          <span class="chat-title-text">{{ activeConversation.title }}</span>
          <span class="mode-badge">{{ currentMode }}</span>
          <!-- ✅ 邮件模式标识 -->
          <span v-if="agentMode === 'email'" class="mode-badge email-badge">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><polyline points="22,6 12,13 2,6"/></svg>
            <span>邮件助手</span>
          </span>
        </div>
        <button class="icon-btn" title="删除该对话" @click="deleteConversation(activeConversation.id)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </header>

      <!-- 消息列表 -->
      <div class="messages-scroll" v-if="activeConversation" ref="messagesRef">
        <div v-for="(m, i) in activeConversation.messages" :key="i" class="message-row" :class="m.role">
          <div class="msg-avatar ai-avatar" v-if="m.role === 'ai'">
            <svg v-if="agentMode === 'email'" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6" fill="none" stroke="white" stroke-width="2"/></svg>
            <svg v-else width="20" height="20" viewBox="0 0 100 100" fill="currentColor"><path d="M50 5 L93 27.5 V72.5 L50 95 L7 72.5 V27.5 Z"/><circle cx="50" cy="50" r="14" fill="white"/></svg>
          </div>

          <div v-if="m.role === 'ai'" class="bubble ai markdown-body" v-html="toHtml(m.content, isTyping)"></div>
          <div v-else class="bubble user">{{ m.content }}</div>

          <div class="msg-avatar user-avatar" v-if="m.role === 'user'">
            <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="User" />
          </div>

          <button class="copy-btn" v-if="m.role === 'ai' && m.content" @click="copyText(m.content)">复制</button>
        </div>

        <!-- 思考中动画 -->
        <div class="message-row ai" v-if="isTyping && lastIsUser">
          <div class="msg-avatar ai-avatar">
            <svg width="20" height="20" viewBox="0 0 100 100" fill="currentColor"><path d="M50 5 L93 27.5 V72.5 L50 95 L7 72.5 V27.5 Z"/><circle cx="50" cy="50" r="14" fill="white"/></svg>
          </div>
          <div class="bubble ai thinking"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
        </div>

        <!-- 停止生成按钮 -->
        <div class="stop-generate-wrap" v-if="isTyping">
          <button class="stop-generate-btn" @click="handleStopStream">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
            停止生成
          </button>
        </div>
      </div>

      <!-- 首页欢迎语 -->
      <div class="center-stage" v-else>
        <div class="welcome-section">
          <div class="logo-icon">
            <!-- ✅ 邮件模式图标 -->
            <svg v-if="agentMode === 'email'" width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6" fill="none" stroke="white" stroke-width="2"/>
            </svg>
            <!-- 普通模式图标 -->
            <svg v-else width="48" height="48" viewBox="0 0 100 100" fill="currentColor">
              <path d="M50 5 L93 27.5 V72.5 L50 95 L7 72.5 V27.5 Z" opacity="0.9"/>
              <circle cx="50" cy="50" r="12" fill="white"/>
            </svg>
          </div>
          <h1 class="welcome-text">
            {{ agentMode === 'email' ? '你好，我们今天发送什么' : '你好，我们今天构建什么？' }}
          </h1>
          <p v-if="agentMode === 'email'" class="welcome-sub">
            告诉我你想发什么邮件，我来帮你起草、修改和发送
          </p>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-dock">
        <div class="input-box" :class="{ 'email-mode': agentMode === 'email' }">
          <textarea
            v-model="inputText"
            :placeholder="agentMode === 'email' ? '描述你的邮件需求，如：帮我写一封请假邮件...' : '你好主人，请问有什么可以帮你？'"
            rows="1"
            @input="autoResize"
            @keydown="onKeydown"
            ref="textareaRef"
          ></textarea>

          <div class="input-actions">
            <div class="action-left">
              <button class="tool-btn" title="添加" @click="showToast('附件上传（模拟）')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              </button>

              <!-- 模式选择 -->
              <div class="dropdown" v-click-outside="() => modeMenuOpen = false">
                <button class="tool-group" @click="modeMenuOpen = !modeMenuOpen">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                  <span>{{ currentMode }}</span>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
                </button>
                <div class="dropdown-menu up" v-if="modeMenuOpen">
                  <button class="dropdown-item" v-for="m in modes" :key="m" @click="selectMode(m)">
                    <span>{{ m }}</span>
                    <svg v-if="m === currentMode" class="check" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                  </button>
                </div>
              </div>

              <!-- ✅ 邮件助手切换按钮 -->
              <button
                class="tool-group"
                :class="{ 'tool-active': agentMode === 'email' }"
                @click="switchAgentMode('email')"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                <span>邮件助手</span>
                <span v-if="agentMode === 'email'" class="tag-purple">活跃</span>
              </button>

              <button class="tool-group" @click="showToast('「工作助理」已连接本地电脑（模拟）')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
                <span>工作助理</span>
                <span class="tag-blue">本地电脑</span>
              </button>

              <button class="tool-group" @click="showToast('「AI生视频」Wan 3.0 模拟中')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"/><line x1="7" y1="2" x2="7" y2="22"/><line x1="17" y1="2" x2="17" y2="22"/><line x1="2" y1="12" x2="22" y2="12"/></svg>
                <span>AI生视频</span>
                <span class="tag-blue">Wan 3.0</span>
              </button>

              <button class="tool-group" @click="showToast('「PPT创作」功能模拟中')">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                <span>PPT创作</span>
              </button>

              <!-- 更多 -->
              <div class="dropdown" v-click-outside="() => moreMenuOpen = false">
                <button class="tool-group" @click="moreMenuOpen = !moreMenuOpen">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
                  <span>更多</span>
                </button>
                <div class="dropdown-menu up" v-if="moreMenuOpen">
                  <button class="dropdown-item" v-for="t in moreTools" :key="t" @click="pickMoreTool(t)">{{ t }}</button>
                </div>
              </div>
            </div>

            <div class="action-right">
              <button class="icon-btn-sm" title="语音输入" @click="showToast('语音输入（模拟）')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
              </button>
              <button class="send-btn" :class="{ active: inputText.trim() }" title="发送" @click="sendMessage">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>
              </button>
            </div>
          </div>
        </div>
        <p class="disclaimer">
          {{ agentMode === 'email' ? '邮件内容由 AI 辅助生成，发送前请仔细确认' : '内容由 AI 生成，仅供参考' }}
        </p>
      </div>
    </main>

    <!-- ========== 搜索弹窗 ========== -->
    <div class="overlay" v-if="searchOpen" @click.self="searchOpen = false">
      <div class="search-panel">
        <div class="search-input-row">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchQuery" placeholder="搜索历史对话..." ref="searchInputRef" @keydown.esc="searchOpen = false" />
          <span class="esc-hint">Esc</span>
        </div>
        <div class="search-results">
          <button class="search-item" v-for="c in searchResults" :key="c.id" @click="openConversation(c.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            {{ c.title }}
          </button>
          <div class="search-empty" v-if="searchResults.length === 0">未找到相关对话</div>
        </div>
      </div>
    </div>

    <!-- ========== 临时对话窗口 ========== -->
    <div class="overlay" v-if="tempOpen" @click.self="closeTemp">
      <div class="temp-window">
        <div class="temp-header">
          <span class="temp-title">临时对话</span>
          <span class="temp-note">· 内容不会被保存</span>
          <button class="icon-btn" title="关闭" @click="closeTemp">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="temp-messages" ref="tempMessagesRef">
          <div v-for="(m, i) in tempMessages" :key="i" class="message-row" :class="m.role">
            <div class="msg-avatar ai-avatar" v-if="m.role === 'ai'">
              <svg width="18" height="18" viewBox="0 0 100 100" fill="currentColor"><path d="M50 5 L93 27.5 V72.5 L50 95 L7 72.5 V27.5 Z"/><circle cx="50" cy="50" r="14" fill="white"/></svg>
            </div>
            <div v-if="m.role === 'ai'" class="bubble ai markdown-body" v-html="toHtml(m.content, tempTyping)"></div>
            <div v-else class="bubble user">{{ m.content }}</div>
            <div class="msg-avatar user-avatar" v-if="m.role === 'user'">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="User" />
            </div>
          </div>
          <div class="message-row ai" v-if="tempTyping && tempLastIsUser">
            <div class="msg-avatar ai-avatar">
              <svg width="18" height="18" viewBox="0 0 100 100" fill="currentColor"><path d="M50 5 L93 27.5 V72.5 L50 95 L7 72.5 V27.5 Z"/><circle cx="50" cy="50" r="14" fill="white"/></svg>
            </div>
            <div class="bubble ai thinking"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
          </div>
          <div class="search-empty" v-if="tempMessages.length === 0 && !tempTyping">开始一段不会被记录的对话吧～</div>
        </div>
        <div class="temp-input-row">
          <input v-model="tempInput" placeholder="输入消息，回车发送" @keydown.enter="sendTemp" />
          <button class="send-btn" :class="{ active: tempInput.trim() }" @click="sendTemp">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 设置面板 ========== -->
    <div class="overlay" v-if="settingsOpen" @click.self="settingsOpen = false">
      <div class="settings-panel">
        <div class="settings-header">
          <span>设置</span>
          <button class="icon-btn" @click="settingsOpen = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="setting-row">
          <span>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            深色模式
          </span>
          <button class="toggle" :class="{ on: settings.darkMode }" @click="settings.darkMode = !settings.darkMode"><span class="knob"></span></button>
        </div>
        <div class="setting-row">
          <span>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 10 4 15 9 20"/><path d="M20 4v7a4 4 0 0 1-4 4H4"/></svg>
            回车发送消息
          </span>
          <button class="toggle" :class="{ on: settings.enterToSend }" @click="settings.enterToSend = !settings.enterToSend"><span class="knob"></span></button>
        </div>
        <div class="setting-row">
          <span>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            清空所有对话
          </span>
          <button class="danger-btn" @click="clearAll">清空</button>
        </div>
        <div class="settings-about">YIFEI Chat · v1.0.0</div>
      </div>
    </div>

    <!-- ========== Toast ========== -->
    <transition name="fade">
      <div class="toast" v-if="toastVisible">{{ showToastMsg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { streamChat, streamEmailChat, stopAllStreams } from '../assets/ts/sse'
import { chatApi } from '../assets/ts/index'
import { renderMarkdown, renderMermaidBlocks } from '../assets/ts/markdown'

const router = useRouter()

/* ========== 点击外部关闭指令 ========== */
const vClickOutside = {
  mounted(el, binding) {
    el.__outside = (e) => { if (!el.contains(e.target)) binding.value() }
    document.addEventListener('click', el.__outside)
  },
  unmounted(el) { document.removeEventListener('click', el.__outside) },
}

/* ========== 响应式状态 ========== */
const inputText = ref('')
const textareaRef = ref(null)
const messagesRef = ref(null)
const isSidebarOpen = ref(true)
const isTyping = ref(false)
const userEmail = ref('')

const settings = reactive({ darkMode: true, enterToSend: true })
const settingsOpen = ref(false)
const userMenuOpen = ref(false)
const openSettings = () => { settingsOpen.value = true; userMenuOpen.value = false }

const conversations = ref([])
const activeId = ref(null)

const activeConversation = computed(
  () => conversations.value.find((c) => c.id === activeId.value) || null
)
const lastIsUser = computed(() => {
  const msgs = activeConversation.value?.messages
  return !!(msgs && msgs.length > 0 && msgs[msgs.length - 1].role === 'user')
})

/* ========== ✅ 智能体模式（不再需要独立的 emailSessionId） ========== */
const agentMode = ref('chat') // 'chat' | 'email'

const switchAgentMode = (mode) => {
  if (agentMode.value === mode) {
    agentMode.value = 'chat'
    showToast('已切换回普通对话模式')
  } else {
    agentMode.value = mode
    if (mode === 'email') {
      showToast('已切换到邮件助手模式')
      if (!activeConversation.value) {
        newChat()
      }
    }
  }
}

/* ---- Markdown 渲染辅助 ---- */
const toHtml = (content, streaming = false) => renderMarkdown(content || '', streaming)
const afterRender = async () => {
  await nextTick()
  if (messagesRef.value) await renderMermaidBlocks(messagesRef.value)
  if (tempMessagesRef.value) await renderMermaidBlocks(tempMessagesRef.value)
}

/* ---- 模式 / 更多工具 ---- */
const modeMenuOpen = ref(false)
const moreMenuOpen = ref(false)
const currentMode = ref('快速')
const modes = ['快速', '深度思考', '联网搜索']
const moreTools = ['翻译', '写作', '代码', '图像生成']
const selectMode = (m) => { currentMode.value = m; modeMenuOpen.value = false; showToast(`已切换到「${m}」模式`) }
const pickMoreTool = (t) => { moreMenuOpen.value = false; showToast(`「${t}」功能模拟中`) }

/* ---- 搜索 ---- */
const searchOpen = ref(false)
const searchQuery = ref('')
const searchInputRef = ref(null)
const searchResults = computed(() =>
  conversations.value.filter((c) => c.title.toLowerCase().includes(searchQuery.value.toLowerCase()))
)
const openSearch = () => {
  searchOpen.value = true; searchQuery.value = ''
  nextTick(() => searchInputRef.value?.focus())
}

/* ---- 临时对话 ---- */
const tempOpen = ref(false)
const tempInput = ref('')
const tempMessages = ref([])
const tempTyping = ref(false)
const tempMessagesRef = ref(null)
const tempSessionId = computed(() => `${userEmail.value || 'default'}_temp`)
const tempLastIsUser = computed(() => {
  const msgs = tempMessages.value
  return !!(msgs && msgs.length > 0 && msgs[msgs.length - 1].role === 'user')
})
const openTemp = () => { tempOpen.value = true; tempMessages.value = [] }
const closeTemp = () => {
  stopAllStreams()
  tempTyping.value = false
  tempOpen.value = false
  tempMessages.value = []
  chatApi.clearMemory(tempSessionId.value).catch(() => {})
  showToast('临时对话已关闭，记忆已清除')
}

/* ========== Toast ========== */
const showToastMsg = ref('')
const toastVisible = ref(false)
let toastTimer = null
const showToast = (msg) => {
  showToastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 2000)
}

/* ========== 工具函数 ========== */
const toggleSidebar = () => { isSidebarOpen.value = !isSidebarOpen.value }
const scrollEl = (el) => { if (el) el.scrollTop = el.scrollHeight }
const autoResize = () => {
  const t = textareaRef.value
  if (t) { t.style.height = 'auto'; t.style.height = Math.min(t.scrollHeight, 200) + 'px' }
}
const onKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey && settings.enterToSend) { e.preventDefault(); sendMessage() }
}
const copyText = async (text) => {
  try { await navigator.clipboard.writeText(text); showToast('已复制到剪贴板') }
  catch { showToast('复制失败，请手动复制') }
}
const logout = () => { localStorage.clear(); sessionStorage.clear(); router.push('/') }

const normalizeMessages = (list) =>
  (list || []).map((m) => ({
    role: String(m.role ?? m.type ?? '').toLowerCase() === 'user' ? 'user' : 'ai',
    content: m.content ?? m.text ?? '',
  }))

/* ========== 加载对话列表 ========== */
const loadConversations = async () => {
  if (!userEmail.value) return
  try {
    const res = await chatApi.getList(userEmail.value)
    conversations.value = (res.data || []).map((item) => ({ ...item, messages: [] }))
  } catch (e) {
    console.error('加载对话列表失败', e)
  }
}

/* ========== 新建对话 ========== */
const newChat = async () => {
  const cur = activeConversation.value
  if (cur && cur.messages.length === 0) {
    inputText.value = ''
    nextTick(() => textareaRef.value?.focus())
    return
  }
  const newId = Date.now().toString()
  const title = agentMode.value === 'email' ? '📧 邮件助手' : '新建对话'
  try {
    await chatApi.create({ id: newId, userEmail: userEmail.value, title })
  } catch (e) { showToast('创建对话失败'); return }

  conversations.value.unshift({ id: newId, title, messages: [] })
  activeId.value = newId
  inputText.value = ''
  nextTick(() => textareaRef.value?.focus())
}

/* ========== ✅ 发送消息（邮件智能体使用 conv.id 作为 sessionId） ========== */
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isTyping.value) return

  /* -------- 邮件智能体模式 -------- */
  if (agentMode.value === 'email') {
    let conv = activeConversation.value

    // 兜底：无活跃对话时自动创建
    if (!conv) {
      const newId = Date.now().toString()
      const newTitle = '📧 ' + text.slice(0, 12) + (text.length > 12 ? '...' : '')
      try {
        await chatApi.create({ id: newId, userEmail: userEmail.value, title: newTitle })
      } catch (e) { showToast('创建对话失败'); return }
      conversations.value.unshift({ id: newId, title: newTitle, messages: [] })
      activeId.value = newId
      conv = conversations.value[0]
    }

    // 首条消息改标题
    if (conv.messages.length === 0 && (conv.title === '📧 邮件助手' || conv.title === '新建对话')) {
      const newTitle = '📧 ' + text.slice(0, 12) + (text.length > 12 ? '...' : '')
      conv.title = newTitle
      chatApi.updateTitle(conv.id, newTitle).catch(() => {})
    }

    // 用户消息上屏
    conv.messages.push({ role: 'user', content: text })
    inputText.value = ''
    autoResize()
    nextTick(() => scrollEl(messagesRef.value))

    // ✅ 直接使用 conv.id 作为 sessionId，与普通对话统一
    const doEmailStream = () => {
      streamEmailChat({
        question: text,
        sessionId: conv.id,       // ← 复用 conv.id
        messages: conv.messages,
        typingRef: isTyping,
        scrollFn: () => scrollEl(messagesRef.value),
        onComplete: () => afterRender(),
        onError: () => showToast('邮件助手连接异常'),
      })
    }
    if (!messagesRef.value) nextTick(() => nextTick(doEmailStream))
    else doEmailStream()
    return
  }

  /* -------- 普通对话模式（原逻辑不变） -------- */
  let conv = activeConversation.value

  if (!conv) {
    const newId = Date.now().toString()
    const newTitle = text.slice(0, 14) + (text.length > 14 ? '...' : '')
    try {
      await chatApi.create({ id: newId, userEmail: userEmail.value, title: newTitle })
    } catch (e) { showToast('创建对话失败'); return }
    conversations.value.unshift({ id: newId, title: newTitle, messages: [] })
    activeId.value = newId
    conv = conversations.value[0]
  }

  if (conv.messages.length === 0 && (conv.title === '新建对话' || !conv.title)) {
    const newTitle = text.slice(0, 14) + (text.length > 14 ? '...' : '')
    conv.title = newTitle
    chatApi.updateTitle(conv.id, newTitle).catch(() => {})
  }

  conv.messages.push({ role: 'user', content: text })
  inputText.value = ''
  autoResize()
  nextTick(() => scrollEl(messagesRef.value))

  const doStream = () => {
    streamChat({
      messages: conv.messages,
      question: text,
      typingRef: isTyping,
      scrollFn: () => scrollEl(messagesRef.value),
      sessionId: conv.id,
      onError: () => showToast('AI 连接异常，已断开'),
    })
  }
  if (!messagesRef.value) nextTick(() => nextTick(doStream))
  else doStream()
}

/* ========== 打开/切换对话 ========== */
const openConversation = async (id) => {
  activeId.value = id
  searchOpen.value = false
  const conv = conversations.value.find((c) => c.id === id)
  if (!conv) return

  try {
    const res = await chatApi.getMessages(id)
    conv.messages = normalizeMessages(res.data)
  } catch (e) {
    conv.messages = []
  }
  nextTick(() => {
    scrollEl(messagesRef.value)
    afterRender()
  })
}

/* ========== ✅ 删除对话（后端 deleteConversation 已联动删除该会话记忆） ========== */
const deleteConversation = async (id) => {
  try {
    await chatApi.delete(id)
    // 若删除的是正在对话中的会话，先停止进行中的流式输出
    if (activeId.value === id) {
      stopAllStreams()
      isTyping.value = false
    }
    // 本地列表即时移除，UI 刷新
    conversations.value = conversations.value.filter((c) => c.id !== id)
    if (activeId.value === id) activeId.value = null
    showToast('对话已删除')
  } catch (e) { showToast('删除失败') }
}

const clearAll = async () => {
  try {
    await chatApi.clearAll(userEmail.value)
    conversations.value = []; activeId.value = null
    settingsOpen.value = false
    showToast('已清空所有对话')
  } catch (e) { showToast('清空失败') }
}

/* ========== 临时对话发送 ========== */
const sendTemp = () => {
  const text = tempInput.value.trim()
  if (!text || tempTyping.value) return
  tempMessages.value.push({ role: 'user', content: text })
  tempInput.value = ''
  streamChat({
    messages: tempMessages.value,
    question: text,
    typingRef: tempTyping,
    scrollFn: () => scrollEl(tempMessagesRef.value),
    sessionId: tempSessionId.value,
    onError: () => showToast('临时对话 AI 连接异常'),
  })
}

/* ========== 停止生成 ========== */
const handleStopStream = () => {
  stopAllStreams()
  isTyping.value = false
  tempTyping.value = false
}

/* ========== 流式结束 → 渲染 mermaid ========== */
watch(isTyping, (v) => { if (!v) afterRender() })
watch(tempTyping, (v) => { if (!v) afterRender() })

/* ========== 生命周期 ========== */
onMounted(() => {
  userEmail.value = localStorage.getItem('userEmail') || ''
  autoResize()
  loadConversations()
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      searchOpen.value = false; settingsOpen.value = false
      modeMenuOpen.value = false; moreMenuOpen.value = false; userMenuOpen.value = false
    }
  })
})
onUnmounted(() => stopAllStreams())
</script>

<style scoped>
@import '@/assets/css/index.css';

</style>

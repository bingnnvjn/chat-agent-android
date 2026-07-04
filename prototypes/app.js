/* ===== ChatGPT iOS App Clone — Application Logic ===== */

(() => {
  'use strict';

  // ─── Configuration ───
  const CONFIG = {
    STORAGE_KEY: 'chatgpt_clone_data',
    THEME_KEY: 'chatgpt_clone_theme',
    API_KEY: 'chatgpt_clone_apikey',
    MODEL_KEY: 'chatgpt_clone_model',
    TYPING_SPEED: 20,
    TYPING_VARIANCE: 15,
    MAX_TITLE_LEN: 30,
  };

  // ─── State ───
  let state = {
    conversations: [],
    currentConvId: null,
    isStreaming: false,
    sidebarOpen: false,
    isDesktop: window.innerWidth >= 768,
  };

  // ─── DOM References ───
  const $ = (sel) => document.querySelector(sel);
  const dom = {
    sidebar: $('#sidebar'),
    sidebarOverlay: $('#sidebar-overlay'),
    menuBtn: $('#menu-btn'),
    closeSidebar: $('#close-sidebar'),
    newChatBtn: $('#new-chat-btn'),
    newChatTop: $('#new-chat-top'),
    conversationList: $('#conversation-list'),
    messagesContainer: $('#messages-container'),
    welcomeScreen: $('#welcome-screen'),
    chatTitle: $('#chat-title'),
    messageInput: $('#message-input'),
    sendBtn: $('#send-btn'),
    settingsBtn: $('#settings-btn'),
    apiKeyModal: $('#api-key-modal'),
    apiKeyInput: $('#api-key-input'),
    saveApiKey: $('#save-api-key'),
    settingsModal: $('#settings-modal'),
    closeSettings: $('#close-settings'),
    settingsApiKey: $('#settings-api-key'),
    updateApiKey: $('#update-api-key'),
    modelSelect: $('#model-select'),
    providerSelect: $('#provider-select'),
    customUrlGroup: $('#custom-url-group'),
    customUrl: $('#custom-url'),
    themeDark: $('#theme-dark'),
    themeLight: $('#theme-light'),
    clearConversations: $('#clear-conversations'),
    mainContent: $('#main-content'),
  };

  // ─── API Providers Configuration ───
  const API_PROVIDERS = {
    openai: {
      name: 'OpenAI',
      url: 'https://api.openai.com/v1/chat/completions',
      models: ['gpt-3.5-turbo', 'gpt-4', 'gpt-4-turbo', 'gpt-4o'],
      defaultModel: 'gpt-3.5-turbo'
    },
    deepseek: {
      name: 'DeepSeek',
      url: 'https://api.deepseek.com/v1/chat/completions',
      models: ['deepseek-chat', 'deepseek-coder', 'deepseek-reasoner'],
      defaultModel: 'deepseek-chat'
    },
    anthropic: {
      name: 'Anthropic (Claude)',
      url: 'https://api.anthropic.com/v1/messages',
      models: ['claude-3-haiku-20240307', 'claude-3-sonnet-20240229', 'claude-3-opus-20240229', 'claude-3-5-sonnet-20241022'],
      defaultModel: 'claude-3-5-sonnet-20241022',
      isAnthropic: true
    },
    gemini: {
      name: 'Google (Gemini)',
      url: 'https://generativelanguage.googleapis.com/v1beta/models/',
      models: ['gemini-pro', 'gemini-1.5-pro', 'gemini-1.5-flash'],
      defaultModel: 'gemini-1.5-flash',
      isGemini: true
    },
    agnes: {
      name: 'Agnes AI (免费)',
      url: 'https://apihub.agnes-ai.com/v1/chat/completions',
      models: ['Agnes-2.0-Flash', 'Agnes-1.5-Flash'],
      defaultModel: 'Agnes-2.0-Flash'
    },
    custom: {
      name: '自定义 (OpenAI兼容)',
      url: '',
      models: [],
      defaultModel: ''
    }
  };

  const PROVIDER_KEY = 'chatgpt_clone_provider';
  const CUSTOM_URL_KEY = 'chatgpt_clone_custom_url';

  // ─── Initialization ───
  function init() {
    loadState();
    loadTheme();
    setupEventListeners();
    renderConversationList();
    handleResize();

    // Show API key modal if no key stored
    const apiKey = localStorage.getItem(CONFIG.API_KEY);
    if (!apiKey) {
      dom.apiKeyModal.classList.add('open');
    } else {
      dom.settingsApiKey.value = apiKey;
    }

    // 初始化提供商选择
    const savedProvider = localStorage.getItem(PROVIDER_KEY) || 'openai';
    dom.providerSelect.value = savedProvider;
    updateModelSelect(savedProvider);
    
    // 显示/隐藏自定义 URL 输入框
    dom.customUrlGroup.style.display = savedProvider === 'custom' ? 'block' : 'none';
    if (savedProvider === 'custom') {
      dom.customUrl.value = localStorage.getItem(CUSTOM_URL_KEY) || '';
    }

    const savedModel = localStorage.getItem(CONFIG.MODEL_KEY);
    if (savedModel) {
      dom.modelSelect.value = savedModel;
    }

    if (state.currentConvId) {
      switchConversation(state.currentConvId);
    } else {
      showWelcome();
    }
  }

  function updateModelSelect(provider) {
    const config = API_PROVIDERS[provider];
    if (!config) return;
    
    dom.modelSelect.innerHTML = '';
    config.models.forEach(model => {
      const option = document.createElement('option');
      option.value = model;
      option.textContent = model;
      dom.modelSelect.appendChild(option);
    });
    
    // 设置默认模型
    const savedModel = localStorage.getItem(CONFIG.MODEL_KEY);
    if (savedModel && config.models.includes(savedModel)) {
      dom.modelSelect.value = savedModel;
    } else {
      dom.modelSelect.value = config.defaultModel;
      localStorage.setItem(CONFIG.MODEL_KEY, config.defaultModel);
    }
  }

  // ─── State Management ───
  function loadState() {
    try {
      const data = localStorage.getItem(CONFIG.STORAGE_KEY);
      if (data) {
        const parsed = JSON.parse(data);
        state.conversations = parsed.conversations || [];
        state.currentConvId = parsed.currentConvId || null;
      }
    } catch (e) {
      console.warn('Failed to load state:', e);
    }
  }

  function saveState() {
    try {
      localStorage.setItem(CONFIG.STORAGE_KEY, JSON.stringify({
        conversations: state.conversations,
        currentConvId: state.currentConvId,
      }));
    } catch (e) {
      console.warn('Failed to save state:', e);
    }
  }

  function loadTheme() {
    const theme = localStorage.getItem(CONFIG.THEME_KEY) || 'dark';
    document.body.classList.toggle('light', theme === 'light');
    dom.themeDark.classList.toggle('active', theme === 'dark');
    dom.themeLight.classList.toggle('active', theme === 'light');
  }

  // ─── Event Listeners ───
  function setupEventListeners() {
    dom.menuBtn.addEventListener('click', toggleSidebar);
    dom.closeSidebar.addEventListener('click', closeSidebar);
    dom.sidebarOverlay.addEventListener('click', closeSidebar);

    dom.newChatBtn.addEventListener('click', () => { createConversation(); closeSidebar(); });
    dom.newChatTop.addEventListener('click', () => createConversation());

    dom.messageInput.addEventListener('input', handleInputChange);
    dom.messageInput.addEventListener('keydown', handleInputKeydown);
    dom.sendBtn.addEventListener('click', sendMessage);

    dom.settingsBtn.addEventListener('click', () => {
      dom.settingsApiKey.value = localStorage.getItem(CONFIG.API_KEY) || '';
      dom.settingsModal.classList.add('open');
    });
    dom.closeSettings.addEventListener('click', () => dom.settingsModal.classList.remove('open'));

    dom.saveApiKey.addEventListener('click', () => saveApiKeyFromModal(dom.apiKeyInput, dom.apiKeyModal));
    dom.apiKeyInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') saveApiKeyFromModal(dom.apiKeyInput, dom.apiKeyModal);
    });

    dom.updateApiKey.addEventListener('click', () => {
      const key = dom.settingsApiKey.value.trim();
      if (key) {
        localStorage.setItem(CONFIG.API_KEY, key);
        showToast('API Key 已更新');
      }
    });
    dom.modelSelect.addEventListener('change', () => {
      localStorage.setItem(CONFIG.MODEL_KEY, dom.modelSelect.value);
    });
    
    // 提供商选择事件
    dom.providerSelect.addEventListener('change', () => {
      const provider = dom.providerSelect.value;
      localStorage.setItem(PROVIDER_KEY, provider);
      updateModelSelect(provider);
      
      // 显示/隐藏自定义 URL 输入框
      dom.customUrlGroup.style.display = provider === 'custom' ? 'block' : 'none';
    });
    
    // 自定义 URL 保存
    dom.customUrl.addEventListener('change', () => {
      localStorage.setItem(CUSTOM_URL_KEY, dom.customUrl.value.trim());
    });
    dom.themeDark.addEventListener('click', () => setTheme('dark'));
    dom.themeLight.addEventListener('click', () => setTheme('light'));
    dom.clearConversations.addEventListener('click', clearAllConversations);

    window.addEventListener('resize', handleResize);

    dom.apiKeyModal.addEventListener('click', (e) => {
      if (e.target === dom.apiKeyModal) dom.apiKeyModal.classList.remove('open');
    });
    dom.settingsModal.addEventListener('click', (e) => {
      if (e.target === dom.settingsModal) dom.settingsModal.classList.remove('open');
    });

    setupSwipeGesture();
  }

  // ─── Swipe Gesture ───
  function setupSwipeGesture() {
    let startX = 0, startY = 0, swiping = false;

    document.addEventListener('touchstart', (e) => {
      if (state.isDesktop) return;
      const touch = e.touches[0];
      startX = touch.clientX;
      startY = touch.clientY;
      swiping = startX < 30;
    }, { passive: true });

    document.addEventListener('touchmove', (e) => {
      if (!swiping || state.isDesktop) return;
      const touch = e.touches[0];
      const dx = touch.clientX - startX;
      const dy = Math.abs(touch.clientY - startY);
      if (dy > 30) { swiping = false; return; }
      if (dx > 50) {
        openSidebar();
        swiping = false;
      }
    }, { passive: true });

    document.addEventListener('touchend', () => { swiping = false; }, { passive: true });
  }

  // ─── Sidebar ───
  function toggleSidebar() {
    state.sidebarOpen ? closeSidebar() : openSidebar();
  }

  function openSidebar() {
    state.sidebarOpen = true;
    dom.sidebar.classList.add('open');
    dom.sidebarOverlay.classList.add('open');
    renderConversationList();
  }

  function closeSidebar() {
    state.sidebarOpen = false;
    dom.sidebar.classList.remove('open');
    dom.sidebarOverlay.classList.remove('open');
  }

  // ─── Responsive ───
  function handleResize() {
    const wasDesktop = state.isDesktop;
    state.isDesktop = window.innerWidth >= 768;

    if (state.isDesktop && !wasDesktop) {
      closeSidebar();
    }
  }

  // ─── Conversations ───
  function createConversation() {
    if (state.isStreaming) return;

    const conv = {
      id: generateId(),
      title: '新对话',
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    state.conversations.unshift(conv);
    state.currentConvId = conv.id;
    saveState();

    renderConversationList();
    showWelcome();
    dom.chatTitle.textContent = 'ChatGPT';
    dom.messageInput.value = '';
    handleInputChange();
    dom.messageInput.focus();
  }

  function switchConversation(convId) {
    if (state.isStreaming) return;

    const conv = state.conversations.find(c => c.id === convId);
    if (!conv) return;

    state.currentConvId = convId;
    saveState();

    renderConversationList();
    renderMessages(conv);
    dom.chatTitle.textContent = conv.title;

    if (!state.isDesktop) closeSidebar();
    dom.messageInput.focus();
  }

  function deleteConversation(convId, e) {
    if (e) e.stopPropagation();
    if (state.isStreaming) return;

    showConfirm('确定要删除这个对话吗？', () => {
      const idx = state.conversations.findIndex(c => c.id === convId);
      if (idx === -1) return;

      state.conversations.splice(idx, 1);

      if (state.currentConvId === convId) {
        state.currentConvId = state.conversations.length > 0 ? state.conversations[0].id : null;
        if (state.currentConvId) {
          switchConversation(state.currentConvId);
        } else {
          showWelcome();
          dom.chatTitle.textContent = 'ChatGPT';
        }
      }

      saveState();
      renderConversationList();
      showToast('对话已删除');
    });
  }

  function renameConversation(convId, e) {
    if (e) e.stopPropagation();
    const conv = state.conversations.find(c => c.id === convId);
    if (!conv) return;

    const item = document.querySelector(`[data-conv-id="${convId}"]`);
    const titleEl = item?.querySelector('.conv-title');
    if (!titleEl) return;

    const input = document.createElement('input');
    input.className = 'rename-input';
    input.value = conv.title;
    titleEl.replaceWith(input);
    input.focus();
    input.select();

    const finish = () => {
      const newTitle = input.value.trim() || conv.title;
      conv.title = newTitle.substring(0, CONFIG.MAX_TITLE_LEN);
      conv.updatedAt = Date.now();
      saveState();
      renderConversationList();
      if (state.currentConvId === convId) {
        dom.chatTitle.textContent = conv.title;
      }
    };

    input.addEventListener('blur', finish);
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') { e.preventDefault(); input.blur(); }
      if (e.key === 'Escape') { input.value = conv.title; input.blur(); }
    });
  }

  // ─── Render ───
  function renderConversationList() {
    dom.conversationList.innerHTML = '';

    if (state.conversations.length === 0) {
      dom.conversationList.innerHTML = `
        <div class="empty-state">
          <p>暂无对话</p>
          <p style="font-size: 12px; margin-top: 4px;">点击上方按钮开始新对话</p>
        </div>`;
      return;
    }

    state.conversations.forEach(conv => {
      const item = document.createElement('div');
      item.className = 'conversation-item' + (conv.id === state.currentConvId ? ' active' : '');
      item.dataset.convId = conv.id;
      item.onclick = () => switchConversation(conv.id);

      const lastMsg = conv.messages.length > 0 ? conv.messages[conv.messages.length - 1] : null;
      const preview = lastMsg
        ? lastMsg.content.substring(0, 40) + (lastMsg.content.length > 40 ? '...' : '')
        : '开始新对话';

      item.innerHTML = `
        <span class="conv-icon">💬</span>
        <div class="conv-text">
          <div class="conv-title">${escapeHtml(conv.title)}</div>
          <div class="conv-preview">${escapeHtml(preview)}</div>
        </div>
        <div class="conv-actions">
          <button class="conv-action-btn rename" title="重命名" onclick="event.stopPropagation()">✏️</button>
          <button class="conv-action-btn delete" title="删除" onclick="event.stopPropagation()">🗑️</button>
        </div>`;

      item.querySelector('.conv-action-btn.rename').addEventListener('click', (e) => renameConversation(conv.id, e));
      item.querySelector('.conv-action-btn.delete').addEventListener('click', (e) => deleteConversation(conv.id, e));

      dom.conversationList.appendChild(item);
    });
  }

  function showWelcome() {
    dom.messagesContainer.innerHTML = `
      <div class="welcome-screen">
        <div class="welcome-logo">✦</div>
        <h2>有什么可以帮你的？</h2>
        <div class="suggestions">
          <button class="suggestion-pill" data-text="帮我解释一下 JavaScript 的闭包">解释 JavaScript 闭包</button>
          <button class="suggestion-pill" data-text="写一个简单的待办事项应用">写一个待办事项应用</button>
          <button class="suggestion-pill" data-text="推荐一些学习编程的资源">推荐编程学习资源</button>
          <button class="suggestion-pill" data-text="帮我写一首关于夏天的诗">写一首关于夏天的诗</button>
        </div>
      </div>`;

    dom.messagesContainer.querySelectorAll('.suggestion-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        dom.messageInput.value = pill.dataset.text;
        handleInputChange();
        sendMessage();
      });
    });
  }

  function renderMessages(conv) {
    if (!conv || conv.messages.length === 0) {
      showWelcome();
      return;
    }

    let html = '<div class="messages-inner">';
    conv.messages.forEach(msg => {
      html += renderMessageHTML(msg);
    });
    html += '</div>';

    dom.messagesContainer.innerHTML = html;
    scrollToBottom();
    applyCodeHighlighting();
  }

  function renderMessageHTML(msg) {
    const isUser = msg.role === 'user';
    const avatar = isUser ? 'U' : '✦';
    const avatarClass = isUser ? 'user' : 'assistant';
    const rendered = renderMarkdown(msg.content);

    return `
      <div class="message ${avatarClass}">
        <div class="message-inner">
          <div class="message-avatar">${avatar}</div>
          <div class="message-body">${rendered}</div>
        </div>
        ${!isUser ? `<div class="message-inner">
          <div style="width:28px;flex-shrink:0"></div>
          <div class="message-actions">
            <button class="msg-action-btn copy-msg" title="复制">📋</button>
            <button class="msg-action-btn" title="重新生成">🔄</button>
          </div>
        </div>` : ''}
      </div>`;
  }

  // ─── Markdown Rendering ───
  function renderMarkdown(text) {
    if (!text) return '';

    if (typeof marked !== 'undefined') {
      marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });
      let html = marked.parse(text);

      // Add copy buttons to code blocks
      html = html.replace(/<pre><code(.*?)>([\s\S]*?)<\/code><\/pre>/g, (match, attrs, code) => {
        const langMatch = attrs.match(/class="language-(\w+)"/);
        const lang = langMatch ? langMatch[1] : 'code';
        return `<div class="code-header"><span>${escapeHtml(lang)}</span><button class="copy-btn" onclick="copyCode(this)">复制</button></div><pre><code${attrs}>${code}</code></pre>`;
      });

      return html;
    }

    // Fallback simple markdown
    let html = escapeHtml(text);
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (m, lang, code) =>
      `<div class="code-header"><span>${lang||'code'}</span><button class="copy-btn" onclick="copyCode(this)">复制</button></div><pre><code class="language-${lang||'code'}">${code.trim()}</code></pre>`);
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');
    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');
    html = html.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>');
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
    html = html.replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>');
    html = html.replace(/\n\n/g, '</p><p>');
    html = html.replace(/\n/g, '<br>');
    return `<p>${html}</p>`;
  }

  function applyCodeHighlighting() {
    if (typeof hljs !== 'undefined') {
      document.querySelectorAll('pre code').forEach(block => hljs.highlightElement(block));
    }
  }

  // ─── Messaging ───
  function sendMessage() {
    const text = dom.messageInput.value.trim();
    if (!text || state.isStreaming) return;

    if (!state.currentConvId) createConversation();
    const conv = getCurrentConv();
    if (!conv) return;

    const userMsg = { id: generateId(), role: 'user', content: text, timestamp: Date.now() };
    conv.messages.push(userMsg);

    if (conv.messages.length === 1) {
      conv.title = text.substring(0, CONFIG.MAX_TITLE_LEN);
      dom.chatTitle.textContent = conv.title;
    }
    conv.updatedAt = Date.now();

    dom.messageInput.value = '';
    handleInputChange();
    renderConversationList();
    appendMessage(userMsg);
    scrollToBottom();
    simulateAIResponse(conv);
  }

  function appendMessage(msg) {
    const welcomeScreen = dom.messagesContainer.querySelector('.welcome-screen');
    if (welcomeScreen) {
      dom.messagesContainer.innerHTML = '<div class="messages-inner"></div>';
    }

    let inner = dom.messagesContainer.querySelector('.messages-inner');
    if (!inner) {
      inner = document.createElement('div');
      inner.className = 'messages-inner';
      dom.messagesContainer.appendChild(inner);
    }

    const temp = document.createElement('div');
    temp.innerHTML = renderMessageHTML(msg);
    inner.appendChild(temp.firstElementChild);
    scrollToBottom();
  }

  function getProviderConfig() {
    const provider = localStorage.getItem(PROVIDER_KEY) || 'openai';
    const config = { ...API_PROVIDERS[provider] };
    
    if (provider === 'custom') {
      config.url = localStorage.getItem(CUSTOM_URL_KEY) || '';
      if (!config.url) throw new Error('请先设置自定义 API 地址');
    }
    
    return { provider, config };
  }

  async function callOpenAI(messages, apiKey, model) {
    const { provider, config } = getProviderConfig();
    
    // Anthropic 使用不同的 API 格式
    if (config.isAnthropic) {
      return await callAnthropicAPI(messages, apiKey, model, config);
    }
    
    // Gemini 使用不同的 API 格式
    if (config.isGemini) {
      return await callGeminiAPI(messages, apiKey, model, config);
    }
    
    // OpenAI 兼容格式（OpenAI、DeepSeek、自定义）
    const response = await fetch(config.url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: model,
        messages: messages,
        stream: true,
        temperature: 0.7,
        max_tokens: 2048
      })
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.error?.message || `API 请求失败: ${response.status}`);
    }

    return response;
  }

  async function callAnthropicAPI(messages, apiKey, model, config) {
    // Anthropic API 格式
    const systemMsg = messages.find(m => m.role === 'system');
    const userMessages = messages.filter(m => m.role !== 'system');
    
    const response = await fetch(config.url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': apiKey,
        'anthropic-version': '2023-06-01'
      },
      body: JSON.stringify({
        model: model,
        max_tokens: 2048,
        system: systemMsg?.content || '',
        messages: userMessages.map(m => ({
          role: m.role,
          content: m.content
        })),
        stream: true
      })
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.error?.message || `Anthropic API 请求失败: ${response.status}`);
    }

    // 包装为 OpenAI 流式格式
    return {
      body: {
        getReader: () => {
          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';
          return {
            read: async () => {
              const { done, value } = await reader.read();
              if (done) return { done: true, value: undefined };
              buffer += decoder.decode(value, { stream: true });
              const lines = buffer.split('\n');
              buffer = lines.pop() || '';
              let content = '';
              for (const line of lines) {
                if (line.startsWith('data: ')) {
                  try {
                    const data = JSON.parse(line.slice(6));
                    if (data.type === 'content_block_delta') {
                      content = data.delta?.text || '';
                    }
                  } catch (e) {}
                }
              }
              return {
                done: false,
                value: new TextEncoder().encode(content ? `data: {"choices":[{"delta":{"content":"${content}"}}]}\n` : '')
              };
            }
          };
        }
      }
    };
  }

  async function callGeminiAPI(messages, apiKey, model, config) {
    // Gemini API 格式
    const url = `${config.url}${model}:streamGenerateContent?key=${apiKey}`;
    
    const contents = messages.filter(m => m.role !== 'system').map(m => ({
      role: m.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: m.content }]
    }));
    
    const systemMsg = messages.find(m => m.role === 'system');
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        contents: contents,
        systemInstruction: systemMsg ? { parts: [{ text: systemMsg.content }] } : undefined,
        generationConfig: {
          temperature: 0.7,
          maxOutputTokens: 2048
        }
      })
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.error?.message || `Gemini API 请求失败: ${response.status}`);
    }

    // 包装为 OpenAI 流式格式
    return {
      body: {
        getReader: () => {
          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';
          return {
            read: async () => {
              const { done, value } = await reader.read();
              if (done) return { done: true, value: undefined };
              buffer += decoder.decode(value, { stream: true });
              let content = '';
              try {
                const json = JSON.parse(buffer);
                content = json.candidates?.[0]?.content?.parts?.[0]?.text || '';
                buffer = '';
              } catch (e) {}
              return {
                done: false,
                value: new TextEncoder().encode(content ? `data: {"choices":[{"delta":{"content":"${content}"}}]}\n` : '')
              };
            }
          };
        }
      }
    };
  }

  async function streamResponse(response, onChunk) {
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const data = line.slice(6);
          if (data === '[DONE]') return;
          try {
            const parsed = JSON.parse(data);
            const content = parsed.choices?.[0]?.delta?.content;
            if (content) onChunk(content);
          } catch (e) {
            // Skip invalid JSON
          }
        }
      }
    }
  }

  async function simulateAIResponse(conv) {
    state.isStreaming = true;
    const apiKey = localStorage.getItem(CONFIG.API_KEY);
    const model = localStorage.getItem(CONFIG.MODEL_KEY) || 'gpt-3.5-turbo';

    if (!apiKey) {
      showToast('请先设置 API Key', 'error');
      dom.apiKeyModal.classList.add('open');
      state.isStreaming = false;
      return;
    }

    const assistantMsg = { id: generateId(), role: 'assistant', content: '', timestamp: Date.now() };
    conv.messages.push(assistantMsg);

    // Show typing indicator
    const inner = dom.messagesContainer.querySelector('.messages-inner');
    const typingEl = document.createElement('div');
    typingEl.className = 'message assistant';
    typingEl.innerHTML = `
      <div class="message-inner">
        <div class="message-avatar">✦</div>
        <div class="message-body">
          <div class="typing-indicator"><span></span><span></span><span></span></div>
        </div>
      </div>`;
    inner.appendChild(typingEl);
    scrollToBottom();

    try {
      // Prepare messages for API
      const apiMessages = [
        { role: 'system', content: '你是 ChatGPT，一个由 OpenAI 训练的 AI 助手。请用中文回答用户的问题，提供准确、有帮助的回复。' },
        ...conv.messages.map(m => ({ role: m.role, content: m.content }))
      ];

      const response = await callOpenAI(apiMessages, apiKey, model);

      typingEl.remove();

      // Create message element for streaming
      const msgEl = document.createElement('div');
      msgEl.className = 'message assistant';
      msgEl.innerHTML = `
        <div class="message-inner">
          <div class="message-avatar">✦</div>
          <div class="message-body"></div>
        </div>
        <div class="message-inner">
          <div style="width:28px;flex-shrink:0"></div>
          <div class="message-actions">
            <button class="msg-action-btn copy-msg" title="复制">📋</button>
            <button class="msg-action-btn" title="重新生成">🔄</button>
          </div>
        </div>`;
      inner.appendChild(msgEl);

      const bodyEl = msgEl.querySelector('.message-body');

      // Stream the response
      await streamResponse(response, (chunk) => {
        assistantMsg.content += chunk;
        bodyEl.innerHTML = renderMarkdown(assistantMsg.content) + '<span class="streaming-cursor"></span>';
        scrollToBottom();
      });

      // Final render
      bodyEl.innerHTML = renderMarkdown(assistantMsg.content);
      applyCodeHighlighting();

      // Copy button
      const copyBtn = msgEl.querySelector('.copy-msg');
      if (copyBtn) {
        copyBtn.addEventListener('click', () => {
          navigator.clipboard.writeText(assistantMsg.content).then(() => showToast('已复制到剪贴板'));
        });
      }

      conv.updatedAt = Date.now();
      saveState();

    } catch (error) {
      typingEl.remove();
      showToast(error.message || '请求失败，请检查 API Key', 'error');
      
      // Remove the failed assistant message
      const idx = conv.messages.findIndex(m => m.id === assistantMsg.id);
      if (idx !== -1) conv.messages.splice(idx, 1);
    } finally {
      state.isStreaming = false;
      renderConversationList();
    }
  }

  // ─── Input Handling ───
  function handleInputChange() {
    dom.sendBtn.disabled = !dom.messageInput.value.trim();
    dom.messageInput.style.height = 'auto';
    dom.messageInput.style.height = Math.min(dom.messageInput.scrollHeight, 150) + 'px';
  }

  function handleInputKeydown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  // ─── Theme ───
  function setTheme(theme) {
    document.body.classList.toggle('light', theme === 'light');
    dom.themeDark.classList.toggle('active', theme === 'dark');
    dom.themeLight.classList.toggle('active', theme === 'light');
    localStorage.setItem(CONFIG.THEME_KEY, theme);
  }

  // ─── API Key ───
  function saveApiKeyFromModal(input, modal) {
    const key = input.value.trim();
    if (key) {
      localStorage.setItem(CONFIG.API_KEY, key);
      modal.classList.remove('open');
      showToast('API Key 已保存');
    }
  }

  // ─── Clear All ───
  function clearAllConversations() {
    if (state.isStreaming) return;
    showConfirm('确定要清除所有对话吗？此操作不可撤销。', () => {
      state.conversations = [];
      state.currentConvId = null;
      saveState();
      renderConversationList();
      showWelcome();
      dom.chatTitle.textContent = 'ChatGPT';
      dom.settingsModal.classList.remove('open');
      showToast('所有对话已清除');
    });
  }

  // ─── Helpers ───
  function getCurrentConv() {
    return state.conversations.find(c => c.id === state.currentConvId);
  }

  function generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substring(2, 8);
  }

  function escapeHtml(text) {
    const d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
  }

  function sleep(ms) {
    return new Promise(r => setTimeout(r, ms));
  }

  function scrollToBottom() {
    requestAnimationFrame(() => {
      dom.messagesContainer.scrollTop = dom.messagesContainer.scrollHeight;
    });
  }

  function showToast(message) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2500);
  }

  function showConfirm(message, onConfirm) {
    const overlay = document.createElement('div');
    overlay.className = 'confirm-overlay';
    overlay.innerHTML = `
      <div class="confirm-box">
        <p>${escapeHtml(message)}</p>
        <div class="confirm-actions">
          <button class="confirm-cancel">取消</button>
          <button class="confirm-ok">确定</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);

    overlay.querySelector('.confirm-cancel').addEventListener('click', () => overlay.remove());
    overlay.querySelector('.confirm-ok').addEventListener('click', () => { overlay.remove(); onConfirm(); });
    overlay.addEventListener('click', (e) => { if (e.target === overlay) overlay.remove(); });
  }

  // ─── Global: Copy Code ───
  window.copyCode = function(btn) {
    const pre = btn.closest('.code-header')?.nextElementSibling || btn.parentElement.nextElementSibling;
    const code = pre?.querySelector('code');
    if (!code) return;
    navigator.clipboard.writeText(code.textContent).then(() => {
      const original = btn.textContent;
      btn.textContent = '已复制 ✓';
      setTimeout(() => btn.textContent = original, 2000);
    });
  };

  // ─── Start ───
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

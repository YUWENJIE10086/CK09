<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, ChatDotRound, Refresh, DataAnalysis, Setting } from '@element-plus/icons-vue'

interface Message {
  role: 'user' | 'assistant'
  content: string
  time: string
}

const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref<HTMLElement>()

// API Key仅保留在当前页面内存中，不写入浏览器持久存储。
localStorage.removeItem('dify_api_key')
const DIFY_API_KEY = ref('')
const conversationId = ref('')

// 快捷问题
const quickQuestions = [
  '烤房列表界面空闲的有多少个烤房？',
  '项目编号为25-420527KF00099的烤房的所有数据信息',
  '哪些烤房健康分低于60？',
  '剩余寿命不足5年的烤房有哪些？',
  '健康等级为危险的烤房有哪些？',
  '各县的烤房数量分布是怎样的？',
]

// ======================== 本地回退查询 ========================

/**
 * 分析用户问题并调用本地后端API获取数据
 * 当Dify API超时或无响应时自动触发
 */
async function localQuery(question: string): Promise<string> {
  const q = question.trim()

  // 1. 查询空闲烤房数量
  if ((q.includes('空闲') || q.includes('闲置')) && (q.includes('多少') || q.includes('几个') || q.includes('数量') || q.includes('count'))) {
    try {
      const resp = await fetch('/dev-api/barn/ai-query/overview')
      const data = await resp.json()
      const idle = data.use_status_dist?.['空闲'] || 0
      const baking = data.use_status_dist?.['在烤'] || 0
      const total = data.total || 0
      return `根据系统数据库查询结果：\n\n📊 烤房使用状态统计：\n• 烤房总数：${total} 栋\n• 空闲烤房：${idle} 栋\n• 在烤烤房：${baking} 栋\n\n目前系统中共有 ${idle} 栋空闲烤房，占烤房总数的 ${(idle / total * 100).toFixed(1)}%。`
    } catch {
      return '查询空闲烤房数量时出错，请稍后重试。'
    }
  }

  // 2. 按项目编号查询烤房详情
  const projectIdMatch = q.match(/(\d{2}-\d{9,}KF\d+)/) || q.match(/(25-\d+KF\d+)/)
  if (projectIdMatch || (q.includes('项目编号') && q.includes('烤房'))) {
    const projectId = projectIdMatch ? projectIdMatch[1] : ''
    if (!projectId) return '请提供有效的烤房项目编号，例如：25-420527KF00099'
    try {
      const resp = await fetch(`/dev-api/barn/ai-query/detail?projectId=${projectId}`)
      const data = await resp.json()
      if (!data.found) {
        return `未找到项目编号为 ${projectId} 的烤房记录。请确认项目编号是否正确。`
      }
      const d = data.data
      const lines = [
        `项目编号为 ${projectId} 的烤房详细信息如下：\n`,
        `📋 基本信息`,
        `• 项目编号：${d.project_id}`,
        `• 所在地区：${d.city} ${d.county} ${d.township} ${d.village}`,
        `• 详细地址：${d.detail_address}`,
        `• 项目类型：${d.project_type}`,
        `• 建设方式：${d.build_method}`,
        `• 技术员：${d.technician_name}`,
        ``,
        `📈 健康与寿命`,
        `• 使用状态：${d.use_status}`,
        `• 设施现状：${d.facility_status}`,
        `• 健康分：${d.health_score}`,
        `• 剩余寿命：${d.life_residual} 年`,
        `• 健康算法：${d.current_health_algo}`,
        `• 寿命算法：${d.current_life_algo}`,
        ``,
        `📅 时间信息`,
        `• 开工日期：${d.start_date || '未记录'}`,
        `• 竣工日期：${d.finish_date || '未记录'}`,
        ``,
        `📍 地理信息`,
        `• 经度：${d.longitude}`,
        `• 纬度：${d.latitude}`,
        `• 海拔：${d.altitude} 米`,
        ``,
        `🔧 运行数据`,
        `• 烤房健康分(ovens)：${d.oven_health_score ?? '-'}`,
        `• 当前温度：${d.temperature ?? '-'} °C`,
        `• 当前湿度：${d.humidity ?? '-'} %`,
        `• 使用次数：${d.usage_count ?? 0} 次`,
        `• 项目造价：¥${d.project_cost ?? '-'}`,
      ]
      return lines.join('\n')
    } catch {
      return `查询项目编号 ${projectId} 的烤房信息时出错，请稍后重试。`
    }
  }

  // 3. 健康分低于某个值
  if (q.includes('健康分') && (q.includes('低于') || q.includes('小于') || q.includes('不足'))) {
    try {
      const resp = await fetch('/dev-api/barn/ai-query/barns?healthLevel=低于60&limit=100')
      const data = await resp.json()
      const total = data.total || 0
      const list = data.data || []
      let result = `健康分低于60的烤房共 ${total} 栋，以下是部分列表：\n\n`
      list.slice(0, 10).forEach((item: any, i: number) => {
        result += `${i + 1}. ${item.project_id} | ${item.county}${item.township} | 健康分：${item.health_score}\n`
      })
      if (total > 10) result += `\n...共 ${total} 栋，仅显示前10栋。`
      return result
    } catch {
      return '查询健康分低的烤房时出错，请稍后重试。'
    }
  }

  // 4. 寿命不足
  if ((q.includes('寿命') || q.includes('剩余')) && (q.includes('不足') || q.includes('低于') || q.includes('小于'))) {
    try {
      const condition = q.includes('5') ? '不足5' : q.includes('3') ? '不足3' : '不足10'
      const resp = await fetch(`/dev-api/barn/ai-query/barns?lifeCondition=${encodeURIComponent(condition)}&limit=100`)
      const data = await resp.json()
      const total = data.total || 0
      const list = data.data || []
      let result = `剩余寿命${condition}年的烤房共 ${total} 栋，以下是部分列表：\n\n`
      list.slice(0, 10).forEach((item: any, i: number) => {
        result += `${i + 1}. ${item.project_id} | ${item.county}${item.township} | 剩余寿命：${item.life_residual}年\n`
      })
      if (total > 10) result += `\n...共 ${total} 栋，仅显示前10栋。`
      return result
    } catch {
      return '查询剩余寿命不足的烤房时出错，请稍后重试。'
    }
  }

  // 5. 健康等级为危险
  if (q.includes('危险') || q.includes('健康等级')) {
    try {
      const resp = await fetch('/dev-api/barn/ai-query/barns?healthLevel=危险&limit=100')
      const data = await resp.json()
      const total = data.total || 0
      const list = data.data || []
      let result = `健康等级为"危险"的烤房共 ${total} 栋，以下是部分列表：\n\n`
      list.slice(0, 10).forEach((item: any, i: number) => {
        result += `${i + 1}. ${item.project_id} | ${item.county}${item.township} | 健康分：${item.health_score}\n`
      })
      if (total > 10) result += `\n...共 ${total} 栋，仅显示前10栋。`
      return result
    } catch {
      return '查询危险等级烤房时出错，请稍后重试。'
    }
  }

  // 6. 各县分布
  if (q.includes('各县') || q.includes('分布') || q.includes('统计')) {
    try {
      const resp = await fetch('/dev-api/barn/ai-query/stats')
      const data = await resp.json()
      let result = `各县烤房数量分布统计：\n\n`
      result += `烤房总数：${data.grand_total} 栋\n`
      result += `在烤：${data.grand_baking} 栋 | 空闲：${data.grand_idle} 栋\n\n`
      data.counties.forEach((c: any, i: number) => {
        result += `${i + 1}. ${c.county}：${c.total}栋（在烤${c.baking}，空闲${c.idle}，平均健康分${c.avg_health}）\n`
      })
      return result
    } catch {
      return '查询各县烤房分布时出错，请稍后重试。'
    }
  }

  // 默认回复
  return `您好！我是烤房管理AI助手。您的问题我已收到，但目前无法通过AI模型直接回答。\n\n您可以尝试以下问题：\n• 烤房列表界面空闲的有多少个烤房？\n• 项目编号为25-420527KF00099的烤房的所有数据信息\n• 哪些烤房健康分低于60？\n• 各县的烤房数量分布是怎样的？`
}

// 发送消息
async function sendMessage(text?: string) {
  const content = text || inputMessage.value.trim()
  if (!content) return

  messages.value.push({
    role: 'user',
    content,
    time: new Date().toLocaleTimeString(),
  })
  inputMessage.value = ''
  loading.value = true

  // 添加AI回复占位 - 使用普通对象确保响应式
  const aiIndex = messages.value.length
  messages.value.push({
    role: 'assistant' as const,
    content: '',
    time: new Date().toLocaleTimeString(),
  })

  await nextTick()
  scrollToBottom()

  // 先尝试Dify API（设置5秒超时），超时后使用本地数据库查询
  let difyAnswered = false

  try {
    if (!DIFY_API_KEY.value) {
      throw new Error('未配置Dify API Key')
    }
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 5000)

    const response = await fetch(`/dify-api/chat-messages`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${DIFY_API_KEY.value}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        inputs: {},
        query: content,
        response_mode: 'streaming',
        conversation_id: conversationId.value,
        user: 'web-user',
      }),
      signal: controller.signal,
    })

    clearTimeout(timeoutId)

    if (!response.ok) {
      throw new Error(`API错误: ${response.status}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    const startTime = Date.now()
    let gotContent = false

    while (reader) {
      // 使用Promise.race防止reader.read()永久阻塞
      const readPromise = reader.read()
      const timeoutPromise = new Promise<{ done: boolean; value: Uint8Array | undefined }>((resolve) =>
        setTimeout(() => resolve({ done: true, value: undefined }), 3000)
      )

      const { done, value } = await Promise.race([readPromise, timeoutPromise])
      if (done || !value) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const data = JSON.parse(line.slice(6))
            if (data.event === 'message' || data.event === 'agent_message') {
              const answer = data.answer || ''
              if (answer) {
                gotContent = true
                messages.value[aiIndex].content += answer
                await nextTick()
                scrollToBottom()
              }
            }
            if (data.conversation_id) {
              conversationId.value = data.conversation_id
            }
          } catch {
            // 忽略解析错误的行
          }
        }
      }

      // 超过5秒还没收到实际内容，放弃Dify
      if (!gotContent && Date.now() - startTime > 5000) {
        reader.cancel()
        break
      }
    }

    if (gotContent && messages.value[aiIndex].content) {
      difyAnswered = true
    }
  } catch (e: any) {
    // Dify API失败，使用本地查询
  }

  // 如果Dify没有返回内容，使用本地后端API查询
  if (!difyAnswered) {
    try {
      messages.value[aiIndex].content = '正在查询系统数据库...'
      await nextTick()
      scrollToBottom()

      await new Promise(r => setTimeout(r, 300))

      const result = await localQuery(content)

      // 逐字显示效果
      messages.value[aiIndex].content = ''
      const chunks = result.match(/[\s\S]{1,5}/g) || []
      for (let i = 0; i < chunks.length; i++) {
        messages.value[aiIndex].content += chunks[i]
        if (i % 5 === 0) {
          await nextTick()
          scrollToBottom()
        }
      }
      await nextTick()
      scrollToBottom()
    } catch (e: any) {
      messages.value[aiIndex].content = `查询失败：${e.message}\n\n请检查后端服务是否正常运行。`
    }
  }

  loading.value = false
  await nextTick()
  scrollToBottom()
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

function clearChat() {
  messages.value = []
  conversationId.value = ''
}

// 设置弹窗
const showSettings = ref(false)
const tempKey = ref('')

function openSettings() {
  tempKey.value = DIFY_API_KEY.value
  showSettings.value = true
}

function saveSettings() {
  DIFY_API_KEY.value = tempKey.value.trim()
  showSettings.value = false
  ElMessage.success('密钥已在当前会话中生效')
}

onMounted(() => {
  if (!DIFY_API_KEY.value) {
    messages.value.push({
      role: 'assistant',
      content:
        '您好！我是烤房管理AI助手。可以回答关于烤房数量、健康状态、使用寿命等问题。\n\n请先点击右上角"设置"按钮，配置Dify API地址和密钥。',
      time: new Date().toLocaleTimeString(),
    })
  } else {
    messages.value.push({
      role: 'assistant',
      content:
        '您好！我是烤房管理AI助手。可以问我关于烤房的任何问题，例如：\n\n• 秭归县有多少空闲的烤房？\n• 哪些烤房健康分低于60？\n• 剩余寿命不足5年的烤房有哪些？',
      time: new Date().toLocaleTimeString(),
    })
  }
})
</script>

<template>
  <div class="ai-assistant">
    <!-- 页面标题栏 -->
    <div class="ai-header">
      <div class="ai-header-left">
        <div class="ai-header-icon">
          <el-icon :size="22" color="#FFFFFF"><ChatDotRound /></el-icon>
        </div>
        <div class="ai-header-title-group">
          <span class="ai-header-title">AI 助手</span>
          <span class="ai-header-subtitle">烤房管理智能问答</span>
        </div>
      </div>
      <div class="ai-header-actions">
        <el-button :icon="Refresh" plain @click="clearChat">清空对话</el-button>
        <el-button :icon="Setting" type="primary" @click="openSettings">设置</el-button>
      </div>
    </div>

    <!-- 主体内容：左侧对话 + 右侧快捷问题 -->
    <div class="ai-body">
      <!-- 左侧聊天区域 -->
      <div class="chat-panel">
        <div ref="chatContainer" class="chat-scroll">
          <div class="chat-list">
            <div
              v-for="(msg, idx) in messages"
              :key="idx"
              class="chat-item"
              :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
            >
              <div class="chat-avatar">
                <el-icon v-if="msg.role === 'user'" :size="20" color="#FFFFFF">
                  <Promotion />
                </el-icon>
                <el-icon v-else :size="20" color="#FFFFFF">
                  <ChatDotRound />
                </el-icon>
              </div>
              <div class="chat-bubble-wrap">
                <div class="chat-bubble">{{ msg.content }}</div>
                <div class="chat-time">{{ msg.time }}</div>
              </div>
            </div>

            <!-- 加载中提示 -->
            <div v-if="loading && messages.length && !messages[messages.length - 1].content" class="chat-typing">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="typing-text">AI 正在思考...</span>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="请输入您的问题，例如：秭归县有多少空闲的烤房？"
            @keydown.enter.exact.prevent="sendMessage()"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="loading"
            class="send-btn"
            @click="sendMessage()"
          >
            发送
          </el-button>
        </div>
      </div>

      <!-- 右侧快捷问题面板 -->
      <div class="quick-panel">
        <div class="quick-panel-header">
          <el-icon :size="18" color="#0D5D46"><DataAnalysis /></el-icon>
          <span class="quick-panel-title">快捷问题</span>
        </div>
        <div class="quick-panel-desc">点击下方问题快速提问</div>
        <div class="quick-scroll">
          <div class="quick-list">
            <button
              v-for="(q, idx) in quickQuestions"
              :key="idx"
              class="quick-item"
              :disabled="loading"
              @click="sendMessage(q)"
            >
              <span class="quick-item-index">{{ idx + 1 }}</span>
              <span class="quick-item-text">{{ q }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 设置弹窗 -->
    <el-dialog v-model="showSettings" title="Dify API 设置" width="520px">
      <div class="settings-form">
        <div class="settings-item">
          <label class="settings-label">API 密钥</label>
          <el-input
            v-model="tempKey"
            type="password"
            show-password
            placeholder="请输入Dify App API Key"
            clearable
          />
          <div class="settings-tip">密钥只在当前页面会话内使用，刷新页面后需重新输入。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ===== 页面根容器 ===== */
.ai-assistant {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #E8F5F0 0%, #F6FAF8 100%);
}

/* ===== 顶部标题栏 ===== */
.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 50%, #1A6B4F 100%);
  box-shadow: 0 2px 12px rgba(13, 93, 70, 0.18);
  flex-shrink: 0;
}

.ai-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.ai-header-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.ai-header-title-group {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.ai-header-title {
  font-size: 18px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 0.08em;
}

.ai-header-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.05em;
}

.ai-header-actions {
  display: flex;
  gap: 10px;
}

/* ===== 主体两栏 ===== */
.ai-body {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  min-height: 0;
  overflow: hidden;
}

/* ===== 左侧聊天面板 ===== */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 14px rgba(13, 93, 70, 0.08);
  border: 1px solid rgba(13, 93, 70, 0.08);
  min-width: 0;
  overflow: hidden;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.chat-list {
  padding: 20px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ===== 单条消息 ===== */
.chat-item {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.chat-item.is-user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.chat-item.is-assistant {
  align-self: flex-start;
}

.chat-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.is-user .chat-avatar {
  background: linear-gradient(135deg, #2E8B6A 0%, #1A6B4F 100%);
  box-shadow: 0 2px 8px rgba(46, 139, 106, 0.3);
}

.is-assistant .chat-avatar {
  background: linear-gradient(135deg, #0D5D46 0%, #0A4D3E 100%);
  box-shadow: 0 2px 8px rgba(13, 93, 70, 0.3);
}

.chat-bubble-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.is-user .chat-bubble-wrap {
  align-items: flex-end;
}

.is-assistant .chat-bubble-wrap {
  align-items: flex-start;
}

.chat-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.is-user .chat-bubble {
  background: linear-gradient(135deg, #0D5D46 0%, #1A6B4F 100%);
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 10px rgba(13, 93, 70, 0.25);
}

.is-assistant .chat-bubble {
  background: #F1F5F3;
  color: #1A2F2E;
  border-top-left-radius: 4px;
  border: 1px solid rgba(13, 93, 70, 0.06);
}

.chat-time {
  font-size: 11px;
  color: #9BB5AC;
  margin-top: 4px;
  padding: 0 4px;
}

/* ===== 打字动画 ===== */
.chat-typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
  align-self: flex-start;
  background: #F1F5F3;
  border-radius: 12px;
  border-top-left-radius: 4px;
  border: 1px solid rgba(13, 93, 70, 0.06);
}

.chat-typing .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2E8B6A;
  animation: typing-bounce 1.2s infinite ease-in-out;
}

.chat-typing .dot:nth-child(2) {
  animation-delay: 0.15s;
}

.chat-typing .dot:nth-child(3) {
  animation-delay: 0.3s;
}

.typing-text {
  margin-left: 6px;
  font-size: 12px;
  color: #2E8B6A;
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

/* ===== 输入区域 ===== */
.chat-input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid rgba(13, 93, 70, 0.08);
  background: #ffffff;
  flex-shrink: 0;
}

.chat-input-area :deep(.el-textarea__inner) {
  border-radius: 10px;
  border-color: rgba(13, 93, 70, 0.2);
  font-size: 14px;
  line-height: 1.6;
  padding: 8px 12px;
}

.chat-input-area :deep(.el-textarea__inner:focus) {
  border-color: #2E8B6A;
  box-shadow: 0 0 0 2px rgba(46, 139, 106, 0.15);
}

.send-btn {
  height: 40px;
  background: linear-gradient(135deg, #0D5D46 0%, #1A6B4F 100%);
  border: none;
  font-weight: 600;
  letter-spacing: 0.05em;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(13, 93, 70, 0.25);
}

.send-btn:hover {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
}

/* ===== 右侧快捷问题面板 ===== */
.quick-panel {
  width: 300px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 14px rgba(13, 93, 70, 0.08);
  border: 1px solid rgba(13, 93, 70, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.quick-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 18px 4px;
}

.quick-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0D5D46;
  letter-spacing: 0.05em;
}

.quick-panel-desc {
  padding: 0 18px 12px;
  font-size: 12px;
  color: #8CBCAC;
}

.quick-scroll {
  flex: 1;
  overflow-y: auto;
}

.quick-list {
  padding: 0 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(46, 139, 106, 0.3);
  border-radius: 8px;
  background: #ffffff;
  color: #0F3D34;
  font-size: 13px;
  line-height: 1.55;
  text-align: left;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.quick-item:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(13, 93, 70, 0.08) 0%, rgba(46, 139, 106, 0.05) 100%);
  border-color: #2E8B6A;
  color: #0D5D46;
  transform: translateX(2px);
  box-shadow: 0 2px 10px rgba(46, 139, 106, 0.15);
}

.quick-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quick-item-index {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: linear-gradient(135deg, #0D5D46 0%, #2E8B6A 100%);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
}

.quick-item:hover:not(:disabled) .quick-item-index {
  background: linear-gradient(135deg, #0A4D3E 0%, #1A6B4F 100%);
}

.quick-item-text {
  flex: 1;
  min-width: 0;
  font-weight: 500;
}

/* ===== 设置弹窗 ===== */
.settings-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.settings-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings-label {
  font-size: 14px;
  font-weight: 600;
  color: #0D5D46;
}

.settings-tip {
  font-size: 12px;
  color: #8CBCAC;
  line-height: 1.5;
}
</style>

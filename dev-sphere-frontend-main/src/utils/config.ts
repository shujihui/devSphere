// src/utils/config.ts

// 你的后端网关地址 (用于 HTTP 和 SSE)
// 使用相对路径，通过 Vite 代理转发，解决 HTTPS 混合内容问题
const API_BASE_URL = '/api'

// 你的 Netty 服务器地址 (用于 WebSocket)
// 使用相对路径，通过 Vite 代理转发
const WS_BASE_URL = '/ws-api'

export default {
  // Axios 使用的 API 基础路径
  API_BASE_PATH: API_BASE_URL,

  // HTTP/SSE 绝对路径 (现在是相对路径)
  API_ABSOLUTE_URL: API_BASE_URL,

  // WebSocket 绝对路径 (现在是相对路径)
  // 最终会拼接成 /ws-api/ws，浏览器会自动识别为 wss://host/ws-api/ws
  WEBSOCKET_URL: `${WS_BASE_URL}/ws`,

  // SSE 连接绝对路径
  SSE_URL: `${API_BASE_URL}/devSphere/notice/userConnect`
}
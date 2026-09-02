// src/assets/ts/index.ts
import request from '../ts/request'

export const chatApi = {
  /** 侧边栏对话列表 */
  getList: (userEmail: string) =>
    request.get('/api/chat/list', { params: { userEmail } }),

  /** 新建对话框o*/
  create: (data: { id: string; userEmail: string; title: string }) =>
    request.post('/api/chat/create', data),

  /** 修改标题o*/
  updateTitle: (id: string, title: string) =>
    request.post('/api/chat/updateTitle', { id, title }),

  /** 历史消息 */
  getMessages: (conversationId: string) =>
    request.get('/api/chat/messages', { params: { conversationId } }),

  /** 删除单个对话o*/
  delete: (id: string) =>
    request.delete(`/api/chat/${id}`),

  /** 清空用户全部对话o*/
  clearAll: (userEmail: string) =>
    request.delete('/api/chat/clearAll', { params: { userEmail } }),

  /** 清空临时对话的记忆 o*/
  clearMemory: (sessionId: string) =>
    request.delete(`/api/chat/memory/${sessionId}`),


}

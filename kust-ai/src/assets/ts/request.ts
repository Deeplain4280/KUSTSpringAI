// src/assets/ts/request.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../../router'

const request = axios.create({ baseURL: '', timeout: 30000 })

request.interceptors.response.use(
  (res) => {
    const body = res.data
    // 后端统一 Result：code !== 200/201/204 视为业务失败
    if (body && typeof body.code === 'number' && ![200, 201, 204].includes(body.code)) {
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) { localStorage.clear(); router.push('/') }
      return Promise.reject(body)
    }
    return body            // ← 返回完整 Result，组件用 res.data 取数据
  },
  (err) => {
    ElMessage.error(err?.response?.data?.message || err.message || '网络异常')
    return Promise.reject(err)
  }
)
export default request
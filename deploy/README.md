# KUSTSpringAI 生产部署指南（Nginx 反向代理 · 仅 HTTP）

> 架构：**Nginx 托管前端静态文件（`kust-ai/dist`）+ 反向代理 `/api` 到 Spring Boot（127.0.0.1:8080）**
> 前端 axios 为同源请求，无 CORS 问题；后端已改为只监听回环地址，公网入口只有 Nginx。

```
浏览器 ──HTTP 80──> Nginx ──静态文件──> kust-ai/dist
                        └──/api 反代──> Spring Boot 127.0.0.1:8080 ──> MySQL / Redis / 阿里百炼
```

---

## 1. 前置条件（服务器）

| 组件 | 要求 | 说明 |
| ---- | ---- | ---- |
| JDK | 25 | 后端运行环境（Temurin/OpenJDK 25） |
| MySQL | 5.7+/8.x | 对话记忆持久化，先建库 `kust_spring_ai` |
| Redis | 任意 | 若后端降级逻辑需要（可暂不装，代码有降级） |
| Nginx | 1.18+ | 反向代理 |
| Node.js | ≥22.18（项目要求） | 仅构建前端用，服务器可不装 |

服务器目录约定（可自行调整，需同步改 nginx 配置与 systemd）：

```
/opt/kust-ai-app/                # 后端：jar + application.properties
/opt/kust-ai/dist/               # 前端构建产物
/etc/kust-ai/kust-ai.env         # 环境变量密钥（chmod 600）
/etc/nginx/conf.d/kust-ai.conf   # Nginx 配置
```

---

## 2. 构建（可在本地/CI 完成，产物拷到服务器）

**后端**（需 JDK 25 + Maven；项目自带 mvnw）：

```bash
./mvnw clean package -DskipTests
# 产物：target/KUSTSpringAI-0.0.1-SNAPSHOT.jar
```

**前端**（需要 Node ≥ 22.18）：

```bash
cd kust-ai
npm ci            # 或 npm install
npm run build
# 产物：kust-ai/dist/
```

> 前端构建产物 `dist` 已 gitignore，务必用 `scp`/`rsync` 单独上传。

---

## 3. 服务器配置

### 3.1 放置文件

```bash
sudo mkdir -p /opt/kust-ai-app /opt/kust-ai /etc/kust-ai
sudo cp KUSTSpringAI-0.0.1-SNAPSHOT.jar /opt/kust-ai-app/
# 关键：jar 同目录放一份外部配置，覆盖 jar 内置的默认值
sudo cp src/main/resources/application.properties /opt/kust-ai-app/
# 前端
sudo cp -r kust-ai/dist /opt/kust-ai/
```

### 3.2 密钥环境变量

```bash
sudo cp deploy/kust-ai.env.example /etc/kust-ai/kust-ai.env
sudo chmod 600 /etc/kust-ai/kust-ai.env
sudo vim /etc/kust-ai/kust-ai.env     # 填 AI_API_KEY / DB_PASSWORD / MAIL_PASSWORD
```

> 若不使用 systemd 的 `EnvironmentFile`，也可直接在 `application.properties` 里改默认值。
> 建议同时在服务器上修改 `/opt/kust-ai-app/application.properties` 中的数据库密码，与 jar 内置默认值隔离。

### 3.3 创建运行用户（可选但推荐）

```bash
sudo useradd -r -s /sbin/nologin kustai
sudo chown -R kustai:kustai /opt/kust-ai-app
```

---

## 4. 启动后端（systemd，推荐）

```bash
sudo cp deploy/kust-ai.service /etc/systemd/system/kust-ai.service
sudo systemctl daemon-reload
sudo systemctl enable --now kust-ai
sudo systemctl status kust-ai
journalctl -u kust-ai -f          # 实时日志
```

手动启动（不用 systemd 时）：

```bash
cd /opt/kust-ai-app
set -a; . /etc/kust-ai/kust-ai.env; set +a
java -jar KUSTSpringAI-0.0.1-SNAPSHOT.jar
```

启动成功后本机自测：

```bash
curl http://127.0.0.1:8080/api/chat/list?userEmail=test
```

---

## 5. 配置 Nginx

```bash
sudo cp deploy/nginx-kust-ai.conf /etc/nginx/conf.d/kust-ai.conf
# 若 nginx.conf 的 http{} 层没有 server_tokens 等，可保持默认
sudo nginx -t
sudo systemctl reload nginx        # 或 nginx -s reload
```

防火墙只放行 80（HTTP）：`sudo ufw allow 80/tcp`（或云安全组）。

---

## 6. 部署后验证清单

```bash
# ① 首页
curl -I http://服务器IP/
# ② SPA 子路由直接访问不 404（history 模式回退）
curl -I http://服务器IP/chat
# ③ 后端接口经 Nginx 可达
curl 'http://服务器IP/api/chat/list?userEmail=test'
# ④ SSE 流式逐字输出（不缓冲）：应看到文字一段段实时出现
curl -N --max-time 20 'http://服务器IP/api/qwen/stream/memory?userInput=你好&sessionId=test'
# ⑤ 浏览器验证：聊天打字机效果流畅 / 上传 >1MB 图片不 413 / 长问题(>60s)不被掐断
```

---

## 7. 常见问题排查

| 现象 | 原因 | 处理 |
| ---- | ---- | ---- |
| 502 Bad Gateway | 后端没起来 / 端口不对 | `systemctl status kust-ai`、`curl 127.0.0.1:8080` |
| 子路由刷新 404 | SPA 回退缺失 | 检查 `location / { try_files $uri $uri/ /index.html; }` |
| 流式输出卡顿/不出字 | SSE 被 Nginx 缓冲 | 确认 `/api/` 里 `proxy_buffering off; proxy_cache off;` 已生效 |
| 上传 413 | 超过大小限制 | Nginx `client_max_body_size 12m` 与后端 multipart 对齐 |
| 长回答被 60s 掐断 | 超时不足 | `proxy_read_timeout 600s`；若仍断，检查后端异步超时/心跳 |
| AI 返回未授权 | API Key 错误 | 检查 `/etc/kust-ai/kust-ai.env` 的 `AI_API_KEY` |
| 登录发码失败 | 邮箱授权码错误 | 检查 `MAIL_USERNAME` / `MAIL_PASSWORD`（QQ 邮箱需 SMTP 授权码） |

---

## 8. 安全提醒（务必阅读）

1. `application.properties` 内含真实密钥，已被 gitignore，**不要提交/外发**；服务器上尽快通过环境变量覆盖。
2. 后端只监听 `127.0.0.1:8080`（`server.address` 已配置），请勿在公网直接暴露 8080。
3. AI 调用按 token 计费，建议给 `/api` 加 `limit_req` 限流（nginx 配置中有注释示例）。
4. 日志已默认 INFO，SQL 不再打印；排查问题时可临时开 DEBUG 后还原。
5. 公网正式使用建议上 HTTPS（Let's Encrypt 免费证书），本文按你的要求仅配置了 HTTP。

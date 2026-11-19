# CI-CD 与部署

## 1. 流水线全景

`.github/workflows/ci.yml`，四个 Job，前两个并行、后两个串行：

```
push(main / develop / sprint/**) 或 PR(main / develop)
        │
        ├──▶ backend  后端构建与验证
        │      1. 配置 JDK 21 (temurin) + Maven 缓存
        │      2. clean compile
        │      3. mvn test                     ← 领域层 + 应用层单测
        │      4. ArchitectureTest             ← 架构约束
        │      5. verify -DskipITs -Djacoco.skip=false   ← 覆盖率门禁 0.70
        │      6. verify -Dsurefire.skip=true  ← 集成测试（Testcontainers: MySQL + Redis）
        │      7. 归档测试报告（artifact: backend-test-reports）
        │
        └──▶ frontend 前端构建
               1. Node 20
               2. npm install --no-audit --no-fund
               3. npm run lint / typecheck / build
        │
        └──▶ images   容器镜像构建（仅 push 事件）
               1. docker build -f deploy/backend.Dockerfile  -t campus-overflow-backend:<sha>
               2. docker build -f deploy/frontend.Dockerfile -t campus-overflow-web:<sha>
        │
        └──▶ deploy   部署（配置 vars.DEPLOY_HOST 后生效，否则自动跳过）
               1. 登录镜像仓库（vars.REGISTRY + secrets.REGISTRY_USERNAME/PASSWORD）
               2. 推送镜像
               3. SSH 到测试机执行 docker compose pull && up -d
               4. Smoke Test：轮询 /actuator/health 直到 UP
```

要点：

- **阶段顺序就是质量策略**：先快后慢，单测挂了不会浪费 5 分钟拉容器；
- 集成测试独立成步，失败能一眼看出是“逻辑错”还是“环境/迁移错”；
- 镜像构建依赖前两个 Job 成功，坏代码不会产出镜像；
- 部署 Job 用 `vars.DEPLOY_HOST` **门控**：没配服务器就跳过，其他阶段照常绿。

## 2. 镜像怎么构建

| 文件 | 阶段 | 产物 |
| --- | --- | --- |
| `deploy/backend.Dockerfile` | 多阶段：Maven 构建 → JRE 21 运行 | 只含 jar 与运行时，镜像小 |
| `deploy/frontend.Dockerfile` | 多阶段：Node 构建 → Nginx 静态托管 | `dist/` + `nginx.conf` |
| `deploy/nginx.conf` | — | SPA fallback（刷新不 404）+ 反代 `/api` 到 backend |

## 3. compose 拓扑

`deploy/docker-compose.yml`（`name: campus-overflow`）：

| 服务 | 镜像 | 端口 | 说明 |
| --- | --- | --- | --- |
| `mysql` | `mysql:8.0.39` | 3306 | `ngram_token_size=2`、关闭停用词、UTC、healthcheck |
| `redis` | `redis:7-alpine` | 6379 | AOF 持久化、`allkeys-lru` 256MB |
| `backend` | 本地构建 | 8080 | 依赖 mysql/redis healthy；healthcheck 打 `/actuator/health/readiness` |
| `web` | 本地构建 | 80 | Nginx 托管前端 |
| `prometheus` | `prom/prometheus:v2.54.1` | 9090 | `profiles: [observability]` |
| `grafana` | `grafana/grafana:11.2.0` | 3000 | `profiles: [observability]` |

启动方式：

```bash
# 只起应用
docker compose -f deploy/docker-compose.yml up -d --build

# 应用 + 监控
docker compose -f deploy/docker-compose.yml --profile observability up -d --build
```

> 容器内默认 `SPRING_PROFILES=prod,demo`（`demo` 会写演示数据，正式环境去掉）。

## 4. 生产化必改的四个开关

| 开关 | 默认 | 上线前 |
| --- | --- | --- |
| `CO_FIELD_KEY` | 仓库内测试密钥 | **换成自己生成的密钥**，否则字段加密等于没加密 |
| `COOKIE_SECURE` | `false` | 走 HTTPS 必须 `true` |
| `SPRING_PROFILES` | `prod,demo` | 去掉 `demo` |
| 数据库密码 | `campus_dev_pwd` | 改强密码并放进 secrets |

## 5. 运维动作（详见 `docs/ops/operations-manual-v1.0.md`）

| 动作 | 命令 |
| --- | --- |
| 启动 | `docker compose -f deploy/docker-compose.yml up -d` |
| 停止 | `docker compose -f deploy/docker-compose.yml down`（加 `-v` 会删数据卷，慎用） |
| 查看状态 | `docker compose ps` + `curl localhost:8080/actuator/health` |
| 看日志 | `docker compose logs -f backend --tail 200` |
| 回滚 | 用上一个 commit sha 的镜像 tag 重新 `up -d`，或 `git checkout <sha> && up -d --build` |
| 备份 | `mysqldump` 导出 `campus_overflow`（脚本与恢复步骤见运维手册） |

**回滚优先于修 bug**：线上先恢复到上一个健康版本，再在本地复现。

## 6. 监控看什么

| 指标 | 含义 | 异常信号 |
| --- | --- | --- |
| `/actuator/health` | 存活与就绪 | 变 DOWN 立即回滚 |
| `co_reputation_mismatches` | 对账不平的账户数 | >0 说明账本与流水漂移 |
| `outbox_event.last_error` | 发件箱投递失败 | 堆积说明消费者报错 |
| HTTP 5xx 比例 | 服务端错误 | 突增看日志 |
| 发件箱积压量 | 事件处理是否跟得上 | 持续增长要扩容或修消费者 |

## 7. 常见流水线失败对照

| 报错 | 原因 | 处理 |
| --- | --- | --- |
| `ArchitectureTest` 失败 | 引入了越层或跨上下文依赖 | 看失败信息里的 FF-x，改依赖而不是改测试 |
| JaCoCo 门禁失败 | 新增代码没测试 | 补边界测试，别调门禁值 |
| 集成测试 `Could not find a valid Docker environment` | CI/本地没有 Docker | 本地跑 `-DskipITs`；CI 上检查 runner |
| 前端 `tsc` 报 `possibly undefined` | `noUncheckedIndexedAccess` | 加判空，不要 `as any` |
| deploy Job 被跳过 | 未配置 `vars.DEPLOY_HOST` | 正常行为；要真正部署再配 vars/secrets |

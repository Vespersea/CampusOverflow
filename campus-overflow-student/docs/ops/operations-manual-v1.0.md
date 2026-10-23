# CampusOverflow 运维手册 v1.0

> 适用范围：课程统一部署环境（校园网内，单机 Docker）与本地开发环境。生产数据每日备份，变更走 CI/CD 流水线。

## 1. 架构概览

```
                         ┌───────────────┐
     浏览器 ── 80 ──▶    │  web (nginx)  │ ── /api 反代 ──▶ backend (Spring Boot 3, :8080)
                         └───────────────┘                      │        │
      SPA 路由 fallback  try_files → index.html                  ▼        ▼
                                                          MySQL 8.0.39  Redis 7
      Prometheus (:9090) ── 抓取 /actuator/prometheus ◀── backend
      Grafana (:3000) ──▶ Prometheus 数据源 ──▶ 告警规则(p95>500ms、5xx>1%)
```

- 容器编排：`deploy/docker-compose.yml`（服务：mysql、redis、backend、web；observability profile 追加 prometheus、grafana）。
- 数据卷：`mysql-data`、`redis-data`、`grafana-data`。
- 后端多阶段镜像（builder JDK21+Maven → runtime JRE，非 root）；前端多阶段镜像（node build → nginx:alpine）。

## 2. 启动

```bash
# 全部环境变量放 deploy/.env（不入库），参考模板：
# MYSQL_ROOT_PASSWORD=root_dev_pwd
# DB_PASSWORD=campus_dev_pwd
# CO_FIELD_KEY=<32字节Base64密钥>
# GRAFANA_PASSWORD=admin
# SPRING_PROFILES=prod        # 正式环境去掉 demo，避免写入演示数据

cd deploy
docker compose up -d --build                        # 应用 + MySQL + Redis
docker compose --profile observability up -d        # 追加 Prometheus + Grafana
docker compose ps                                    # 确认 backend 状态为 healthy
curl -s http://localhost:8080/actuator/health        # 后端就绪探针
```

验证：浏览器打开 `http://<服务器>/`，能看到问题列表；Swagger：`http://<服务器>:8080/swagger-ui.html`。

## 3. 停止与重启

```bash
docker compose stop          # 停止（保留数据卷）
docker compose start         # 恢复
docker compose restart backend   # 只重启后端
docker compose down          # 停止并删除容器（数据卷保留）
docker compose down -v       # ⚠ 连同数据一起删除，仅测试环境使用
```

## 4. 回滚

```bash
# 流水线镜像 tag = git SHA。查看当前版本：
docker inspect co-backend --format '{{index .Config.Labels "org.opencontainers.image.revision"}}'

# 回滚到上一个稳定 SHA：
REV=<上次稳定的git SHA>
docker pull registry.example.com/campus-overflow-backend:$REV
docker compose -f deploy/docker-compose.yml up -d   # 使用环境变量指定镜像 tag 重新编排

# 数据库回滚（仅结构变更时）：flyway_schema_history 按版本撤销
docker compose exec mysql mysql -uroot -p campus_overflow -e \
  "DELETE FROM flyway_schema_history WHERE version > '<目标版本>';"
# 然后重启 backend 让 Flyway 自动迁移到目标版本；数据内容回滚需从备份恢复（§7）
```

## 5. 常见故障处置

| 现象 | 排查 | 处置 |
| --- | --- | --- |
| backend 反复重启，日志 `Communications link failure` | `docker compose ps` 看 mysql 是否 healthy；`docker compose logs mysql` | 等 mysql healthy 后 `docker compose restart backend`；确认 .env 的 DB_PASSWORD 与容器一致 |
| 声誉不变化 / 通知收不到 | 事件走发件箱异步投递，查 `outbox_event` 表 `attempts`、`last_error` | `SELECT * FROM outbox_event WHERE published_at IS NULL AND last_error IS NOT NULL;` 定位失败原因后人工处理后 `UPDATE outbox_event SET attempts=0, last_error=NULL, locked_until=NULL;` 重放 |
| 中文搜索没结果 | 必须使用 compose 内的 MySQL（`--ngram-token-size=2 --innodb-ft-enable-stopword=0`） | 自建库对照 compose 参数重建；重建后重启 backend 让投影重放（或从演示数据重建） |
| 声誉对账不一致 | `GET /api/v1/admin/reputation/reconciliation` 返回 mismatch | 检查 `rep_ledger` 是否有手工 UPDATE/DELETE（ADR-004 禁止）；按流水重算余额修复账户表 |
| 排行榜周期榜为空 | Redis ZSET 数据过期或未写入 | 确认 backend 消费 ReputationChanged 事件正常（`co_outbox_backlog` 指标）；总榜不受影响（读账户表） |
| 部署后访问 404（刷新详情页白屏） | 前端 SPA 路由需要 fallback | 确认使用 deploy/nginx.conf（`try_files $uri $uri/ /index.html;`）而非裸 nginx 镜像 |
| 5xx 错误率上升 | `docker compose logs backend --tail 200` 看异常堆栈 | 常见：数据库死锁（重试）、乐观锁冲突（前端提示刷新）；按 §6 看 Grafana 面板定位 |

## 6. 监控与告警

- 指标：Prometheus 抓取 `/actuator/prometheus`，核心指标至少 5 个：
  1. `http_server_requests_seconds`（p95 延迟）
  2. `http_server_requests_seconds_count{status=~"5.."}`（5xx 错误率）
  3. `co_outbox_backlog` / `co_outbox_dead_letters`（发件箱积压与死信）
  4. `co_reputation_mismatches`（声誉对账不一致数）
  5. `jvm_memory_used_bytes`（JVM 内存）
- 告警规则（Prometheus，`deploy/prometheus.yml` 配套）：
  - `histogram_quantile(0.95, ...) > 0.5`（p95 > 500ms）
  - `rate(5xx)[5m] / rate(all)[5m] > 0.01`（5xx 错误率 > 1%）
  - `co_outbox_dead_letters > 0`（死信即告警）
- 登录 Grafana（:3000，admin/环境变量密码），Dashboard 导入课程模板后确认面板有数据。

## 7. 备份与恢复

```bash
# 每日备份（建议 cron 定时）
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers campus_overflow \
  | gzip > backup/campus_overflow_$(date +%F).sql.gz

# 恢复
gunzip < backup/campus_overflow_2026-10-23.sql.gz \
  | docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" campus_overflow
```

- Redis 排行榜/会话：`redis-data` 卷开启 AOF（compose 已配 `appendonly yes`）；如需重建排行榜，重放 `outbox_event` 中已发布事件即可。
- 演练要求：每 Sprint 至少执行一次"备份 → 删除卷 → 恢复"演练，确认备份可恢复。

## 8. 安全清单

- 所有密码/密钥经 .env 注入（CI 用 Secrets），禁止写进 Dockerfile 或提交仓库。
- `COOKIE_SECURE=true`（HTTPS 发布时）；`CO_FIELD_KEY` 为字段级加密密钥，泄露需轮换。
- 容器均以非 root 运行（Dockerfile 已配 `USER`）；定期 `docker scan` 检查基础镜像。

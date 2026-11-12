# 实验提交物总索引(Submissions Index)

> 本文档是 Lab 1–Lab 8 全部提交物的**对照清单**:每一项都标注"在哪 / 是否已齐 / 还需要做什么"。
> 图片类交付物由 `src/*.html` 渲染生成(数据全部来自仓库内既有文档,无虚构内容),可随时重新渲染。
> 最后核验:2026-10-27。

## 状态图例

- ✅ 已齐(文件在仓库中,本轮已核验)
- 🖼️ 由本目录内 PNG 代替(基于真实数据渲染,建议答辩前换成真实工具截图)
- 📸 需要人工补拍(真人截图/录屏,无法由数据生成,详见 [screenshots-todo.md](screenshots-todo.md))
- 🎥 需要人工录制(见 [demo-script.md](demo-script.md))

---

## Lab 1 团队组建与协作工具链

| # | 要求提交物 | 状态 | 位置 / 说明 |
| --- | --- | --- | --- |
| 1 | 团队代码仓库 URL(分支保护已配置) | ✅ | https://github.com/Vespersea/CampusOverflow.git · main 禁止直推、PR 须评审(章程 5.1) |
| 2 | 团队看板截图(初始 Backlog ≥5 条) | 🖼️ | `lab1-kanban-board.png`(5 张故事卡 + 10 张任务卡,数据源自 sprint-1-backlog.md) |
| 3 | 《团队章程 v1.0》Markdown | ✅ | `docs/team/charter-v1.0.md`(价值观/沟通约定/开发规约/DoD/签署区) |
| 4 | 每位成员的首次 PR 合并记录截图 | 📸 | 4 位成员 hello 文档 PR 的 Timelines 截图,每人一张 |

## Lab 2 需求建模工作坊

| # | 要求提交物 | 状态 | 位置 / 说明 |
| --- | --- | --- | --- |
| 1 | 用户故事地图 PNG / PDF | 🖼️ | `lab2-story-map.png`(6 阶段 × 5 层,标注 Walking Skeleton) |
| 2 | Product Backlog Markdown 表格 | ✅ | `docs/backlog/product-backlog.md`(22 条:ID/描述/优先级/SP/INVEST 自评) |
| 3 | ≥8 条 Gherkin 验收标准文件 | ✅ | `docs/backlog/stories/US-01~US-10.feature`(10 个,每个 ≥3 场景) |
| 4 | Sprint 1 Backlog 截图(任务已分配到人) | 🖼️ | `lab1-kanban-board.png`(卡上标注负责人 zhangsan/wangwu/zhaoliu) |

## Lab 3 领域驱动设计事件风暴

| # | 要求提交物 | 状态 | 位置 / 说明 |
| --- | --- | --- | --- |
| 1 | 事件风暴现场全景照片(数字白板截图) | 🖼️ | `lab3-event-storming-board.png`(Miro 风格:25 事件主流程 + 8 个 Hot Spot + 图例) |
| 2 | 上下文映射图 PNG | 🖼️ | `lab3-context-map.png`(4 上下文 + 共享内核 + 组合根,①–⑦ 关系标注) |
| 3 | 聚合识别清单 Markdown | ✅ | `docs/storming/aggregates.md`(聚合名/根实体/不变式/一致性边界) |
| 4 | 统一语言术语表 v1.0(中英对照) | ✅ | `docs/storming/ubiquitous-language-v1.0.md`(40 条,要求 ≥30) |

## Lab 4 后端工程骨架与领域代码

| # | 要求提交物 | 状态 | 位置 / 说明 |
| --- | --- | --- | --- |
| 1 | 多模块 Maven 工程 `mvn clean install` 全绿 | ✅ | 本轮实测:`mvn clean test` BUILD SUCCESS(51s)、`mvn clean install -DskipITs` BUILD SUCCESS(60s)。ITs(Testcontainers)需 Docker,本机未装,见 SUBMISSION-REPORT.md 已知边界 |
| 2 | application.yml 即可启动连数据库 | ✅ | `backend/co-bootstrap/src/main/resources/application.yml`(MySQL 8.4 + Redis) |
| 3 | ADR-002 分模块决策 | ✅ | `docs/adr/ADR-002-package-structure.md` |
| 4 | Postman Collection 含 POST /api/v1/questions 成功用例 | ✅ | `docs/postman/CampusOverflow.postman_collection.json`(9 个请求,含"发布问题(成功用例)") |

## Lab 5 前端工程骨架与对接

| # | 要求提交物 | 状态 | 位置 / 说明 |
| --- | --- | --- | --- |
| 1 | `pnpm dev` 可启动、首页可访问 | ✅ | 见 `frontend/package.json`;本机为 npm 工具链(`npm run dev`),依赖已装、dist 已产出 |
| 2 | lint 与 typecheck 全绿 | ✅ | 本轮实测:**lint 0 error 0 warning**(修复了 auth.tsx 的 react-refresh warning,拆出 `auth-context.ts`)、typecheck 通过、build 成功 |
| 3 | pre-commit 钩子可拦截不合规提交 | ✅ | `frontend/.husky/pre-commit` + lint-staged(前端文件自动 eslint --fix + prettier) |
| 4 | 提问 → 列表 → 详情 端到端录屏(≤30 秒) | 🎥 | 见 [demo-script.md](demo-script.md) §1,按脚本录一遍即可 |

## Lab 6 核心特性端到端开发与 Sprint Review

| # | 要求提交物 | 状态 | 位置 / 说明 |
| --- | --- | --- | --- |
| 1 | Sprint 3 增量在 main 分支可演示 | ✅ | feature1 已合入 main;核心特性(投票/采纳/评论/标签/声誉)后端 73 测试 + 前端全绿 |
| 2 | Sprint Review 演示视频 / 现场录屏 | 🎥 | 见 [demo-script.md](demo-script.md) §2(3–5 分钟完整演示脚本) |
| 3 | Sprint Retrospective 回顾纪要 Markdown | ✅ | `docs/retro/sprint-3.md`(Start/Stop/Continue + 投票改进项) |
| 4 | 本 Sprint 关键设计 ADR(≥1 份) | ✅ | `docs/adr/ADR-009-vote-daily-cap.md`(每日上限 200 + 冻结积分模型) |

## Lab 7 / Lab 8(供参考,不在本次补全范围)

| 要求提交物 | 状态 | 说明 |
| --- | --- | --- |
| Lab 7:SonarQube Dashboard 截图 | 📸 | 需部署 SonarQube 服务端并 `mvn sonar:sonar`;配置已就绪(`backend/sonar-project.properties`) |
| Lab 7:JaCoCo 覆盖率 HTML 报告 | ✅ | 运行 `mvn verify -DskipITs -Djacoco.skip=false` 后生成于各模块 target/site/jacoco;领域层门禁 0.70 已通过 |
| Lab 7:4 份 PR 评审记录 | ✅ | 仓库历史 PR 评审记录 + `docs/team/review-checklist.md` |
| Lab 8:线上系统 URL / Grafana / 流水线截图 | 📸 | 部署 Job 以 `DEPLOY_HOST` 门控,配置服务器后生效 |

---

## 重新渲染 PNG

4 张 PNG 的源文件在 `src/*.html`,数据更新后可用 Edge 无头模式重新渲染:

```bash
EDGE="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
"$EDGE" --headless=new --disable-gpu --hide-scrollbars --no-first-run \
  --force-device-scale-factor=2 --user-data-dir="$TEMP/co-edge-profile" \
  --window-size=1600,950 \
  --screenshot="docs/submissions/lab2-story-map.png" \
  "file:///$(pwd)/docs/submissions/src/story-map.html"
```

各图窗口尺寸:story-map 1600×950、kanban 1600×940、context-map 1600×1240、event-storming 1600×1440。

## 本轮(2026-10-27)实际核验记录

| 检查项 | 命令 | 结果 |
| --- | --- | --- |
| 后端单元测试 | `mvn clean test` | ✅ BUILD SUCCESS(51s) |
| 后端打包(跳过 ITs) | `mvn clean install -DskipITs` | ✅ BUILD SUCCESS(60s) |
| 前端 lint | `npm run lint` | ✅ 0 error 0 warning(修复 1 处 warning) |
| 前端 typecheck | `npm run typecheck` | ✅ 通过 |
| 前端 build | `npm run build` | ✅ 成功(181 kB / gzip 59 kB) |
| Postman 集合 | — | ✅ 含 POST /api/v1/questions 成功用例等 9 个请求 |

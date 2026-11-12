# 真人截图待办清单(Screenshots TODO)

> 本清单列出**必须由真人操作截图/录屏**的提交物——它们涉及 GitHub 界面与现场过程,无法由仓库数据渲染,建议全队分工、每人 10 分钟完成。
> 已由渲染 PNG 代替的项(看板/故事地图/上下文映射图/事件风暴白板)如果老师要求真实工具截图,也列在对应条目中。

## Lab 1 需要补拍(2 项)

### 1. 仓库分支保护截图(1 张)
1. 打开 https://github.com/Vespersea/CampusOverflow/settings/branches
2. 截图应同时包含:branch name pattern `main`、勾选的 "Require a pull request before merging"(≥1 approval)与 "Require status checks to pass before merging"
3. 保存为 `lab1-branch-protection.png`,放入本目录

### 2. 每位成员首次 PR 合并记录(4 张)
1. 打开仓库 Pull requests 页 → 筛选 Closed/Merged → 找到自己的 `feature/<姓名拼音>-hello` PR(对应 `docs/team/hello-*.md`)
2. 点进 PR,截图包含:PR 标题、reviewer 的 LGTM 评论、紫色的 Merged 标记
3. 4 人各截 1 张:`lab1-pr-merge-<姓名拼音>.png`
4. 参考提交:main 分支历史上 `docs/team/hello-{zhangsan,lisi,wangwu,zhaoliu}.md` 的首次合入提交

## Lab 1/2 可选:真实看板截图

- 目前 `lab1-kanban-board.png` 是按 `docs/backlog/sprint-1-backlog.md` 渲染的等价图;
- 若老师要求工具内截图:GitHub → 仓库 Projects 页 → 新建 "CampusOverflow Sprint Board" → 按 sprint-1-backlog.md 建 To Do / In Progress / In Review / Done 四列与卡片 → 截图。10 分钟可完成。

## Lab 6 需要补录(1 项)

- Sprint Review 演示视频:按 `demo-script.md` §2 走一遍(5 分钟/队),或上课现场录制老师评审环节。现场录屏优先——Lab 6 交付物 2 原文为"演示视频或现场录屏"。

## Lab 5 需要补录(1 项)

- 30 秒端到端录屏:按 `demo-script.md` §1 录制,提交 `lab5-demo-30s.mp4`。

## Lab 7 / Lab 8 后续需要(供排期)

| 项 | 操作 | 预计耗时 |
| --- | --- | --- |
| SonarQube Dashboard 截图 | 部署 SonarQube(团队 Docker)→ `mvn sonar:sonar` → 截图 Coverage/Bugs/Smells 面板 | 1 小时(首次) |
| JaCoCo HTML 报告 | `mvn verify -DskipITs -Djacoco.skip=false` → 各模块 `target/site/jacoco/index.html` | 5 分钟 |
| CI 流水线成功截图 | GitHub Actions → 最新 run → 全绿截图 | 2 分钟 |
| Grafana Dashboard 截图 | 部署后访问 Grafana → 截图含真实流量数据 | 15 分钟 |

## 分工建议

| 成员 | 任务 |
| --- | --- |
| zhangsan(PO) | Lab 1 分支保护截图 + Lab 5 录屏 |
| lisi(SM) | 检查每位成员 PR 截图是否合格,汇总到本目录 |
| wangwu | 自己 + 提醒 zhaoliu 的 PR 合并截图 |
| zhaoliu(QA) | SonarQube 部署与截图(第 13 周 Lab 7 前完成) |

> 完成后请把截图直接放入本目录(`docs/submissions/`),并勾选 README.md 状态表中的对应项(🖼️/📸 → ✅)。

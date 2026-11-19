# 真人截图待办清单(Screenshots TODO)

> 更新日期:2026-11-19。本清单列出**必须由真人操作截图/录屏**的提交物——它们涉及 GitHub 界面与现场过程,无法由仓库数据渲染,建议全队分工、每人 10 分钟完成。
> 交付口径:个人仓 https://github.com/Vespersea/CampusOverflow,默认分支 **main**。
> 已由渲染 PNG 代替的项(看板/故事地图/上下文映射图/事件风暴白板)如果老师要求真实工具截图,也列在对应条目中。

## 0. 成员与账号对照

| 姓名 | 角色 | GitHub 账号 |
| --- | --- | --- |
| 蔡锦莉 | PO(Product Owner) | Vespersea |
| 张玉馨 | SM(Scrum Master) | zyxd336 |
| 唐颖 | Developer(co-qa 问答核心) | ty666-lang |
| 吴超阳 | Developer + QA(co-reputation / co-discovery) | youngbv |

> 账号以仓库提交记录为准,请四人各核对一遍。

## Lab 1 需要补拍(2 项)

### 1. 仓库分支保护截图(1 张)

1. 打开 https://github.com/Vespersea/CampusOverflow/settings/branches
2. 截图应同时包含:branch name pattern `main`、勾选的 "Require a pull request before merging"(approvals ≥1)与 "Require status checks to pass before merging"
3. 保存为 `lab1-branch-protection.png`,放入本目录

### 2. 每位成员首次 PR 合并记录(4 张)

**注意:首次 PR 不必是 hello 文档。**仓库里四人的首次合并 PR 都是功能 PR,按下面这张表去截:

| 成员 | 账号 | 首次合并 PR | 标题 | 合并日期 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 唐颖 | ty666-lang | #1 | 第一次提交(更改README) | 2026-10-22 | 已合并,直接截 |
| 吴超阳 | youngbv | #3 | 2.3.3 声誉与激励子域实现 | 2026-10-22 | 已合并,直接截 |
| 张玉馨 | zyxd336 | #4 | 2.3.4内容发送子域 | 2026-10-22 | 已合并,直接截 |
| 蔡锦莉 | Vespersea | —— | —— | —— | **无 PR,需先补一条** |

**操作步骤**

1. 打开 `https://github.com/Vespersea/CampusOverflow/pulls?q=is%3Apr+is%3Aclosed`(默认页只显示 Open,本仓 Open=0,所以要点 `Closed` 标签)
2. 点进上表对应的 PR 详情页(**不要截列表页**)
3. 截图必须同时包含:
   - PR 编号与标题、`by <账号名>`
   - 紫色 **Merged** 徽标、合并人与合并时间
   - 分支流向 `main ← feature2`(或其他 compare 分支)
   - `Files changed` 数量与 `+N −M`
   - **浏览器地址栏**(证明是哪个仓库的哪条 PR)
4. 4 人各截 1 张,命名 `lab1-pr-merge-<账号名>.png`

**蔡锦莉(Vespersea)补做步骤**

他是仓库所有者,历史上一直直接合并/直接推送,没开过 PR。补一条:

```
git checkout main
git pull origin main
git checkout -b feature/vespersea-lab1-merge
# 改一处文档,例如 docs/team/hello-Vespersea.md 补一行
git add .
git commit -m "docs(team): 更新个人介绍(Lab1 首次 PR)"
git push origin feature/vespersea-lab1-merge
```

推完到 GitHub 开 PR,目标分支选 **main**,由另一名成员点 Approve 后合并,再按上面第 3 步截图。
**必须本人提交、本人开 PR**——别人代推会让作者变成对方账号,截图与账号对不上。

## Lab 1 / Lab 2 可选:真实看板与故事地图截图

- `lab1-kanban-board.png` 与 `lab2-story-map.png` 已按**真实姓名与账号**重新渲染,内容与 `docs/backlog/sprint-1-backlog.md` 一致;
- 若老师要求工具内截图:GitHub → 仓库 Projects 页 → 新建 "CampusOverflow Sprint Board" → 按 `sprint-1-backlog.md` 建 To Do / In Progress / In Review / Done 四列与卡片 → 截图,约 10 分钟;
- 故事地图若要求"工作坊现场白板照",用 ProcessOn 或飞书画板重画一张(6 阶段 × 5 层 + Walking Skeleton 横切线)再截全屏。

## Lab 5 需要补录(1 项)

- 30 秒端到端录屏:按 `demo-script.md` §1 录制(提问 → 列表 → 详情),提交 `lab5-demo-30s.mp4`。

## Lab 6 需要补录(1 项)

- Sprint Review 演示视频:按 `demo-script.md` §2 走一遍(3–5 分钟/队),或上课现场录制老师评审环节。现场录屏优先——Lab 6 交付物 2 原文为"演示视频或现场录屏"。

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
| 蔡锦莉(PO, Vespersea) | Lab 1 分支保护截图 + 补自己那条 PR(改文档 → 开 PR → 请人 Approve) |
| 张玉馨(SM, zyxd336) | 截 `pull/4`;检查 4 张 PR 截图是否合格,汇总到本目录 |
| 唐颖(Dev, ty666-lang) | 截 `pull/1`;负责 Approve 蔡锦莉那条 PR |
| 吴超阳(Dev+QA, youngbv) | 截 `pull/3`;Lab 7 的 SonarQube 部署与截图(第 13 教学周前完成) |

> 完成后请把截图直接放入本目录(`docs/submissions/`),并勾选 README.md 状态表中的对应项(🖼️/📸 → ✅)。
</content>

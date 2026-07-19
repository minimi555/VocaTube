# 前端更新计划：学习咨询模块（ConsultScreen）

## 目标

将 `ConsultScreen` 从占位页面升级为完整功能页面，对接后端三组新 API：

| 后端端点 | 功能 |
|----------|------|
| `POST /ask` | RAG 查询英语备考方法（CET4/CET6/IELTS/TOEFL/SAT/考研） |
| `GET /schools` | 获取 QS 前 150 学校列表 |
| `POST /school/search` | 搜索学校英语要求（LangChain Agent） |
| `GET /school/history?limit=N` | 学校搜索历史记录 |

---

## 架构设计

遵循现有代码风格：**MVVM + Compose + Retrofit + StateFlow**

```
data/remote/
  ├── Dtos.kt           ← 新增 DTO（AskRequest, AskResponse, SchoolItem 等）
  ├── ApiService.kt     ← 新增 4 个接口方法
  └── Network.kt        ← 不变

ui/consult/
  ├── ConsultScreen.kt  ← 重写：双 Tab 布局（备考咨询 / 学校查询）
  └── ConsultViewModel.kt ← 新建：管理两种查询的状态
```

---

## 详细文件改动

### 1. `data/remote/Dtos.kt` — 新增 DTO

```kotlin
// ---- RAG (备考咨询) ----
@Serializable
data class AskRequest(val question: String, val k: Int = 4)

@Serializable
data class SourceItem(
    val source: String? = null,
    val section: String? = null
)

@Serializable
data class AskResponse(
    val answer: String = "",
    val sources: List<SourceItem> = emptyList()
)

// ---- School Search (学校查询) ----
@Serializable
data class SchoolItem(
    val id: Int,
    val name: String = "",
    val domain: String = "",
    @SerialName("qs_rank") val qsRank: Int = 0
)

@Serializable
data class SchoolSearchRequest(val question: String)

@Serializable
data class SchoolSearchResponse(val answer: String = "")

@Serializable
data class SchoolSearchHistoryItem(
    val id: Int,
    val question: String = "",
    val answer: String = "",
    @SerialName("created_at") val createdAt: String = ""
)
```

---

### 2. `data/remote/ApiService.kt` — 新增接口

```kotlin
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// RAG
@POST("ask")
suspend fun ask(@Body request: AskRequest): AskResponse

// School
@GET("schools")
suspend fun getSchools(): List<SchoolItem>

@POST("school/search")
suspend fun schoolSearch(@Body request: SchoolSearchRequest): SchoolSearchResponse

@GET("school/history")
suspend fun schoolHistory(@Query("limit") limit: Int = 20): List<SchoolSearchHistoryItem>
```

---

### 3. `ui/consult/ConsultViewModel.kt` — 新建

**UI State 设计：**

```kotlin
data class ConsultUiState(
    // 当前 Tab：RAG 备考 vs 学校查询
    val activeTab: ConsultTab = ConsultTab.TestPrep,

    // --- 备考咨询 (RAG) ---
    val prepQuery: String = "",
    val prepLoading: Boolean = false,
    val prepAnswer: String? = null,
    val prepSources: List<SourceItem> = emptyList(),
    val prepError: String? = null,

    // --- 学校查询 ---
    val schoolQuery: String = "",
    val schoolLoading: Boolean = false,
    val schoolAnswer: String? = null,
    val schoolError: String? = null,
    val schools: List<SchoolItem> = emptyList(),      // 学校列表（初始加载）
    val history: List<SchoolSearchHistoryItem> = emptyList(),
    val showHistory: Boolean = false,
)

enum class ConsultTab { TestPrep, SchoolSearch }
```

**ViewModel 职责：**

| 方法 | 说明 |
|------|------|
| `switchTab(tab)` | 切换 Tab |
| `onPrepQueryChange(value)` | 更新备考查询输入 |
| `askPrep()` | 调用 `POST /ask`，展示回答+来源 |
| `onSchoolQueryChange(value)` | 更新学校查询输入 |
| `searchSchool()` | 调用 `POST /school/search`，展示回答 |
| `loadSchools()` | 初始化加载 `GET /schools` |
| `loadHistory()` | 加载 `GET /school/history` |
| `toggleHistory()` | 显示/隐藏历史记录面板 |

---

### 4. `ui/consult/ConsultScreen.kt` — 重写

**布局结构：**

```
┌─────────────────────────────────────────┐
│  [备考咨询]  [学校查询]    ← Tab Row     │
├─────────────────────────────────────────┤
│                                         │
│  (Tab = 备考咨询)                        │
│  ┌─────────────────────────────────┐    │
│  │ 输入框: "雅思口语如何备考"        │ 提问│
│  └─────────────────────────────────┘    │
│  ┌─ Answer Card ───────────────────┐    │
│  │ 回答文本（Markdown 简单渲染）     │    │
│  │ ─── 参考来源 ───                 │    │
│  │ • CET4备考.md / 听力技巧         │    │
│  │ • IELTS备考.md / 口语评分标准     │    │
│  └─────────────────────────────────┘    │
│                                         │
│  (Tab = 学校查询)                        │
│  ┌─────────────────────────────────┐    │
│  │ 输入框: "港中文CS硕士雅思要求"    │ 搜索│
│  └─────────────────────────────────┘    │
│  [📋 历史记录]  ← Toggle Button         │
│  ┌─ Answer Card ───────────────────┐    │
│  │ Agent 回答（含 URL 引用）         │    │
│  └─────────────────────────────────┘    │
│  ┌─ School List (collapsible) ─────┐    │
│  │ 1. MIT - mit.edu                │    │
│  │ 2. Cambridge - cam.ac.uk        │    │
│  │ ...可滚动                        │    │
│  └─────────────────────────────────┘    │
│                                         │
└─────────────────────────────────────────┘
```

**UI 组件拆分：**

| Composable | 职责 |
|------------|------|
| `ConsultScreen()` | 顶层：Tab + 内容切换 |
| `TestPrepTab()` | 备考咨询：输入 + 回答 + 来源 |
| `SchoolSearchTab()` | 学校查询：输入 + 回答 + 历史 + 学校列表 |
| `AnswerCard(answer, sources?)` | 通用回答卡片 |
| `HistoryPanel(items)` | 历史搜索记录列表 |
| `SchoolListSection(schools)` | QS 学校列表（折叠式） |

---

## 交互细节

### 备考咨询 Tab
1. 用户输入问题（如"雅思写作小作文怎么练"）
2. 点击"提问"按钮或键盘 Search
3. 显示 loading 动画
4. 返回后展示 **回答文本** + **参考来源列表**（source + section）
5. 错误时显示红色提示（503 = 向量库未初始化，提示"请联系管理员"）

### 学校查询 Tab
1. 初次进入时自动加载学校列表（`GET /schools`）
2. 用户输入问题（如"帝国理工数据科学硕士托福要求"）
3. 点击"搜索"按钮
4. 显示 loading（Agent 搜索较慢，可能 10-30s）+ 提示"正在搜索学校官网…"
5. 返回后展示回答，同时刷新历史
6. 点击"历史记录"按钮展开/收起过往搜索

---

## 注意事项

1. **超时配置**：学校搜索 Agent 响应慢（10-30s），需为该请求设置更长超时。在 `Network.kt` 中创建第二个 `OkHttpClient`，或在 `ApiService` 添加 `@Headers("X-Timeout: 60")` + Interceptor 处理。推荐做法：对 `POST /school/search` 单独配置 60s 读超时。

2. **错误码处理**：
   - `POST /ask` → 503 = 向量库未就绪
   - `POST /school/search` → 502 = Agent 执行失败
   - 通用 → 网络错误、超时

3. **createdAt 解析**：后端返回 ISO 格式 `"2026-07-19T10:30:00"`，前端用 String 接收后格式化显示即可（避免引入额外日期库）。

4. **长文本渲染**：Agent 回答可能很长（含 URL），建议 `Text` 设置 `selectable = true`（`SelectionContainer`），方便用户复制链接。

5. **学校列表初始化**：使用 `LaunchedEffect` 在 Tab 首次展示时触发加载，缓存到 ViewModel，不重复请求。

---

## 文件清单（改动/新建）

| 文件路径（相对 `frontend/app/src/main/java/com/example/frontend/`） | 操作 |
|-----|------|
| `data/remote/Dtos.kt` | **修改** — 追加 7 个 DTO |
| `data/remote/ApiService.kt` | **修改** — 追加 4 个接口方法 + import |
| `ui/consult/ConsultViewModel.kt` | **新建** |
| `ui/consult/ConsultScreen.kt` | **重写**（保留包名和文件名） |

共 **4 个文件**，其中 2 个修改、1 个新建、1 个重写。

---

## 运行验证

```bash
# 后端启动（确保 seed 已执行）
cd /mnt/VocaTube/backend/app && uvicorn main:app --host 0.0.0.0 --port 8000

# USB 端口转发
adb reverse tcp:8000 tcp:8000

# Android Studio Build & Run（本机无 JDK，需在开发机执行）
./gradlew assembleDebug
```

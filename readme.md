# ChatRobot

聊天与多模态助手应用，支持文本对话、图片理解与图片生成，面向移动端的顺畅体验与清晰架构。项目采用 Kotlin + Jetpack Compose 构建界面，Room + DataStore 管理持久化数据，Retrofit/OkHttp 接入 Doubao Ark（火山方舟）端点，MVVM + Repository 保持清晰的层次与可测试性。

> 本 README 为详细版，包含完整的功能说明、架构解析、实现细节、构建部署、测试与运维指南、常见问题、开发约定与贡献流程，帮助你在最短时间内理解、构建并扩展本项目。

## 目录
- 1. 概述与目标
- 2. 功能特性
- 3. 产品体验细节
- 4. 技术栈与版本
- 5. 快速开始
- 6. 架构总览
- 7. 持久层设计（Room + DataStore）
- 8. 网络层与模型接入（Retrofit/OkHttp + Doubao Ark）
- 9. 视图模型与状态管理（MVVM + Flow/StateFlow）
- 10. UI 层（Jetpack Compose）
- 11. 核心功能实现与关键代码片段
- 12. 导航与路由（Navigation）
- 13. 登录注册与安全策略
- 14. 历史会话与摘要生成
- 15. 图片理解与图片生成链路
- 16. 主题与字体偏好（DataStore）
- 17. 错误处理与失败重试机制
- 18. 构建与发布
- 19. 配置与密钥管理
- 20. 测试（单元/仪器/UI）
- 21. 性能优化
- 22. 安全与隐私
- 23. 国际化与无障碍
- 24. 日志与监控
- 25. CI/CD 流程（可选）
- 26. 代码结构索引
- 27. 开发约定与代码风格
- 28. 分支与版本管理
- 29. 贡献指南
- 30. Roadmap（路线图）
- 31. 常见问题与故障排查
- 32. 变更记录建议
- 33. 许可证

---

## 1. 概述与目标
- 定位为“小型 AI 对话流 App”，统一文本对话、视觉理解、文生图三条能力链路，提供流畅移动端体验。
- 目标强调：易扩展、清晰架构、可维护、可测试、对开发者友好。
- 面向场景：日常问答、图像理解（截图/海报/表格/照片）、快速图像生成与灵感探索。

## 2. 功能特性
- 文本对话
  - 多轮会话，自动携带最近 N 条上下文（默认 10）
  - 自动生成会话标题（≤12 字），保证历史列表简洁
- 图片理解
  - 支持本地图片 `content://` 与网络图片 `http(s)`
  - 本地图片自动编码为 Base64 并以 `input_image` 提交
  - 网络图片直接以 `image_url` 提交
- 图片生成
  - 文生图返回 `b64_json`，在气泡内直接显示
  - 支持保存当前图片到会话或设备（系统分享）
- 历史会话管理
  - 标题、摘要（首条问题 + 最近回答）、重命名、删除
  - 会话切换不会丢失上下文
- 模型选择
  - 横向卡片切换文本/视觉/文生图，以及自定义 `ep-...` 接入点
- 主题与字体
  - 统一浅灰主题，随浅色/深色模式变化
  - 全局字体大小“小/中/大”三档，实时生效
- 失败重试
  - 失败消息右侧显示红色感叹号，点击即可重试最近用户消息
  - 列表尾部统一 Typing 指示，避免气泡内“转圈”干扰阅读

## 3. 产品体验细节
- 输入栏贴底，适配键盘与安全区域，保证操作顺手
- 顶部栏随主题切换呈现浅色/深色背景，操作入口集中且不复杂
- 历史与配置在顶部/抽屉中快速切换，避免多层级导航
- 图片理解对比文生图的入口清晰，避免误操作
- 错误提示采用非阻断式，保持对话流畅性

## 4. 技术栈与版本
- Kotlin：`1.9.24`
- Jetpack Compose 编译器扩展：`1.5.14`
- Gradle Wrapper：`8.9`
- Android Gradle Plugin：`8.7.0`
- JDK：`17`
- 核心库：Room、DataStore、Retrofit、OkHttp、Navigation、Coroutines、Flow

## 5. 快速开始
1. 克隆仓库并在 Android Studio 打开
2. 在项目根目录创建或编辑 `local.properties`：
   ```
   DOUBAO_API_KEY=你的Ark密钥
   ```
3. 同步并构建：
   ```
   ./gradlew.bat :app:assembleDebug
   ```
4. 连接设备或启动模拟器，运行 App
5. 在配置页输入或管理模型接入点与参数（如 `ep-...`）

提示：`local.properties` 已在 `.gitignore` 中忽略，切勿提交密钥到仓库。

## 6. 架构总览
- 分层：UI（Compose）→ ViewModel（MVVM）→ Repository → 数据源（Room/DataStore/Network）
- 单向数据流：Repository 暴露 `Flow/StateFlow`，ViewModel 收集并转换为 UI 状态，Compose 使用 `collectAsState()` 渲染。
- 依赖注入：通过 `AppContainer` 与 `AppViewModelProvider` 管理仓库实例与偏好存储注入，应用级 `ChatApplication` 负责初始化。

## 7. 持久层设计（Room + DataStore）
- Room
  - 实体：`Message` / `User` / `Config` / `Conversation`（按项目需要扩展）
  - DAO：基础 CRUD 与特定查询（按用户、时间排序等）
  - Database：`ChatDb` 单例，`fallbackToDestructiveMigration()` 简化迁移（生产环境建议自定义迁移）
- DataStore（Preferences）
  - 存储主题与字体偏好等简单键值对
  - 通过 `Flow` 暴露数据，界面实时响应

### Room 示例
```kotlin
@Entity(tableName = "message")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val time: Long,
    val content: String,
    @ColumnInfo(name = "user_id") val userId: Int,
    val isSelf: Boolean
)

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: Message)

    @Query("SELECT * from message WHERE id = :id")
    fun getMessage(id: Int): Flow<Message>

    @Query("Select * from  message where user_id = :userId order by time asc")
    fun getAllMessagesByUserId(userId: Int): Flow<List<Message>>
}
```

### DataStore 示例
```kotlin
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val FONT_SIZE = stringPreferencesKey("font_size")
    }

    val themeConfig: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[IS_DARK_THEME] ?: false }

    val fontConfig: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[FONT_SIZE] ?: "小" }

    suspend fun saveUserPreference(value: Boolean) {
        dataStore.edit { it[IS_DARK_THEME] = value }
    }

    suspend fun saveUserFontPreference(value: String) {
        dataStore.edit { it[FONT_SIZE] = value }
    }
}
```

## 8. 网络层与模型接入（Retrofit/OkHttp + Doubao Ark）
- 超时策略：读/连/写均设为 60s，兼顾视觉与文生图长耗时请求
- Retrofit 单例：避免重复构建的性能浪费
- 端点：
  - 文本/视觉对话：`/api/v3/chat/completions`
  - 文生图：`/api/v3/images/generations`

### Retrofit 示例
```kotlin
private const val BASE_URL = "https://api.openai.com/v1/" // 示例，占位

val client = OkHttpClient.Builder()
    .readTimeout(60, TimeUnit.SECONDS)
    .connectTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface ChatApiService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:Bearer YOUR_KEY"
    )
    @POST("completions")
    fun getReply(@Body requestData: RequestBody): Call<ChatResponse>
}

object ChatApi { val retrofitService: ChatApiService by lazy { retrofit.create(ChatApiService::class.java) } }
```

## 9. 视图模型与状态管理（MVVM + Flow/StateFlow）
- ViewModel 收集 Repository 暴露的流，映射为 `UiState`，暴露 `StateFlow` 供 Compose 使用。
- 统一入口方法：发送消息、请求视觉/文生图、更新配置、切换会话等。

### StateFlow 示例
```kotlin
val chatListState: StateFlow<ChatUiState> =
    messageRepository.getMessagesStreamByUserId(userId)
        .map { ChatUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatUiState()
        )
```

## 10. UI 层（Jetpack Compose）
- 列表采用 `LazyColumn`，消息项组件 `MessageItem` 展示头像、昵称、时间与内容。
- 输入组件 `UserInput` 提供文本输入、图片选择与发送按钮。
- 顶部栏 `TopBarView` 提供标题、菜单与配置入口。

### ChatScreen 示例
```kotlin
@Composable
fun ChatScreen(viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val listState = viewModel.listState
    Surface {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ChatDisplay(viewModel, Modifier.height(510.dp), listState)
                UserInput(viewModel, listState)
            }
        }
    }
}
```

## 11. 核心功能实现与关键代码片段
- 自动标题生成：基于用户首条问题与最近机器人回答生成简洁标题
- 上下文打包：最近 N 条消息整理为模型上下文，控制长度与 token
- 视觉/文生图输入类型选择：本地图片走 `input_image`，网络图片走 `image_url`
- 图片展示：文生图 `b64_json` 解码为位图后在气泡中显示

## 12. 导航与路由（Navigation）
- 组成：`NavController`、`NavGraph`、`NavHost`
- 路由：`login` → `home/{userId}`
- 参数传递：登录成功后将 `userId` 放入路由参数，主页加载用户数据。

### NavHost 示例
```kotlin
@Composable
fun ChatNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "login") {
        composable(route = "login") { LoginScreen(navController = navController) }
        composable(route = "home/{userId}", arguments = listOf(navArgument("userId") { type = NavType.IntType })) {
            MainScreen(navController = navController)
        }
    }
}
```

## 13. 登录注册与安全策略
- 注册：校验两次密码一致、校验账号是否存在、对密码进行不可逆加密（如 MD5）后入库。
- 登录：校验账号密码，返回用户信息时屏蔽密码字段为 `******`。
- 安全：避免在日志与 UI 中暴露密钥与敏感数据。

## 14. 历史会话与摘要生成
- 存储结构：`Conversation` + `Message` 多表关联（如需要）
- 标题策略：优先用首条问题与最近回答合成的短标题；可手动重命名
- 摘要生成：展示首条用户问题 + 最近机器人回答片段，帮助快速回忆上下文

## 15. 图片理解与图片生成链路
- 图片理解：
  - 本地图片通过 ContentResolver 读取并编码为 Base64，以 `input_image` 形式提交
  - 网络图片以 URL 直接提交为 `image_url`
- 图片生成：
  - 文生图请求到 `b64_json`，解码为位图并在消息气泡中展示
  - 支持保存与分享

## 16. 主题与字体偏好（DataStore）
- 通过 `LoginViewModel` 获取主题与字体偏好数据流，`ChatRobotTheme` 中决定配色与字体套件。
- 字体三档：小/中/大，对应 TypographySmall/Medium/Large。

## 17. 错误处理与失败重试机制
- 网络失败：标记对应消息为失败，展示红色感叹号；点击感叹号重试最近一次用户消息。
- 超时控制：统一 60s 超时避免请求卡死。
- UI 状态：失败与重试操作通过 ViewModel 修改 `UiState`，Compose 跟随变化。

## 18. 构建与发布
- Debug：`./gradlew.bat :app:assembleDebug`
- Release：生成签名文件（`.jks`），在 `build.gradle` 配置签名，再执行 `:app:assembleRelease`
- 产物位置：`app/build/outputs/apk/` 下分渠道构建产物
- 版本号策略：遵循 `versionCode` + `versionName`，配合语义化发布

## 19. 配置与密钥管理
- 在根目录 `local.properties` 注入：
  ```
  DOUBAO_API_KEY=你的Ark密钥
  ```
- 不提交 `local.properties`；`.gitignore` 已忽略
- BuildConfig 注入：在编译期将密钥注入 `BuildConfig` 使用，避免硬编码在源码中

## 20. 测试（单元/仪器/UI）
- 单元测试：对 Repository 与业务逻辑进行隔离测试，使用 Fake/Mock Dao 与网络层
- 仪器测试：验证数据库与 DataStore 行为，确保迁移与数据流正确
- UI 测试：Compose UI 测试，验证组件渲染与交互（点击重试、会话切换等）

## 21. 性能优化
- 避免频繁创建 Retrofit/OkHttp 客户端，统一单例
- `LazyColumn` 优化：使用 `key` 保持项稳定，减少重组
- 图片解码与展示：按需采样与缓存，避免主线程阻塞
- Flow 背压与冷流：合理使用 `stateIn` 与 `SharingStarted`

## 22. 安全与隐私
- 密钥只在 `local.properties` 配置，不入库
- 返回给 UI 的用户信息屏蔽敏感字段（密码）
- 避免在日志打印敏感数据
- 如果接入云端存储图片，需遵循隐私合规与用户授权

## 23. 国际化与无障碍
- 文案与格式可抽取到资源以便国际化
- 无障碍（A11y）：为关键按钮与图片提供 contentDescription
- 字体大小适配：全局字体偏好三档，配合系统字体与缩放

## 24. 日志与监控
- 基础日志：关键流程与失败信息记录到本地
- 可选埋点：统计用户常用功能与错误分布（遵循隐私合规）
- 远程日志：可选接入 Crash 与性能分析平台

## 25. CI/CD 流程（可选）
- 检查与构建：在 CI 中执行 `./gradlew :app:assembleDebug` 与基础测试
- 代码规范：运行静态检查与格式化（如 ktlint/Detekt）
- 签名与发布：Release 流程在本地或 CI 管理，避免泄露签名与密钥

## 26. 代码结构索引
- `app/src/main/java/com/yx/chatrobot/ChatApplication.kt`
- `app/src/main/java/com/yx/chatrobot/MainViewModel.kt`
- `app/src/main/java/com/yx/chatrobot/network/*`
- `app/src/main/java/com/yx/chatrobot/data/*`
- `app/src/main/java/com/yx/chatrobot/ui/*`
- `app/src/main/res/*`

## 27. 开发约定与代码风格
- Kotlin 代码风格一致、命名清晰
- 避免在 View 与 ViewModel 中混写网络/数据库逻辑，保持分层
- 所有可长耗时操作放入协程，避免阻塞 UI
- 不提交构建产物、IDE 配置与私密文件，遵循 `.gitignore`

## 28. 分支与版本管理
- 主分支：`main`，保持稳定与发布
- 功能分支：`feature/*`，合并前通过测试与代码评审
- 修复分支：`fix/*`，快速修复后合并并回归测试

## 29. 贡献指南
- 提交规范：语义化提交消息（feat/fix/docs/refactor/test/chore）
- Pull Request：填写变更说明，关联 Issue，通过 CI 检查
- 代码评审：关注可读性、测试覆盖与性能影响

## 30. Roadmap（路线图）
- 第一阶段：稳定文本/视觉/文生图链路，完善历史与配置
- 第二阶段：图片生成的高级参数与样式控制
- 第三阶段：离线缓存与更丰富的会话管理
- 第四阶段：国际化与更多主题方案

## 31. 常见问题与故障排查
- 网络超时或失败
  - 确认设备网络与代理设置
  - 增大超时或重试策略
- 401 未授权
  - 检查 `DOUBAO_API_KEY` 有效与权限
  - 确认对应模型能力已开通
- 图片理解失败
  - 本地图片编码与字段类型是否匹配
  - 网络图片 URL 是否可公网访问
- 构建失败
  - 检查 Gradle/AGP/Kotlin/Compose 版本与 JDK 17 配置

## 32. 变更记录建议
- 每次发布版本在 `CHANGELOG` 记录新增、修复与变更
- 对应 Issue 与 PR 进行链接，方便追踪来源与讨论

## 33. 许可证
- 本项目遵循开源许可证（见仓库内 `LICENSE`）。

---

## 附录 A：数据结构示例
```kotlin
data class ChatResponse(
    val id: String,
    val created: Int,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(val text: String)
data class Usage(val prompt_tokens: Int, val completion_tokens: Int, val total_tokens: Int)
```

## 附录 B：消息渲染与交互示例
```kotlin
@Composable
fun MessageItem(messageUiState: MessageUiState) {
    Row(Modifier.padding(8.dp)) {
        Image(
            painter = painterResource(if (messageUiState.isSelf) R.drawable.user_avatar else R.drawable.robot_avatar),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colors.secondaryVariant, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Row {
                Text(text = messageUiState.name, color = MaterialTheme.colors.secondaryVariant, style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.width(4.dp))
                Text(text = messageUiState.dateStr, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Light, letterSpacing = 0.25.sp)
            }
            Spacer(Modifier.height(4.dp))
            Surface(shape = MaterialTheme.shapes.medium, elevation = 3.dp, color = if (messageUiState.isSelf) MaterialTheme.colors.secondary else MaterialTheme.colors.surface, modifier = Modifier.animateContentSize().padding(1.dp)) {
                SelectionContainer { Text(text = messageUiState.content, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.body1) }
            }
        }
    }
}
```

## 附录 C：构建脚本与常用命令
- 构建 Debug：`./gradlew.bat :app:assembleDebug`
- 构建 Release：`./gradlew.bat :app:assembleRelease`
- 清理构建：`./gradlew.bat clean`
- 同步依赖：Android Studio → Sync Project with Gradle Files

## 附录 D：密钥注入示例
- 在 `build.gradle` 或构建配置中读取 `local.properties` 并注入 `BuildConfig`（示例因项目而异）：
```kotlin
// 示例：在 Gradle 中读取 local.properties 并注入到 BuildConfig
// 请根据实际项目的 build.gradle 进行调整
```

## 附录 E：图片 Base64 处理提示
- 大图片建议压缩或采样后编码，避免超大请求体影响时延与成功率。
- 避免在主线程执行编码与磁盘/网络 IO。

## 附录 F：错误与重试策略建议
- 可增加指数退避重试或对关键失败进行提示与日志。
- 在 UI 保持非阻断体验，避免 Modal 阻塞对话流程。

## 附录 G：样式与主题建议
- 统一浅灰色系，减少视觉负担，适配暗色模式。
- 字体三档与 Breakpoint 匹配，保证不同设备上的可读性。

## 附录 H：版本与依赖管理建议
- 固定依赖版本，避免隐式升级带来的不兼容。
- 使用 Gradle Versions Plugin 定期检查可升级项（可选）。

## 附录 I：发布与签名管理建议
- 签名文件与密钥管理在本地或安全秘钥库，切勿入库。
- Release 构建需验证 Proguard/R8 混淆规则与崩溃栈可读性。

## 附录 J：未来扩展点
- 多账户与多模型配置集管理
- 图片生成的风格与参数预设
- 会话导出与导入（JSON/Markdown）
- 云端同步与跨设备共享（需权限与隐私合规）

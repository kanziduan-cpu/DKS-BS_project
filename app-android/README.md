# 智能地下仓库环境监测调控系统 - Android移动端

基于STM32的智能地下仓库环境监测调控系统Android移动端应用，采用微信风格界面设计，支持远程监控和控制。

## 项目简介

本应用是智能仓库环境监测系统的Android客户端，用于实时监控地下仓库的环境数据（温度、湿度、有害气体浓度等），远程控制设备（通风扇、除湿机、排气装置等），接收和处理报警信息。

## 核心功能

### 1. 用户认证
- 用户登录/注册
- 记住密码功能
- 登录状态持久化
- 安全退出登录

### 2. 环境数据监测（首页）
- 实时显示温度、湿度、甲醛、CO、CO₂、AQI等参数
- 卡片化展示，超阈值红色预警
- 历史数据查询与图表分析
- 下拉刷新功能

### 3. 设备远程控制（设备页）
- 设备列表展示（通风扇、除湿机、排气装置、STM32边缘端）
- 在线/离线状态显示
- 远程启停控制
- 档位调节（低/中/高）

### 4. 报警管理（报警页）
- 实时报警推送
- 报警列表展示（环境异常、设备异常、系统异常）
- 未读/已读状态标识
- 一键标为已读功能
- 按时间倒序排列

### 5. 个人中心（我的页）
- 用户信息展示
- 仓库管理
- 系统设置
- 账号安全
- 退出登录

### 6. 系统设置
- 报警阈值设置（温度、湿度等）
- 温度单位切换（摄氏度/华氏度）
- 提示音开关
- 数据上传频率设置

## 技术架构

### 开发环境
- **语言**: Java 17
- **最低SDK**: API 26 (Android 8.0)
- **目标SDK**: API 34 (Android 14)
- **构建工具**: Gradle 8.0
- **IDE**: Android Studio

### 核心技术栈
- **UI框架**: Material Design Components
- **网络通信**: 
  - Retrofit2 + OkHttp3 (HTTPS)
  - MQTT (阿里云EMQX)
- **数据存储**: SharedPreferences
- **JSON解析**: Gson
- **架构模式**: MVVM

### 项目结构
```
WarehouseMonitor/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/warehouse/monitor/
│   │       │   ├── adapter/          # RecyclerView适配器
│   │       │   ├── model/            # 数据模型
│   │       │   ├── network/          # 网络API接口
│   │       │   ├── ui/               # Activity和Fragment
│   │       │   │   ├── fragments/    # Fragment页面
│   │       │   │   └── ...          # Activity页面
│   │       │   └── utils/            # 工具类
│   │       ├── res/                   # 资源文件
│   │       │   ├── layout/           # 布局文件
│   │       │   ├── drawable/         # 图标资源
│   │       │   ├── values/           # 值资源
│   │       │   └── menu/            # 菜单文件
│   │       └── AndroidManifest.xml    # 应用清单
│   └── build.gradle                 # 应用级构建配置
├── build.gradle                      # 项目级构建配置
├── settings.gradle                  # Gradle设置
└── README.md                       # 项目说明
```

## 界面设计

### 设计风格
- **配色方案**: 微信绿色主题 (#07C160)
- **布局风格**: 微信式底部导航、卡片化设计
- **字体大小**: 适配手机端，支持单手操作
- **交互设计**: 大尺寸触控按钮，即时反馈

### 主要页面
1. **登录页**: 居中表单设计，简洁明了
2. **首页**: 数据看板 + 设备快捷栏，仿微信"发现"页
3. **设备页**: 列表式展示，仿微信"聊天"列表
4. **报警页**: 时间倒序列表，仿微信"消息"页
5. **我的页**: 微信"我"页风格，功能入口列表

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 26+
- Gradle 8.0+

### 构建步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd WarehouseMonitor
```

2. **打开项目**
- 使用Android Studio打开项目根目录
- 等待Gradle同步完成

3. **配置服务器地址**
- 修改 `ApiService.java` 中的 `BASE_URL`
- 替换为实际的阿里云EMQX服务器地址

4. **构建APK**
```bash
# 在Android Studio中
Build -> Build Bundle(s) / APK(s) -> Build APK(s)

# 或使用命令行
./gradlew assembleDebug
```

5. **安装到设备**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 运行应用

1. **连接Android设备**
- 开启开发者选项
- 启用USB调试

2. **运行项目**
- 在Android Studio中点击运行按钮
- 或使用命令: `adb install app-debug.apk`

3. **登录测试**
- 用户名: 任意用户名
- 密码: 任意密码
- （演示模式无需真实服务器）

## 核心依赖

### 第三方库
```gradle
// Material Design
implementation 'com.google.android.material:material:1.11.0'

// 网络请求
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

// JSON解析
implementation 'com.google.code.gson:gson:2.10.1'

// ViewPager2
implementation 'androidx.viewpager2:viewpager2:1.0.0'

// SwipeRefreshLayout
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
```

## 网络通信

### HTTPS API
- 基于Retrofit2实现RESTful API
- 支持JWT Token认证
- 自动处理请求/响应拦截

### MQTT协议
- 连接阿里云EMQX服务器
- 订阅实时环境数据主题
- 发布设备控制指令
- 自动重连机制

## 数据存储

### SharedPreferences
- 用户登录信息
- 报警阈值设置
- 个性化设置（主题、单位等）
- 历史报警记录缓存

## 待完成功能

- [ ] 实际API接口对接
- [ ] MQTT实时数据订阅
- [ ] 推送通知集成
- [ ] 图表数据展示（MPAndroidChart）
- [ ] 仓库切换功能
- [ ] 用户注册功能
- [ ] 密码修改功能
- [ ] 数据导出功能
- [ ] 深色模式支持
- [ ] 多语言支持

## 注意事项

### 网络权限
应用需要以下权限（已在AndroidManifest.xml中声明）：
- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检查
- `VIBRATE` - 震动提醒
- `POST_NOTIFICATIONS` - 通知权限（Android 13+）

### 安全建议
1. 生产环境应使用HTTPS证书校验
2. Token应定期刷新
3. 敏感数据应加密存储
4. 建议添加ProGuard混淆

## 故障排查

### 编译错误
1. 清理项目: `Build -> Clean Project`
2. 重新构建: `Build -> Rebuild Project`
3. 更新SDK: `Tools -> SDK Manager`

### 运行时错误
1. 检查网络连接
2. 查看Logcat日志
3. 验证服务器地址配置

## 开发者信息

- **项目名称**: 智能地下仓库环境监测调控系统
- **版本**: 1.0.0
- **开发语言**: Java 17
- **UI框架**: Material Design
- **设计风格**: 微信风格

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题或建议，请联系开发团队。

---

**最后更新**: 2026年3月3日

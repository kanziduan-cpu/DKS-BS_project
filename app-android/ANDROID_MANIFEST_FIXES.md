# AndroidManifest.xml 关键问题修复说明

## 🔍 三个关键问题分析与解决

### 问题 1：缺少 usesCleartextTraffic 属性 ❌ → ✅ 已修复

**问题描述：**
- Android 9.0+ 默认禁止明文 HTTP 流量
- 即使配置了 `network_security_config.xml`，也必须在 `<application>` 标签中添加 `android:usesCleartextTraffic="true"`
- 这是导致 App 无法连接 HTTP 服务器的关键原因之一

**修复内容：**
```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="true"  <!-- ✅ 新增此行 -->
    ...>
```

**说明：**
- `android:networkSecurityConfig`：指定网络安全配置文件位置
- `android:usesCleartextTraffic`：全局允许明文流量
- 两个配置**缺一不可**，需要配合使用

---

### 问题 2：intent-filter 写法检查 ✅ 正确

**检查结果：**
```xml
<!-- SplashActivity 的 intent-filter 写法正确 ✅ -->
<activity
    android:name=".ui.SplashActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.WarehouseMonitor.NoActionBar">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**说明：**
- `<intent-filter>` 标签写法正确
- `android:exported="true"` 设置正确（启动页面必须为 true）
- MAIN 和 LAUNCHER 的 action/category 配置正确

---

### 问题 3：network_security_config.xml 验证 ✅ 正确

**文件路径：** `app/src/main/res/xml/network_security_config.xml`

**文件内容：**
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**配置说明：**
- `cleartextTrafficPermitted="true"`：允许明文 HTTP/MQTT 流量
- `<certificates src="system" />`：信任系统证书
- 该配置适用于毕设场景，无需 HTTPS

---

## 📋 完整的 Application 标签配置

```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@drawable/ic_app_logo"
    android:label="@string/app_name"
    android:roundIcon="@drawable/ic_app_logo"
    android:supportsRtl="true"
    android:theme="@style/Theme.WarehouseMonitor"
    android:networkSecurityConfig="@xml/network_security_config"     <!-- ✅ 网络安全配置文件 -->
    android:usesCleartextTraffic="true"                          <!-- ✅ 允许明文流量 -->
    android:requestLegacyExternalStorage="true"
    tools:targetApi="31">
    
    <!-- Activities 和 Services... -->
</application>
```

---

## 🔧 Android 9+ 网络安全机制说明

### 为什么需要两个配置？

| 配置位置 | 配置项 | 作用 |
|---------|-------|------|
| AndroidManifest.xml | `android:usesCleartextTraffic="true"` | 全局开关，允许应用使用明文流量 |
| xml/network_security_config.xml | `<base-config cleartextTrafficPermitted="true">` | 详细配置，指定哪些域名/IP允许明文流量 |

### 两个配置的区别：

1. **`android:usesCleartextTraffic`**（全局开关）
   - 位置：AndroidManifest.xml 的 `<application>` 标签
   - 作用：简单的全局开关
   - 优点：配置简单
   - 缺点：无法针对特定域名

2. **`networkSecurityConfig`**（详细配置）
   - 位置：AndroidManifest.xml 引用 + xml 文件
   - 作用：可以针对不同域名设置不同策略
   - 优点：更灵活，可以指定某些域名用 HTTPS，某些用 HTTP
   - 示例：
     ```xml
     <network-security-config>
         <base-config cleartextTrafficPermitted="true">
             <trust-anchors>
                 <certificates src="system" />
             </trust-anchors>
         </base-config>
         <domain-config cleartextTrafficPermitted="false">
             <domain includeSubdomains="true">example.com</domain>
         </domain-config>
     </network-security-config>
     ```

### 毕设场景建议：
- **同时配置两个**：最稳妥
- `android:usesCleartextTraffic="true"`：确保应用能正常工作
- `networkSecurityConfig`：为未来扩展留下空间

---

## 🧪 修改后测试步骤

### 1. 清理并重新编译
```bash
./gradlew clean
./gradlew assembleDebug
```

### 2. 卸载旧版本 App
```bash
adb uninstall com.warehouse.monitor
```

### 3. 安装新版本
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. 测试 HTTP 连接
- 启动 App
- 尝试登录功能（会调用 HTTP API）
- 检查 Logcat 日志，确认没有 "Cleartext traffic not permitted" 错误

### 5. 测试 MQTT 连接
- 观察首页连接状态圆点
- 应该显示绿色（已连接）

---

## 📊 配置检查清单

| 检查项 | 状态 | 说明 |
|-------|------|------|
| `android:usesCleartextTraffic="true"` | ✅ 已修复 | 必须添加 |
| `android:networkSecurityConfig` | ✅ 已配置 | 引用 xml 文件 |
| `network_security_config.xml` | ✅ 正确 | 内容正确 |
| `INTERNET` 权限 | ✅ 已配置 | 正确 |
| 服务器地址 (ApiService.java) | ✅ 正确 | 43.99.24.178 |
| HTTP 端口 (ApiService.java) | ✅ 正确 | 8000 |
| MQTT 端口 (MqttConfig.java) | ✅ 正确 | 1883 |
| intent-filter 写法 | ✅ 正确 | SplashActivity 配置正确 |

---

## 🐛 常见错误及解决方案

### 错误 1：Cleartext traffic not permitted
**原因：** 缺少 `android:usesCleartextTraffic="true"`  
**解决：** 在 AndroidManifest.xml 的 `<application>` 标签中添加该属性

### 错误 2：Connection refused
**原因：** 服务器地址或端口错误  
**解决：** 检查 ApiService.java 中的 BASE_URL 是否正确

### 错误 3：SSLHandshakeException
**原因：** 服务器证书问题（HTTPS）  
**解决：** 确保使用 HTTP 而不是 HTTPS（毕设场景）

### 错误 4：App 无法启动
**原因：** AndroidManifest.xml 语法错误  
**解决：** 检查 XML 标签是否闭合，属性拼写是否正确

---

## 🎯 总结

### 已完成的修复：
1. ✅ 添加 `android:usesCleartextTraffic="true"` 到 `<application>` 标签
2. ✅ 验证 `intent-filter` 写法正确
3. ✅ 验证 `network_security_config.xml` 内容正确
4. ✅ 修复 ApiService.java HTTP 端口（8080 → 8000）

### 下一步：
1. 清理并重新编译项目
2. 卸载旧版本 App
3. 安装新版本并测试
4. 检查 Logcat 日志确认无错误

---

## 📞 如果还有问题

请提供以下信息：
1. Logcat 中的错误日志
2. App 具体哪个功能无法使用
3. 网络环境（WiFi/4G/5G）
4. 服务器是否正常运行

祝您毕设顺利！🎓

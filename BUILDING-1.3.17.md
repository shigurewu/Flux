# Flux 1.3.17 构建与来源

## 来源

- 个人基线：`d73829a7a3b9591f292f2507be37b3678da2b640`（1.3.13，versionCode 54）。
- 选取功能的上游：[makise2060/FluxTie](https://github.com/makise2060/FluxTie)，截至 `4956c452525b1e3a7dc46dcb6b57ba2fc78dc01e`（1.4.0-beta.1）。
- 交付版本：1.3.17，versionCode 58，应用 ID `com.shigurewu.flux`。
- 未继承原工作区未提交的 1.3.14–1.3.16 修改。没有合入上游消息标签栏、信息流长图/热榜样式或底部导航及下拉刷新改版。
- 保留原工程 GPL-3.0 许可证与署名。

## 本地构建

使用 JDK 17、Android SDK 37、Build Tools 36.0.0 和 Gradle 9.7.1。首次构建需要下载工程依赖。可通过 `local.properties` 配置 `sdk.dir`。

```powershell
gradle :app:assembleRelease
gradle :app:testDebugUnitTest
node tools/verify-im-database.cjs
```

第三条需要支持 `node:sqlite` 的 Node.js（本次使用 24.21）。Gradle 配置使用 6 GiB 堆与单工作线程，避免编译大批生成协议源码时内存不足；可按机器内存调整。

源码交付包不包含签名私钥、密码、个人 SDK 路径或构建缓存。自建发行包需自行配置签名；不同签名无法直接覆盖原安装。原有签名配置读取方式参见 `app/build.gradle.kts`，不要将私钥或 `keystore.properties` 上传到公共仓库。

## 验证边界

- JVM 测试覆盖 IM 编解码/加密、消息去重后的会话更新、断线收集器退出、柔和文字开关与亮色容器对比度等。
- 主机 SQLite 检查执行工程内实际迁移 SQL，检查 39/40/41 → 42、已有草稿保留、重复消息忽略、账号隔离和已读截止位置。
- Android Room 迁移及 Material 调色板仪器测试已随源码提供，但本次没有可用模拟器/设备，未执行仪器测试。调色板调用 Android 原生颜色 API，因此其集成断言放在设备测试中；纯颜色覆盖策略由 JVM 测试验证。
- 未使用真实账号验证服务端私信收发、后台驻留、断网切号全过程，亦未进行真机逐页深色模式视觉验收。
- 上游私信当前范围是前台文本收发；初次连接从服务端当前游标开始，旧历史不会自动回填。

建议真机依次确认：1.3.13 覆盖安装及原数据保留；消息第四行私信入口；两账号相互收发与未读；发送失败保留输入；切号和重连；深色柔和文字开关在帖子/回复/消息/设置/个人页及弹层中的效果。

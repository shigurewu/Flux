<p align="center">
  <img src="docs/images/logo.png" width="112" alt="Flux" />
</p>

# Flux

一个基于 [FluxTie](https://github.com/makise2060/FluxTie)（上游基于 [TiebaLite](https://github.com/huanchengfly/tieba-post)）的个人分支，
以「轻量、玻璃质感、顺手的交互」为方向做持续改进。

> 非官方第三方客户端，与百度 / 贴吧官方无关。

## 与上游的关系

- 基线：上游 `v1.2.0`（commit `edf61d0`）
- 本仓库在上游基础上做了界面与交互层面的改动（见 [`CHANGELOG.md`](CHANGELOG.md)）
- 后续跟版：`git fetch upstream && git merge upstream/main`

## 功能

延续上游能力（液态玻璃底栏、紧凑卡片、查看楼层、保存原图、竖屏视频、缓存清理等），并在其上做了这些改动：

### 帖子页

- 底栏改为「左：头像 + 评论框；右：点赞 / 评论 / 收藏」，不再有 ⋮（菜单保留在顶栏）
- 点评论框不跳转，在本页弹出**快捷回复面板**：跟随键盘上移，点外部 / 返回键 / 发送成功都会收起，并防重复提交
- 主楼标题加粗；字号保持克制，只放开行距与字间距
- 楼中楼从外部进入定位到阅读区 30% 位置，并有一次中性灰呼吸高亮
- 点赞数显示为净分值（`+12 / -3 / 0`）
- 列表触底显示「已经到底了」，并保留额外拉出间距

### 信息流

- 卡片头部显示**所在吧**（原版贴吧风格）：吧头像、吧名（自动补「吧」后缀）、关注人数与贴子数；可在设置里切回作者模式
- 图片按真实宽高比展示，同时限制单图高度，整页截图不会占满一屏
- 动态页玻璃圆环下拉刷新；再次点击底栏「动态」平滑回顶并刷新；不在顶部时底栏图标变为刷新圆圈

### 消息

- 消息中心改为分类入口（回复我的 / 提到的我 / 赞我的），有未读显示红点，支持「全部已读」
- 分类列表为贴吧式条目：头像 + 昵称/时间 + 动作文案 + 引用块 + 吧名
- 设置里可「屏蔽小红点」，同时关掉底栏消息角标

### 其他

- 个人页帖子卡片上的可见性胶囊（公开 / 仅自己可见），长按删除
- 发主题时可选「仅自己可见」
- 关闭应用内更新检查、备份排除账号数据、收紧明文流量

## 构建

环境要求：

| 项 | 版本 |
|---|---|
| JDK | 17 |
| Gradle | 9.7.1（仓库自带 wrapper） |
| Android SDK | compileSdk 37，build-tools 36.0.0 |
| Kotlin | 2.3.x |

```bash
# 1. 准备 local.properties（不要提交）
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 2. 准备签名（不要提交）
cat > keystore.properties <<'EOF'
keystore.file=your.jks
keystore.password=******
keystore.key.alias=your_alias
keystore.key.password=******
EOF

# 3. 构建
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

## 致谢

本项目不是从零写的，能跑起来几乎全靠上游：

- **[FluxTie](https://github.com/makise2060/FluxTie)** —— 本仓库的直接上游，基线为它的 `v1.2.0`。
  Flux 的绝大多数功能、架构与交互设计来自这里，感谢作者的长期维护。
- **[TiebaLite](https://github.com/huanchengfly/tieba-post)** —— FluxTie 的上游，也是这条脉络的起点。
  第三方贴吧客户端里少见的完整实现，感谢作者打下的地基。

也感谢上游所有提交者、issue 报告者，以及一路把接口与协议摸清楚的同好。

本仓库与上述项目及作者均无隶属关系，这里的改动由本仓库自行维护；若上游愿意，欢迎把其中有用的部分反向合并回去。

## 许可

本项目以 **GNU General Public License v3.0** 发布，见 [`LICENSE`](LICENSE)。

上游版权归其作者所有；本仓库的修改同样以 GPL-3.0 授权。
如果你分发本项目的 APK，需要同时提供对应版本的源码。

## 免责声明

- 本项目为个人学习与自用目的开发的非官方客户端，与百度公司及贴吧官方无任何关联；
- 使用第三方客户端登录存在账号风险，请自行判断；
- 贴吧接口可能随时变更，导致部分功能失效，这是此类项目的固有限制。

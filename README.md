<p align="center">
  <img src="docs/images/logo.png" width="112" alt="Flux" />
</p>

<p align="center">
  <a href="https://github.com/shigurewu/Flux/releases/latest"><img src="https://img.shields.io/github/v/release/shigurewu/Flux?label=latest&color=3b82f6" alt="latest release" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="GPL-3.0" />
</p>

# Flux

**个人自用**的贴吧客户端，基于 [FluxTie](https://github.com/makise2060/FluxTie)（上游基于 [TiebaLite](https://github.com/huanchengfly/tieba-post)）做的一点个性化修改。

说清楚一点：**这个仓库几乎就是原版**。功能、架构、界面绝大多数都来自上游，我只是在少数几个自己用着不太顺手的地方做了小改动，图个顺手而已。不是要另做一个客户端，也不打算维护成一个产品，更不建议拿它当主力推荐给别人。

想要更完整、有人正经维护的版本，请直接去 [FluxTie](https://github.com/makise2060/FluxTie)。

> 非官方第三方客户端，与百度 / 贴吧官方无关。

## 与原版的关系

- 基线：上游 `v1.2.0`（commit `edf61d0`），**没有大改结构，没有重写模块**
- 改动范围：基本都在界面与交互层面，逐条列在 [`CHANGELOG.md`](CHANGELOG.md) 里
- 后续跟版：`git fetch upstream && git merge upstream/main`

如果上游合了其中某些改动，这个仓库随时可以放弃对应补丁、直接跟上游。

## 相对原版改了什么

下面这些是**我个人不顺手才动的地方**，都不涉及核心逻辑：

### 帖子页

- 底栏改为「左：头像 + 评论框；右：点赞 / 评论 / 收藏」，不再有 ⋮（菜单保留在顶栏）
- 点评论框不跳转，在本页弹出**快捷回复面板**：跟随键盘上移，点外部 / 返回键 / 发送成功都会收起，并防重复提交
- 主楼排版单独调过：标题 18sp / 字重 600 / 行高 1.35，正文 14.5sp / 行高 1.7 / 字距 0，按换行拆成独立段落
- 标题以全角左括号或引号开头时做**悬挂标点**，让第一个汉字与正文左对齐
- 楼中楼从外部进入定位到阅读区 30% 位置，并有一次中性灰呼吸高亮
- **楼中楼图片可点开**：缩略图直接进大图查看器，左右滑动切换同楼层图片
- 点赞数显示为净分值（`+12 / -3 / 0`）
- 列表触底显示「已经到底了」，并保留额外拉出间距

### 信息流

- 卡片头部显示**所在吧**（原版贴吧风格）：吧头像、吧名（自动补「吧」后缀）、关注人数与贴子数；可在设置里切回作者模式
- 图片按真实宽高比展示，同时限制单图高度，整页截图不会占满一屏
- 玻璃圆环下拉刷新；再次点击底栏「动态」平滑回顶并刷新

### 消息

- 消息中心改为分类入口（回复我的 / 提到的我 / 赞我的），有未读显示红点，支持「全部已读」
- 分类列表为贴吧式条目：头像 + 昵称/时间 + 动作文案 + 引用块 + 吧名
- 设置里可「屏蔽小红点」

### 其他零碎

- 应用内更新检查指向**本仓库**的 Release，可覆盖安装升级
- 个人页帖子卡片上的可见性胶囊（公开 / 仅自己可见），长按删除
- 发主题时可选「仅自己可见」
- 备份排除账号数据、收紧明文流量

## 下载

从 [Releases](https://github.com/shigurewu/Flux/releases/latest) 下载最新的 `Flux-vX.Y.Z.apk` 直接安装。

- 应用内也带更新检查（设置 → 关于 → 检查更新），检查的是**本仓库**的 Release
- 首次应用内安装需要允许「安装未知应用」
- 如果某次更新装不上，可以先用浏览器下载 APK 手动装一次

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

## 致谢（要谢就去谢他们，别谢我）

这里的东西几乎都不是我写的。真正把客户端做出来的是上游：

- **[FluxTie](https://github.com/makise2060/FluxTie)** —— 本仓库的直接上游，基线为它的 `v1.2.0`。
  功能、架构、交互设计基本都来自这里，作者长期维护才是这个客户端能用的原因。
- **[TiebaLite](https://github.com/huanchengfly/tieba-post)** —— FluxTie 的上游，也是这条脉络的起点。
  第三方贴吧客户端里少见的完整实现，感谢作者打下的地基。

**觉得好用的话，功劳是他们的。** 去上游点 star、提 issue、反馈问题，都比谢我有用得多。
我在这个仓库里做的只是几个自己用着不顺手的补丁，随时可能被上游一个提交取代。

也感谢上游所有提交者、issue 报告者，以及一路把接口与协议摸清楚的同好。

本仓库与上述项目及作者均无隶属关系；若上游需要，欢迎把这里有用的部分直接拿回去。

## 许可

本项目以 **GNU General Public License v3.0** 发布，见 [`LICENSE`](LICENSE)。

上游版权归其作者所有；本仓库的修改同样以 GPL-3.0 授权。
如果你分发本项目的 APK，需要同时提供对应版本的源码。

## 免责声明

- 本项目为个人学习与自用目的开发的非官方客户端，与百度公司及贴吧官方无任何关联；
- 使用第三方客户端登录存在账号风险，请自行判断；
- 贴吧接口可能随时变更，导致部分功能失效，这是此类项目的固有限制。

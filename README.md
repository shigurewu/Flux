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

延续上游能力（液态玻璃底栏、紧凑卡片、吧名模式、查看楼层、保存原图、竖屏视频、缓存清理等），并在此基础上增加：

- 帖子页底栏「评论」入口、顶部 ⋮ 菜单（字体大小 / 帖子隐私 / 只看楼主 / 删除）
- 楼中楼从外部进入时定位到阅读区 30% 位置，并有一次中性的灰色呼吸高亮
- 点赞数显示为净分值（`+12 / -3 / 0`）
- 列表触底显示「已经到底了」，并保留额外拉出间距
- 消息页「赞我的」列表
- 个人页帖子卡片上的可见性胶囊（公开 / 仅自己可见），长按删除
- 发主题时可选「仅自己可见」
- 动态页：玻璃圆环下拉刷新；再次点击底栏「动态」平滑回顶并刷新；不在顶部时底栏图标变为刷新圆圈
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

## 许可

本项目以 **GNU General Public License v3.0** 发布，见 [`LICENSE`](LICENSE)。

上游版权归其作者所有；本仓库的修改同样以 GPL-3.0 授权。
如果你分发本项目的 APK，需要同时提供对应版本的源码。

## 免责声明

- 本项目为个人学习与自用目的开发的非官方客户端，与百度公司及贴吧官方无任何关联；
- 使用第三方客户端登录存在账号风险，请自行判断；
- 贴吧接口可能随时变更，导致部分功能失效，这是此类项目的固有限制。

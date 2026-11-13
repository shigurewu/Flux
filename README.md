<p align="center">
  <img src="docs/images/logo.png" width="112" alt="Flux" />
</p>

<p align="center">
  <a href="https://github.com/shigurewu/Flux/releases/latest"><img src="https://img.shields.io/github/v/release/shigurewu/Flux?label=latest&color=3b82f6" alt="latest release" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="GPL-3.0" />
</p>

# Flux

非官方的贴吧客户端。基于 [FluxTie](https://github.com/makise2060/FluxTie) 做的一点个性化修改，功能和逻辑大多来自上游。

<p align="center">
  <a href="https://github.com/shigurewu/Flux/releases/latest"><b>下载最新版</b></a>
</p>

## 能做什么

**刷帖**
- 信息流卡片显示所在吧：吧头像、吧名、关注人数与贴子数
- 图片按真实比例显示，长截图不会占满一屏
- 下拉刷新、再次点击底栏回到顶部

**看帖**
- 帖子详情排版优化：标题更大、行距更松、段落独立分开
- 楼中楼自动定位到上次看到的位置，图片可点开看大图
- 点赞数显示净分值（+12 / -3 / 0）

**回帖**
- 底部评论框点开即输入，不用跳转页面
- 支持只看楼主、调字号、帖子隐私、收藏

**消息**
- 分为「回复我的 / 提到我的 / 赞我的」三类，有未读显示红点
- 可以一键全部已读，也可以在设置里关掉红点

**个人主页**
- 帖子卡片显示公开 / 仅自己可见
- 粉丝数、回帖数可点开查看

**外观**
- 深色、AMOLED、动态取色
- 深色模式下文字柔和，不刺眼；可在设置里开关
- 行内表情随字号缩放

## 安装

1. 从 [Releases](https://github.com/shigurewu/Flux/releases/latest) 下载最新的 `Flux-x.y.z.apk`
2. 覆盖安装即可，无需卸载旧版
3. 首次安装需要在系统里允许「安装未知应用」

应用内也带更新检查，会检查本仓库的版本。

## 关于这个项目

这不是一个独立开发的新客户端。功能、架构、交互设计基本都来自上游，这里只是把一些自己用着不顺手的地方改了改。

- 直接上游：[FluxTie](https://github.com/makise2060/FluxTie)
- 上游的上游：[TiebaLite](https://github.com/huanchengfly/tieba-post)

**觉得好用的话，功劳是他们的。** 去上游点 star、提 issue，都比谢作者有用。若上游需要，欢迎把这里有用的部分直接拿回去。

## 从源码构建

需要 JDK 17、Android SDK 37、Gradle 9.7.1。配好 `local.properties` 与签名用的 `keystore.properties` 后：

```bash
./gradlew :app:assembleRelease
```

产物在 `app/build/outputs/apk/release/`。更详细的构建说明见 [`BUILDING-1.3.18.md`](BUILDING-1.3.18.md)。

## 许可

以 **GNU General Public License v3.0** 发布，完整条款见 [`LICENSE`](LICENSE)。上游版权归其作者所有，本仓库的修改同样以 GPL-3.0 授权；每个发布版本都对应仓库内同名的 Git 标签，可据此获取源码。版权归属与修改声明见 [`NOTICE`](NOTICE)。

## 免责声明

- 本项目为个人自用开发，与百度公司及贴吧官方**无任何关联**
- 使用第三方客户端**可能导致贴吧账号被封禁或限制**，风险由使用者自行承担
- 不含广告、统计埋点与付费功能，不收集、不上传你的个人数据

## 反馈

有问题或建议请到 [Issues](https://github.com/shigurewu/Flux/issues) 提出。

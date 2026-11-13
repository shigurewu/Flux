# 1.3.19-im.1 私信握手修复

本版基于 1.3.18 修订 2，内部版本号 61。保留个人主页公开/仅自己可见的混合展示和状态校验、动态页刷新修复。重新开放消息列表模式 1 的第四行私信入口与个人主页私信入口，供实际账号验证。

## 已查明的原因

截图中的 `Request header not permitted: 'Sec-WebSocket-Extensions'` 是 OkHttp 5.3.2 在发起网络请求之前拒绝调用方直接设置扩展头产生的异常，不是贴吧返回的账号错误。

直接删掉请求头仍不完整：OkHttp 会自动添加 `permessage-deflate`，而 2026-11-13 在本机对 im.tieba.baidu.com:8000 的无账号握手对照中：

- `im_version=2.3`：HTTP 101，Sec-WebSocket-Accept 校验通过；
- 无扩展头：HTTP 101，校验通过；
- `permessage-deflate`：服务器在升级完成前关闭连接。

因此仅给独立 IM 客户端安装 application interceptor，在 OkHttp 生成升级请求后把扩展头替换为贴吧版本标记，不协商 WebSocket 压缩。响应只允许空扩展或完全匹配的版本标记；未经请求的压缩或其他扩展仍然拒绝。HTTP 状态、Upgrade、Connection、Sec-WebSocket-Accept、帧合法性继续由 OkHttp 校验。

使用实际编译出的生产适配器和当前 OkHttp 库连接真实端点，已进入 onOpen，状态为 HTTP 101。该探测没有提供账号、没有鉴权、没有发送或读取私人消息。

## 其他必要修正

- 鉴权响应补全 userInfo 和 isUserAvailable 字段。返回用户必须与当前登录账号一致且通过可用性校验，否则不能显示已连接。
- 未返回私信消息组时明确提示不能收发。
- 区分连接、账号鉴权、收信阶段的失败；缺少 STOKEN 提示重新登录。
- 发送响应没有正数 msgId 时视为未确认，保留草稿，避免误报发送成功。
- 推送拉取保留协程取消，切号和停止时不吞掉取消信号。

## 验证边界

本地 TCP 服务测试实际 OkHttp 升级过程，包括旧错误重现、版本头发出、无扩展/版本标记响应、拒绝未经请求的压缩、二进制消息和真实 ImConnection 请求响应配对。另测试空鉴权、错账号、不可用账号、有效鉴权和发送确认。

没有在手机上验证，也没有使用用户登录态完成鉴权、文本收发和已读回执。因此本版是私信测试版，不代表实际收发已经确认可用。仍限文本，历史消息暂不回填；没有独立后台保活。

安装后可从原消息分类列表的私信行进入。若仍失败，页面会显示具体失败阶段；只需提供错误截图，不要提供 BDUSS、STOKEN 或其他登录凭据。实际收发需用户自行与熟悉的联系人验证。

## 参考实现

- [aiotieba WebSocket 握手](https://github.com/lumina37/aiotieba/blob/5baf2ab5539606c0b346cba7105145fe7330129b/src/aiotieba/core/websocket/core.py)
- [aiotieba 鉴权及可用性校验](https://github.com/lumina37/aiotieba/blob/5baf2ab5539606c0b346cba7105145fe7330129b/src/aiotieba/api/upload_ws_sec/_api.py)
- [aiotieba 发送确认](https://github.com/lumina37/aiotieba/blob/5baf2ab5539606c0b346cba7105145fe7330129b/src/aiotieba/api/send_msg/_api.py)

这些代码是协议参考，不是贴吧官方的可用性承诺。未替换应用其他页面使用的网络库。

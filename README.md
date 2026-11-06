# plugin-moments

Halo 2.0 的瞬间管理插件，提供一个轻量级的内容发布功能，支持发布图文、视频、音频等内容。

![Preview](./images/plugin-moments-preview.png)

## 功能特性

- 支持发布图文、视频、音频等多媒体内容
- 情感寄托：选择心情，一键获取当前位置和天气，随瞬间保存当时的感受
- 后来的我：给旧瞬间追加带时间的后续，留下后来变化，保留当时的原文
- 提供标签管理功能，支持按标签筛选瞬间
- 主题端 `/moments` 列表路由和 `/moments/{name}` 详情路由
- 提供匿名可访问的公共 REST API，方便前端框架构建客户端渲染瞬间列表
- RSS 订阅支持，路由为 `/moments/rss.xml`
- 支持将瞬间数据同步至 Halo 搜索系统
- 支持通过 MCP Server 查询、获取、发布和删除瞬间

## 安装使用

1. 下载，目前提供以下两个下载方式：
    - Halo 应用市场：<https://halo.run/store/apps/app-SnwWD>
    - GitHub Releases：访问 [Releases](https://github.com/halo-sigs/plugin-moments/releases) 下载 Assets 中的 JAR 文件。
2. 安装，插件安装和更新方式可参考：<https://docs.halo.run/user-guide/plugins>
3. 安装完成之后，访问 Console 左侧的**瞬间**菜单项，即可进行管理。
4. 前台访问地址为 `/moments`，需要注意的是，此插件需要主题提供模板（`moments.html`）才能访问 `/moments`。
5. 此插件也提供了 RSS 订阅的路由，可以访问 `/moments/rss.xml`。
6. 此插件将数据同步至 Halo 搜索，type 为 `moment.moment.halo.run`。

## 情感寄托

在 Console 或个人中心的瞬间发布框中，点击图片按钮旁的爱心图标，在浮层中选择心情，再点击「获取位置和天气」。也可以手动填写，或只记录心情。点击「完成」、右上角关闭按钮、浮层外部或按 Esc 可收起浮层，内容会保留在当前瞬间草稿中；再次点击已选心情可以取消，点击「移除」可删除整份寄托。

自动获取需要 HTTPS（本地 localhost 也可）和浏览器定位权限。仅在点击获取时，将设备当前位置发送至 [BigDataCloud](https://www.bigdatacloud.com/docs/free-api/free-reverse-geocode-to-city-api) 获取地名，以及 [Open-Meteo](https://open-meteo.com/en/docs) 获取天气；不保存经纬度。任一服务失败时可重试或手动填写，不影响其他内容。

位置和天气以获取时的快照保存，随瞬间的公开/私有状态展示。默认免密钥天气服务适用于非商业用途，商业使用请遵守 [Open-Meteo 使用条款](https://open-meteo.com/en/terms)。插件内置列表和详情模板已支持展示，自定义主题可参考[情感寄托适配说明](./dev/theme-api.md#情感寄托)。

## 后来的我

在 Console 或个人中心，自己的瞬间下方会显示「写下后续」。输入现在想说的话并点击「追加后续」，原文下方就会新增一条带时间的记录。支持追加多次，每条最多 1000 个字符，每条瞬间最多 100 条后续。

后续只追加纯文本，时间和编号由服务器生成，不修改原文、发布时间、心情、天气或位置。只有作者本人且拥有相应发布/管理权限时可以追加；没有免审核权限的个人中心用户追加后，整条瞬间会重新进入审核。后续与原瞬间沿用相同的可见性。普通编辑会保留已追加的后续。

Finder、列表和详情 API 会通过 `spec.followUps` 一起返回后续记录。插件内置模板已支持展示，自定义主题可参考[主题适配示例](./dev/theme-api.md#后来的我)。

地球主题适配后的展示效果：

![情感寄托与后来的我](./images/moments-emotion-follow-ups-preview.jpg)

## MCP Server 集成

安装并启用 [Halo MCP Server](https://github.com/halo-dev/plugin-mcp-server) 1.x 后，本插件会自动提供以下 MCP 工具：

| 工具 | 说明 |
| --- | --- |
| `PluginMoments__list_moments` | 分页查询瞬间，支持按作者、标签、可见性、审核状态和发布时间筛选 |
| `PluginMoments__get_moment` | 按资源名称获取瞬间详情、作者和统计信息 |
| `PluginMoments__create_moment` | 以当前 MCP 访问密钥所有者的身份发布图文瞬间 |
| `PluginMoments__delete_moment` | 按资源名称永久删除瞬间，调用前必须获得用户明确确认 |

需要在 MCP Server 的访问密钥设置中选择需要开放的工具。未安装 MCP Server 时，不影响瞬间插件的其他功能。

## 主题适配

此插件为主题端提供了：

- **列表路由** `/moments`（模板 `moments.html`）和**详情路由** `/moments/{name}`（模板 `moment.html`）
- **Finder API**（`momentFinder`）：可在主题任意位置渲染瞬间列表，无需依赖路由页面
- **公共 REST API**：供前端框架构建客户端渲染瞬间列表使用

详细的主题适配文档请参考：

- [主题 API 文档](./dev/theme-api.md) — 模板路由、模板变量、Finder API、类型定义
- [REST API 文档](./dev/rest-api.md) — 公共 API 端点说明

## 开发文档

- [开发环境搭建](./dev/development.md) — 本地开发、构建、测试

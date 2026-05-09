# REST API 文档

本文档介绍 plugin-moments 提供的公共 REST API。

> **在线查看完整 Swagger 文档**：访问 [Swagger Editor](https://editor.swagger.io/)，点击左上角 **File → Import URL**，输入以下地址即可：

```
https://raw.githubusercontent.com/halo-sigs/plugin-moments/refs/heads/main/api-docs/openapi/v3_0/momentsApi.json
```

## 公共 API（匿名可访问）

此插件提供了一组公共、匿名、只读的 JSON API，位于 `api.moment.halo.run/v1alpha1`，方便使用 React / Vue / Svelte 等前端框架构建客户端渲染瞬间列表的主题使用。

### 端点列表

| 端点 | 方法 | 说明 |
| ---- | ---- | ---- |
| `/apis/api.moment.halo.run/v1alpha1/moments` | `GET` | 分页列出瞬间，支持 `page`、`size`、`tag`、`ownerName`、`startDate`、`endDate`、`sort` 查询参数 |
| `/apis/api.moment.halo.run/v1alpha1/moments/{name}` | `GET` | 根据 `metadata.name` 获取单个瞬间详情 |

### 查询瞬间列表

`/apis/api.moment.halo.run/v1alpha1/moments`

**参数**：

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| `page` | `int` | 分页页码，从 1 开始 |
| `size` | `int` | 分页条数 |
| `tag` | `string` | 标签名称，用于筛选 |
| `ownerName` | `string` | 创建者用户名 name |
| `startDate` | `string` | 开始时间，通过时间区间筛选发布时间 |
| `endDate` | `string` | 结束时间 |
| `sort` | `string[]` | 排序字段，格式为 `字段名,排序方式`，排序方式可选值为 `asc` 或 `desc`，如 `spec.releaseTime,desc` |

**返回值类型**：`ListResult<MomentVo>`

### 查询瞬间详情

`/apis/api.moment.halo.run/v1alpha1/moments/{name}`

**参数**：

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| `name` | `string` | 瞬间的唯一标识 name |

**返回值类型**：`MomentVo`

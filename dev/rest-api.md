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

每条记录的情感寄托位于 `items[i].spec.emotionalAttachment`，字段结构见[情感寄托字段](#情感寄托字段)。

### 查询瞬间详情

`/apis/api.moment.halo.run/v1alpha1/moments/{name}`

**参数**：

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| `name` | `string` | 瞬间的唯一标识 name |

**返回值类型**：`MomentVo`

情感寄托位于 `spec.emotionalAttachment`，与 Finder API 和列表中单条记录的结构一致。

### 情感寄托字段

`spec.emotionalAttachment` 是可选对象，未填写时可能缺失或为 `null`，对象内的各字段也均为可选。客户端需兼容旧瞬间、仅填写心情、部分获取成功，以及用户移除寄托的情况。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `mood` | `string` | 用户选择的心情，如 `😌 平静`，最长 32 个字符 |
| `location` | `string` | 城市或地区名称，最长 100 个字符；不包含经纬度 |
| `weather` | `string` | 保存时的天气文本，如 `晴 · 22°C`，最长 64 个字符；也可手动填写 |
| `weatherSource` | `string` | 自动获取的天气来源，目前为 `open-meteo`；手动填写或修改天气时可省略 |

详情响应示例（节选）：

```json
{
    "metadata": {
        "name": "moment-example"
    },
    "spec": {
        "content": {
            "raw": "<p>散步之后，心情慢慢平静下来。</p>",
            "html": "<p>散步之后，心情慢慢平静下来。</p>",
            "medium": []
        },
        "owner": "admin",
        "visible": "PUBLIC",
        "emotionalAttachment": {
            "mood": "😌 平静",
            "location": "杭州市",
            "weather": "晴 · 22°C",
            "weatherSource": "open-meteo"
        }
    }
}
```

请求示例：

```javascript
const response = await fetch("/apis/api.moment.halo.run/v1alpha1/moments?page=1&size=10");
if (!response.ok) {
    throw new Error("获取瞬间失败");
}
const result = await response.json();
for (const moment of result.items) {
    const emotion = moment.spec.emotionalAttachment;
    console.log({
        name: moment.metadata.name,
        mood: emotion?.mood ?? "",
        location: emotion?.location ?? "",
        weather: emotion?.weather ?? ""
    });
}
```

这些字段保存的是用户记录时的快照，查询接口不会重新定位或请求天气服务，也不会返回精确坐标。公开查询继续遵循瞬间的可见性、审核及删除状态规则。公共 API 仅提供读取；创建、修改和移除寄托需通过具备权限的 Console 或个人中心接口保存 `spec.emotionalAttachment`。

模板和 Finder 调用示例见[主题 API 文档](./theme-api.md#情感寄托)。

### 后来的我字段

列表和详情继续使用原有接口，追加的后续位于 `spec.followUps`（列表中为 `items[i].spec.followUps`），无需单独请求。未追加时此字段可能缺失、为 `null` 或为空数组。

```json
{
    "followUps": [
        {
            "id": "server-generated-id",
            "content": "当时很难过的事，现在已经放下了。",
            "createdAt": "2026-11-06T08:00:00Z"
        }
    ]
}
```

数组按追加顺序排列，`id` 和 ISO 8601 格式的 `createdAt` 由服务器生成，`content` 为纯文本。原文和发布时间继续从 `spec.content`、`spec.releaseTime` 读取；客户端展示后续时应使用文本插值或 `textContent`，不应直接作为 HTML 渲染。

## 追加后续

追加操作需要登录及对应权限，公共只读 API 和 Finder 均不会开放匿名写入。

| 端点 | 方法 | 权限 |
| ---- | ---- | ---- |
| `/apis/console.api.moment.halo.run/v1alpha1/moments/{name}/follow-ups` | `POST` | 瞬间管理权限，且必须是该瞬间作者 |
| `/apis/uc.api.moment.halo.run/v1alpha1/moments/{name}/follow-ups` | `POST` | 个人中心瞬间发布权限，且必须是该瞬间作者 |

请求头为 `Content-Type: application/json`，请求体：

```json
{
    "content": "当时很难过的事，现在已经放下了。"
}
```

内容去除首尾空白后须为 1～1000 个字符，每条瞬间最多追加 100 条。不要传入 `id` 或 `createdAt`，客户端传入的这些值不会作为记录信息使用。成功返回 `200` 和更新后的 `Moment`，后续位于 `spec.followUps`。

错误状态：未登录返回 `401`（或进入 Halo 登录流程）；无发布/管理权限返回 `403`；瞬间不存在、已删除或属于他人返回 `404`；内容为空、过长或后续达到上限返回 `400`。

追加只更新后续列表，不修改原文、发布时间、可见性或情感寄托。没有免审核权限的个人中心用户追加后，整条瞬间会重新进入审核；已有审核未通过的内容不会因追加而自动获准公开。普通个人中心编辑接口会保留已有后续，不能用完整替换 `spec` 的方式覆盖这些记录。

主题展示和 Finder 示例见[后来的我](./theme-api.md#后来的我)。

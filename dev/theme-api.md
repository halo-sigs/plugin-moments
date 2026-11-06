# 主题 API 文档

本文档介绍 plugin-moments 为主题端提供的模板路由、模板变量和 Finder API。

## 路由

### 列表页面 /moments

- 模板路径：`/templates/moments.html`
- 访问路径：`/moments?tag={tag}` 或 `/moments/page/{page}?tag={tag}`

#### 路由可选参数

| 参数 | 说明 |
| ---- | ---- |
| `tag` | 标签名称，用于筛选 |
| `page` | 分页页码（从 1 开始） |

#### 模板变量

| 变量 | 类型 | 说明 |
| ---- | ---- | ---- |
| `moments` | `UrlContextListResult<MomentVo>` | 当前页瞬间分页结果 |
| `tags` | `List<MomentTagVo>` | 所有标签列表 |

`tags` 示例：

```html
<ul>
    <li th:each="tag : ${tags}">
        <a
            th:href="|/moments?tag=${tag.name}|"
            th:classappend="${#lists.contains(param.tag, tag.name) ? 'active' : ''}"
        >
            <span th:text="${tag.name}"></span>
            <span th:text="${tag.momentCount}"></span>
        </a>
    </li>
</ul>
```

`moments` 示例：

```html
<div>
    <ul>
        <li th:each="moment : ${moments.items}" th:with="content=${moment.spec.content}">
            <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
            <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
                <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
                <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
                <audio th:if="${momentItem.type.name == 'AUDIO'}" th:src="${momentItem.url}" controls="true"></audio>
            </th:block>
        </li>
    </ul>
    <div th:if="${moments.hasPrevious() || moments.hasNext()}">
        <a th:href="@{${moments.prevUrl}}">
            <span>上一页</span>
        </a>
        <span th:text="${moments.page}"></span>
        <a th:href="@{${moments.nextUrl}}">
            <span>下一页</span>
        </a>
    </div>
</div>
```

---

### 详情页面 /moments/{name}

- 模板路径：`/templates/moment.html`
- 访问路径：`/moments/{name}`

#### 模板变量

| 变量 | 类型 | 说明 |
| ---- | ---- | ---- |
| `moment` | `MomentVo` | 当前瞬间 |

示例：

```html
<div>
    <div th:with="content=${moment.spec.content}">
        <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
        <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
            <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
            <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
            <audio th:if="${momentItem.type.name == 'AUDIO'}" th:src="${momentItem.url}" controls="true"></audio>
        </th:block>
    </div>
</div>
```

---

### 搜索路由

此插件将数据同步至 Halo 搜索，搜索类型为：

| 变量 | 值 |
| ---- | ---- |
| `type` | `moment.moment.halo.run` |

---

## Finder API

Finder API 由 `momentFinder` 对象提供，可在主题模板的任意位置使用，无需依赖路由页面。

返回瞬间的 Finder 方法都通过 `MomentVo.spec.emotionalAttachment` 提供[情感寄托](#情感寄托)，无需额外请求天气或位置接口：

| 调用方式 | 情感寄托的读取位置 |
| ---- | ---- |
| `momentFinder.listAll()`、`momentFinder.listBy(tag)` | 遍历结果后读取 `moment.spec.emotionalAttachment` |
| `momentFinder.list(page, size)`、`momentFinder.list({...})`、`momentFinder.listByTag(page, size, tag)` | 遍历 `items` 后读取 `moment.spec.emotionalAttachment` |
| `momentFinder.get(name)` | 直接读取 `moment.spec.emotionalAttachment` |

例如，读取单个瞬间的心情：

```html
<th:block th:with="moment=${momentFinder.get('moment-example')}">
    <th:block th:if="${moment != null and moment.spec.emotionalAttachment != null}">
        <span th:text="${moment.spec.emotionalAttachment.mood}"></span>
    </th:block>
</th:block>
```

`moment-example` 需替换为实际的瞬间名称。所有字段均为可选，主题需兼容未填写情感寄托的旧瞬间。

### listAll()

获取全部瞬间内容。

**参数**：无

**返回值类型**：`List<MomentVo>`

**示例**：

```html
<ul>
    <li th:each="moment : ${momentFinder.listAll()}" th:with="content = ${moment.spec.content}">
        <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
        <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
            <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
            <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
            <audio th:if="${momentItem.type.name == 'AUDIO'}" th:src="${momentItem.url}" controls="true"></audio>
        </th:block>
    </li>
</ul>
```

---

### list({...})

统一参数的瞬间列表查询方法，支持分页、标签、创建者、排序等参数，且均为可选参数。

**参数**：

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| `page` | `int` | 分页页码，从 1 开始 |
| `size` | `int` | 分页条数 |
| `tagName` | `string` | 标签名称 |
| `owner` | `string` | 创建者用户名 name |
| `sort` | `string[]` | 排序字段，格式为 `字段名,排序方式`，排序方式可选值为 `asc` 或 `desc`，如 `spec.releaseTime,desc`，传递时需要使用 `{}` 形式并用逗号分隔表示数组 |

**返回值类型**：`ListResult<MomentVo>`

**示例**：

```html
<th:block th:with="moments = ${momentFinder.list({
  page: 1,
  size: 10,
  tagName: 'fake-tag',
  owner: 'fake-owner',
  sort: {'spec.releaseTime,desc', 'metadata.creationTimestamp,asc'}
})}">
    <ul>
        <li th:each="moment : ${moments.items}" th:with="content = ${moment.spec.content}">
            <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
            <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
                <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
                <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
                <audio th:if="${momentItem.type.name == 'AUDIO'}" th:src="${momentItem.url}" controls="true"></audio>
            </th:block>
        </li>
    </ul>
    <div>
        <span th:text="${moments.page}"></span>
    </div>
</th:block>
```

---

### list(page, size)

根据分页参数获取瞬间列表。

**参数**：

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| `page` | `int` | 分页页码，从 1 开始 |
| `size` | `int` | 分页条数 |

**返回值类型**：`ListResult<MomentVo>`

**示例**：

```html
<th:block th:with="moments = ${momentFinder.list(1, 10)}">
    <ul>
        <li th:each="moment : ${moments.items}" th:with="content = ${moment.spec.content}">
            <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
            <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
                <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
                <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
                <audio th:if="${momentItem.type.name == 'AUDIO'}" th:src="${momentItem.url}" controls="true"></audio>
            </th:block>
        </li>
    </ul>
    <div>
        <span th:text="${moments.page}"></span>
    </div>
</th:block>
```

---

## 情感寄托

`MomentVo.spec.emotionalAttachment` 是可选对象，未填写时可能缺失或为 `null`。Finder 和[公共 REST API](./rest-api.md#情感寄托字段) 都会原样返回已保存的信息；查看瞬间时不会重新定位或刷新天气。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `mood` | `string` | 可选，用户选择的心情，如 `😌 平静` |
| `location` | `string` | 可选，城市或地区名称，不保存经纬度 |
| `weather` | `string` | 可选，获取时的天气快照，如 `晴 · 22°C`，也可手动填写 |
| `weatherSource` | `string` | 自动天气来源，目前为 `open-meteo`；手动填写时可省略 |

插件内置的列表和详情模板会展示这些信息。主题自行提供 `moments.html`、`moment.html` 或通过 Finder 渲染瞬间时，可在内容下方加入：

```html
<div th:if="${moment.spec.emotionalAttachment != null}"
     th:with="emotion=${moment.spec.emotionalAttachment}" aria-label="情感寄托">
    <span th:if="${not #strings.isEmpty(emotion.mood)}" th:text="${emotion.mood}"></span>
    <span th:if="${not #strings.isEmpty(emotion.weather)}" th:text="${emotion.weather}"
          th:title="${emotion.weatherSource == 'open-meteo' ? '天气数据来自 Open-Meteo（https://open-meteo.com/）' : null}"></span>
    <span th:if="${not #strings.isEmpty(emotion.location)}" th:text="${emotion.location}"></span>
</div>
```

来源信息保留在天气的悬停提示中。展示内容使用 `th:text` 进行转义；访问权限沿用瞬间本身的可见性和审核规则。

## 后来的我

`MomentVo.spec.followUps` 是可选的后续记录数组，未追加时可能缺失、为 `null` 或为空数组。它与原瞬间一起返回，适用于全部返回 `MomentVo` 的 Finder 方法、列表模板和详情模板，无需额外查询。

数组按追加顺序排列，每条瞬间最多 100 条，每条记录包含：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | `string` | 服务器生成的记录编号，可用作前端列表 key |
| `content` | `string` | 1～1000 个字符的纯文本后续 |
| `createdAt` | `Instant` / ISO 8601 字符串 | 服务器记录的追加时间，不会覆盖瞬间的 `releaseTime` |

原文保持在 `moment.spec.content`，后续单独读取 `moment.spec.followUps`。可以在原文下方加入：

```html
<section th:if="${not #lists.isEmpty(moment.spec.followUps)}" aria-label="后来的我">
    <h3>🌱 后来的我</h3>
    <ol>
        <li th:each="followUp : ${moment.spec.followUps}">
            <time th:datetime="${followUp.createdAt}"
                  th:text="${#temporals.format(followUp.createdAt, 'yyyy-MM-dd HH:mm')}"></time>
            <p style="white-space: pre-wrap" th:text="${followUp.content}"></p>
        </li>
    </ol>
</section>
```

后续是纯文本，请使用 `th:text` 转义。可见性和审核状态沿用原瞬间；没有免审核权限的个人中心用户追加后，整条瞬间会重新进入审核。Finder 不提供追加方法，写入需使用[受保护的追加接口](./rest-api.md#追加后续)。

## 类型定义

### MomentVo

```json
{
    "metadata": {
        "name": "string",
        "labels": {
            "additionalProp1": "string"
        },
        "annotations": {
            "additionalProp1": "string"
        },
        "creationTimestamp": "2022-11-20T13:06:38.512Z"
    },
    "spec": {
        "content": {
            "raw": "string",
            "html": "string",
            "medium": [
                {
                    "type": "PHOTO",
                    "url": "string",
                    "originType": "string"
                }
            ]
        },
        "releaseTime": "string",
        "visible": "PUBLIC",
        "owner": "string",
        "tags": ["string"],
        "emotionalAttachment": {
            "mood": "😌 平静",
            "location": "杭州市",
            "weather": "晴 · 22°C",
            "weatherSource": "open-meteo"
        },
        "followUps": [
            {
                "id": "server-generated-id",
                "content": "当时很难过的事，现在已经放下了。",
                "createdAt": "2026-11-06T08:00:00Z"
            }
        ]
    },
    "owner": {
        "name": "string",
        "avatar": "string",
        "bio": "string",
        "displayName": "string"
    },
    "stats": {
        "upvote": 0,
        "totalComment": 0,
        "approvedComment": 0
    }
}
```

### MomentMediaType

```java
enum MomentMediaType {
  PHOTO,   // 图片
  VIDEO,   // 视频
  POST,    // 文章
  AUDIO;   // 音频
}
```

### ListResult\<MomentVo>

```json
{
    "page": 0,
    "size": 0,
    "total": 0,
    "items": "List<MomentVo>",
    "first": true,
    "last": true,
    "hasNext": true,
    "hasPrevious": true,
    "totalPages": 0
}
```

### UrlContextListResult\<MomentVo>

```json
{
    "page": 0,
    "size": 0,
    "total": 0,
    "items": "List<MomentVo>",
    "first": true,
    "last": true,
    "hasNext": true,
    "hasPrevious": true,
    "totalPages": 0,
    "prevUrl": "string",
    "nextUrl": "string"
}
```

### MomentTagVo

```json
{
    "name": "string",
    "permalink": "string",
    "momentCount": 0
}
```

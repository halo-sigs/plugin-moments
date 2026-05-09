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
        "tags": ["string"]
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

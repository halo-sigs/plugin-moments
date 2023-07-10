# plugin-moments

Halo 2.0 的瞬间管理插件, 支持在 Console 进行管理以及为主题端提供 `/moments` 页面路由。

## 使用方式

1. 下载，目前提供以下两个下载方式：
    - GitHub Releases：访问 [Releases](https://github.com/halo-sigs/plugin-moments/releases) 下载 Assets 中的 JAR 文件。
    - Halo 应用市场：<https://halo.run/store/apps/app-SnwWD>
2. 安装，插件安装和更新方式可参考：<https://docs.halo.run/user-guide/plugins>
3. 安装完成之后，访问 Console 左侧的**瞬间**菜单项，即可进行管理。
4. 前台访问地址为 `/moments`，需要注意的是，此插件需要主题提供模板（moments.html）才能访问 `/moments`。

## 主题适配

目前此插件为主题端提供了 `/moments` 路由，模板为 `moments.html`，也提供了 [Finder API](https://docs.halo.run/developer-guide/theme/finder-apis)，可以将瞬间列表渲染到任何地方。

## 开发环境

```bash
git clone git@github.com:halo-sigs/plugin-moments.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-moments.git
```

```bash
cd path/to/plugin-moments
```

```bash
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    classes-directories:
      - "build/classes"
      - "build/resources"
    lib-directories:
      - "libs"
    fixedPluginPath:
      - "/path/to/plugin-moments"
```

### 模板变量

#### 路由信息

- 模板路径：/templates/moments.html
- 访问路径：/moments?tag={tag} | moments/page/{page}?tag={tag}

#### 参数

tag - 标签名称

#### 变量

moments

##### 变量类型

[#UrlContextListResult\<MomentVo>](#urlcontextlistresult-momentvo)

##### 示例

```html
<div>
    <ul>
        <li th:each="moment : ${moments.items}" th:with="content=${moment.spec.content}">
            <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
            <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
                <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
                <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
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

#### 变量

tags

##### 变量类型

[#List\<MomentTagVo>](#momenttagvo)

##### 示例

```html
<ul>
  <li th:each="tag : ${tags}">
    <div th:text="${tag.name}"></div>
  </li>
</ul>
```

#### 详情路由

- 模板路径：/templates/moment.html
- 访问路径：/moments/{name}

#### 变量

moment

##### 变量类型

[#MomentVo](#momentvo)

##### 示例

```html
<div>
    <div th:with="content=${moment.spec.content}">
        <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
        <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
            <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
            <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
        </th:block>
    </div>
</div>
```

### RSS 订阅地址

- 访问路径：/moments/rss.xml

### Finder API

#### listAll()

##### 描述

获取全部瞬间内容。

##### 参数

无

##### 返回值

List<[#MomentVo](#momentvo)>

##### 示例

```html
<ul>
    <li th:each="moment : ${momentFinder.listAll()}" th:with="content = ${moment.spec.content}">
        <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
        <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
            <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
            <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
        </th:block>
    </li>
</ul>
```

#### list(page, size)

##### 描述

根据分页参数获取瞬间列表。

##### 参数

1. `page: int` - 分页页码，从 1 开始
2. `size: int` - 分页条数

##### 返回值

[ListResult\<MomentVo>](#listresult-momentvo)

##### 示例

```html
<th:block th:with="moments = ${momentFinder.list(1, 10)}">
    <ul>
        <li th:each="moment : ${moments.items}" th:with="content = ${moment.spec.content}">
            <div th:if="${not #strings.isEmpty(content.html)}" th:utext="${content.html}"></div>
            <th:block th:if="${not #lists.isEmpty(content.medium)}" th:each="momentItem : ${content.medium}">
                <img th:if="${momentItem.type.name == 'PHOTO'}" th:src="${momentItem.url}" />
                <video th:if="${momentItem.type.name == 'VIDEO'}" th:src="${momentItem.url}"></video>
            </th:block>
        </li>
    </ul>
    <div>
        <span th:text="${moments.page}"></span>
    </div>
</th:block>
```

### 类型定义

#### MomentVo

```json
{
  "metadata": {
    "name": "string",                                   // 唯一标识
    "labels": {
      "additionalProp1": "string"
    },
    "annotations": {
      "additionalProp1": "string"
    },
    "creationTimestamp": "2022-11-20T13:06:38.512Z",    // 创建时间
  },
  "spec": {
    "content": {
      "raw": "string",                                  // 原始内容，一般用于编辑器使用
      "html": "string",                                 // HTML 内容，用于主题端进行最终渲染的内容
      "medium": [                                       // 媒体内容
        {
          "type": "#MomentMediaType",                   // 类型                            
          "url": "string",                              // 链接
          "originType": "string",                       // 原始类型，例如：image/jpeg
        }
      ]
    },
    "releaseTime": "string",                             // 发布时间
    "visible": "PUBLIC",                                 // 可见性              
    "owner": "string",                                   // 所属用户
    "tags": ["string"]                                   // 所拥有的标签
  },
  "owner": {
    "name": "string",                                     // 用户名
    "avatar": "string",                                   // 头像
    "bio": "string",                                      // 描述
    "displayName": "string",                              // 显示名称
  },
  "stats": {
    "upvote": 0,                                          // 点赞数
    "totalComment": 0,                                    // 评论数
    "approvedComment": 0                                  // 审核通过的评论数
  }
}
```

#### MomentMediaType

```java
enum Target {
  PHOTO,                                     // 图片
  VIDEO,                                     // 视频
  POST;                                      // 文章
}
```

#### ListResult<MomentVo>

```json
{
  "page": 0,                                   // 当前页码
  "size": 0,                                   // 每页条数
  "total": 0,                                  // 总条数
  "items": "List<#MomentVo>",                  // 瞬间列表数据
  "first": true,                               // 是否为第一页
  "last": true,                                // 是否为最后一页
  "hasNext": true,                             // 是否有下一页
  "hasPrevious": true,                         // 是否有上一页
  "totalPages": 0                              // 总页数
}
```

#### UrlContextListResult<MomentVo>

```json
{
  "page": 0,                                   // 当前页码
  "size": 0,                                   // 每页条数
  "total": 0,                                  // 总条数
  "items": "List<#MomentVo>",                  // 瞬间列表数据
  "first": true,                               // 是否为第一页
  "last": true,                                // 是否为最后一页
  "hasNext": true,                             // 是否有下一页
  "hasPrevious": true,                         // 是否有上一页
  "totalPages": 0,                             // 总页数
  "prevUrl": "string",                         // 上一页链接
  "nextUrl": "string"                          // 下一页链接
}
```

#### MomentTagVo

```json
{
    "name": "string",                           // 标签名称
    "momentCount": "string"                     // 标签名称
}
```

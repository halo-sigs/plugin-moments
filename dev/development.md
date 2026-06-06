# 开发环境搭建

## 克隆仓库

```bash
git clone git@github.com:halo-sigs/plugin-moments.git

# 或者当你 fork 之后
git clone git@github.com:{your_github_id}/plugin-moments.git
```

## 启动开发容器

所需环境依赖：

1. Java 21
2. Docker
3. Node.js 24
4. pnpm 10

```bash
# macOS / Linux
./gradlew haloServer

# Windows
./gradlew.bat haloServer
```

执行此命令后，会自动创建一个 Halo 的 Docker 容器并加载当前的插件，更多文档可查阅：<https://docs.halo.run/developer-guide/plugin/basics/devtools>

## 前端开发

```bash
cd console

pnpm install
pnpm dev          # 开发监听模式
pnpm build        # 生产构建
pnpm lint         # ESLint（Vue/TS）
pnpm type-check   # vue-tsc --noEmit
pnpm prettier     # 格式化代码
```

## 重新生成 API 客户端

后端 Endpoint 或 DTO / Extension 字段发生变更后，需要重新生成 TypeScript API 客户端：

```bash
./gradlew generateApiClient
```

生成的文件位于 `console/src/api/generated/`，**请勿手动编辑**。

## 运行测试

```bash
./gradlew test
```

## 源码运行 Halo（方式 2）

> 此方式需要使用源码运行 Halo

编译插件：

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
        fixedPluginPath:
            - "/path/to/plugin-moments"
```

最后重启 Halo 项目即可。

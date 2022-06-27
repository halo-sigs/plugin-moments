# halo-plugin-template

A Halo plugin template for backend development

## 如何开发一个 Halo 插件

前期准备：

1. 使用此仓库作为模版创建仓库
2. 克隆新创建好的仓库到本地

### 项目结构介绍

```text
.
├── LICENSE
├── README.md
├── admin-frontend
│   ├── ...
├── build
│   ├── classes
│   ├── libs
│   │   ├── halo-plugin-template-0.0.1-SNAPSHOT-plain.jar
│   ├── resources
├── build.gradle
├── lib
│   └── halo-2.0.0-SNAPSHOT-plain.jar
├── settings.gradle
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── guqing
        │               └── template
        │                   ├── Apple.java
        │                   ├── ApplePlugin.java
        │                   └── ApplesController.java
        └── resources
            ├── admin
            │   ├── main.js
            │   └── style.css
            ├── extensions
            │   ├── apple.yaml
            │   ├── reverseProxy.yaml
            │   └── roleTemplate.yaml
            ├── plugin.yaml
            └── static
                ├── ...
```

对于以上目录树：

- `admin-frontend`： 插件前端项目目录，为一个 vue 项目；
- `build`：插件后端构建目录，`build/libs` 下的 jar 包为最终插件产物；
- `lib`：为临时的 halo 依赖，为了使用 halo 中提供的类在 `build.gradle` 中作为编译时依赖引入 `compileOnly files("lib/halo-2.0.0-SNAPSHOT-plain.jar")`，待 `2.0` 正式发布会将其发布到 `maven` 中央仓库，便可通过 `gradle `依赖；

- `src`: 为插件后端源码目录；
- `Apple.java` 为自定义模型

```java
@GVK(group = "apple.guqing.xyz", kind = "Apple",
    version = "v1alpha1", singular = "apple", plural = "apples")
@Data
@EqualsAndHashCode(callSuper = true)
public class Apple extends AbstractExtension {}
```

关键在于标注 `@GVK `注解和 `extends AbstractExtension`，当如此定义了一个模型后，插件启动时会根据 `@GVK` 配置自动生成`CRUD`的 `RESTful API`，以此为例子会生成如下`APIs`

```text
GET /apis/apple.guqing.xyz/v1alpha1/apples

POST /apis/apple.guqing.xyz/v1alpha1/apples

GET /apis/apple.guqing.xyz/v1alpha1/apples/{name}

PUT /apis/apple.guqing.xyz/v1alpha1/apples/{name}

DELETE /apis/apple.guqing.xyz/v1alpha1/apples/{name}
```

生成规则见：[Halo extension RFC](https://github.com/halo-dev/rfcs/tree/main/extension)

- `ApplePlugin.java`：插件生命周期入口，它继承 `BasePlugin`，可以通过`getApplicationContext()`方法获取到 `SchemeManager`，然后在 `start()` 方法中注册自定义模型，这一步必不可少，所有定义的自定义模型都需要在此注册，并在 `stop()` 生命周期方法中清理资源。

```java
@Override
public void start() {
  schemeManager.register(Apple.class);
}

@Override
public void stop() {
  // TODO 优化取消注册自定义模型的写法
  Scheme scheme = schemeManager.get(Apple.class);
  schemeManager.unregister(scheme);
}
```

注意：该类不能标注 `@Component` 等能将其声明为 `Spring Bean` 的注解

- `ApplesController.java`：如果根据模型自动生成的 `CURD RESTful APIs` 无法满足业务需要，可以写常规 `Controller` 来自定义`APIs`，示例：

```java
@ApiVersion("v1alpha1")
@RestController
@RequestMapping("colors")
public class ApplesController {

    @GetMapping
    public Mono<String> hello() {
        return Mono.just("Hello world");
    }
}
```

插件定义 `Controller` 必须要标注 `@ApiVersion` 注解，否则启动时会报错。如果定义了这样的 `Controller` ，插件启动后会生成如下的 `APIs`

```text
GET /api/v1alpha1/plugins/apples/colors
```

生成规则为 `/api/{version}/plugins/{plugin-name}/{mapping-in-class}/{mapping-in-method}`

其中:

- `version`：来自 `@ApiVersion("v1alpha1") `的 value，详情参考：[Halo plugin API composition](https://github.com/halo-dev/rfcs/blob/main/plugin/pluggable-design.md#api-%E6%9E%84%E6%88%90%E8%AE%A8%E8%AE%BA)

- `plugin-name`：值来自 `plugin.yaml` 中的 `metadata.name` 属性
- `mapping-in-class`：来自标注在类上的 `@RequestMapping("colors")`
- `mapping-in-method`：来自标注在方法上的 `@GetMapping`

插件还允许使用 `@Service`、`@Component` 注解将其声明为一个 `Spring Bean`，便可通过依赖注入使用 `Spring Bean`，示例：

```java
@Service
public class ColorService {
  public String getColor(String appleName) {
    return "red";
  }
}

@ApiVersion("v1alpha1")
@RestController
@RequestMapping("colors")
public class ApplesController {
	private final ColorService colorService;
  // 构造器注入，当然也同样允许 @Autowired 注入 和 setter 方法注入
  public ApplesController(ColorService colorService) {
    this.colorService = colorService;
  }
}
```

- `resources`：目录为插件资源目录

`admin` 目录下为插件前端打包后的产物存放目录，固定为 `main.js` 和 `style.css `两个文件

`extensions` 存放自定义模型资源配置

`plugin.yaml`为插件描述配置

`static` 为静态资源示例目录

            ├── admin
            │   ├── main.js
            │   └── style.css
            ├── extensions
            │   ├── apple.yaml
            │   ├── reverseProxy.yaml
            │   └── roleTemplate.yaml
            ├── plugin.yaml
            └── static
                ├── image.jpeg
                ├── some.txt
                └── test.html

插件启动时会加载 `extensions` 目录的 `yaml` 保存到数据库，`apple.yaml` 为本项目自定义的模型的数据示例，当启用插件后调用

```text
GET /apis/apple.guqing.xyz/v1alpha1/apples
```

便可查询到 `apple.yaml` 中定义的记录

```json
[
  {
    "spec": {
      "varieties": "Fuji",
      "color": "red",
      "size": "middle",
      "producingArea": "China"
    },
    "apiVersion": "apple.guqing.xyz/v1alpha1",
    "kind": "Apple",
    "metadata": {
      "name": "Fuji-apple",
      "labels": {
        "plugin.halo.run/plugin-name": "apples"
      },
      "version": 0,
      "creationTimestamp": "2022-06-24T04:03:22.890741Z"
    }
  }
]
```

`reverseProxy.yaml` 为 Halo 中提供的模型，表示反向代理，允许插件配置规则代理到插件目录

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: ReverseProxy
metadata:
  name: reverse-proxy-template
rules:
  - path: /static/**
    file:
      directory: static
```

它表示访问 `GET /assets/{plugin-name}/static/**` 时代理访问到插件的 `resources/static` 目录，本插件便可访问到一下静态资源

```
GET /assets/apples/static/image.jpeg
GET /assets/apples/static/some.txt
GET /assets/apples/static/test.html
```

`roleTemplate.yaml` 文件中 `kind: Role` 表示插件允许提供角色模版，定义格式如下：

```yaml
apiVersion: v1alpha1
kind: Role
metadata:
  name: a name here
  labels:
    plugin.halo.run/role-template: "true"
  annotations:
    plugin.halo.run/module: "module name"
    plugin.halo.run/alias-name: "display name"
rules:
  # ...
```

必须带有`plugin.halo.run/role-template: "true"` labels，表示该角色为角色模版，当用户启用插件后，创建角色或修改角色时会将其列在权限列表位置。

插件如果不提供角色模版除非是超级管理员否则其他账号没有权限访问，因为 Halo 规定 `/api` 和 `/apis` 开头的 `api` 都需要授权才能访问，因此插件不提供角色模版的自定义资源，就无法将其分配给用户。

更多详情参考：[Halo security RFC](https://github.com/halo-dev/rfcs/blob/main/identity/002-security.md)

### 打包

#### 打包前端项目

1. 到 `admin-frontend` 目录下执行

```shell
pnpm install
```

2. 开发时执行

```shell
pnpm run dev
```

3. 打包时执行

```shell
pnpm run build
```

#### 构建后端

1. 开发时可以使用 `command + F9 / ctrl + F9`

2. 生产时：点击 `gradle` 的 `build`

或者执行

```
./gradlew -x build
```

然后只需复制例如`build/libs/halo-plugin-template-0.0.1-SNAPSHOT-plain.jar` 的 `jar` 包即可使用

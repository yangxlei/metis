# Metis

SPI for Android。 

## 简介
SPI(Service Provider Interface) 是 JDK 提供的一种服务发现机制。

在 Android 项目中，有时需要动态的查找指定的服务。对于调用方而言，不知道在工程内都有哪些实现。尤其对于多团队共同开发，约定的实现可能分别散落在各自的模块内。

Metis 参照 SPI 的机制，通过注解标记任意类。在工程任何地方都可以通过 Metis 的 api 获取到所有的 Class。

## 集成
在工程的 build.gradle 中添加插件依赖
```gradle
    dependencies {
        classpath 'io.github.yangxlei:metis:1.0.0'
   }
```

在 Application 的 Module 中依赖插件
```gradle
apply plugin: 'io.github.yangxlei.metis'
```

## 使用
标注作为服务使用的类
```java
@Metis(TestAction.class)
public class TestAction1 {
}
```

```java
@Metis(TestAction.class)
public class TestAction2 {
}
```

获取被标注的类
```java
for (Class<TestAction> clazz : MetisLoader.load(TestAction.class)) {
     System.out.println("@@@ " + clazz);
}
```

## 实现原理
1. 在 Gradle build 过程中，插入一个 Task，排在 javaCompile 之后。 这样在所有文件都编译完成之后，可以获取到整个工程的所有 class 文件。

2. 如果 class 是被 Metis 注解标记过的，将其保存下来

3. 利用 poet 工具库，生成一个 MetisRegistry.java 工具类， 其中保存了所有的被注解标记过的 class。 并且 MetisRegistry 的包名和 MetisLoader 包名一致

4. 生成的 java 文件被保存在一个临时目录中。对这个 java 文件进行 javaCompile 操作，合并到工程中来。

## 使用场景想法
1. 一般应用有很多使用 WebView 的场景。会定义很多 js 和 java 之间交互的 url 或者 action。 对 action 的处理可以使用 metis 做到解耦和
2. 应用外通过链接跳转到 app 时， 需要解析 url。 对解析结果的处理也可以通过 metis 做到解耦和
以及一些其他类似的场景。


## 说明
本工程供学习使用，内部使用的是本地 maven 仓库，并没有提交到 jcenter 和 maven 上。

# CHANGELOG
使用 Transfrom Api 重新实现插件, 支持 Instant Run

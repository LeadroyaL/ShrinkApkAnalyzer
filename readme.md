# Shrink Apk Analyzer

[EnglishVersion](EnglishVersion.md)

`${SDK_HOME}/tools/bin/apkanalyzer` 提供访问 axml 文件（例如 AndroidManifest.xml、res/layout/*.xml）的功能，适用于我们仅想 dump 单个 axml 的情景。

各个工具的对比：

| platform | tool | performance |
| ---- | ---- | ---- |
| PC | JEB | 太重，需要GUI，不提供处理单个 axml 的功能 |
| PC | apktool | 太重，不提供处理单个 axml 的功能 |
| PC | AXMLPrinter2 | 2008年的工具，太老 |
| PC | aapt/aapt2 | 无法转换为 xml 格式 |
| PC | apkanalyzer | **非常好，满足要求** |
| Android | drozer | 效果一般，有转义的bug，有显示不全的bug |
| Android | dumpsys package | 只能打印部分信息，无法转换为 xml 格式 |

综上，在 PC 上有 apkanalyzer，在 Android 上缺乏一个好用的工具。

本项目，将 apkanalyzer 移植到 Android 上（删除了部分依赖 `aapt` 和 `dexlib` 的功能），使用最官方、最优雅的方式将手机里的 AndroidManifest.xml 打印出来。

# 优点和适用场景

- 直接运行在手机里，不需要 `adb pull` 文件到电脑上
- 比 `drozer` 的效果好（可替代 drozer 里的 `app.package.manifest` ）

情景1：dump 手机里所有 APP 的 manifest
情景2：随手打印某 APP 的 manifest

# 用法

### CLI

```
adb shell
export CLASSPATH=`pm path com.leadroyal.shrink.analyzer | cut -d: -f2`
app_process /system/bin com.android.tools.apk.analyzer.ApkAnalyzerCli manifest print com.android.shell
app_process /system/bin com.android.tools.apk.analyzer.ApkAnalyzerCli manifest print /data/local/tmp/1.apk
```


### MainActivity 的 demo

当然也可以直接调用 API，点击 `FloatingActionButton` 就会执行一遍所有的指令。

# 已知缺陷

- 由于源码里有 java8 的特性 `Arrays.stream`，最低支持 Android N
- 由于源码里使用 `java.nio.Path`，最低支持 Android O

# 实现

1. apkanalyzer 源码来源

```
git clone https://android.googlesource.com/platform/tools/base
git checkout studio-4.0.0
```

主要有以下几个项目：

- `apkparser/cli`
- `apkparser/analyzer`
- `apkparser/binary-resources`
- `common`
- `sdk-common`
- `layoutlib-api`


2. 兼容 Android

从各个 lib 里摘出主要文件，添加功能，删除无用 API。

做出如下添加

| 改动 | 原因 | 位置 |
| ---- | ---- | ----|
| 支持输入包名 | 默认只支持输入 apk 路径，shell 里获取路径太麻烦 | com.android.tools.apk.analyzer.ApkAnalyzerCli |

主要做出如下 patch

| 改动 | 原因 | 位置 |
| ---- | ---- | ----|
| 依赖 zipfs.jar | 安卓不支持 ZipFileSystem | com.android.tools.apk.analyzer.internal.ZipArchive |
| 移除 aapt 相关代码 | 安卓没有 aapt | com.android.tools.apk.analyzer.ApkAnalyzerCli |
| 移除 SaxFactory 关于 XXE 的防御代码 | 安卓设置这些特性时会抛出异常 | com.android.ide.common.xml.AndroidManifestParser |
| 移除 SdkConstants 里 AndroidX 的相关代码 | 依赖 kotlin，难以编译 | com.android.SDKConstant |
| 移除 SdkConstants 里 "android.support.design.widget" 字符串 | 检查 lib 时，编译不通过 | com.android.SDKConstant |


大部分依赖已通过源码的方式植入，目前仅依赖

- zipfs.jar
- com.android.tools.apkparser:binary-resources
- net.sf.jopt-simple:jopt-simple
- com.google.guava:guava
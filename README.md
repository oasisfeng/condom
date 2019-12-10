[![Download](https://api.bintray.com/packages/oasisfeng/maven/condom/images/download.svg)](https://bintray.com/oasisfeng/maven/condom/_latestVersion)
[![Build Status](https://travis-ci.org/oasisfeng/condom.svg?branch=master)](https://travis-ci.org/oasisfeng/condom)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

# Project Condom

Project Condom is a thin library to wrap the naked `Context` in your Android project before passing it to the 3rd-party SDK. It is designed to prevent the 3rd-party SDK from common unwanted behaviors which may harm the user experience of your app.

* Massive launch of processes in other apps (common in 3rd-party push SDKs), causing slow app starting and notable lagging on low to middle-end devices. This behavior has "chain reaction" effects among apps with similar SDKs, greatly aggravating the overall device performance.

## Quick Start

1. Add dependency to this library in build.gradle of your project module.

   ```
   compile 'com.oasisfeng.condom:library:2.5.0'
   ```

2. Migration the initialization code of 3rd-party SDK.

   Most 3rd-party SDKs require explicit initialization with a `Context` instance, something like:
   ```
   XxxClient.init(context, ...);
   ```

   Just change the `context` parameter to `CondomContext.wrap(context)`, like this:
   ```
   XxxClient.init(CondomContext.wrap(context, "XxxSDK"), ...);
   ```

3. If the 3rd-party SDK contains its own components (`<activity>`, `<service>`, `<receiver>` or `<provider>`), they will not be running with `CondomContext`. To also prevent them from unwanted behaviors, `CondomProcess` is introduced to apply the process-level condom protection, assuming that those components are already isolated from your application process (with separate `android:process` specified). Add the following initialization code in the very beginning of your `Application.onCreate()`.
   ```
   public class MyApplication extends Application {

     @Override public void onCreate() {
       CondomProcess.installExceptDefaultProcess(this);
       ...
     }
   }
   ```

That's all! Just have the confidence of condom, to protect your users from untrustworthy libraries.

---------------

# 保险套项目

『保险套』是一个超轻超薄的 Android 工具库，将它套在 Android 应用工程里裸露的 `Context` 上，再传入第三方 SDK（通常是其初始化方法），即可防止三方 SDK 中常见的损害用户体验的行为：

* 在后台启动大量其它应用的进程（在三方推送SDK中较为常见），导致应用启动非常缓慢，启动后一段时间内出现严重的卡顿（在中低端机型上尤其明显）。
这是由于在这些 SDK 初始化阶段启动的其它应用中往往也存在三方 SDK 的类似行为，造成了进程启动的『链式反应』，在短时间内消耗大量的 CPU、文件 IO 及
内存资源，使得当前应用所能得到的资源被大量挤占（甚至耗尽）。

**注意：此项目通常并不适用于核心功能强依赖特定外部应用或组件的 SDK（如Facebook SDK、Google Play services SDK）。** 如果希望在使用此类 SDK 时避免后台唤醒依赖的应用，仅在特定条件下（如用户主动作出相关操作时）调用 SDK 所依赖的应用，则可以使用本项目，并通过 `CondomContext.setOutboundJudge()` 自主控制何时放行。

## 快速开始

1. 首先在工程中添加对此项目的依赖项。

   对于 Gradle 工程，直接在模块的依赖项清单中添加下面这一行：

   ```
   compile 'com.oasisfeng.condom:library:2.5.0'
   ```

   对于非 Gradle 工程，请[下载AAR文件](http://jcenter.bintray.com/com/oasisfeng/condom/library/)放进项目模块本地的 `libs` 路径中，并在工程的 ProGuard 配置文件中增加以下规则：（Gradle 工程和不使用 ProGuard 的工程不需要这一步）

   ```
   -dontwarn com.oasisfeng.condom.CondomContext$CondomContentResolver
   -dontwarn com.oasisfeng.condom.ContentResolverWrapper
   -dontwarn com.oasisfeng.condom.PackageManagerWrapper
   -dontwarn com.oasisfeng.condom.PseudoContextWrapper
   -dontwarn com.oasisfeng.condom.kit.NullDeviceIdKit$CondomTelephonyManager
   -keep class com.oasisfeng.condom.**
   ```
   （Gradle 工程无需手动添加上述 ProGuard 规则）

2. 略微修改三方 SDK 的初始化代码。

   常见的三方 SDK 需要调用其初始化方法，一般包含 `Context` 参数，例如：

   ```
   XxxClient.init(context, ...);
   ```

   只需将其修改为：

   ```
   XxxClient.init(CondomContext.wrap(context, "XxxSDK"), ...);
   ```

   其中参数 `tag`（上例中的"XxxSDK"）为开发者根据需要指定的用于区分多个不同 `CondomContext` 实例的标识，将出现在日志的TAG后缀。如果只有一个 `CondomContext` 实例，或者不需要区分，则传入 `null` 亦可。

3. 如果三方 SDK 含有自己的组件（Activity、Service、Receiver 或 Provider），为防止这些组件内的有害行为，还需要确保这些组件的工作进程与应用自己的进程隔离（`android:process` 使用非应用自有组件的进程名），并在应用的 `Application.onCreate()` 起始部分调用 `CondomProcess.installExceptDefaultProcess(this)` 或 `CondomProcess.installExcept(this, ...)`，如下所示：

   ```
   public class MyApplication extends Application {

     @Override public void onCreate() {
       CondomProcess.installExceptDefaultProcess(this);
       ...
     }
   }
   ```

   如果需要注入 CondomProcess 的进程是明确且单一的，还可以使用另一种初始化方式：定义一个使用相同进程（"android:process"）的 ContentProvider，并在其 onCreate() 方法中调用 `CondomProcess.installInCurrentProcess((Application) context().getApplicationContext(), ...)` 。它避免了查询进程名的开销，相比上面两个初始化方法更为高效。

4. 如果需要在 layout XML 中使用第三方 SDK 提供的定制 View，建议使用 `LayoutInflater.cloneInContext()` 创建一个受 Condom 保护的 `LayoutInflater` 进行布局加载。

   如果使用 support-fragment 库，则还可以更进一步简化为重载 `Fragment.onGetLayoutInflater()` 方法：

   ```
   @Override public LayoutInflater onGetLayoutInflater() {
       return super.onGetLayoutInflater().cloneInContext(CondomContext.wrap(getContext(), "...SDK"));
   }
   ```

完成以上的简单修改后，三方 SDK 就无法再使用这个套上了保险套的 `Context` 去唤醒当前并没有进程在运行的其它应用。（已有进程在运行中的应用仍可以被关联调用，由于此时不存在大量进程连锁创建的巨大资源开销，因此是被允许的）

## 高级用法

### OutboundJudge

在初始化 `CondomContext` 或 `CondomProcess` 时，可通过 `CondomOptions.setOutboundJudge()` 设置一个完全自由控制的仲裁逻辑，控制是否允许三方 SDK 感知及调用其它应用。

### Condom Kit

Project Condom 从 2.0.0 版本新增了全新的 Condom Kit 扩展机制，为开发者提供了一个可自由添加各种所需 Kit 的开放框架，用于实现两个关联性很强的功能：

1. 拦截 `Context.getSystemService()` 返回的系统服务实例，实现对特定 API 调用的屏蔽或改写。
2. 不必在应用中请求某些三方库强制要求的不合理权限（例如 `READ_PHONE_STATE`）。（涵盖 `AndroidManifest.xml` 中的静态权限声明及 Android 6.0 以上的运行期权限请求）

在初始化 `CondomContext` 时，调用 `CondomOptions.addKit()` 即可激活需要的 Kit。

内置的 `NullDeviceIdKit` 提供了一个 Condom Kit 的参考实现。它实现了屏蔽 IMEI 等设备硬件标识的读取（始终返回 `null`，相当于在没有基带功能的平板设备上的正常情况），并让应用不必声明和请求 `READ_PHONE_STATE` 权限。

## 工作原理

`CondomContext` 是一个加入了特定 API 拦截和调整机制的 `ContextWrapper`，它只作用于通过这个 `CondomContext` 实例发生的行为，完全不会触及除此之外的其它 `Context`，因此不必担心对应用的自有功能造成影响，可以放心的使用。（`CondomProcess` 除外，它作用于所在的整个进程）其中涉及到的调整和拦截包括：（可通过配置 `CondomOptions` 选择性使用）

* 开发者可主动设置一个 `OutboundJudge` 回调，方便根据需求定制拦截策略。
* 避免通过此 Context 发出的广播启动其它应用的进程。在 Android N 以上，通过为非应用内广播的 `Intent` 添加 `FLAG_RECEIVER_EXCLUDE_BACKGROUND` 标志达成；在低版本Android系统中，通过添加 `FLAG_RECEIVER_REGISTERED_ONLY` 达到类似的效果。
* 避免通过此 Context 发出的广播或请求的服务启动已被用户强行停止的应用。通过为发往应用之外的广播或服务请求 `Intent` 添加 `FLAG_EXCLUDE_STOPPED_PACKAGES` 标识达成。

`CondomProcess` 采用了更偏底层的 API 拦截策略对整个进程内与系统服务之间的 IPC 通信进行拦截和调整，达到与 `CondomContext` 类似的效果。由于它被设计为仅作用于三方 SDK 组件所在的独立进程内，因此也不会对应用的自有功能造成任何影响。

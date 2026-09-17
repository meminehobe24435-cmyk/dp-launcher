# 安装与打不开的排查（手机 / 投影仪通用）

> 本版本已在 **Android 11 / 1280×720 / density 1.0** 的设备上跑通完整流程
> （安装 → 启动 → 遥控器导航 → 打开应用列表 → 设为系统桌面），7 项自动检查全部通过。
> 复现命令：`python tools/verify_on_device.py`

## 0. 装哪个文件

```
dist/DP-Launcher-v1.0.0.apk      ← 直接装这个（约 4.0 MB）
```

包名 `com.dp.launcher`，minSdk 23（Android 6.0 及以上），targetSdk 34。

## 1. 传到手机时最常见的三个坑

| 现象 | 原因 | 解决 |
|---|---|---|
| 文件名变成 `xxx.apk.1` / `xxx.apk.txt` | 微信、钉钉、企业微信为防误装会自动改名 | 用文件管理器把文件名改回 `xxx.apk` 再点安装 |
| "解析软件包时出现问题" | 传输被截断 / 文件被 IM 压缩 | 用数据线、QQ 面对面、或网盘原文件下载；对比大小 4,035,105 字节 |
| "已阻止安装未知应用" | 系统安全策略 | 设置 → 应用 → 特殊权限 → 安装未知应用 → 允许对应 App（浏览器/文件管理器） |

## 2. 打开方式（两种都行）

**A. 像普通 App 一样点开**
v1.0.0 起 APK 里同时声明了 `LAUNCHER` 图标，装在手机/投影仪上会出现在应用列表里，点图标即可进入。

**B. 设为默认桌面（投影仪推荐）**
- 原生 Android / Android TV：设置 → 应用 → 默认应用 → **主屏幕应用** → 选 `DP Launcher`
- 小米 / 澎湃：设置 → 应用设置 → 默认应用管理 → 桌面 → 选 `DP Launcher`
- 华为 / 鸿蒙：设置 → 应用 → 默认应用 → 桌面 → 选 `DP Launcher`
- 部分投影仪固件屏蔽了第三方桌面，属于厂商限制，此时用方式 A 手动进入。

设成默认桌面后，按遥控器 Home 键就会回到这个界面；按返回键不会退出桌面（代码里已拦截）。

## 3. 之前"点了打不开"的原因（v1.0.0 已修）

真机上会**启动即闪退**，有两个必崩点，都是我在没有真机的情况下静态审查漏掉的，
现在都由 Robolectric 冒烟测试（`app/src/test/.../LauncherSmokeTest.kt`）覆盖：

| # | 崩溃点 | 原因 | 修复 |
|---|---|---|---|
| 1 | 状态栏初始化 | 给 `LinearLayout` 的子 View 塞了 `ViewGroup.LayoutParams`，又强转成 `MarginLayoutParams` → `ClassCastException` | 改用 `LinearLayout.LayoutParams` |
| 2 | 时钟刷新 | `DateFormat.is24HourFormat(null)`，框架内部 `context.getUserId()` → `NullPointerException` | `StatusClock` 持有 application context |

另外真机跑起来后还修了 3 个问题（详见 `CHANGELOG.md`）：
找不到图标（补 `LAUNCHER` intent-filter）、
"任意焦点跳应用列表"只有 My Apps 键生效（容器上的 `setOnKeyListener` 在子 View 有焦点时不会被调用，
改为在 Activity 的 `onKeyDown` 处理）、
焦点会掉到根布局（去掉根布局的 `focusable`）。

修复后 `./gradlew :app:testDebugUnitTest` 10 项测试全部通过
（5 项 Robolectric 真布局冒烟 + 5 项几何自检），并在真机上通过了
`tools/verify_on_device.py` 的 7 项端到端检查。

## 4. 如果再遇到闪退

用数据线连电脑，抓一份日志发我，能直接定位：

```bash
adb logcat -d | findstr /I "AndroidRuntime dp.launcher"
```

或安装后立刻运行：

```bash
adb install -r dist/DP-Launcher-v1.0.0.apk
adb logcat -c && adb shell am start -n com.dp.launcher/.LauncherActivity
adb logcat -d -s AndroidRuntime:E
```

## 5. 手机上的表现说明

这个界面是按 **1280×720 横屏**（投影仪）设计的，手机上会：

- 强制横屏显示（Manifest 里 `screenOrientation="landscape"`）；
- 按 `min(宽/1280, 高/720)` 等比缩放，比例不对的屏幕上下会留边，但不会拉伸变形；
- 手机上通常没有"信号源 / Keystone / Miracast"三个厂商功能，点了会退到系统显示设置并弹提示，
  这是设计好的兜底行为，不是崩溃。

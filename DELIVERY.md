# 交付说明（给需求方）

## 1. 代码位置

本次交付的完整工程在：**`D:\23178\DP-Launcher`**

> ⚠️ 会话指定的工作目录 `D:\DP工作区` 对当前用户是**只读**的
> （ACL：`BUILTIN\Users = ReadAndExecute`，属主是 `Administrators`），
> 无法在其中创建文件，所以工程建在同盘的 `D:\23178\DP-Launcher`。
>
> 想放回 `D:\DP工作区` 二选一：
> 1. 用**管理员** PowerShell 执行一次：
>    `icacls "D:\DP工作区" /grant "$env:USERNAME:(OI)(CI)M"` 之后我把工程搬过去；
> 2. 直接剪切整个 `D:\23178\DP-Launcher` 目录到 `D:\DP工作区\` 也行（工程内无绝对路径依赖，
>    唯一例外是脚本里 Chrome 的默认安装路径）。

## 2. 交付物清单

| 交付物 | 路径 | 说明 |
|---|---|---|
| Android 源码工程 | `D:\23178\DP-Launcher` | Kotlin + XML + RecyclerView，可直接 `gradlew assembleDebug` |
| **安装包（传手机用这个）** | `dist\DP-Launcher-v1.0.0.apk` | 3.9 MB，minSdk 23 / targetSdk 34，含普通应用图标（可直接点开） |
| 安装排查指南 | `docs\install.md` | 传输被改名、"解析包出错"、闪退等按现象排查 |
| 变更记录 | `CHANGELOG.md` | v1.0.0 修复的两个真机闪退 + LAUNCHER 图标 |
| 演示动图 | `demo/launcher-demo.gif`（720p，2.3 MB）<br>`demo/launcher-demo.webp`（960×540，0.5 MB） | 自动录制，含焦点移动 → 跳应用列表 → 打开应用 |
| 演示静帧 | `demo/home.png`、`demo/drawer.png` | 首页 / 全部应用列表 |
| 1:1 预览页 | `preview/index.html` | 浏览器直接打开，方向键即可操作（与 APK 同一套尺寸常量） |
| 像素比对报告 | `preview/out/side_by_side.png`、`diff.png` | 与参考图的对照与差分热力图 |
| 设计规格 | `docs/design-spec.md` | 每个数值的来源（量测 or 拟合） |
| 复验方法 | `docs/verification.md` | 复现命令 + 本次结果数字 |
| 工程说明 | `README.md` | 需求对照表、架构、编译安装、厂商适配 |

## 3. 四条要求的完成情况

1. **倒影** — `ReflectionContainer` 自绘：卡片栅格化 → 镜像变换 → 竖直渐变遮罩渐隐。
   参数从参考图反解（可见高度 50 px、起始 alpha 0.60、线性衰减）。
2. **任意焦点都能跳进全部已安装 APPLIST** — 4 个入口：底部栏 ↓、卡片行 ↑、任意处 MENU 键、
   长按 OK；"My Apps" 键直接进。返回键 / 第一行 ↑ 退出，焦点精确还原。
   列表数据来自 `PackageManager`，安装/卸载广播实时刷新。
3. **demo 质量 + 演示视频** — 见第 2 节：动图为脚本自动录制（Chrome DevTools screencast +
   PIL 合成），并给出与参考图的量化相似度：**产品区域 MAE 9.3/255，83.6% 像素在容差内**，
   主要元素边缘误差 ≤ 1.5 px。
4. **GitHub 源码 DEMO** — 工程已按开源仓库规范整理（README / LICENSE / .gitignore /
   工具脚本 / 文档），并已本地 `git init` 提交；推送命令见第 4 节。

## 4. 上传 GitHub

```powershell
cd D:\23178\DP-Launcher
git remote add origin https://github.com/<你的账号>/dp-launcher.git
git branch -M main
git push -u origin main
```

（仓库里不含 token、密钥、绝对路径；`build/`、`node_modules/`、`preview/out/` 已在 .gitignore 中。）

## 5. 还需要你确认的两点

1. **厂商功能键（Keystone / Miracast / Signal Source）**：AOSP 没有标准 intent，目前
   `app/src/main/res/values/arrays.xml` 里放的是常见候选（`android.settings.CAST_SETTINGS`、
   `com.android.tv.action.INPUT_SELECT` 等），在一台**真机**上才能确定厂商实际 Activity 名。
   把真机的 `adb shell dumpsys package | findstr Activity` 结果发我，我直接把 XML 填成准确值。
2. **卡片行放哪些应用**：参考图是 Netflix / YouTube / Google Play / chrome 四个，
   目前用 `arrays.xml` 的 `featured_packages` 按包名固定；如果希望改成"最近使用"或
   "按安装顺序"，告诉我改哪种策略。

## 6. 修复记录（真机反馈）

首版 APK 在真机上**启动即闪退**，两个必崩点已修，并补了 Robolectric 冒烟测试防回归：

| # | 崩溃点 | 原因 |
|---|---|---|
| 1 | 状态栏初始化 | `LinearLayout` 子 View 用了 `ViewGroup.LayoutParams` 后强转 `MarginLayoutParams` → `ClassCastException` |
| 2 | 时钟刷新 | `DateFormat.is24HourFormat(null)` → 框架内 `NullPointerException` |
| 3 | 找不到图标 | 只声明了 `HOME`，应用列表里没有入口 → 补 `LAUNCHER` intent-filter |

现在 `./gradlew :app:testDebugUnitTest` 会真实启动 Activity、inflate 真实布局并强制
measure/layout，这三点都会在 CI 阶段被拦住。

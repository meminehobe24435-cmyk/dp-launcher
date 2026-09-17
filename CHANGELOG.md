# 变更记录

## v1.0.0 — 2026-09-17

首个交付版本：投影仪桌面 1:1 复刻（Android / Kotlin）。

**功能**

- 首页：状态栏（时钟 / 日期 / Wi-Fi / 指针设备）、四个应用大卡 + 倒影、底部五个功能键
- 全部已安装应用列表：底部栏 ↓、卡片行 ↑、MENU 键、长按 OK、My Apps 键 共 5 个入口，返回键退出并还原焦点
- 应用列表来自 `PackageManager`，安装 / 卸载 / 更新广播实时刷新
- 卡片底色：参考图品牌色 → 图标 Palette 主色 → 包名哈希兜底
- Keystone / Miracast / Signal Source 通过 `arrays.xml` 资源数组路由到厂商 Activity，失配时退到系统设置并提示

**修复（真机启动即闪退，感谢真机反馈）**

- 状态栏：`LinearLayout` 的子 View 使用 `ViewGroup.LayoutParams` 并在之后强转
  `MarginLayoutParams`，父容器测量时抛 `ClassCastException` → 改用 `LinearLayout.LayoutParams`
- 时钟：`DateFormat.is24HourFormat(null)` 在框架内抛 `NullPointerException` →
  `StatusClock` 改为持有 application context
- Manifest 增加 `LAUNCHER` intent-filter：APK 现在有普通应用图标，可以直接点开，
  不再只能作为桌面被系统调用

**修复（真机运行后发现的交互问题）**

- "任意焦点跳应用列表"在真机上只有 My Apps 键有效：`setOnKeyListener` 注册在容器上时，
  焦点落在子 View 上就**不会被调用**（Android key 分发机制）。改为在 Activity 的
  `onKeyDown` 里处理未被消费的按键，MENU / 底部栏 ↓ / 卡片行 ↑ 三条入口全部生效
- 首页根布局原本 `focusable="true"`：焦点搜索在"下方没有可聚焦 View"时会落到根布局，
  导致整屏失去焦点框。去掉根布局的 focusable
- 应用列表背景色与首页壁纸不一致（`colors.xml` 仍是 90% 透明度），改为不透明

**真机验证**（Android 11，1280×720 / density 1.0，与参考图同规格）

- `tools/verify_on_device.py` 自动跑完整流程，7 项检查全部 PASS：安装启动无崩溃、
  首焦点在第一个卡片、↓ 进入底部栏、底部栏 ↓ 打开应用列表、↑ 关闭并还原焦点、
  MENU 打开应用列表、可被设为系统桌面（按 Home 键回到本桌面）
- 实机边缘测量与参考图误差 ≤ 1 px（卡片左边缘 −0.3、底部键上边缘 +0.5、下边缘 +0.9）

**测试**

- `LauncherSmokeTest`（Robolectric）：真实启动 Activity、inflate 真实布局并强制 measure/layout，
  覆盖首页、应用列表开关、返回键不退出桌面、MENU 打开应用列表、↑ 关闭应用列表
- `DesignSpecTest`：行宽 / 留白 / 不重叠等几何自检

**验证**

- 产品区域与参考图平均绝对误差 9.3/255，83.5% 像素在容差内，主要边缘误差 ≤ 1.5 px
- 演示动图与静帧由 `tools/` 自动生成

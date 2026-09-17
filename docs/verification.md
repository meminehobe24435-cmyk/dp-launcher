# 复验与工具说明

这套脚本的作用是：**让"1:1 复刻"变成可复现的数字**，而不是一句主观判断。
所有脚本只依赖参考截图与本仓库，不联网（下载字体/依赖除外）。

## 目录

| 脚本 | 作用 |
|---|---|
| `tools/probe.py` | 粗扫参考图：主色直方图、横竖扫描线的颜色游程 |
| `tools/measure.py` | 精测：卡片/按键边缘、文字包围盒、倒影衰减 |
| `tools/measure2.py` | 区块均值取色 + 关键剖面（生成 `_ref/c_*.png` 放大裁切图） |
| `tools/measure3.py` | 最终几何：卡片矩形/圆角、按键矩形、状态栏元素、倒影范围 |
| `tools/measure4.py` | 卡片内图标包围盒、标题条配色、聚焦态剖面 |
| `tools/measure5.py` | 各卡片图标尺寸（按颜色掩膜）、按键图标描边色 |
| `tools/export_tokens.py` | **DesignSpec.kt → preview/tokens.css / tokens.json**（单一数据源） |
| `tools/fetch_fonts.py` | 下载 Roboto 400/500 到预览资源目录（Apache-2.0） |
| `tools/compare.py` | 无头 Chrome 渲染预览 → 与参考图比对 → MAE / 容差占比 / 差分热力图 |
| `tools/geometry_report.py` | 同一套检测代码量两图的关键边缘，直接给出 delta 表 |
| `tools/calibrate.py` | 参数拟合：按候选几何渲染并选误差最小的组合 |
| `tools/capture_video.mjs` | 用 Chrome DevTools screencast 录制演示帧（puppeteer-core） |
| `tools/make_video.py` | 帧序列 → `demo/launcher-demo.gif` / `.webp` |
| `tools/verify_on_device.py` | **真机/模拟器端到端验证**：安装→启动→按 D-pad 走完整流程→抓截图→查崩溃→设为桌面 |

## 复现步骤

```bash
python tools/export_tokens.py        # 先生成预览用的 CSS 变量
python tools/compare.py              # 渲染 + 比对，产物在 preview/out/
python tools/geometry_report.py      # 逐项边缘对照
python tools/calibrate.py stage1     # 复现卡片宽/间距的拟合（约 30 秒）
npm install
node tools/capture_video.mjs         # 录制演示帧（需要本机 Chrome/Edge）
python tools/make_video.py           # 合成动图

# 真机 / 模拟器端到端验证（需要一台已连接的设备）
./gradlew :app:assembleDebug
python tools/verify_on_device.py --apk app/build/outputs/apk/debug/app-debug.apk
```

## 真机验证结果

在 Android 11 模拟器（1280×720、density 160，即参考图同规格画布）上实测：

```
device: emulator-5554
-- launch            focused view: [122,134][372,490]    第一个卡片获得焦点
-- navigate          focused view: [903,506][1137,649]   ↓ 进入底部栏 → → → → 到 Settings
-- DOWN on the dock  focused view: [71,88][275,272]      打开应用列表，首格获得焦点
-- UP on first row   focused view: [903,506][1137,649]   关闭列表并把焦点还原到 Settings
-- MENU              focused view: [71,88][275,272]      MENU 同样能打开应用列表
   [PASS] app launched without a crash
   [PASS] first card focused on start
   [PASS] dock reachable with DOWN
   [PASS] DOWN on the dock opens the app list
   [PASS] UP restores the focus
   [PASS] MENU opens the app list
   [PASS] accepted as the home activity
```

布局对照（真机截图 vs 参考图，同一套边缘检测）：

| 指标 | 参考图 | 真机 | delta |
|---|---|---|---|
| 卡片1 左边缘 | 121.8 | 121.5 | −0.3 |
| 底部键 左边缘 | 119.2 | 119.5 | +0.3 |
| 底部键 上边缘 | 515.4 | 516.0 | +0.5 |
| 底部键 下边缘 | 637.1 | 638.1 | +0.9 |

真机首页的整体像素误差不能直接和参考图比：真机上装的是系统自带应用（Calendar / Camera /
Clock / Contacts），卡片底色是**从这些图标自动提取**的，与参考图里的四个品牌应用本就不同；
可比的是几何（上表，≤1 px）、壁纸（MAE 0.41）、底部功能栏（MAE 27，差异集中在图标手绘细节
与参考图的压缩模糊）。

`preview/out/` 下的产物：

| 文件 | 内容 |
|---|---|
| `render.png` | 预览页在 1280×720 下的截图 |
| `side_by_side.png` | 左参考图 / 右渲染图 |
| `diff.png` | 4 倍增强的差分热力图（绿框为统计区域） |
| `frames/` | 录制的原始帧 + 时间戳 `index.json` |
| `stills/` | README 用的首页 / 应用列表静态图 |

## 本次复验结果

```
overall  MAE=12.43  pixels within tolerance= 78.0%
clean (below the reviewer's annotation, y>=146)  MAE= 9.33  within tolerance= 83.5%
  status bar   MAE=32.29  within tolerance= 49.9%
  card row     MAE=12.26  within tolerance= 78.8%
  dock row     MAE=13.72  within tolerance= 74.6%
  wallpaper    MAE= 0.41  within tolerance= 98.8%
```

边缘对照（`geometry_report.py`，两图用同一套"半高穿越点"检测口径）：

| 指标 | 参考图 | 渲染图 | delta |
|---|---|---|---|
| 卡片1 左边缘 | 121.8 | 121.5 | −0.3 |
| 卡片1 右边缘 | 371.4 | 371.5 | +0.1 |
| 卡片2 左边缘 | 382.6 | 383.5 | +0.9 |
| 卡片4 右边缘 | 1157.5 | 1157.5 | 0.0 |
| 按键1 左/右/上/下 | 119.2 / 319.6 / 515.4 / 637.1 | 119.5 / 319.5 / 515.5 / 638.0 | ≤0.9 |
| 聚焦按键（Settings）右边缘 | 1176.5 | 1176.7 | +0.2 |
| 状态栏时间字块 左/上 | 770 / 64 | 761 / 64 | −9 / 0 |
| 卡片文字「NETFLIX」上边 | 379 | 378 | −1 |

> `card1.bottom` 一类的"卡片→倒影"过渡不是硬边缘，该指标仅作参考，不计入上表。

`status bar` 一行误差大是因为参考图该区域被需求方的红色批注浮层覆盖（红字 + 紫色边框），
这部分不属于产品 UI；把 y ≥ 146 的"产品界面"单独统计即为上表第二行。
状态栏左侧偏差 9 px 的原因是参考图时钟用的字体数字更宽（`9:21 PM` vs `8:08 AM` 宽度差），
右侧终点已对齐（1162 vs 1164）。

## 判读建议

- **MAE 9.3 / 255 ≈ 3.6 %**：考虑到参考图本身是被大幅压缩、放大的低清截图（边缘模糊约 10 px），
  这个量级基本等价于"视觉一致"。
- 想更严格时，可只看边缘坐标 delta（上表下半部分）：主要元素位置误差都在 1.5 px 以内。
- 差分热力图中最亮的位置集中在：各应用的**品牌字标**与**品牌图标**（定制字体/图形，不可复刻），
  以及参考图批注浮层。

# 第三方素材说明

代码本身是 MIT（见 [`LICENSE`](LICENSE)）。仓库里另外包含以下第三方素材：

| 素材 | 来源 / 授权 |
|---|---|
| `preview/assets/fonts/roboto-400.woff2`、`roboto-500.woff2` | Roboto，Copyright 2011 Google Inc.，Apache License 2.0 |
| `preview/assets/netflix.svg`、`youtube.svg`、`googleplay.svg`、`chrome.svg` | 按参考截图**重绘的示意图**，仅用于与本 demo 的视觉对照 |
| `_ref/reference.png` | 需求方提供的参考截图（复刻目标），仅用于像素比对 |
| `demo/*.gif`、`demo/*.webp`、`demo/*.png` | 本仓库脚本自动生成的演示素材 |

**商标声明**：Netflix、YouTube、Google Play、Chrome 等名称与标识归各自公司所有。
仓库里的重绘图形只为"复刻度对照"存在，不构成授权，请勿用于任何商业产品或对外发布的应用。
正式 App（`app/`）里不包含任何品牌素材：应用图标一律在运行时从 `PackageManager` 读取真实安装的应用。

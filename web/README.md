# StudentManage Web 模块

这是 JavaFX 学生管理系统的网页移植界面（演示版）。

## 页面说明

- `index.html`：欢迎页，包含进入系统按钮。
- `pages/app.html`：主业务页，使用侧边栏导航布局。

## 目录结构

```text
web/
├─ index.html
├─ pages/
│  └─ app.html
├─ assets/
│  ├─ css/
│  │  ├─ base.css
│  │  └─ app.css
│  ├─ js/
│  │  ├─ welcome.js
│  │  ├─ mock-data.js
│  │  └─ app.js
│  └─ images/
│     └─ .gitkeep
└─ package.json
```

## 快速启动（可选）

```powershell
Set-Location "E:\Projects\StudentManage\web"
npm install
npm run start
```

然后在浏览器打开 `http://localhost:8080`。

## 图片资源说明

后续请把网站图片上传到 `web/assets/images/`，建议命名：

- `site-logo.png`
- `welcome-bg.png`
- `avatar-default.png`

上传后即可在页面中替换占位元素。


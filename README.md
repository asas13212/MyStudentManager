# 学生信息管理系统（JavaFX）

一个适合课程作业展示的中文学生信息管理系统，支持本科生与研究生两类学生的增删改查、查询、排序、统计与导入导出。

## 主要功能

- 新增、修改、删除学生信息
- 浏览全部学生 / 本科生 / 研究生
- 按学号、姓名、班级查询
- 按学号、总分、单科成绩排序
- 统计本科生人数、研究生人数、学生总人数
- 支持 Excel（`.xlsx`）导入 / 导出
- 图形化界面包含“关于”信息页

## 当前仓库结构

```text
StudentManage/
├─ build.ps1                  # 一键编译 / 运行脚本
├─ README.md                  # 项目说明
├─ .gitignore                 # Git 忽略规则
├─ DevNotes.md                # 聊天记录 / 开发日志（主记录）
├─ src/                       # Java 源码
│  ├─ module-info.java
│  └─ studentmanage/
│     ├─ Main.java
│     ├─ MainApp.java
│     ├─ MenuController.java
│     ├─ StudentService.java
│     └─ ...
├─ resources/                 # 静态资源（样式、图片）
│  ├─ style.css
│  └─ Image/
│     ├─ deepin.png
│     └─ ncu.png
├─ web/                       # 网站移植模块（欢迎页 + 侧边栏主业务页）
│  ├─ index.html
│  ├─ pages/
│  │  └─ app.html
│  ├─ assets/
│  │  ├─ css/
│  │  ├─ js/
│  │  └─ images/
│  └─ README.md
└─ docs/                      # 说明与历史资料
   ├─ images/
   │  └─ img.png
   ├─ DevNotes.txt
   └─ archive/
      └─ MainApp.java.bak
```

> 说明：`out/`、`.idea/`、`*.iml`、`*.class` 等内容已按发布仓库习惯纳入忽略规则，不建议提交到 GitHub。

## 设计要点

- **继承关系**：`Student`（抽象类） -> `Undergraduate` / `Postgraduate`
- **组合关系**：`Student` 组合 `Address`
- **统计成员**：
  - `Student.totalCount`
  - `Undergraduate.count`
  - `Postgraduate.count`

## 构建与运行

### 1. 准备 JavaFX SDK

先下载 JavaFX SDK，并设置环境变量 `JAVAFX_HOME` 指向 SDK 根目录（其中应包含 `lib` 目录）。

PowerShell 示例：

```powershell
$env:JAVAFX_HOME = "C:\javafx-sdk-21.0.2"
```

### 2. 编译

```powershell
.\build.ps1
```

编译结果会输出到 `out/classes`，脚本会自动复制 `resources/` 下的静态资源。

### 3. 运行

一键编译并运行：

```powershell
.\build.ps1 -Run
```

脚本默认会启用 `--enable-native-access=javafx.graphics`，用于消除 JavaFX 26 在 JDK 25 下的 native-access 警告。

如需手动启动，命令请写成：

```powershell
java --module-path "$env:JAVAFX_HOME\lib;.\out\classes" --add-modules javafx.controls --enable-native-access=javafx.graphics -m student.manage/studentmanage.Main
```

## 后续扩展方向

- 文件 / 数据库持久化（JSON、CSV、MySQL）
- 分页浏览与组合查询
- 登录与角色权限控制
- 新学生类型扩展

## Web 模块（网站移植版）

- 入口页：`web/index.html`（欢迎页，点击按钮进入系统）
- 主业务页：`web/pages/app.html`（侧边栏导航布局）
- 图片目录：`web/assets/images/`（你后续上传网站图片到这里即可）

本地预览（可选）：

```powershell
Set-Location "E:\Projects\StudentManage\web"
npm install
npm run start
```


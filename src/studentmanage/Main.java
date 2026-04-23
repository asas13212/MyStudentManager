package studentmanage;

public class Main {
    public static void main(String[] args) {
        StudentRepository repository = new StudentRepository(500);
        StudentService service = new StudentService(repository);
        MainApp.launchApp(service, args);
    }
}


/**
 *   需要改进的点
 *
 *   1.成绩添加
 *   成绩添加功能内容不够简洁，要这样做，首先设置点击添加成绩按钮
 *   然后跳出一个窗口，里面应该是要分为两栏，一栏是学科，一栏是成绩，
 *   我还要你实现这样一个功能，在学科部分可以加载预置好的学科
 *   比如高数，大学物理，面向对象实践，也可以自己在里面添加新增按钮
 *   让自己可以添加新的学科，在设置好后，你需要给出绿色的添加按钮来实现成功添加
 *
 *   2.更改图标
 *   每次打开这个管理界面，任务栏的窗口图标好丑，我需要你去教我更改它
 *
 *   3.聊天记录
 *   工程文件中的DevNotes 虽然可以简单记载聊天文本，但是不能记录时间
 *   我希望你可以用更好更规范的模式去记录我们的聊天记录，你也可以使用别的文件如markdown来记录
 *
 *   4.README
 *   我希望你可以做一些小巧思，让界面更好看
 *
 *   5.学生管理界面
 *   界面过于丑陋，美化界面
 *
 *   6.滑轮功能
 *   学生信息导入，查询那三个折叠列表中，我想让你加一个滑轮功能，在打开学生信息导入时候，我可以通过滑轮来往下滑，看我的查询与排序功能
 *   同时，在数据列表也要有滑轮功能，可以把下面两个主界面往上移动，直到把新手操作给覆盖
 *   还要这样做，可以把新手操作设置可以关闭，关闭后可以自动向上移动，也就是下面的区域变大，具体的你自己看着办，以简洁为主
 *
 *   7.学生管理系统后面的javaFX全中文文字删去,影响了我的阅读
 *
 *   8.在年龄，班级，省份，城市部分增加自己选择的按钮
 *
 *   9.无法正常的更换表单，需要可以删除的按钮
 */





       //<editor-fold desc="    豆包神威">
//请帮我美化这个 JavaFX 学生信息管理系统，要求**保留所有原有功能逻辑不变**，只对界面进行现代化重构和美化。
//我会把要求拆解得非常详细，你严格按照要求执行即可。
//
//        ---
//
//        ## 一、 整体布局重构（先改结构）
//        1. 保持原有的“左表格 + 右表单”的大结构，但优化间距和对齐
//2. 顶部标题栏：
//        - 标题“学生信息管理系统”居中，字号 24px，加粗，深蓝色
//   - 下方加一条细灰色分割线
//3. 左侧表格区域：
//        - 占界面宽度的 65%，上下左右留 15px 内边距
//   - 表格上方加“数据列表”小标题（16px，灰色）
//        4. 右侧表单区域：
//        - 占界面宽度的 35%，上下左右留 15px 内边距
//   - 表单内控件垂直间距统一为 12px，标签和输入框左对齐
//5. 底部状态栏：
//        - 显示统计信息（本科生/研究生/总人数），字号 12px，浅灰色
//   - 背景色为浅灰，和主界面区分开
//
//---
//
//        ## 二、 配色方案（严格按这个来）
//        - 主背景色：#f5f7fa（浅灰蓝，不刺眼）
//        - 卡片/表格背景：#ffffff（纯白，加阴影）
//        - 主色调（按钮/选中）：#00b42a（清新绿，对应“新增”）
//        - 次色调（取消/重置）：#86909c（中性灰）
//        - 文字主色：#1d2129（深黑灰，不刺眼）
//        - 文字次色：#4e5969（中灰，用于提示）
//        - 边框/分割线：#e5e6eb（浅灰）
//        - 警告/错误：#f53f3f（红色）
//        - 成功提示：#00b42a（绿色）
//
//        ---
//
//        ## 三、 控件样式详细要求（每个控件都要改）
//        ### 1. 表格（TableView）—— 核心美化重点
//- 整体：白色背景，8px 圆角，加轻微阴影（dropshadow，透明度 0.1，半径 8，Y轴偏移 2）
//        - 表头：背景色 #f0f2f5，文字加粗 #4e5969，底部边框 #e5e6eb
//- 行高：36px，内容垂直居中
//- 单元格：内边距 12px，文字左对齐
//- 选中行：背景色 #e8f3ff，文字 #1677ff
//- 斑马纹（可选）：偶数行背景 #fafafa
//- 滚动条：细条，浅灰色，不突兀
//
//### 2. 按钮（Button）
//        - 主按钮（新增学生、执行查询）：
//        - 背景色 #00b42a，文字白色，加粗
//  - 6px 圆角，内边距 10px 20px
//  - 悬停：背景色 #00a026
//  - 点击：背景色 #008c22
//- 次按钮（清空表单、重置列表）：
//        - 背景色 #f2f3f5，文字 #4e5969
//        - 6px 圆角，内边距 10px 20px
//  - 悬停：背景色 #e5e6eb
//- 按钮间距：8px
//
//### 3. 输入框（TextField）、下拉框（ComboBox）、文本域（TextArea）
//        - 背景：白色
//- 边框：#dcdfe6，6px 圆角
//- 内边距：8px 12px
//- 聚焦状态：
//        - 边框变 #409eff
//  - 加极淡蓝色阴影（dropshadow，rgba(64,158,255,0.2)，半径 4）
//        - 提示文字（Prompt Text）：#c9cdd4
//
//### 4. 标签（Label）
//        - 标题标签（如“学生信息管理系统”）：24px，加粗，#2c3e50
//- 表单标签（如“学号：”、“姓名：”）：14px，#1d2129，右对齐（和输入框对应）
//        - 提示标签（如“必填”、“示例”）：12px，#86909c
//- 统计标签（底部）：12px，#86909c
//
//### 5. 分割线（Separator）
//        - 颜色 #e5e6eb，粗细 1px
//
//---
//
//        ## 四、 交互体验优化
//1. 按钮悬停：所有按钮都要有颜色变化的悬停效果
//2. 表格行：鼠标悬停时背景变 #f7f8fa
//3. 输入框：聚焦时有明显的蓝色边框提示
//4. 操作反馈：
//        - 新增/删除/修改成功：弹出绿色提示框（或在状态栏显示绿色文字）
//        - 操作失败：弹出红色警告
//5. 窗口：最小化/关闭按钮正常，窗口可拖拽
//
//---
//
//        ## 五、 字体规范
//- 全局默认字体：“Microsoft YaHei”（微软雅黑），如果系统没有，回退到 “Segoe UI” 或系统默认 sans-serif
//- 字号层级：
//        - 大标题：24px
//  - 小标题：16px
//  - 正文/控件：14px
//  - 提示/统计：12px
//
//---
//
//        ## 六、 技术实现要求
//1. **不要改业务逻辑**：原有的增删改查、统计、排序、关于页面功能全部保留，只改 UI 代码
//2. **使用 CSS 美化**：
//        - 新建一个 `style.css` 文件，把所有样式写在里面
//   - 在 Java 代码中加载 CSS：`scene.getStylesheets().add("style.css");`
//        3. **兼容 JDK 8**：不要用 JDK 9+ 的新特性，确保能在 JDK 8 下直接运行
//4. **代码结构**：如果原项目是纯 Java 代码（没有 FXML），就在 Java 代码里用 `setStyle()` 或加载 CSS 文件；如果有 FXML，在 FXML 里绑定 CSS 类
//
//---
//
//        ## 七、 给你一个 CSS 模板（直接参考/复用）
//你可以直接把下面的 CSS 作为基础，在此之上调整：
//        ```css
///* 全局重置 */
//* {
//        -fx-font-family: "Microsoft YaHei", "Segoe UI", sans-serif;
//    -fx-font-size: 14px;
//}
//
//        /* 主界面背景 */
//        .root {
//    -fx-background-color: #f5f7fa;
//}
//
///* 标题样式 */
//.label-title {
//    -fx-font-size: 24px;
//    -fx-font-weight: bold;
//    -fx-text-fill: #2c3e50;
//}
//
///* 表格样式 */
//.table-view {
//    -fx-background-color: #ffffff;
//    -fx-background-radius: 8px;
//    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);
//    -fx-border-radius: 8px;
//}
//.table-view .column-header {
//    -fx-background-color: #f0f2f5;
//    -fx-font-weight: bold;
//    -fx-text-fill: #4e5969;
//    -fx-border-color: transparent transparent #e5e6eb transparent;
//}
//.table-view .table-row-cell {
//    -fx-border-color: transparent transparent #f2f3f5 transparent;
//    -fx-cell-size: 36px;
//}
//.table-view .table-row-cell:selected {
//    -fx-background-color: #e8f3ff;
//    -fx-text-fill: #1677ff;
//}
//.table-view .table-cell {
//    -fx-padding: 12px;
//    -fx-alignment: CENTER_LEFT;
//}
//
///* 主按钮 */
//.button-primary {
//    -fx-background-color: #00b42a;
//    -fx-text-fill: white;
//    -fx-font-weight: bold;
//    -fx-background-radius: 6px;
//    -fx-padding: 10px 20px;
//}
//.button-primary:hover {
//    -fx-background-color: #00a026;
//}
//
///* 次按钮 */
//.button-secondary {
//    -fx-background-color: #f2f3f5;
//    -fx-text-fill: #4e5969;
//    -fx-background-radius: 6px;
//    -fx-padding: 10px 20px;
//}
//.button-secondary:hover {
//    -fx-background-color: #e5e6eb;
//}
//
///* 输入框 */
//.text-field, .combo-box {
//    -fx-background-color: white;
//    -fx-border-color: #dcdfe6;
//    -fx-border-radius: 6px;
//    -fx-background-radius: 6px;
//    -fx-padding: 8px 12px;
//}
//.text-field:focused, .combo-box:focused {
//    -fx-border-color: #409eff;
//    -fx-effect: dropshadow(gaussian, rgba(64,158,255,0.2), 4, 0, 0, 0);
//}
//</editor-fold>

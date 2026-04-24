package studentmanage;

public class Main {
    public static void main(String[] args) {
        StudentRepository repository = new StudentRepository(500);
        StudentService service = new StudentService(repository);
        MainApp.launchApp(service, args);
    }
}


/*
   1.精简界面提示与网站格式
    ( 学生信息管理系统----JavaFX 全中文界面 )
    ( 数据列表处添加扩大区域大小的功能，让数据列表区域可以增大缩小,使得全屏观感更好  )
    ( 数据列表(滚轮可上移主界面) )
    ( 教学作业版 - javaFX 可视化管理 )

    2.文件操作

 */
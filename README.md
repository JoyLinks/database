# SQL参数化组件

#### 介绍
为JDBC数据库操作标准SQL语句提供命名参数支持

JDBC参数
```SQL
SELECT * FROM `users` WHERE `mobile`=?;
```
命名参数
```SQL
SELECT * FROM `users` WHERE `mobile`=?mobile;
```

#### 软件架构
封装JDBC操作，提供SQL命名参数支持，需要数据库驱动，除外没有其它依赖；
1. 提供SQL命名参数支持；
2. 命名参数设置方法为setValue(name,value)，当参数类型调整时无须修改方法名；
3. 数据读取方法为getValue(name,default)，强制其指定默认值，如果数据库返回null用默认值替代；
4. 将SQLException处理为RuntimeException；
5. 支持MySQL和Oracle；

#### 使用说明

1.  执行数据操作之前必须初始化
```java
Database.initialize(int type, String url, String user, String password);
```
2.  查询
```java
try (Statement statement = Database.instance("SELECT * FROM `users` WHERE `mobile`=?mobile")){
    statement.setValue("mobile", "1388306****");
     if (statement.execute()) {
        if (statement.nextRecord()) {
            User user = new User();
            user.setId(statement.getValue("id", 0));
            user.setAlias(statement.getValue("alias", ""));
            user.setName(statement.getValue("name", ""));
        }
    }
}
```
3.  记录创建并获取自增id
```java
try (Statement statement = Database.instance("INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)")){
    statement.setValue("name", "ZhangXi");
    statement.setValue("mobile", "1388306****");
    if (statement.execute()) {
        // 获取自增id,数据库字段必须具有AUTO_INCREMENT属性
        user.setId(statement.getAutoId());
    }
}
```
4. 批量操作
```java
try (Statement statement = Database.instance("INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)")){
    statement.setValue("name", "ZhangXi");
    statement.setValue("mobile", "1388306****");
    statement.batch();

    statement.setValue("name", "ChenLuo");
    statement.setValue("mobile", "1310130****");
    statement.batch();

    if (statement.execute()) {
        // ...
    }
}
```
5. 事务操作
```java
try (Statement statement1 = Database.instance("INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)", true)){
    statement1.setValue("name", "ZhangXi");
    statement1.setValue("mobile", "1388306****");

     if (statement1.execute()) {
        Statement statement2 = Database.instance("INSERT INTO `employees` (`name`,`mobile`) VALUES (?name,?mobile)", statement1);
        statement2.setValue("name", "ChenLuo");
        statement2.setValue("mobile", "1310130****");
        if (statement2.execute()) {
            // ...
        }
    }
}
```
6. 存储过程/函数执行
```java
try (Statement statement = Database.instance("{CALL register(?name,?mobile,?id:LONG)}")){
    statement.setValue("name", "ZhangXi");
    statement.setValue("mobile", "1388306****");

    if (statement.execute()) {
         // 获取OUT参数
         user.setId(statement.getValue("id", 0L));
         while(statement.nextRecord()){
             // 获取记录集
        }
    }
}
```
7. 资源释放
```java
Database.destory();
```

#### 参与贡献

1. ZhangXi
2. 中翌智联 www.joyzl.com
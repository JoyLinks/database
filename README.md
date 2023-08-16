# SQL参数化组件

#### 介绍
为JDBC数据库操作标准SQL语句提供命名参数支持

JDBC参数，'?'问号占位符

```SQL
SELECT * FROM `users` WHERE `mobile`=?;
```
命名参数，'?'问号名称占位符

```SQL
SELECT * FROM `users` WHERE `mobile`=?mobile;
```

#### 软件架构
封装JDBC操作，提供SQL命名参数支持，需要数据库驱动，除外没有其它依赖；所有功能的实现均来自JDBC的功能支持。
1. 提供SQL命名参数支持；
2. 命名参数设置方法为setValue(name,value)，当参数类型调整时无须修改方法名；
3. 数据读取方法为getValue(name,default)，强制其指定默认值，如果数据库返回null用默认值替代；
4. 将SQLException处理为RuntimeException；
5. 支持MySQL和Oracle。

#### 使用说明

添加 Maven 依赖，在项目的pom.xml文件中

```xml
<dependency>
	<groupId>com.joyzl</groupId>
	<artifactId>database</artifactId>
	<version>2.1</version>
</dependency>
```

1.  执行数据操作之前必须初始化

```java
Database.initialize(int type, String url, String user, String password);
```

2.  查询

```java
final String SQL = "SELECT * FROM `users` WHERE `mobile`=?mobile";

try (Statement statement = Database.instance(SQL)){
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

3.  记录创建并获取自增主键

```java
final String SQL = "INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)";

try (Statement statement = Database.instance(SQL)){
    statement.setValue("name", "ZhangXi");
    statement.setValue("mobile", "1388306****");
    if (statement.execute()) {
        // 获取自增主键
        // 主键字段必须具有AUTO_INCREMENT属性
        user.setId(statement.getAutoId());
    }
}
```

4. 批量操作

```java
final String SQL = "INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)";

try (Statement statement = Database.instance(SQL)){
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
final String SQL1 = "INSERT INTO `users` (`id`,`name`,`mobile`) VALUES (?name,?mobile)";
final String SQL2 = "INSERT INTO `employees` (`user`,`number`) VALUES (?user,?number)";

try (Statement statement1 = Database.instance(SQL1, true);
     Statement statement2 = Database.instance(SQL2, statement1);){
    statement1.setValue("id", 1);
    statement1.setValue("name", "ZhangXi");
    statement1.setValue("mobile", "1388306****");
    if (statement1.execute()) {
        statement2.setValue("user", 1);
        statement2.setValue("number", "10002");
        if (statement2.execute()) {
            // ...
        }
    }
}
```

try{}块完成后事务会自动提交或回滚（如果出现错误或异常）。

6. 存储过程/函数执行

```java
final String SQL = "{CALL register(?name,?mobile,?id:LONG)}";

try (Statement statement = Database.instance(SQL)){
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

支持存储过程IN、INOUT、OUT参数，INOUT和OUT参数必须指定类型以标记参数类型，参数类型参考java.sql.Types，必须全部大写。
可同时获取存储过程输出参数和返回的结果集，但必须先获取输出参数然后获取结果集。

7. 资源释放

```java
Database.destory();
```

#### 高级特性

##### 多次执行 Statement 实例

通常情况下获取的 Statement 实例完成特定的数据操作然后并关闭连接释放资源；
在可预计的情况下，Statement 实例可以重复使用多次执行然后再释放。

```java
try (Statement statement = Database.instance("SELECT * FROM `users` WHERE `enable`=?enable")) {
    // 第一次执行
    statement.setValue("enable", true);
    if (statement.execute()) {
        while (statement.nextRecord()) {
            assertTrue(statement.getValue("id", 0) > 0);
        }
    }

    // 第二次执行
    statement.setValue("enable", false);
    if (statement.execute()) {
        while (statement.nextRecord()) {
            assertTrue(statement.getValue("id", 0) > 0);
        }
    }
}
```

注意：不要将Statement实例缓存起来，任何时候使用完成后都应立即释放资源。

##### 交叉执行多个 Statement 实例

在单个数据库连接实例交叉或几乎同时执行多个 Statement 实例。

```java
final String SQL1="SELECT * FROM `users` WHERE `enable`=?enable ORDER BY `id`";
final String SQL2="SELECT * FROM `employees` WHERE `enable`=?enable ORDER BY `id`";

try (Statement statement1 = Database.instance(SQL1);
    Statement statement2 = Database.instance(SQL2, statement1)) {
    statement1.setValue("enable", true);
    statement2.setValue("enable", true);
    if (statement1.execute()) {
        if (statement2.execute()) {
            while (statement1.nextRecord() && statement2.nextRecord()) {
                ...
            }
        }
    }
}
```

示例中的两个 Statement 实例使用相同的数据库连接实例，可以几乎同时获取并读取两个查询结果集。
未能验证可以支持多少个 Statement 实例同时存在以及其性能，因此不推荐在单个数据库连接实例时同时使用过多 Statement 实例。

##### 获取批量插入的多个自增主键

```java
final String SQL = "INSERT INTO `energies` (`number`)VALUES(?number)";

try (Statement statement = Database.instance(SQL)) {
    for (int index = 0; index < 10; index++) {
        statement.setValue("number", index);
        statement.batch();
    }
    if (statement.execute()) {
        // 获取批量插入的总数量
        int count = statement.getUpdatedCount();
        // 获取批量插入每项数量
        int[] resulrs = statement.getUpdatedBatchs();
        for (int index = 0; index < resulrs.length; index++) {
            ...
        }
        // 获取批量插入的自增主键
        while (statement.nextAutoId()) {
            assertTrue(statement.getAutoId() > 0);
        }
    }
}
```

##### 执行存储过程的特殊情况

大多数情况下
``"{CALL `enable_users`(?enable,?count:INTEGER)}"``
这种方式执行数据库存储过程都是正常的，即可使用输入参数也可使用输出参数；
但是如果在数据库连接字符串中未指定数据库名称时（通过``USE `database-name`;``指定当前数据库），情况将发生变化，可能会收到以下异常：
``SQLException: Parameter number 2 is not an OUT parameter``
异常的描述信息非常有迷惑性，实际上是数据库连接URL中未指定数据库名称导致的。

此时必须在存储过程名称前冠以数据库名称才能正常执行存储过程
``"{CALL `joyzl-database-test`.`enable_users`(?enable,?count:INTEGER)}"``


#### 参与贡献

1. 张希 ZhangXi
2. 中翌智联 www.joyzl.com
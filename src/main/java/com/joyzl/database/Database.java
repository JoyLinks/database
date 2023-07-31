/*
 * www.joyzl.net
 * 中翌智联（重庆）科技有限公司
 * Copyright © JOY-Links Company. All rights reserved.
 */
package com.joyzl.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 数据库操作，对JDBC接口进行封装<br>
 * 1.提供SQL命名参数支持<br>
 * 2.统一命名参数设置方法为setValue(name,value)，当参数类型调整时无须修改方法名<br>
 * 3.统一数据读取方法为getValue(name,default)，强制其指定默认值，如果数据库返回null用默认值替代<br>
 * 4.统一将SQLException处理为RuntimeException<br>
 * 5.统一自动释放/回收数据库连接资源<br>
 *
 * <pre>
 * <code>
 * // 执行数据操作之前必须初始化
 * Database.initialize(int type, String url, String user, String password);
 *
 * // 程序停止时释放数据库资源
 * Database.destory();
 * </code>
 * </pre>
 *
 * <pre>
 * <code>
 * // 数据库查询
 * try (Statement statement = Database.instance("SELECT * FROM `users` WHERE `mobile=?mobile")){
 *     statement.setValue("mobile", "13883833982");
 *     if (statement.execute()) {
 *         if (statement.nextRecord()) {
 *             User user = new User();
 *             user.setId(statement.getValue("id", 0));
 *             user.setAlias(statement.getValue("alias", ""));
 *             user.setName(statement.getValue("name", ""));
 *         }
 *     }
 * }</code>
 * </pre>
 *
 * <pre>
 * <code>
 * // 数据库创建记并获取自增id
 * try (Statement statement = Database.instance("INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)")){
 *     statement.setValue("name", "ZhangXi");
 *     statement.setValue("mobile", "13883833982");
 *     if (statement.execute()) {
 *         // 获取自增id,数据库字段必须具有AUTO_INCREMENT属性
 *         user.setId(statement.getAutoId());
 *     }
 * }</code>
 * </pre>
 *
 * <pre>
 * <code>
 * // 数据库批量操作
 * try (Statement statement = Database.instance("INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)")){
 *     statement.setValue("name", "ZhangXi");
 *     statement.setValue("mobile", "13883833982");
 *     statement.batch();
 *
 *     statement.setValue("name", "ChenLuo");
 *     statement.setValue("mobile", "13101301860");
 *     statement.batch();
 *
 *     if (statement.execute()) {
 *         // ...
 *     }
 * }</code>
 * </pre>
 *
 * <pre>
 * <code>
 * // 数据库事务操作
 * try (Statement statement1 = Database.instance("INSERT INTO `users` (`name`,`mobile`) VALUES (?name,?mobile)", true)){
 *     statement1.setValue("name", "ZhangXi");
 *     statement1.setValue("mobile", "13883833982");
 *
 *     if (statement1.execute()) {
 *         Statement statement2 = Database.instance("INSERT INTO `employees` (`name`,`mobile`) VALUES (?name,?mobile)", statement1);
 *         statement2.setValue("name", "ChenLuo");
 *         statement2.setValue("mobile", "13101301860");
 *         if (statement2.execute()) {
 *             // ...
 *         }
 *     }
 * }</code>
 * </pre>
 *
 * <pre>
 * <code>
 * // 数据库存储过程/函数执行
 * try (Statement statement = Database.instance("{CALL register(?name,?mobile,?id:LONG)}")){
 *     statement.setValue("name", "ZhangXi");
 *     statement.setValue("mobile", "13883833982");
 *
 *     if (statement.execute()) {
 *         // 获取OUT参数
 *         user.setId(statement.getValue("id", 0L));
 *         while(statement.nextRecord()){
 *             // 获取记录集
 *         }
 *     }
 * }</code>
 * </pre>
 *
 * @author simon(ZhangXi TEL:13883833982) 2020年3月20日
 *
 */
public final class Database {

	public final static int MYSQL = 1;
	public final static int ORACLE = 2;

	private static int TYPE;
	// 数据库用户名
	private static String USERNAME = "";
	// 数据库用户密码
	private static String PASSWORD = "";
	// 数据库连接字符串
	private static String CONNECTION_STRING = "";
	// 数据库连接队列
	static Queue<Connection> CONNECTIONS;

	/**
	 * 初始化数据库驱动
	 *
	 * @param type     {@link #MYSQL}/{@link #ORACLE}
	 * @param url      数据库URL
	 * @param user     数据库访问用户
	 * @param password 数据库访问密码
	 * @param maximum  最大连接数
	 */
	public static void initialize(int type, String url, String user, String password, int maximum) {
		TYPE = type;
		USERNAME = user;
		PASSWORD = password;
		CONNECTION_STRING = url;
		CONNECTIONS = new ArrayBlockingQueue<>(maximum);

		try {
			switch (type) {
			case MYSQL:
				// Class.forName("com.mysql.jdbc.Driver");
				// MySQL 采用了新的包名称
				Class.forName("com.mysql.cj.jdbc.Driver");
				break;
			case ORACLE:
				// jdbc:oracle:thin:@myhost:1521/myorcldbservicename
				Class.forName("oracle.jdbc.driver.OracleDriver");
				break;
			default:
				throw new IllegalArgumentException("不支持的数据库类型 " + type);
			}
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException("mysql Deiver not found", ex);
		}

		// JNDI
		// Context ctx = new InitialContext();
		// DataSource ds = (DataSource)
		// ctx.lookup("java:comp/env/jdbc/MySQLDB");
		// ds.getConnection();
	}

	/**
	 * 检查数据库链路是否正常，此方法柱塞当前线程直至数据库连接恢复
	 */
	public final static void checkWait() {
		if (CONNECTIONS == null || CONNECTIONS.isEmpty()) {
			while (true) {
				try {
					Connection connection = getConnection();
					if (connection.isValid(10)) {
						if (CONNECTIONS.offer(connection)) {
							// OK
						} else {
							connection.close();
						}
						return;
					} else {
						connection.close();
					}
				} catch (Exception e) {
					System.err.println("数据库无法连接，等待重试:" + e.getMessage());
					try {
						Thread.sleep(10 * 1000);
					} catch (InterruptedException e1) {
						// 忽略此异常
					}
				}
			}
		} else {
			int size = CONNECTIONS.size();
			while (size-- > 0) {
				try {
					Connection connection = CONNECTIONS.poll();
					if (connection.isValid(1)) {
						CONNECTIONS.offer(connection);
					} else {
						connection.close();
					}
				} catch (SQLException e) {
					// 忽略错误
				}
			}
			if (CONNECTIONS.isEmpty()) {
				checkWait();
			}
		}
	}

	/**
	 * 销毁数据库及所有缓存连接
	 */
	public final static void destory() {
		final Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			try {
				final Driver driver = drivers.nextElement();
				DriverManager.deregisterDriver(driver);
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
		}

		if (CONNECTIONS == null || CONNECTIONS.isEmpty()) {
			// EMPTY
		} else {
			Connection connection;
			while ((connection = CONNECTIONS.poll()) != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// 忽略错误
				}
			}
		}

		if (TYPE == MYSQL) {
			// http://docs.oracle.com/cd/E17952_01/connector-j-relnotes-en/news-5-1-23.html
			// import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
			// AbandonedConnectionCleanupThread.checkedShutdown();
		}
	}

	/**
	 * 指示数据库是否初始化
	 * 
	 * @return 是否初始化
	 */
	public static boolean isInitialized() {
		return CONNECTIONS != null;
	}

	////////////////////////////////////////////////////////////////////////////////

	/**
	 * 获取数据库连接，优先从连接队列获取，如果连接队列为空则新建连接
	 *
	 * @return Connection
	 * @throws SQLException
	 */
	static Connection getConnection() throws SQLException {
		if (CONNECTIONS.isEmpty()) {
			return DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD);
		} else {
			Connection connection = CONNECTIONS.poll();
			if (connection == null) {
				return getConnection();
			} else {
				if (connection.isValid(1)) {
					// isValid(8) 提交一个查询到数据库验证连接是否有效
					// 最长等待 8秒
					// 8秒过长,等待一秒即可
					return connection;
				} else {
					connection.close();
					return getConnection();
				}
			}
		}
	}

	/**
	 * 实例化数据访问对象<br>
	 * {@code SELECT * FROM `users` WHERE `id`=?id}<br>
	 * {@code statement.setValue("id",1);}
	 *
	 * @param sql 命名参数SQL语句
	 * @return Statement 实例
	 */
	public static Statement instance(String sql) {
		return new Statement(sql, false);
	}

	/**
	 * 实例化数据访问对象<br>
	 * {@code SELECT * FROM `users` WHERE `id`=?id}<br>
	 * {@code statement.setValue("id",1);}<br>
	 * 如果开启事务(transaction 参数为 true) 则会将自动提交设置为 false, 执行完成后将自动提交或回滚事务
	 *
	 * @param sql         命名参数SQL语句
	 * @param transaction 是否开启事务
	 * @return Statement 实例
	 */
	public static Statement instance(String sql, boolean transaction) {
		return new Statement(sql, transaction);
	}

	/**
	 * 实例化数据访问对象<br>
	 * {@code SELECT * FROM `users` WHERE `id`=?id}<br>
	 * {@code statement.setValue("id",1);}<br>
	 * 指定一个已有的Statement作为关联，将与当前的 Statement 形成事务，执行完成后将自动提交或回滚事务<br>
	 * 如果未开启事务，则共用数据库链路，避免短时多次获取链路。
	 *
	 * @param sql 命名参数SQL语句
	 * @return Statement 实例
	 */
	public static Statement instance(String sql, Statement statement) {
		return new Statement(sql, statement);
	}
}

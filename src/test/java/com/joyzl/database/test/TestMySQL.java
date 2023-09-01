package com.joyzl.database.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.joyzl.database.Database;
import com.joyzl.database.Statement;

/**
 * MySQL数据库操作测试，必须全部执行，单个测试方法执行将缺失上下文
 * 
 * @author ZhangXi
 * @date 2023年8月15日
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestMySQL {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		final String url = "jdbc:mysql://192.168.8.2:3306/?characterEncoding=utf8&serverTimezone=GMT%2B8";
		final String user = "root";
		final String pswd = "JOYZL.mysql.2020";
		Database.initialize(Database.MYSQL, url, user, pswd, 1);
		databaseCreate();
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		databaseDrop();
		Database.destory();
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	static void databaseCreate() {
		final String SQL1 = "CREATE DATABASE `joyzl-database-test`";
		final String SQL2 = "USE `joyzl-database-test`";
		try (Statement statement1 = Database.instance(SQL1);
			Statement statement2 = Database.instance(SQL2)) {
			if (statement1.execute()) {
				statement2.execute();
			} else {
				fail("DATABASE CREATE FAIL");
			}
		}
	}

	static void databaseDrop() {
		final String SQL = "DROP DATABASE `joyzl-database-test`";
		try (Statement statement = Database.instance(SQL)) {
			if (statement.execute()) {
			} else {
				fail("DATABASE DROP FAIL");
			}
		}
	}

	@Test
	@Order(1)
	void testCreateTable() {
		final String TABLE1 = "CREATE TABLE `users` (`id` BIGINT(20) UNSIGNED NOT NULL,`mobile` VARCHAR(16) NOT NULL COMMENT '手机',`name` VARCHAR(32) DEFAULT NULL COMMENT '姓名',`enable` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '启用',`created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,`updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,PRIMARY KEY (`id`),UNIQUE KEY `uq.users.mobile` (`mobile`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户'";
		try (Statement statement = Database.instance(TABLE1)) {
			statement.execute();
		}

		final String TABLE2 = "CREATE TABLE `energies` (`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',`number` INT(10) UNSIGNED DEFAULT '0' COMMENT '累计',`created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
		try (Statement statement = Database.instance(TABLE2)) {
			statement.execute();
		}
	}

	@Test
	@Order(2)
	void testCreateProcedure() {
		final String PROCEDURE = "CREATE PROCEDURE `enable_users`(enable BOOLEAN,OUT c INT)BEGIN \n SELECT COUNT(*) INTO c FROM `users` WHERE `enable`=enable; \n SELECT * FROM `users` WHERE `enable`=enable;\nEND;";
		try (Statement statement = Database.instance(PROCEDURE)) {
			statement.execute();
		}
	}

	@Test
	@Order(10)
	void testInsert() {
		final String SQL = "INSERT INTO `users` (`id`,`mobile`,`name`,`enable`)VALUES(?id,?mobile,?name,?enable)";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("id", 1);
			statement.setValue("mobile", "00000000001");
			statement.setValue("name", "姓名1");
			statement.setValue("enable", true);
			if (statement.execute()) {
			} else {
				fail("INSERT FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("id", 2);
			statement.setValue("mobile", "00000000002");
			statement.setValue("name", "姓名1");
			statement.setValue("enable", true);
			if (statement.execute()) {
				assertEquals(statement.getAutoId(), 0);
			} else {
				fail("INSERT FAIL");
			}

			statement.setValue("id", 3);
			statement.setValue("mobile", "00000000003");
			statement.setValue("name", "姓名2");
			statement.setValue("enable", true);
			if (statement.execute()) {
				assertEquals(statement.getAutoId(), 0);
			} else {
				fail("INSERT FAIL");
			}
		}
	}

	@Test
	@Order(11)
	void testInsertBatch() {
		final String SQL = "INSERT INTO `users` (`id`,`mobile`,`name`,`enable`)VALUES(?id,?mobile,?name,?enable)";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 10);
				statement.setValue("mobile", "000000001" + index);
				statement.setValue("name", "姓名" + index);
				statement.setValue("enable", true);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("INSERT BATCH FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 20);
				statement.setValue("mobile", "000000002" + index);
				statement.setValue("name", "姓名" + index);
				statement.setValue("enable", true);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("INSERT BATCH FAIL");
			}

			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 30);
				statement.setValue("mobile", "000000003" + index);
				statement.setValue("name", "姓名" + index);
				statement.setValue("enable", true);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("INSERT BATCH FAIL");
			}
		}
	}

	@Test
	@Order(20)
	void testSelect() {
		final String SQL = "SELECT * FROM `users` WHERE `enable`=?enable";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("enable", true);
			if (statement.execute()) {
				while (statement.nextRecord()) {
					assertTrue(statement.getValue("id", 0) > 0);
				}
			} else {
				fail("SELECT FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("enable", true);
			if (statement.execute()) {
				while (statement.nextRecord()) {
					assertTrue(statement.getValue("id", 0) > 0);
				}
			} else {
				fail("SELECT FAIL");
			}

			statement.setValue("enable", false);
			if (statement.execute()) {
				while (statement.nextRecord()) {
					assertTrue(statement.getValue("id", 0) > 0);
				}
			} else {
				fail("SELECT FAIL");
			}
		}
	}

	@Test
	@Order(20)
	void testSelectCross() {
		final String SQL = "SELECT * FROM `users` WHERE `enable`=?enable ORDER BY `id`";
		// 多端查询
		try (Statement statement1 = Database.instance(SQL);
			Statement statement2 = Database.instance(SQL, statement1)) {
			statement1.setValue("enable", true);
			statement2.setValue("enable", true);
			if (statement1.execute()) {
				if (statement2.execute()) {
					while (statement1.nextRecord() && statement2.nextRecord()) {
						assertEquals(statement1.getValue("id", 0), statement2.getValue("id", 0));
					}
				} else {
					fail("SELECT FAIL");
				}
			} else {
				fail("SELECT FAIL");
			}
		}
	}

	@Test
	@Order(30)
	void testUpdate() {
		final String SQL = "UPDATE `users` SET `name`=?name,`enable`=?enable WHERE `id`=?id";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("id", 1);
			statement.setValue("name", "姓名1");
			statement.setValue("enable", true);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
			} else {
				fail("UPDATE FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("id", 2);
			statement.setValue("name", "姓名2");
			statement.setValue("enable", true);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
			} else {
				fail("UPDATE FAIL");
			}

			statement.setValue("id", 3);
			statement.setValue("name", "姓名3");
			statement.setValue("enable", true);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
			} else {
				fail("UPDATE FAIL");
			}
		}
	}

	@Test
	@Order(31)
	void testUpdateBatch() {
		final String SQL = "UPDATE `users` SET `name`=?name,`enable`=?enable WHERE `id`=?id";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 10);
				statement.setValue("name", "n" + index);
				statement.setValue("enable", true);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("UPDATE BATCH FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 20);
				statement.setValue("name", "n" + index);
				statement.setValue("enable", true);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("UPDATE BATCH FAIL");
			}

			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 30);
				statement.setValue("name", "n" + index);
				statement.setValue("enable", true);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("UPDATE BATCH FAIL");
			}
		}
	}

	@Test
	@Order(40)
	void testDelete() {
		final String SQL = "DELETE FROM `users` WHERE `id`=?id";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("id", 1);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
			} else {
				fail("DELETE FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("id", 2);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
			} else {
				fail("DELETE FAIL");
			}

			statement.setValue("id", 3);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
			} else {
				fail("DELETE FAIL");
			}
		}
	}

	@Test
	@Order(41)
	void testDeleteBatch() {
		final String SQL = "DELETE FROM `users` WHERE `id`=?id";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 10);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("DELETE BATCH FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 20);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("DELETE BATCH FAIL");
			}

			for (int index = 0; index < 10; index++) {
				statement.setValue("id", index + 30);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
			} else {
				fail("DELETE BATCH FAIL");
			}
		}
	}

	@Test
	@Order(50)
	void testAutoId() {
		final String SQL = "INSERT INTO `energies` (`number`)VALUES(?number)";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("number", 1);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
				assertEquals(statement.getAutoId(), 1);
			} else {
				fail("AUTO ID FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			statement.setValue("number", 2);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
				assertEquals(statement.getAutoId(), 2);
			} else {
				fail("AUTO ID FAIL");
			}

			statement.setValue("number", 3);
			if (statement.execute()) {
				assertEquals(statement.getUpdatedCount(), 1);
				assertEquals(statement.getAutoId(), 3);
			} else {
				fail("AUTO ID FAIL");
			}
		}
	}

	@Test
	@Order(51)
	void testAutoIdBatch() {
		final String SQL = "INSERT INTO `energies` (`number`)VALUES(?number)";
		// 单次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("number", index + 10);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
				while (statement.nextAutoId()) {
					assertTrue(statement.getAutoId() > 0);
				}
			} else {
				fail("INSERT BATCH FAIL");
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(SQL)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("number", index + 20);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
				while (statement.nextAutoId()) {
					assertTrue(statement.getAutoId() > 0);
				}
			} else {
				fail("INSERT BATCH FAIL");
			}

			for (int index = 0; index < 10; index++) {
				statement.setValue("number", index + 30);
				statement.batch();
			}
			if (statement.execute()) {
				assertEquals(10, statement.getUpdatedCount());
				final int[] resulrs = statement.getUpdatedBatchs();
				for (int index = 0; index < resulrs.length; index++) {
					assertEquals(resulrs[index], 1);
				}
				while (statement.nextAutoId()) {
					assertTrue(statement.getAutoId() > 0);
				}
			} else {
				fail("INSERT BATCH FAIL");
			}
		}
	}

	@Test
	@Order(60)
	void testProcedure() {
		/*-
		 * 如果数据库连接URL字符串中未指定数据库，则调用存储过程时需要明确指定数据库名称；
		 * 好像只在使用OUT返回参数时存在此情形，如果不指定数据库名称则会抛出 SQLException: Parameter number 2 is not an OUT parameter
		 */
		final String PROCEDURE = "{CALL `joyzl-database-test`.`enable_users`(?enable,?count:INTEGER)}";
		// 单次执行
		try (Statement statement = Database.instance(PROCEDURE)) {
			statement.setValue("enable", true);
			if (statement.execute()) {
				int count = statement.getValue("count", 0);
				int size = 0;
				while (statement.nextRecord()) {
					assertTrue(statement.getValue("id", 0) > 0);
					size++;
				}
				assertEquals(count, size);
			}
		}
		// 多次执行
		try (Statement statement = Database.instance(PROCEDURE)) {
			for (int index = 0; index < 10; index++) {
				statement.setValue("enable", true);
				if (statement.execute()) {
					statement.getValue("count", 0);
					if (statement.nextRecord()) {
						assertTrue(statement.getValue("id", 0) > 0);
					}
				}
			}
		}
	}
}

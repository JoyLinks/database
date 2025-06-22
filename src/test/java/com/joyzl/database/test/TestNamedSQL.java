/*
 * Copyright © 2017-2025 重庆骄智科技有限公司.
 * 本软件根据 Apache License 2.0 开源，详见 LICENSE 文件。
 */
package com.joyzl.database.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.joyzl.database.NamedSQL;

class TestNamedSQL {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		final String sql = """
				SELECT `department` FROM `domains`
				WHERE `company`=?company AND (?parent IS NULL OR `parent`=?parent) AND `department`=?target
				""";
		final NamedSQL namesSql = NamedSQL.get(sql);
		System.out.println("\"" + sql + "\"");
		System.out.println("\"" + namesSql.getNamedSQL() + "\"");
		System.out.println("\"" + namesSql.getExcuteSQL() + "\"");
	}

}

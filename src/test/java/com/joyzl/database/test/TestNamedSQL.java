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

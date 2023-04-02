package com.fathzer.plugin.loader.jar;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ServiceClassNameBuilderTest {

//	@Test
	void test() {
		assertEquals("toto", ServiceClassNameBuilder.INSTANCE.uncommentAndTrim(" toto\t #comment"));
	}

}

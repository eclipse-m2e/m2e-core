package bnd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ATest {

	@Test
	void testName() throws Exception {
		assertEquals("AClass", new AClass().getName());
	}
}

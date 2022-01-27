package simpleProjectWithJUnit5Test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleTest {
	@Test
	public void testSimple() {
		Assertions.assertTrue(true); // NoOp Test
	}
}

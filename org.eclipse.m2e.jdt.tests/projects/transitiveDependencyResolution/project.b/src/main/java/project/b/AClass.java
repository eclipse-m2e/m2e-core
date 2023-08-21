package project.b;

import org.junit.jupiter.api.Assertions;

public class AClass {

	public void aMethod() {
		// Added in junit-juptier-api 5.8
		Assertions.assertInstanceOf(String.class, "");
	}
}

package felix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AClass {
	private static final Logger LOGGER = LoggerFactory.getLogger(AClass.class);

	public static void main(String[] args) {
		LOGGER.info("Hello world");
	}

	String getName() {
		return "AClass";
	}
}

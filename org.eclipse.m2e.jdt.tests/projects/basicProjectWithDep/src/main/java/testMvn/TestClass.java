package testMvn;

import java.io.StringWriter;

import org.apache.log4j.Appender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

public class TestClass {

	public static void main(String[] args) {
		StringWriter errorOutputWriter = new StringWriter();
		Appender appender = new WriterAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN),
				errorOutputWriter);

		org.apache.log4j.Logger.getRootLogger().addAppender(appender);
		System.out.print("ok");
	}

}

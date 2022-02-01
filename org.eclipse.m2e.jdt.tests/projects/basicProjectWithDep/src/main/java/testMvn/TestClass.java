package testMvn;

import java.io.StringWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class TestClass {

	public static void main(String[] args) {
		StringWriter errorOutputWriter = new StringWriter();
		Appender appender = WriterAppender.newBuilder()
				.setLayout(PatternLayout.newBuilder().withPattern(PatternLayout.TTCC_CONVERSION_PATTERN).build())
				.setTarget(errorOutputWriter).setName("testMvn").build();

		final LoggerContext context = LoggerContext.getContext(false);
	    final Configuration config = context.getConfiguration();
	    config.addAppender(appender);
	    config.getRootLogger().addAppender(appender, Level.ALL, null);
		System.out.print("ok");
	}

}

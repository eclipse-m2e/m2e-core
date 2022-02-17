package foo.bar;

public class DummyStuff {

	public void nonSense() {
		Dummy d = AutoValue_Dummy.builder().age(18).name("Fred").build();
	}
}

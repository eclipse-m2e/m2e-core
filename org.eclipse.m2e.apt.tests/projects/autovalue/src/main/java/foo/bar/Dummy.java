package foo.bar;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Dummy {

  public static Builder builder() {
    return new AutoValue_Dummy.Builder();
  }

  public abstract int age();

  public abstract String name();

  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder age(int age);

    abstract Builder name(String name);

    abstract Dummy build();
  }
}
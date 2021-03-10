package services.question;

import java.util.Optional;

public enum ScalarType {
  INT(int.class),
  STRING(String.class);

  ScalarType(Class classOf) {
    this.classOf = classOf;
  }

  private final Class classOf;

  public Optional<Class> getClassFor() {
    return Optional.ofNullable(classOf);
  }
}

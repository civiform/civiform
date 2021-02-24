package services.question;

import java.util.Optional;

public enum ScalarType {
  BOOLEAN(boolean.class),
  BYTE(byte.class),
  CHAR(char.class),
  DOUBLE(double.class),
  FLOAT(float.class),
  INT(int.class),
  SHORT(short.class),
  STRING(String.class);

  ScalarType(Class classOf) {
    this.classOf = classOf;
  }

  private final Class classOf;

  public Optional<Class> getClassFor() {
    return Optional.ofNullable(classOf);
  }
}

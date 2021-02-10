package services.question;

public enum ScalarType {
  BOOLEAN,
  BYTE,
  CHAR,
  DOUBLE,
  FLOAT,
  INT,
  SHORT,
  STRING;

  private Class classOf;

  static {
    BOOLEAN.classOf = boolean.class;
    BYTE.classOf = byte.class;
    CHAR.classOf = char.class;
    DOUBLE.classOf = double.class;
    FLOAT.classOf = float.class;
    INT.classOf = int.class;
    SHORT.classOf = short.class;
    STRING.classOf = String.class;
  }

  public Class getClassFor() {
    return classOf;
  }
}

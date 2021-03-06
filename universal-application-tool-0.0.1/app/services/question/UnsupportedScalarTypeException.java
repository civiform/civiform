package services.question;

public class UnsupportedScalarTypeException extends Exception {

  public UnsupportedScalarTypeException(ScalarType type) {
    super(String.format("Scalar type %s is unsupported", type));
  }
}

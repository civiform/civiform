package services.question.exceptions;

import services.question.types.ScalarType;

public class UnsupportedScalarTypeException extends Exception {

  public UnsupportedScalarTypeException(ScalarType type) {
    super(String.format("Scalar type %s is unsupported", type));
  }
}

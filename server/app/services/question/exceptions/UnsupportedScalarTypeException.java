package services.question.exceptions;

import services.question.types.ScalarType;

/** UnsupportedScalarTypeException is thrown if a scalar is not supported yet. */
public class UnsupportedScalarTypeException extends Exception {

  public UnsupportedScalarTypeException(ScalarType type) {
    super(String.format("Scalar type %s is unsupported", type));
  }
}

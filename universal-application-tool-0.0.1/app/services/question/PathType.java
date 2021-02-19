package services.question;

enum PathType {
  /** Indicates that the path does not exist in the QuestionService. */
  NONE,
  /** Indicates that the path pointts to a QuestionDefinition. */
  QUESTION,
  /** Indicates that the path pointts to a ScalarType. */
  SCALAR
}

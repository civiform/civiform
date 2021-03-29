package services.question;

import java.util.List;
import java.util.Optional;

/**
 * An enum defining the scalar types supported by {@link QuestionDefinition}s and {@link
 * services.applicant.ApplicantService}. Individual inputs in the applicant forms generally
 * correspond to a single scalar value within a question. Scalars are stored in the {@link
 * models.Applicant} JSON column and serialized using {@link services.applicant.ApplicantData}.
 */
public enum ScalarType {
  LONG(long.class),
  STRING(String.class);

  private final Class classOf;

  ScalarType(Class classOf) {
    this.classOf = classOf;
  }

  public Optional<Class> getClassFor() {
    return Optional.ofNullable(classOf);
  }
}

package services.applicant.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.inject.Inject;
import services.DateConverter;
import services.applicant.RepeatedEntity;
import services.question.types.QuestionDefinition;

/** Creates instances of {@link JsonPathPredicateGenerator}. */
public final class JsonPathPredicateGeneratorFactory {

  private final DateConverter dateConverter;

  @Inject
  public JsonPathPredicateGeneratorFactory(DateConverter dateConverter) {
    this.dateConverter = checkNotNull(dateConverter);
  }

  public JsonPathPredicateGenerator create(
      ImmutableList<QuestionDefinition> programQuestions,
      Optional<RepeatedEntity> currentRepeatedContext) {
    return new JsonPathPredicateGenerator(
        dateConverter, checkNotNull(programQuestions), checkNotNull(currentRepeatedContext));
  }
}

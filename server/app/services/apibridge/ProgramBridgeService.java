package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Shared filters and streams used by the program api bridge */
public final class ProgramBridgeService {
  public record HtmlOptionElement(String display, String value) {}

  /**
   * Gets a list of {@link QuestionDefinition} from the {@link ProgramDefinition} that are of the
   * question types that the api bridge supports
   */
  public ImmutableList<QuestionDefinition> getAllowedQuestions(ProgramDefinition program) {
    checkNotNull(program);

    return program.getAllQuestions().stream()
        .filter(
            question ->
                question.getQuestionType() != QuestionType.ENUMERATOR
                    && question.getQuestionType() != QuestionType.STATIC
                    && question.getQuestionType() != QuestionType.FILEUPLOAD)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Creates an {@link ImmutableMap} of {@link QuestionDefinition} admin name to the {@link
   * ImmutableList} of related {@link Scalar} strings suitable for binding to a select element.
   */
  public ImmutableMap<String, ImmutableList<HtmlOptionElement>> getQuestionScalarMap(
      ImmutableList<QuestionDefinition> allQuestions) {
    checkNotNull(allQuestions);

    return allQuestions.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                QuestionDefinition::getQuestionNameKey,
                question -> {
                  try {
                    return Scalar.getScalars(question.getQuestionType()).stream()
                        .map(
                            scalar ->
                                new HtmlOptionElement(scalar.toDisplayString(), scalar.name()))
                        .collect(ImmutableList.toImmutableList());
                  } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
                    return ImmutableList.of();
                  }
                }));
  }
}

package forms;

import java.time.LocalDate;
import java.util.Optional;
import services.question.types.DateQuestionDefinition;
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating a date question. */
public class DateQuestionForm extends QuestionForm {
  private Optional<DateType> minDateType;
  private Optional<LocalDate> minCustomDate;
  private Optional<DateType> maxDateType;
  private Optional<LocalDate> maxCustomDate;

  public DateQuestionForm() {
    super();
    this.minDateType = Optional.empty();
    this.maxDateType = Optional.empty();
    this.minCustomDate = Optional.empty();
    this.maxCustomDate = Optional.empty();
  }

  public DateQuestionForm(DateQuestionDefinition qd) {
    super(qd);
    this.minDateType = qd.getMinDate().map(DateValidationOption::dateType);
    this.minCustomDate = qd.getMinDate().flatMap(DateValidationOption::customDate);
    this.maxDateType = qd.getMaxDate().map(DateValidationOption::dateType);
    this.maxCustomDate = qd.getMaxDate().flatMap(DateValidationOption::customDate);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DATE;
  }

  public Optional<DateType> getMinDateType() {
    return minDateType;
  }

  public Optional<LocalDate> getMinCustomDate() {
    return minCustomDate;
  }

  public Optional<DateType> getMaxDateType() {
    return maxDateType;
  }

  public Optional<LocalDate> getMaxCustomDate() {
    return maxCustomDate;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    DateQuestionDefinition.DateValidationPredicates.Builder dateValidationPredicatesBuilder =
        DateQuestionDefinition.DateValidationPredicates.builder();

    if (getMinDateType().isPresent()) {
      dateValidationPredicatesBuilder.setMinDate(
          DateValidationOption.builder()
              .setDateType(getMinDateType().get())
              .setCustomDate(getMinCustomDate())
              .build());
    }
    if (getMaxDateType().isPresent()) {
      dateValidationPredicatesBuilder.setMaxDate(
          DateValidationOption.builder()
              .setDateType(getMaxDateType().get())
              .setCustomDate(getMaxCustomDate())
              .build());
    }

    return super.getBuilder().setValidationPredicates(dateValidationPredicatesBuilder.build());
  }
}

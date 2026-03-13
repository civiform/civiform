package forms;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import services.question.types.DateQuestionDefinition;
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.QuestionType;

/** Form for updating a date question. */
public class DateQuestionForm extends QuestionForm {
  private Optional<DateType> minDateType;
  private Optional<String> minCustomDay;
  private Optional<String> minCustomMonth;
  private Optional<String> minCustomYear;

  private Optional<DateType> maxDateType;
  private Optional<String> maxCustomDay;
  private Optional<String> maxCustomMonth;
  private Optional<String> maxCustomYear;

  public DateQuestionForm() {
    super();
    this.minDateType = Optional.empty();
    this.minCustomDay = Optional.empty();
    this.minCustomMonth = Optional.empty();
    this.minCustomYear = Optional.empty();

    this.maxDateType = Optional.empty();
    this.maxCustomDay = Optional.empty();
    this.maxCustomMonth = Optional.empty();
    this.maxCustomYear = Optional.empty();
  }

  public DateQuestionForm(DateQuestionDefinition qd) {
    super(qd);
    this.minDateType = qd.getMinDate().map(DateValidationOption::dateType);
    this.minCustomDay =
        qd.getMinDate()
            .flatMap(DateValidationOption::customDate)
            .map(LocalDate::getDayOfMonth)
            .map(day -> String.valueOf(day));
    this.minCustomMonth =
        qd.getMinDate()
            .flatMap(DateValidationOption::customDate)
            .map(LocalDate::getMonthValue)
            .map(month -> String.valueOf(month));
    this.minCustomYear =
        qd.getMinDate()
            .flatMap(DateValidationOption::customDate)
            .map(LocalDate::getYear)
            .map(year -> String.valueOf(year));

    this.maxDateType = qd.getMaxDate().map(DateValidationOption::dateType);
    this.maxCustomDay =
        qd.getMaxDate()
            .flatMap(DateValidationOption::customDate)
            .map(LocalDate::getDayOfMonth)
            .map(day -> String.valueOf(day));
    this.maxCustomMonth =
        qd.getMaxDate()
            .flatMap(DateValidationOption::customDate)
            .map(LocalDate::getMonthValue)
            .map(month -> String.valueOf(month));
    this.maxCustomYear =
        qd.getMaxDate()
            .flatMap(DateValidationOption::customDate)
            .map(LocalDate::getYear)
            .map(year -> String.valueOf(year));
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DATE;
  }

  public Optional<DateType> getMinDateType() {
    return minDateType;
  }

  public void setMinDateType(String minDateType) {
    this.minDateType = Optional.of(DateType.valueOf(minDateType));
  }

  public Optional<String> getMinCustomDay() {
    return minCustomDay;
  }

  public void setMinCustomDay(String minCustomDay) {
    this.minCustomDay = Optional.of(minCustomDay);
  }

  public Optional<String> getMinCustomMonth() {
    return minCustomMonth;
  }

  public void setMinCustomMonth(String minCustomMonth) {
    this.minCustomMonth = Optional.of(minCustomMonth);
  }

  public Optional<String> getMinCustomYear() {
    return minCustomYear;
  }

  public void setMinCustomYear(String minCustomYear) {
    this.minCustomYear = Optional.of(minCustomYear);
  }

  public Optional<DateType> getMaxDateType() {
    return maxDateType;
  }

  public void setMaxDateType(String maxDateType) {
    this.maxDateType = Optional.of(DateType.valueOf(maxDateType));
  }

  public Optional<String> getMaxCustomDay() {
    return maxCustomDay;
  }

  public void setMaxCustomDay(String maxCustomDay) {
    this.maxCustomDay = Optional.of(maxCustomDay);
  }

  public Optional<String> getMaxCustomMonth() {
    return maxCustomMonth;
  }

  public void setMaxCustomMonth(String maxCustomMonth) {
    this.maxCustomMonth = Optional.of(maxCustomMonth);
  }

  public Optional<String> getMaxCustomYear() {
    return maxCustomYear;
  }

  public void setMaxCustomYear(String maxCustomYear) {
    this.maxCustomYear = Optional.of(maxCustomYear);
  }

  @Override
  public DateQuestionDefinition.DateValidationPredicates getValidationPredicates() {
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

    return dateValidationPredicatesBuilder.build();
  }

  private Optional<LocalDate> getMinCustomDate() {
    if (getMinCustomDay().isEmpty()
        || getMinCustomMonth().isEmpty()
        || getMinCustomYear().isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(
          LocalDate.of(
              Integer.valueOf(getMinCustomYear().get()),
              Integer.valueOf(getMinCustomMonth().get()),
              Integer.valueOf(getMinCustomDay().get())));
    } catch (NumberFormatException | DateTimeException e) {
      return Optional.empty();
    }
  }

  private Optional<LocalDate> getMaxCustomDate() {
    if (getMaxCustomDay().isEmpty()
        || getMaxCustomMonth().isEmpty()
        || getMaxCustomYear().isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(
          LocalDate.of(
              Integer.valueOf(getMaxCustomYear().get()),
              Integer.valueOf(getMaxCustomMonth().get()),
              Integer.valueOf(getMaxCustomDay().get())));
    } catch (NumberFormatException | DateTimeException e) {
      return Optional.empty();
    }
  }
}

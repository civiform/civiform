package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionType;

public class DateQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<Long> dayValue;
  private Optional<Long> monthValue;
  private Optional<Long> yearValue;

  public DateQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a date question.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getDayPath())
        && applicantQuestion.getApplicantData().hasPath(getMonthPath())
        && applicantQuestion.getApplicantData().hasPath(getYearPath());
  }

  public Optional<Long> getDayValue() {
    if (dayValue != null) {
      return dayValue;
    }

    dayValue = applicantQuestion.getApplicantData().readLong(getDayPath());

    return dayValue;
  }

  public Optional<Long> getMonthValue() {
    if (monthValue != null) {
      return monthValue;
    }

    monthValue = applicantQuestion.getApplicantData().readLong(getMonthPath());

    return monthValue;
  }

  public Optional<Long> getYearValue() {
    if (yearValue != null) {
      return yearValue;
    }

    yearValue = applicantQuestion.getApplicantData().readLong(getYearPath());

    return yearValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.DATE)) {
      throw new RuntimeException(
          String.format(
              "Question is not a DATE question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public DateQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (DateQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getDayPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.DAY);
  }

  public Path getMonthPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.MONTH);
  }

  public Path getYearPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.YEAR);
  }

  @Override
  public String getAnswerString() {
    String[] parts = {
      getDayValue().map(Object::toString).orElse("-"),
      getMonthValue().map(Object::toString).orElse("-"),
      getYearValue().map(Object::toString).orElse("-")
    };

    return Arrays.stream(parts).filter(part -> part.length() > 0).collect(Collectors.joining(" "));
  }
}

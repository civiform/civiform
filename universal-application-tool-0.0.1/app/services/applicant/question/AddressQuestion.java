package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents an address question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class AddressQuestion implements Question {
  private static final String PO_BOX_REGEX =
      "(?i)(.*(P(OST|.)?\\s*((O(FF(ICE)?)?)?.?\\s*(B(IN|OX|.?)))+)).*";

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> streetValue;
  private Optional<String> line2Value;
  private Optional<String> cityValue;
  private Optional<String> stateValue;
  private Optional<String> zipValue;

  public AddressQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasConditionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    AddressQuestionDefinition definition = getQuestionDefinition();
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (definition.getDisallowPoBox()) {
      Pattern poBoxPattern = Pattern.compile(PO_BOX_REGEX);
      Matcher poBoxMatcher1 = poBoxPattern.matcher(getStreetValue().orElse(""));
      Matcher poBoxMatcher2 = poBoxPattern.matcher(getLine2Value().orElse(""));

      if (poBoxMatcher1.matches() || poBoxMatcher2.matches()) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_NO_PO_BOX));
      }
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.<ValidationErrorMessage>builder()
        .addAll(getAddressErrors())
        .addAll(getStreetErrors())
        .addAll(getCityErrors())
        .addAll(getStateErrors())
        .addAll(getZipErrors())
        .build();
  }

  public ImmutableSet<ValidationErrorMessage> getAddressErrors() {
    // TODO: Implement address validation.
    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStreetErrors() {
    if (isAnswered() && getStreetValue().isEmpty()) {
      return getStreetErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStreetErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED));
  }

  public ImmutableSet<ValidationErrorMessage> getCityErrors() {
    if (isAnswered() && getCityValue().isEmpty()) {
      return getCityErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getCityErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED));
  }

  public ImmutableSet<ValidationErrorMessage> getStateErrors() {
    // TODO: Validate state further.
    if (isAnswered() && getStateValue().isEmpty()) {
      return getStateErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStateErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED));
  }

  public ImmutableSet<ValidationErrorMessage> getZipErrors() {
    if (isAnswered()) {
      Optional<String> zipValue = getZipValue();
      if (zipValue.isEmpty()) {
        return ImmutableSet.of(
            ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_ZIPCODE_REQUIRED));
      }

      Pattern pattern = Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$");
      Matcher matcher = pattern.matcher(zipValue.get());
      if (!matcher.matches()) {
        return getZipErrorMessage();
      }
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getZipErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_INVALID_ZIPCODE));
  }

  public Optional<String> getStreetValue() {
    if (streetValue != null) {
      return streetValue;
    }

    streetValue = applicantQuestion.getApplicantData().readString(getStreetPath());
    return streetValue;
  }

  public Optional<String> getLine2Value() {
    if (line2Value != null) {
      return line2Value;
    }

    line2Value = applicantQuestion.getApplicantData().readString(getLine2Path());
    return line2Value;
  }

  public Optional<String> getCityValue() {
    if (cityValue != null) {
      return cityValue;
    }

    cityValue = applicantQuestion.getApplicantData().readString(getCityPath());
    return cityValue;
  }

  public Optional<String> getStateValue() {
    if (stateValue != null) {
      return stateValue;
    }

    stateValue = applicantQuestion.getApplicantData().readString(getStatePath());
    return stateValue;
  }

  public Optional<String> getZipValue() {
    if (zipValue != null) {
      return zipValue;
    }

    zipValue = applicantQuestion.getApplicantData().readString(getZipPath());
    return zipValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.ADDRESS)) {
      throw new RuntimeException(
          String.format(
              "Question is not an ADDRESS question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public AddressQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (AddressQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getStreetPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.STREET);
  }

  public Path getLine2Path() {
    return applicantQuestion.getContextualizedPath().join(Scalar.LINE2);
  }

  public Path getCityPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.CITY);
  }

  public Path getStatePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.STATE);
  }

  public Path getZipPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.ZIP);
  }

  private boolean isStreetAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getStreetPath());
  }

  private boolean isLine2Answered() {
    return applicantQuestion.getApplicantData().hasPath(getLine2Path());
  }

  private boolean isCityAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getCityPath());
  }

  private boolean isStateAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getStatePath());
  }

  private boolean isZipAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getZipPath());
  }

  /** Returns true if any field is answered. Returns false if all are not. */
  @Override
  public boolean isAnswered() {
    return isStreetAnswered()
        || isLine2Answered()
        || isCityAnswered()
        || isStateAnswered()
        || isZipAnswered();
  }

  @Override
  public String getAnswerString() {
    String displayLine1 = getStreetValue().orElse("");
    String displayLine2 = getLine2Value().orElse("");

    String cityDisplayString = getCityValue().isPresent() ? getCityValue().get() + "," : "";
    String stateDisplayString = getStateValue().orElse("");
    String displayLine3 =
        stateDisplayString.isEmpty()
            ? String.format("%s %s", cityDisplayString, getZipValue().orElse("")).trim()
            : String.format(
                    "%s %s %s", cityDisplayString, stateDisplayString, getZipValue().orElse(""))
                .trim();

    return ImmutableList.of(displayLine1, displayLine2, displayLine3).stream()
        .filter(line -> line.length() > 0)
        .collect(Collectors.joining("\n"));
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(
        getStreetPath(), getLine2Path(), getCityPath(), getStatePath(), getZipPath());
  }
}

package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
public class AddressQuestion extends QuestionImpl {
  private static final String PO_BOX_REGEX =
      "(?i)(.*(P(OST|.)?\\s*((O(FF(ICE)?)?)?.?\\s*(B(IN|OX|.?)))+)).*";

  private Optional<String> streetValue;
  private Optional<String> line2Value;
  private Optional<String> cityValue;
  private Optional<String> stateValue;
  private Optional<String> zipValue;

  public AddressQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.ADDRESS);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.<Path, ImmutableSet<ValidationErrorMessage>>builder()
        .put(applicantQuestion.getContextualizedPath(), getQuestionErrors())
        .put(getStreetPath(), validateStreet())
        .put(getLine2Path(), validateAddress())
        .put(getCityPath(), validateCity())
        .put(getStatePath(), validateState())
        .put(getZipPath(), validateZipCode())
        .build();
  }

  private ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
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

  private ImmutableSet<ValidationErrorMessage> validateAddress() {
    // TODO: Implement address validation.
    return ImmutableSet.of();
  }

  private ImmutableSet<ValidationErrorMessage> validateStreet() {
    if (getStreetValue().isEmpty()) {
      return getStreetErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStreetErrorMessage() {
    return ImmutableSet.of(ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STREET_REQUIRED));
  }

  private ImmutableSet<ValidationErrorMessage> validateCity() {
    if (getCityValue().isEmpty()) {
      return getCityErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getCityErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_CITY_REQUIRED));
  }

  private ImmutableSet<ValidationErrorMessage> validateState() {
    // TODO: Validate state further.
    if (getStateValue().isEmpty()) {
      return getStateErrorMessage();
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<ValidationErrorMessage> getStateErrorMessage() {
    return ImmutableSet.of(
        ValidationErrorMessage.create(MessageKey.ADDRESS_VALIDATION_STATE_REQUIRED));
  }

  private ImmutableSet<ValidationErrorMessage> validateZipCode() {
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

  public AddressQuestionDefinition getQuestionDefinition() {
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

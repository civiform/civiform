package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.i18n.Messages;
import services.Path;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionType;

public class AddressQuestion implements PresentsErrors {
  private static final String PO_BOX_REGEX =
      "(?i)(.*(P(OST|.)?\\s*((O(FF(ICE)?)?)?.?\\s*(B(IN|OX|.?)))+)).*";

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> streetValue;
  private Optional<String> cityValue;
  private Optional<String> stateValue;
  private Optional<String> zipValue;

  public AddressQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors(Messages messages) {
    return !getQuestionErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getQuestionErrors(Messages messages) {
    if (!isAnswered()) {
      return ImmutableSet.of();
    }

    AddressQuestionDefinition definition = getQuestionDefinition();
    ImmutableSet.Builder<String> errors = ImmutableSet.builder();

    if (definition.getDisallowPoBox() && getStreetValue().isPresent()) {
      Pattern poBoxPattern = Pattern.compile(PO_BOX_REGEX);
      Matcher poBoxMatcher = poBoxPattern.matcher(getStreetValue().get());

      if (poBoxMatcher.matches()) {
        return ImmutableSet.of(messages.at("validation.noPoBox"));
      }
    }

    return errors.build();
  }

  @Override
  public boolean hasTypeSpecificErrors(Messages messages) {
    return !getAllTypeSpecificErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getAllTypeSpecificErrors(Messages messages) {
    return ImmutableSet.<String>builder()
        .addAll(getAddressErrors())
        .addAll(getStreetErrors(messages))
        .addAll(getCityErrors(messages))
        .addAll(getStateErrors(messages))
        .addAll(getZipErrors(messages))
        .build();
  }

  public ImmutableSet<String> getAddressErrors() {
    // TODO: Implement address validation.
    return ImmutableSet.of();
  }

  public ImmutableSet<String> getStreetErrors(Messages messages) {
    if (isStreetAnswered() && getStreetValue().isEmpty()) {
      return ImmutableSet.of(messages.at("validation.streetRequired"));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<String> getCityErrors(Messages messages) {
    if (isCityAnswered() && getCityValue().isEmpty()) {
      return ImmutableSet.of(messages.at("validation.cityRequired"));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<String> getStateErrors(Messages messages) {
    // TODO: Validate state further.
    if (isStateAnswered() && getStateValue().isEmpty()) {
      return ImmutableSet.of(messages.at("validation.streetRequired"));
    }

    return ImmutableSet.of();
  }

  public ImmutableSet<String> getZipErrors(Messages messages) {
    if (isZipAnswered()) {
      Optional<String> zipValue = getZipValue();
      if (zipValue.isEmpty()) {
        return ImmutableSet.of(messages.at("validation.zipcodeRequired"));
      }

      Pattern pattern = Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$");
      Matcher matcher = pattern.matcher(zipValue.get());
      if (!matcher.matches()) {
        return ImmutableSet.of(messages.at("validation.invalidZipcode"));
      }
    }

    return ImmutableSet.of();
  }

  public Optional<String> getStreetValue() {
    if (streetValue != null) {
      return streetValue;
    }

    streetValue = applicantQuestion.getApplicantData().readString(getStreetPath());
    return streetValue;
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

  private boolean isCityAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getCityPath());
  }

  private boolean isStateAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getStatePath());
  }

  private boolean isZipAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getZipPath());
  }

  /**
   * Returns true if any one of the address fields is answered. Returns false if all are not
   * answered.
   */
  @Override
  public boolean isAnswered() {
    return isStreetAnswered() || isCityAnswered() || isStateAnswered() || isZipAnswered();
  }
}

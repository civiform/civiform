package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import models.QuestionDisplayMode;
import models.QuestionModel;
import models.QuestionTag;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.UrlUtils;
import services.export.CsvExporterService;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionOption;
import services.question.QuestionSetting;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;

/** Superclass for all forms updating a question. */
public abstract class QuestionForm {
  public static final String REDIRECT_URL_PARAM = "redirectUrl";

  private String questionName;
  private String questionDescription;
  private Optional<Long> enumeratorId;
  private String questionText;
  private String questionHelpText;
  private Optional<String> questionExportState;
  private QuestionDefinition qd;
  private String redirectUrl;
  private boolean isUniversal;
  private UUID concurrencyToken;
  private QuestionDisplayMode displayMode;
  private ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags;

  protected QuestionForm() {
    questionName = "";
    questionDescription = "";
    enumeratorId = Optional.empty();
    questionText = "";
    questionHelpText = "";
    questionExportState = Optional.of("");
    redirectUrl = "";
    isUniversal = false;
    // If we don't get a token from the client, generate one so any updates fail.
    concurrencyToken = UUID.randomUUID();
    primaryApplicantInfoTags = ImmutableSet.of();
    displayMode = QuestionDisplayMode.VISIBLE;
  }

  protected QuestionForm(QuestionDefinition qd) {
    this.qd = qd;
    questionExportState = Optional.empty();
    questionName = qd.getName();
    questionDescription = qd.getDescription();
    enumeratorId = qd.getEnumeratorId();
    redirectUrl = "";

    try {
      questionText = qd.getQuestionText().get(LocalizedStrings.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      questionText = "Missing Text";
    }

    try {
      questionHelpText = qd.getQuestionHelpText().get(LocalizedStrings.DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      questionHelpText = "Missing Text";
    }

    isUniversal = qd.isUniversal();
    concurrencyToken = qd.getConcurrencyToken().orElse(UUID.randomUUID());
    primaryApplicantInfoTags = qd.getPrimaryApplicantInfoTags();
    displayMode = qd.getDisplayMode();
  }

  public final String getQuestionName() {
    return questionName;
  }

  public final void setQuestionName(String questionName) {
    this.questionName = checkNotNull(questionName);
  }

  public final String getQuestionDescription() {
    return questionDescription;
  }

  public final void setQuestionDescription(String questionDescription) {
    this.questionDescription = checkNotNull(questionDescription);
  }

  public final Optional<Long> getEnumeratorId() {
    return enumeratorId;
  }

  public final String getConcurrencyToken() {
    return concurrencyToken.toString();
  }

  public final void setEnumeratorId(String enumeratorId) {
    this.enumeratorId =
        enumeratorId.isEmpty() ? Optional.empty() : Optional.of(Long.valueOf(enumeratorId));
  }

  public final void setConcurrencyToken(UUID concurrencyToken) {
    this.concurrencyToken = concurrencyToken;
  }

  public abstract QuestionType getQuestionType();

  public final String getQuestionText() {
    return questionText;
  }

  public final void setQuestionText(String questionText) {
    this.questionText = checkNotNull(questionText);
  }

  public final String getQuestionHelpText() {
    return questionHelpText;
  }

  public final void setQuestionHelpText(String questionHelpText) {
    this.questionHelpText = checkNotNull(questionHelpText);
  }

  public final QuestionDisplayMode getDisplayMode() {
    return displayMode;
  }

  public final void setDisplayMode(QuestionDisplayMode displayMode) {
    this.displayMode = displayMode;
  }

  public final String getRedirectUrl() {
    // Adding validation to prevent a non-absolute URL from being returned by the form.
    // There are potentially many ways for the private variable to be set (constructor,
    // reflection, etc) and the bad value being consumed is what we care to guard against.
    if (redirectUrl == null) {
      return "";
    }
    // Only allow relative URLs to ensure that we redirect to the same domain.
    return UrlUtils.checkIsRelativeUrl(redirectUrl);
  }

  public final void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = checkNotNull(redirectUrl);
  }

  /**
   * @deprecated Use {@link #getConfigBuilder()} with {@link QuestionDefinition#create} instead.
   */
  @Deprecated
  public QuestionDefinitionBuilder getBuilder() {
    LocalizedStrings questionTextMap =
        questionText.isEmpty()
            ? LocalizedStrings.of()
            : LocalizedStrings.of(Locale.US, questionText);
    LocalizedStrings questionHelpTextMap =
        questionHelpText.isEmpty()
            ? LocalizedStrings.empty()
            : LocalizedStrings.of(Locale.US, questionHelpText);

    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setQuestionType(getQuestionType())
            .setName(questionName)
            .setDescription(questionDescription)
            .setEnumeratorId(enumeratorId)
            .setQuestionText(questionTextMap)
            .setQuestionHelpText(questionHelpTextMap)
            .setUniversal(isUniversal)
            .setConcurrencyToken(concurrencyToken)
            .setDisplayMode(displayMode)
            .setPrimaryApplicantInfoTags(primaryApplicantInfoTags);

    ImmutableList<QuestionOption> questionOptions = getQuestionOptions();
    if (questionOptions != null && !questionOptions.isEmpty()) {
      builder.setQuestionOptions(questionOptions);
    }

    LocalizedStrings entityType = getEntityTypeLocalizedStrings();
    if (entityType != null) {
      builder.setEntityType(entityType);
    }

    ImmutableSet<QuestionSetting> questionSettings = getQuestionSettings();
    if (questionSettings != null && !questionSettings.isEmpty()) {
      builder.setQuestionSettings(questionSettings);
    }

    QuestionDefinition.ValidationPredicates predicates = getValidationPredicates();
    if (predicates != null) {
      builder.setValidationPredicates(predicates);
    }

    return builder;
  }

  public QuestionDefinitionConfig.Builder getConfigBuilder() {
    LocalizedStrings questionTextMap =
        questionText.isEmpty()
            ? LocalizedStrings.of()
            : LocalizedStrings.of(Locale.US, questionText);
    LocalizedStrings questionHelpTextMap =
        questionHelpText.isEmpty()
            ? LocalizedStrings.empty()
            : LocalizedStrings.of(Locale.US, questionHelpText);

    QuestionDefinitionConfig.Builder configBuilder =
        QuestionDefinitionConfig.builder()
            .setName(questionName)
            .setDescription(questionDescription)
            .setQuestionText(questionTextMap)
            .setEnumeratorId(enumeratorId)
            .setQuestionHelpText(questionHelpTextMap)
            .setUniversal(isUniversal)
            .setConcurrencyToken(concurrencyToken)
            .setDisplayMode(displayMode)
            .setPrimaryApplicantInfoTags(primaryApplicantInfoTags);

    ImmutableSet<QuestionSetting> questionSettings = getQuestionSettings();
    if (questionSettings != null && !questionSettings.isEmpty()) {
      configBuilder.setQuestionSettings(questionSettings);
    }

    QuestionDefinition.ValidationPredicates predicates = getValidationPredicates();
    if (predicates != null) {
      configBuilder.setValidationPredicates(predicates);
    }

    return configBuilder;
  }

  /** Returns the validation predicates. Subclasses should override if they have validation. */
  public QuestionDefinition.ValidationPredicates getValidationPredicates() {
    return null;
  }

  /** Returns the validation predicates as a serialized string. */
  public String getValidationPredicatesString() {
    QuestionDefinition.ValidationPredicates predicates = getValidationPredicates();
    return predicates == null ? "" : predicates.serializeAsString();
  }

  /** Returns the question options. Subclasses should override for multi-option questions. */
  public ImmutableList<QuestionOption> getQuestionOptions() {
    return ImmutableList.of();
  }

  /** Returns the entity type. Subclasses should override for enumerator questions. */
  public LocalizedStrings getEntityTypeLocalizedStrings() {
    return null;
  }

  /** Returns the question settings. Subclasses should override if they have settings. */
  public ImmutableSet<QuestionSetting> getQuestionSettings() {
    return null;
  }

  public final void setQuestionExportState(String questionExportState) {
    this.questionExportState = Optional.of(questionExportState);
  }

  private void populateQuestionExportStateFromTags(List<QuestionTag> questionTags) {
    if (questionTags.contains(QuestionTag.DEMOGRAPHIC)) {
      questionExportState = Optional.of(QuestionTag.DEMOGRAPHIC.getValue());
    } else if (questionTags.contains(QuestionTag.DEMOGRAPHIC_PII)) {
      questionExportState = Optional.of(QuestionTag.DEMOGRAPHIC_PII.getValue());
    } else {
      questionExportState = Optional.of(QuestionTag.NON_DEMOGRAPHIC.getValue());
    }
  }

  /**
   * The {@link QuestionTag} to use for export state. Callers external to this class should use this
   * rather than {@link getQuestionExportState}.
   */
  public final QuestionTag getQuestionExportStateTag() {
    String rawState = getQuestionExportState();
    return rawState.isEmpty() ? QuestionTag.NON_DEMOGRAPHIC : QuestionTag.valueOf(rawState);
  }

  /**
   * The string representation of export state. This is only public since the Play framework
   * requires public getters and setters. Callers external to this class should prefer {@link
   * getQuestionExportStateTag}.
   */
  public final String getQuestionExportState() {
    if (CsvExporterService.NON_EXPORTED_QUESTION_TYPES.contains(getQuestionType())) {
      return QuestionTag.NON_DEMOGRAPHIC.getValue();
    }

    if (questionExportState.isEmpty()) {
      QuestionModel q = new QuestionModel(this.qd);
      q.refresh();
      populateQuestionExportStateFromTags(q.getQuestionTags());
    }
    return questionExportState.get();
  }

  public final boolean isUniversal() {
    return this.isUniversal;
  }

  /**
   * The name of this function must be setIsUniversal in order to match the field name in the view
   * so that automatic binding of the form field to the QuestionForm data works correctly.
   *
   * @param universal Whether the question is marked as universal or not.
   */
  public final void setIsUniversal(boolean universal) {
    this.isUniversal = universal;
  }

  public final ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags() {
    return this.primaryApplicantInfoTags;
  }

  /**
   * The name of this function must be in the form of set<field name> and match the fieldName
   * parameter of PrimaryApplicantInfoTag.APPLICANT_DOB in order for automatic binding of the form
   * field to the QuestionForm data to work correctly.
   *
   * @param val When true, add the tag. When false, remove the tag.
   */
  public final void setPrimaryApplicantDob(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_DOB, val);
  }

  /**
   * The name of this function must be in the form of set<field name> and match the fieldName
   * parameter of PrimaryApplicantInfoTag.APPLICANT_EMAIL in order for automatic binding of the form
   * field to the QuestionForm data to work correctly.
   *
   * @param val When true, add the tag. When false, remove the tag.
   */
  public final void setPrimaryApplicantEmail(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_EMAIL, val);
  }

  /**
   * The name of this function must be in the form of set<field name> and match the fieldName
   * parameter of PrimaryApplicantInfoTag.APPLICANT_NAME in order for automatic binding of the form
   * field to the QuestionForm data to work correctly.
   *
   * @param val When true, add the tag. When false, remove the tag.
   */
  public final void setPrimaryApplicantName(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_NAME, val);
  }

  /**
   * The name of this function must be in the form of set<field name> and match the fieldName
   * parameter of PrimaryApplicantInfoTag.APPLICANT_PHONE in order for automatic binding of the form
   * field to the QuestionForm data to work correctly.
   *
   * @param val When true, add the tag. When false, remove the tag.
   */
  public final void setPrimaryApplicantPhone(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_PHONE, val);
  }

  private void setTagState(PrimaryApplicantInfoTag primaryApplicantInfoTag, boolean isSet) {
    boolean currentlyContainsTag = this.primaryApplicantInfoTags.contains(primaryApplicantInfoTag);
    if (isSet && !currentlyContainsTag) {
      this.primaryApplicantInfoTags =
          new ImmutableSet.Builder<PrimaryApplicantInfoTag>()
              .addAll(this.primaryApplicantInfoTags)
              .add(primaryApplicantInfoTag)
              .build();
    } else if (!isSet && currentlyContainsTag) {
      this.primaryApplicantInfoTags =
          this.primaryApplicantInfoTags().stream()
              .filter(t -> t != primaryApplicantInfoTag)
              .collect(ImmutableSet.toImmutableSet());
    }
  }
}

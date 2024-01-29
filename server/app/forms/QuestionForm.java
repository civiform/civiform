package forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.QuestionModel;
import models.QuestionTag;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.UrlUtils;
import services.export.CsvExporterService;
import services.question.PrimaryApplicantInfoTag;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
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
    primaryApplicantInfoTags = ImmutableSet.of();
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
    primaryApplicantInfoTags = qd.getPrimaryApplicantInfoTags();
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

  public final void setEnumeratorId(String enumeratorId) {
    this.enumeratorId =
        enumeratorId.isEmpty() ? Optional.empty() : Optional.of(Long.valueOf(enumeratorId));
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
            .setPrimaryApplicantInfoTags(primaryApplicantInfoTags);
    return builder;
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

  // Awkwardly named, but must match the field name
  public final void setIsUniversal(boolean universal) {
    this.isUniversal = universal;
  }

  public final ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags() {
    return this.primaryApplicantInfoTags;
  }

  // The names of these functions must match up to the fieldName value in the
  // PrimaryApplicantInfoTag
  public final void setPrimaryApplicantDob(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_DOB, val);
  }

  public final void setPrimaryApplicantEmail(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_EMAIL, val);
  }

  public final void setPrimaryApplicantName(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_NAME, val);
  }

  public final void setPrimaryApplicantPhone(boolean val) {
    setTagState(PrimaryApplicantInfoTag.APPLICANT_PHONE, val);
  }

  private void setTagState(PrimaryApplicantInfoTag primaryApplicantInfoTag, boolean isSet) {
    boolean present = this.primaryApplicantInfoTags.contains(primaryApplicantInfoTag);
    if (isSet) {
      this.primaryApplicantInfoTags =
          present
              ? this.primaryApplicantInfoTags
              : new ImmutableSet.Builder<PrimaryApplicantInfoTag>()
                  .addAll(this.primaryApplicantInfoTags)
                  .add(primaryApplicantInfoTag)
                  .build();
    } else {
      this.primaryApplicantInfoTags =
          present
              ? this.primaryApplicantInfoTags().stream()
                  .filter(t -> t != primaryApplicantInfoTag)
                  .collect(ImmutableSet.toImmutableSet())
              : this.primaryApplicantInfoTags;
    }
  }
}

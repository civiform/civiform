package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.DivTag;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DateConverter;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.RepeatedEntity;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.ApplicationBaseView;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.TextFormatter;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Shows all questions in the applying program and answers to the questions if present. */
public final class ApplicantProgramSummaryView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final DateConverter dateConverter;
  private final SettingsManifest settingsManifest;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantProgramSummaryView(
      ApplicantLayout layout,
      DateConverter dateConverter,
      SettingsManifest settingsManifest,
      ApplicantRoutes applicantRoutes) {
    this.layout = checkNotNull(layout);
    this.dateConverter = checkNotNull(dateConverter);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  /**
   * Renders a summary of all the applicant's data for a specific application.
   *
   * <p>Data includes:
   *
   * <ul>
   *   <li>Program Id
   *   <li>Applicant Id - Needed for link context (submit & edit)
   *   <li>Question Data for each question:
   *       <ul>
   *         <li>question text
   *         <li>answer text
   *         <li>block id (for edit link)
   *       </ul>
   * </ul>
   */
  public Content render(Params params) {
    Messages messages = params.messages();
    HtmlBundle bundle = layout.getBundle(params.request());

    if (params.loginPromptModal().isPresent()) {
      bundle.addModals(params.loginPromptModal().get());
    }

    DivTag applicationSummary = div().withId("application-summary").withClasses("mb-8");
    Optional<RepeatedEntity> previousRepeatedEntity = Optional.empty();
    for (AnswerData answerData : params.summaryData()) {
      Optional<RepeatedEntity> currentRepeatedEntity = answerData.repeatedEntity();
      if (!currentRepeatedEntity.equals(previousRepeatedEntity)
          && currentRepeatedEntity.isPresent()) {
        applicationSummary.with(renderRepeatedEntitySection(currentRepeatedEntity.get(), messages));
      }
      applicationSummary.with(
          renderQuestionSummary(answerData, messages, params.applicantId(), params.profile()));
      previousRepeatedEntity = currentRepeatedEntity;
    }

    // Add submit action (POST).
    String submitLink =
        applicantRoutes.submit(params.profile(), params.applicantId(), params.programId()).url();

    ContainerTag continueOrSubmitButton;
    if (params.completedBlockCount() == params.totalBlockCount()) {
      continueOrSubmitButton =
          submitButton(messages.at(MessageKey.BUTTON_SUBMIT.getKeyName()))
              .withClasses(ReferenceClasses.SUBMIT_BUTTON, ButtonStyles.SOLID_BLUE, "mx-auto");
    } else {
      String applyUrl =
          applicantRoutes.edit(params.profile(), params.applicantId(), params.programId()).url();
      continueOrSubmitButton =
          a().withHref(applyUrl)
              .withText(messages.at(MessageKey.BUTTON_CONTINUE.getKeyName()))
              .withId("continue-application-button")
              .withClasses(ReferenceClasses.CONTINUE_BUTTON, ButtonStyles.SOLID_BLUE, "mx-auto");
    }

    DivTag content =
        div()
            .with(applicationSummary)
            .with(
                form()
                    .withClasses(ReferenceClasses.DEBOUNCED_FORM)
                    .withAction(submitLink)
                    .withMethod(Http.HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(params.request()))
                    .with(continueOrSubmitButton));

    params.bannerMessages().stream()
        .forEach(message -> message.ifPresent(bundle::addToastMessages));
    params
        .request()
        .flash()
        .get("error")
        .map(msg -> ToastMessage.error(msg, messages))
        .ifPresent(bundle::addToastMessages);

    String pageTitle =
        settingsManifest.getIntakeFormEnabled(params.request())
                && params.programType().equals(ProgramType.COMMON_INTAKE_FORM)
            ? messages.at(MessageKey.TITLE_COMMON_INTAKE_SUMMARY.getKeyName())
            : messages.at(MessageKey.TITLE_PROGRAM_SUMMARY.getKeyName());
    bundle.setTitle(String.format("%s — %s", pageTitle, params.programTitle()));
    Optional<DivTag> maybeBackToAdminViewButton =
        layout.maybeRenderBackToAdminViewButton(params.request(), params.programId());
    if (maybeBackToAdminViewButton.isPresent()) {
      bundle.addMainContent(maybeBackToAdminViewButton.get());
    }
    bundle.addMainContent(
        layout.renderProgramApplicationTitleAndProgressIndicator(
            params.programTitle(),
            params.completedBlockCount(),
            params.totalBlockCount(),
            true,
            messages),
        h2(pageTitle).withClasses(ApplicantStyles.PROGRAM_APPLICATION_TITLE),
        ApplicationBaseView.requiredFieldsExplanationContent(messages),
        content);
    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    return layout.renderWithNav(
        params.request(),
        params.applicantPersonalInfo(),
        params.messages(),
        bundle,
        params.applicantId());
  }

  /** Renders {@code data} including the question and any existing answer to it. */
  private DivTag renderQuestionSummary(
      AnswerData data, Messages messages, long applicantId, CiviFormProfile profile) {
    DivTag questionContent =
        div(div()
                .with(
                    TextFormatter.formatTextWithAriaLabel(
                        data.questionText(),
                        /* preserveEmptyLines */ true,
                        !data.applicantQuestion().isOptional(),
                        messages
                            .at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName())
                            .toLowerCase(Locale.ROOT)))
                .withClasses("font-semibold"))
            .withClasses("pr-2");

    // When applicant info is pre-populated by TI entry, the question is not
    // considered "answered" but we want the answers to show on the review screen
    String defaultAnswerString = data.applicantQuestion().getQuestion().getDefaultAnswerString();
    boolean haveAnswerText =
        !data.answerText().isBlank() && !data.answerText().equals(defaultAnswerString);

    if (data.isAnswered() || haveAnswerText) {
      final ContainerTag answerContent;
      if (data.encodedFileKey().isPresent()) {
        String encodedFileKey = data.encodedFileKey().get();
        String fileLink = controllers.routes.FileController.show(applicantId, encodedFileKey).url();
        answerContent = a().withHref(fileLink);
      } else {
        answerContent = div();
      }
      answerContent.withClasses("font-light", "text-sm");
      // Add answer text, converting newlines to <br/> tags.
      String[] texts = data.answerText().split("\n");
      texts = Arrays.stream(texts).filter(text -> text.length() > 0).toArray(String[]::new);
      for (int i = 0; i < texts.length; i++) {
        if (i > 0) {
          answerContent.with(br());
        }
        answerContent.withText(texts[i]);
      }
      questionContent.with(answerContent);
    }

    DivTag actionAndTimestampDiv = div().withClasses("pr-2", "flex", "flex-col", "text-right");
    // Show timestamp if answered elsewhere.
    if (data.isAnswered()) {
      LocalDate date = this.dateConverter.renderLocalDate(data.timestamp());
      // TODO(#4003): Translate this text.
      DivTag timestampContent =
          div(messages.at(MessageKey.CONTENT_PREVIOUSLY_ANSWERED_ON.getKeyName(), date))
              .withClasses(
                  ReferenceClasses.BT_DATE,
                  ReferenceClasses.APPLICANT_QUESTION_PREVIOUSLY_ANSWERED,
                  "font-light",
                  "text-xs",
                  "flex-grow");
      actionAndTimestampDiv.with(timestampContent);
    }

    // Show that the question makes the application ineligible if it is answered and is a reason the
    // application is ineligible.
    if (data.eligibilityIsGating() && !data.isEligible() && data.isAnswered()) {
      actionAndTimestampDiv.with(
          div(messages.at(MessageKey.CONTENT_DOES_NOT_QUALIFY.getKeyName()))
              .withClasses(
                  "text-m",
                  "font-medium",
                  "flex-grow",
                  "py-1",
                  ReferenceClasses.APPLICANT_NOT_ELIGIBLE_TEXT));
    }

    LinkElement editElement =
        new LinkElement()
            .setStyles("bottom-0", "right-0", "text-blue-600", StyleUtils.hover("text-blue-700"));
  
    QuestionDefinition questionDefinition = data.questionDefinition();
    Optional<String> questionName = Optional.of(questionDefinition.getName());
    if (data.isAnswered() || haveAnswerText) {
      editElement
          .setHref(
              applicantRoutes
                  .blockReview(profile, applicantId, data.programId(), data.blockId(), questionName)
                  .url())
          .setText(messages.at(MessageKey.LINK_EDIT.getKeyName()))
          .setIcon(Icons.EDIT, LinkElement.IconPosition.START);
    } else {
      editElement
          .setHref(
              applicantRoutes
                  .blockEdit(profile, applicantId, data.programId(), data.blockId(), questionName)
                  .url())
          .setText(messages.at(MessageKey.LINK_ANSWER.getKeyName()))
          .setIcon(Icons.ARROW_FORWARD, LinkElement.IconPosition.END);
    }
    DivTag editContent =
        div()
            .with(
                editElement
                    .asAnchorText()
                    .attr(
                        "aria-label",
                        data.isAnswered()
                            ? messages.at(
                                MessageKey.ARIA_LABEL_EDIT.getKeyName(), data.questionText())
                            : messages.at(
                                MessageKey.ARIA_LABEL_ANSWER.getKeyName(), data.questionText())))
            .withClasses(
                "font-medium", "break-normal", "flex", "flex-grow", "justify-end", "items-center");

    actionAndTimestampDiv.with(editContent);

    return div(questionContent, actionAndTimestampDiv)
        .withClasses(
            ReferenceClasses.APPLICANT_SUMMARY_ROW,
            marginIndentClass(data.repeatedEntity().map(RepeatedEntity::depth).orElse(0)),
            data.isAnswered() ? "" : "bg-amber-50",
            "my-0",
            "p-2",
            "pt-4",
            "border-b",
            "border-gray-300",
            "flex",
            "justify-between")
        .withStyle("word-break:break-word");
  }

  private DivTag renderRepeatedEntitySection(RepeatedEntity repeatedEntity, Messages messages) {
    String content =
        String.format(
            "%s: %s",
            repeatedEntity
                .enumeratorQuestionDefinition()
                .getEntityType()
                .getOrDefault(messages.lang().toLocale()),
            repeatedEntity.entityName());
    return div(content)
        .withClasses(
            marginIndentClass(repeatedEntity.depth() - 1),
            "my-2",
            "py-2",
            "pl-4",
            "flex-auto",
            "bg-gray-200",
            "font-semibold",
            "rounded-lg");
  }

  private String marginIndentClass(int depth) {
    return "ml-" + (depth * 4);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_ApplicantProgramSummaryView_Params.Builder();
    }

    abstract Http.Request request();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract ImmutableList<Optional<ToastMessage>> bannerMessages();

    abstract int completedBlockCount();

    abstract Messages messages();

    abstract long programId();

    abstract String programTitle();

    abstract ProgramType programType();

    abstract ImmutableList<AnswerData> summaryData();

    abstract int totalBlockCount();

    abstract Optional<Modal> loginPromptModal();

    abstract CiviFormProfile profile();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Http.Request request);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

      public abstract Builder setBannerMessages(ImmutableList<Optional<ToastMessage>> banners);

      public abstract Builder setCompletedBlockCount(int completedBlockCount);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setProgramType(ProgramType programType);

      public abstract Builder setSummaryData(ImmutableList<AnswerData> summaryData);

      public abstract Builder setTotalBlockCount(int totalBlockCount);

      public abstract Builder setLoginPromptModal(Modal loginPromptModal);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Params build();
    }
  }
}

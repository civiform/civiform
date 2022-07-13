package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.RepeatedEntity;
import views.ApplicantUtils;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Shows all questions in the applying program and answers to the questions if present. */
public final class ApplicantProgramSummaryView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantProgramSummaryView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /**
   * Renders a summary of all of the applicant's data for a specific application. Data includes:
   *
   * <p>Program Id, Applicant Id - Needed for link context (submit & edit)
   *
   * <p>Question Data for each question:
   *
   * <ul>
   *   <li>question text
   *   <li>answer text
   *   <li>block id (for edit link)
   * </ul>
   */
  public Content render(Params params) {
    Messages messages = params.messages();
    String pageTitle =
        params.inReview()
            ? messages.at(MessageKey.TITLE_PROGRAM_REVIEW.getKeyName())
            : messages.at(MessageKey.TITLE_PROGRAM_PREVIEW.getKeyName());
    HtmlBundle bundle =
        layout.getBundle().setTitle(String.format("%s — %s", pageTitle, params.programTitle()));

    DivTag applicationSummary = div().withId("application-summary").withClasses(Styles.MB_8);
    Optional<RepeatedEntity> previousRepeatedEntity = Optional.empty();
    boolean isFirstUnanswered = true;
    for (AnswerData answerData : params.summaryData()) {
      Optional<RepeatedEntity> currentRepeatedEntity = answerData.repeatedEntity();
      if (!currentRepeatedEntity.equals(previousRepeatedEntity)
          && currentRepeatedEntity.isPresent()) {
        applicationSummary.with(renderRepeatedEntitySection(currentRepeatedEntity.get(), messages));
      }
      applicationSummary.with(
          renderQuestionSummary(
              answerData, messages, params.applicantId(), params.inReview(), isFirstUnanswered));
      isFirstUnanswered = isFirstUnanswered && answerData.isAnswered();
      previousRepeatedEntity = currentRepeatedEntity;
    }

    // Add submit action (POST).
    String submitLink =
        routes.ApplicantProgramReviewController.submit(params.applicantId(), params.programId())
            .url();

    ContainerTag continueOrSubmitButton;
    if (params.completedBlockCount() == params.totalBlockCount()) {
      continueOrSubmitButton =
          submitButton(messages.at(MessageKey.BUTTON_SUBMIT.getKeyName()))
              .withClasses(
                  ReferenceClasses.SUBMIT_BUTTON, ApplicantStyles.BUTTON_SUBMIT_APPLICATION);
    } else {
      String applyUrl =
          routes.ApplicantProgramsController.edit(params.applicantId(), params.programId()).url();
      continueOrSubmitButton =
          a().withHref(applyUrl)
              .withText(messages.at(MessageKey.BUTTON_CONTINUE.getKeyName()))
              .withId("continue-application-button")
              .withClasses(
                  ReferenceClasses.CONTINUE_BUTTON, ApplicantStyles.BUTTON_SUBMIT_APPLICATION);
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

    if (!params.banner().isEmpty()) {
      bundle.addToastMessages(ToastMessage.error(params.banner()));
    }

    bundle.addMainContent(
        layout.renderProgramApplicationTitleAndProgressIndicator(
            params.programTitle(), params.completedBlockCount(), params.totalBlockCount(), true),
        h1(pageTitle).withClasses(ApplicantStyles.H1_PROGRAM_APPLICATION),
        content);
    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    return layout.renderWithNav(
        params.request(), params.applicantName(), params.messages(), bundle);
  }

  private DivTag renderQuestionSummary(
      AnswerData data,
      Messages messages,
      long applicantId,
      boolean inReview,
      boolean isFirstUnanswered) {
    DivTag questionPrompt = div(data.questionText()).withClasses(Styles.FONT_SEMIBOLD);
    DivTag questionContent = div(questionPrompt).withClasses(Styles.PR_2);

    // Add existing answer.
    if (data.isAnswered()) {
      final ContainerTag answerContent;
      if (data.fileKey().isPresent()) {
        String encodedFileKey = URLEncoder.encode(data.fileKey().get(), StandardCharsets.UTF_8);
        String fileLink = controllers.routes.FileController.show(applicantId, encodedFileKey).url();
        answerContent = a().withHref(fileLink);
      } else {
        answerContent = div();
      }
      answerContent.withClasses(Styles.FONT_LIGHT, Styles.TEXT_SM);
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

    DivTag actionAndTimestampDiv =
        div().withClasses(Styles.PR_2, Styles.FLEX, Styles.FLEX_COL, Styles.TEXT_RIGHT);
    // Show timestamp if answered elsewhere.
    if (data.isPreviousResponse()) {
      LocalDate date =
          Instant.ofEpochMilli(data.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
      DivTag timestampContent =
          div("Previously answered on " + date)
              .withClasses(Styles.FONT_LIGHT, Styles.TEXT_XS, Styles.FLEX_GROW);
      actionAndTimestampDiv.with(timestampContent);
    }

    // Maybe link to block containing specific question.
    if (data.isAnswered() || isFirstUnanswered) {
      String editText = messages.at(MessageKey.LINK_EDIT.getKeyName());
      if (!data.isAnswered()) {
        editText =
            inReview
                ? messages.at(MessageKey.BUTTON_CONTINUE.getKeyName())
                : messages.at(MessageKey.LINK_BEGIN.getKeyName());
      }
      String editLink =
          (!data.isAnswered() && !inReview)
              ? routes.ApplicantProgramBlocksController.edit(
                      applicantId, data.programId(), data.blockId())
                  .url()
              : routes.ApplicantProgramBlocksController.review(
                      applicantId, data.programId(), data.blockId())
                  .url();

      ATag editAction =
          new LinkElement()
              .setHref(editLink)
              .setText(editText)
              .setStyles(
                  Styles.BOTTOM_0,
                  Styles.RIGHT_0,
                  Styles.TEXT_BLUE_600,
                  StyleUtils.hover(Styles.TEXT_BLUE_700))
              .asAnchorText()
              .attr(
                  "aria-label",
                  messages.at(MessageKey.ARIA_LABEL_EDIT.getKeyName(), data.questionText()));
      DivTag editContent =
          div(editAction)
              .withClasses(
                  Styles.FONT_MEDIUM,
                  Styles.BREAK_NORMAL,
                  Styles.FLEX,
                  Styles.FLEX_GROW,
                  Styles.JUSTIFY_END,
                  Styles.ITEMS_CENTER);

      actionAndTimestampDiv.with(editContent);
    }

    return div(questionContent, actionAndTimestampDiv)
        .withClasses(
            ReferenceClasses.APPLICANT_SUMMARY_ROW,
            marginIndentClass(data.repeatedEntity().map(RepeatedEntity::depth).orElse(0)),
            data.isAnswered() ? "" : Styles.BG_YELLOW_50,
            Styles.MY_0,
            Styles.P_2,
            Styles.PT_4,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            Styles.FLEX,
            Styles.JUSTIFY_BETWEEN)
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
            Styles.MY_2,
            Styles.PY_2,
            Styles.PL_4,
            Styles.FLEX_AUTO,
            Styles.BG_GRAY_200,
            Styles.FONT_SEMIBOLD,
            Styles.ROUNDED_LG);
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

    abstract Optional<String> applicantName();

    abstract String banner();

    abstract int completedBlockCount();

    abstract boolean inReview();

    abstract Messages messages();

    abstract long programId();

    abstract String programTitle();

    abstract ImmutableList<AnswerData> summaryData();

    abstract int totalBlockCount();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Http.Request request);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantName(Optional<String> applicantName);

      public abstract Builder setBanner(String banner);

      public abstract Builder setCompletedBlockCount(int completedBlockCount);

      public abstract Builder setInReview(boolean inReview);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setSummaryData(ImmutableList<AnswerData> summaryData);

      public abstract Builder setTotalBlockCount(int totalBlockCount);

      abstract Optional<String> applicantName();

      abstract Messages messages();

      abstract Params autoBuild();

      public final Params build() {
        setApplicantName(Optional.of(ApplicantUtils.getApplicantName(applicantName(), messages())));
        return autoBuild();
      }
    }
  }
}

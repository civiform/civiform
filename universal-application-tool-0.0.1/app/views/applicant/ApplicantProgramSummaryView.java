package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.RepeatedEntity;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

public final class ApplicantProgramSummaryView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantProgramSummaryView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /**
   * Renders a summary of all of the applicant's data for a specific application. Data includes:
   * Program Id, Applicant Id - Needed for link context (submit & edit) Question Data for each
   * question: - question text - answer text - block id (for edit link)
   */
  public Content render(
      Request request,
      Long applicantId,
      String userName,
      Long programId,
      String programTitle,
      ImmutableList<AnswerData> data,
      int completedBlockCount,
      int totalBlockCount,
      Messages messages,
      Optional<String> banner) {
    String pageTitle = "Application summary";
    HtmlBundle bundle =
        layout.getBundle().setTitle(String.format("%s — %s", pageTitle, programTitle));

    ContainerTag applicationSummary = div().withId("application-summary").withClasses(Styles.MB_8);
    Optional<RepeatedEntity> previousRepeatedEntity = Optional.empty();
    for (AnswerData answerData : data) {
      Optional<RepeatedEntity> currentRepeatedEntity = answerData.repeatedEntity();
      if (!currentRepeatedEntity.equals(previousRepeatedEntity)
          && currentRepeatedEntity.isPresent()) {
        applicationSummary.with(renderRepeatedEntitySection(currentRepeatedEntity.get(), messages));
      }
      applicationSummary.with(renderQuestionSummary(answerData, applicantId));
      previousRepeatedEntity = currentRepeatedEntity;
    }

    // Add submit action (POST).
    String submitLink =
        routes.ApplicantProgramReviewController.submit(applicantId, programId).url();

    Tag continueOrSubmitButton;
    if (completedBlockCount == totalBlockCount) {
      continueOrSubmitButton =
          submitButton(messages.at(MessageKey.BUTTON_SUBMIT.getKeyName()))
              .withClasses(
                  ReferenceClasses.SUBMIT_BUTTON, ApplicantStyles.BUTTON_SUBMIT_APPLICATION);
    } else {
      String applyUrl = routes.ApplicantProgramsController.edit(applicantId, programId).url();
      continueOrSubmitButton =
          a().attr(HREF, applyUrl)
              .withText(messages.at(MessageKey.BUTTON_CONTINUE.getKeyName()))
              .withId("continue-application-button")
              .withClasses(ReferenceClasses.CONTINUE_BUTTON, ApplicantStyles.BUTTON_PROGRAM_APPLY);
    }

    ContainerTag content =
        div()
            .with(applicationSummary)
            .with(
                form()
                    .withAction(submitLink)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(continueOrSubmitButton));

    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.error(banner.get()));
    }
    bundle.addMainContent(
        layout.renderProgramApplicationTitleAndProgressIndicator(
            programTitle, completedBlockCount, totalBlockCount, true),
        h1(pageTitle).withClasses(ApplicantStyles.H1_PROGRAM_APPLICATION),
        content);
    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private ContainerTag renderQuestionSummary(AnswerData data, Long applicantId) {
    ContainerTag questionPrompt =
        div(data.questionText()).withClasses(Styles.FLEX_AUTO, Styles.FONT_SEMIBOLD);
    ContainerTag questionContent =
        div(questionPrompt).withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.PR_2);

    // Show timestamp if answered elsewhere.
    if (data.isPreviousResponse()) {
      LocalDate date =
          Instant.ofEpochMilli(data.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
      ContainerTag timestampContent =
          div("Previously answered on " + date)
              .withClasses(Styles.FLEX_AUTO, Styles.TEXT_RIGHT, Styles.FONT_LIGHT, Styles.TEXT_XS);
      questionContent.with(timestampContent);
    }

    // Add answer text, converting newlines to <br/> tags.
    ContainerTag answerContent =
        div().withClasses(Styles.FLEX_AUTO, Styles.TEXT_LEFT, Styles.FONT_LIGHT, Styles.TEXT_SM);
    String[] texts = data.answerText().split("\n");
    texts = Arrays.stream(texts).filter(text -> text.length() > 0).toArray(String[]::new);
    for (int i = 0; i < texts.length; i++) {
      if (i > 0) {
        answerContent.with(br());
      }
      answerContent.withText(texts[i]);
    }

    // Link to block containing specific question.
    String editLink =
        routes.ApplicantProgramBlocksController.review(
                applicantId, data.programId(), data.blockId())
            .url();
    ContainerTag editAction =
        new LinkElement()
            .setHref(editLink)
            .setText("Edit")
            .setStyles(Styles.ABSOLUTE, Styles.BOTTOM_0, Styles.RIGHT_0)
            .asAnchorText();
    ContainerTag editContent =
        div(editAction)
            .withClasses(
                Styles.FLEX_AUTO,
                Styles.TEXT_RIGHT,
                Styles.FONT_LIGHT,
                Styles.ITALIC,
                Styles.RELATIVE);
    ContainerTag answerDiv =
        div(answerContent, editContent).withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.PR_2);

    return div(questionContent, answerDiv)
        .withClasses(
            marginIndentClass(data.repeatedEntity().map(RepeatedEntity::depth).orElse(0)),
            Styles.MY_2,
            Styles.PY_2,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            ReferenceClasses.APPLICANT_SUMMARY_ROW);
  }

  private ContainerTag renderRepeatedEntitySection(
      RepeatedEntity repeatedEntity, Messages messages) {
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
            Styles.BG_ORANGE_200,
            Styles.FONT_SEMIBOLD,
            Styles.ROUNDED_LG);
  }

  private String marginIndentClass(int depth) {
    return "ml-" + (depth * 4);
  }
}

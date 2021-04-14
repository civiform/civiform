package views.admin.questions;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.p;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import play.twirl.api.Content;
import services.question.exceptions.TranslationNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.Icons;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public final class QuestionsListView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionsListView(AdminLayout layout) {
    this.layout = layout;
  }

  /** Renders a page with a table view of all questions. */
  public Content render(ImmutableList<QuestionDefinition> questions, Optional<String> maybeFlash) {

    ContainerTag bodyContent =
        body(
            renderHeader("All Questions"),
            div(renderQuestionTable(
                    questions.stream()
                        .collect(new GroupByKeyCollector<>(QuestionDefinition::getName))))
                .withClasses(Styles.M_4),
            renderAddQuestionLink(),
            renderSummary(questions));

    if (maybeFlash.isPresent()) {
      bodyContent.with(
          ToastMessage.alert(maybeFlash.get()).setDismissible(false).getContainerTag());
    }

    return layout.render(bodyContent);
  }

  private Tag renderAddQuestionLink() {
    String parentId = "create-question-button";
    String dropdownId = parentId + "-dropdown";
    ContainerTag linkButton =
        new LinkElement().setId(parentId).setText("Create new question").asButton();
    ContainerTag dropdown =
        div()
            .withId(dropdownId)
            .withClasses(
                Styles.BORDER,
                Styles.BG_WHITE,
                Styles.TEXT_GRAY_600,
                Styles.SHADOW_LG,
                Styles.ABSOLUTE,
                Styles.MT_3,
                Styles.HIDDEN);

    for (QuestionType type : QuestionType.values()) {
      String typeString = type.toString().toLowerCase();
      String link = controllers.admin.routes.QuestionController.newOne(typeString).url();
      ContainerTag linkTag =
          a().withHref(link)
              .withId(String.format("create-%s-question", typeString))
              .withClasses(
                  Styles.BLOCK,
                  Styles.P_4,
                  Styles.BG_WHITE,
                  Styles.TEXT_GRAY_600,
                  StyleUtils.hover(Styles.BG_GRAY_100, Styles.TEXT_GRAY_800))
              .with(
                  Icons.questionTypeSvg(type, 24)
                      .withClasses(
                          Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6, Styles.MR_1, Styles.TEXT_SM))
              .with(
                  p(typeString)
                      .withClasses(
                          Styles.ML_2,
                          Styles.MR_4,
                          Styles.INLINE,
                          Styles.TEXT_SM,
                          Styles.UPPERCASE));
      dropdown.with(linkTag);
    }
    return linkButton.with(dropdown);
  }

  private Tag renderSummary(ImmutableList<QuestionDefinition> questions) {
    return div("Total Questions: " + questions.size())
        .withClasses(Styles.FLOAT_RIGHT, Styles.TEXT_BASE, Styles.PX_4, Styles.MY_2);
  }

  /** Renders the full table. */
  private Tag renderQuestionTable(
      ImmutableMap<String, ImmutableList<QuestionDefinition>> questions) {
    return table()
        .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
        .with(renderQuestionTableHeaderRow())
        .with(
            tbody(
                each(
                    questions,
                    (questionName, questionList) -> renderQuestionTableRow(questionList))));
  }

  /** Render the question table header row. */
  private Tag renderQuestionTableHeaderRow() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5))
            .with(th("Question text").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_2_5))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES,
                        Styles.TEXT_RIGHT,
                        Styles.PR_8,
                        Styles.W_1_5)));
  }

  /** Display this as a table row with all fields. */
  private Tag renderQuestionTableRow(ImmutableList<QuestionDefinition> questionVersions) {
    ImmutableMap<String, ImmutableList<QuestionDefinition>> questionsByLifecycle =
        questionVersions.stream()
            .collect(
                new GroupByKeyCollector<QuestionDefinition>(
                    questionDefinition -> questionDefinition.getLifecycleStage().getValue()));
    Optional<QuestionDefinition> activeDefinition =
        questionsByLifecycle
            .getOrDefault(LifecycleStage.ACTIVE.getValue(), ImmutableList.of())
            .stream()
            .findAny();
    Optional<QuestionDefinition> draftDefinition =
        questionsByLifecycle
            .getOrDefault(LifecycleStage.DRAFT.getValue(), ImmutableList.of())
            .stream()
            .findAny();
    QuestionDefinition definition;
    // Find the main definition to display information from.  Prefer the latest draft.  If there
    // is no draft, choose an active one if exists.  If there are neither, choose any.  There will
    // be at least one or we wouldn't have gotten here!
    if (draftDefinition.isPresent()) {
      definition = draftDefinition.get();
    } else if (activeDefinition.isPresent()) {
      definition = activeDefinition.get();
    } else {
      definition = questionVersions.stream().findAny().get();
    }
    return tr().withClasses(
            Styles.BORDER_B, Styles.BORDER_GRAY_300, StyleUtils.even(Styles.BG_GRAY_100))
        .with(renderInfoCell(definition))
        .with(renderQuestionTextCell(definition))
        .with(renderActionsCell(activeDefinition, draftDefinition, definition));
  }

  private Tag renderInfoCell(QuestionDefinition definition) {
    return td().with(div(definition.getName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(definition.getDescription()).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private Tag renderQuestionTextCell(QuestionDefinition definition) {
    String questionText = "";
    String questionHelpText = "";

    try {
      questionText = definition.getQuestionText(Locale.US);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    try {
      questionHelpText = definition.getQuestionHelpText(Locale.US);
    } catch (TranslationNotFoundException e) { // Ignore. Leaving blank
    }

    return td().with(div(questionText).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(questionHelpText).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private Tag renderQuestionEditLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.QuestionController.edit(definition.getId()).url();
    return new LinkElement()
        .setId("edit-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }

  private Tag renderQuestionViewLink(QuestionDefinition definition, String linkText) {
    String link = controllers.admin.routes.QuestionController.show(definition.getId()).url();
    return new LinkElement()
        .setId("view-question-link-" + definition.getId())
        .setHref(link)
        .setText(linkText)
        .setStyles(Styles.MR_2)
        .asAnchorText();
  }

  private Tag renderActionsCell(
      Optional<QuestionDefinition> active,
      Optional<QuestionDefinition> draft,
      QuestionDefinition definition) {
    ContainerTag td = td().withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.TEXT_RIGHT);
    if (active.isPresent() && draft.isEmpty()) {
      td.with(renderQuestionViewLink(active.get(), "View →"));
      td.with(renderQuestionEditLink(active.get(), "New Version →"));
    } else if (active.isEmpty() && draft.isPresent()) {
      td.with(renderQuestionEditLink(draft.get(), "Edit Draft →"));
    } else if (active.isPresent() && draft.isPresent()) {
      td.with(renderQuestionViewLink(active.get(), "View Published →"));
      td.with(renderQuestionEditLink(draft.get(), "Edit Draft →"));
    } else if (active.isEmpty() && draft.isEmpty()) {
      td.with(renderQuestionViewLink(definition, "View →"));
    }
    return td;
  }
}

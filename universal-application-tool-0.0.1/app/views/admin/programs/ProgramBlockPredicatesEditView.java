package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.wbr;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.Icons;
import views.style.StyleUtils;
import views.style.Styles;

public class ProgramBlockPredicatesEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramBlockPredicatesEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> potentialPredicateQuestions) {
    String title = String.format("%s: Add a condition to show or hide this block", blockDefinition.name());
    Tag csrfTag = makeCsrfTokenInputTag(request);
    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                form().with(csrfTag),
                layout.renderProgramInfo(programDefinition),
                h1().withText(title).withClasses(Styles.MY_4, Styles.FONT_BOLD, Styles.TEXT_XL),
                renderQuestionDefinitions(potentialPredicateQuestions));

    return layout.renderCentered(htmlBundle);
  }

  private ContainerTag renderQuestionDefinitions(
      ImmutableList<QuestionDefinition> questionDefinitions) {
    return div()
        .withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.GAP_2)
        .with(
            each(
                questionDefinitions,
                questionDefinition -> renderQuestionDefinition(questionDefinition)));
  }

  private ContainerTag renderQuestionDefinition(QuestionDefinition questionDefinition) {
    return div()
        .withClasses(
            Styles.FLEX,
            Styles.FLEX_ROW,
            Styles.GAP_4,
            Styles.PX_4,
            Styles.PY_2,
            Styles.BORDER,
            Styles.BORDER_GRAY_200,
            StyleUtils.hover(Styles.TEXT_GRAY_800, Styles.BG_GRAY_100))
        .with(
            Icons.questionTypeSvg(questionDefinition.getQuestionType(), 24)
                .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6))
        .with(
            div()
                .withClasses()
                .with(
                    div(questionDefinition.getName()),
                    div(questionDefinition.getDescription())
                        .withClasses(Styles.MT_1, Styles.TEXT_SM)));
  }
}

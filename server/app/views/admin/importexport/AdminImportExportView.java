package views.admin.importexport;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;

public final class AdminImportExportView extends BaseHtmlView {
  // TODO: Do the same as ProgramImageDescriptionForm
  private static final String PROGRAMS_FIELD_NAME = "programIds[]";

  private final AdminLayout layout;

  @Inject
  public AdminImportExportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT_EXPORT);
  }

  public Content render(
      Http.Request request,
      ImmutableList<ProgramDefinition> activePrograms,
      ImmutableList<QuestionDefinition> activeQuestions) {
    String title = "Import and export questions and programs";
    DivTag contentDiv = div().with(h1(title));

    contentDiv.with(programSelection(request, activePrograms));
    contentDiv.with(questionSelection(activeQuestions));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent programSelection(
      Http.Request request, ImmutableList<ProgramDefinition> activePrograms) {
    DivTag mainDiv = div();
    mainDiv.with(h2("Programs"));

    AdminProgramExportForm form = new AdminProgramExportForm(activePrograms);

    FieldsetTag fields = fieldset();

    for (int i = 0; i < form.getProgramIds().size(); i++) {
      System.out.println(
          "program " + form.getProgramNames().get(i) + "  is ID=" + form.getProgramIds().get(i));
      fields.with(
          FieldWithLabel.checkbox()
              .setFieldName(PROGRAMS_FIELD_NAME)
              .setLabelText(form.getProgramNames().get(i))
              .setValue(String.valueOf(form.getProgramIds().get(i)))
              .getCheckboxTag());
    }

    return mainDiv.with(
        form()
            .withMethod("GET")
            .withAction(routes.AdminImportExportController.exportPrograms().url())
            .with(makeCsrfTokenInputTag(request), fields)
            .with(submitButton("Export programs").withClass(ButtonStyles.SOLID_BLUE)));
  }

  private DomContent questionSelection(ImmutableList<QuestionDefinition> activeQuestions) {
    DivTag mainDiv = div();
    mainDiv.with(h2("Questions"));
    mainDiv.with(
        each(
            activeQuestions,
            question -> {
              System.out.println(
                  "question "
                      + question.getQuestionText().getDefault()
                      + "  has ID="
                      + question.getId());
              return p(question.getQuestionText().getDefault());
            }));
    return mainDiv;
  }
}

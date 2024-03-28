package views.admin.importexport;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.AdminImportExportController;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.fileupload.FileUploadViewStrategy;

public class AdminImportView extends BaseHtmlView {
  private final AdminLayout layout;
  private final MessagesApi messagesApi;

  @Inject
  public AdminImportView(AdminLayoutFactory layoutFactory, MessagesApi messagesApi) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT);
    this.messagesApi = messagesApi;
  }

  public Content render(
      Http.Request request, Optional<AdminImportExportController.JsonExportingClass> dataToImport) {
    String title = "Import programs";
    DivTag contentDiv = div().with(h1(title));

    contentDiv.with(importProgram(request));

    if (dataToImport.isPresent()) {
      contentDiv.with(h2("Programs:"));
      contentDiv.with(p(dataToImport.get().getPrograms().toString()));
      contentDiv.with(h2("Questions:"));
      List<String> questions = dataToImport.get().getQuestions().stream().map(QuestionDefinition::getConfig).map(QuestionDefinitionConfig::toString).collect(Collectors.toList());
      contentDiv.with(each(questions, question -> p(question)));
    } else {
      contentDiv.with(p("Nothing imported yet"));
    }

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent importProgram(Http.Request request) {
    DivTag fileUploadElement =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            "fake-id",
            Http.MimeTypes.JSON,
            ImmutableList.of(),
            /* disabled= */ false,
            /* fileLimitMb= */ 5,
            messagesApi.preferred(request));
    return div()
        .with(h2("Import program"))
        .with(
            form()
                .withEnctype("multipart/form-data")
                .withMethod("POST")
                .with(makeCsrfTokenInputTag(request), fileUploadElement)
                .with(submitButton("Import content"))
                .withAction(routes.AdminImportExportController.importPrograms().url()));
  }
}

package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import forms.QuestionForm;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import views.admin.questions.QuestionEditView;
import views.admin.questions.QuestionsListView;

public class QuestionController extends Controller {

  private final QuestionService service;
  private final QuestionsListView listView;
  private final QuestionEditView editView;
  private final FormFactory formFactory;

  @Inject
  public QuestionController(
      QuestionService service,
      QuestionsListView listView,
      QuestionEditView editView,
      FormFactory formFactory) {
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
  }

  public CompletionStage<Result> list(String renderAs) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              return ok(listView.render(readOnlyService.getAllQuestions(), renderAs));
            });
  }

  public CompletionStage<Result> write(Request request) {
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                QuestionDefinition definition =
                    questionForm
                        .getBuilder()
                        .setId(readOnlyService.getNextId())
                        .setVersion(1L)
                        .build();
                service.create(definition);
              } catch (Exception e) {
                // UnsupportedQuestionTypeException
                return redirect("/admin/questions");
              }
              return redirect("/admin/questions");
            });
  }

  public Result create(Request request) {
    return ok(editView.render(request, Optional.empty()));
  }

  public CompletionStage<Result> edit(Request request, String path) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              Optional<QuestionDefinition> definition = Optional.empty();
              try {
                definition = Optional.of(readOnlyService.getQuestionDefinition(path));
              } catch (Exception e) {
                // If the path doesn't exist, redirect to create.
                return redirect("/admin/questions/new");
              }
              return ok(editView.render(request, definition));
            });
  }
}

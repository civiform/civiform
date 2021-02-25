package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import forms.QuestionForm;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.question.InvalidPathException;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.UnsupportedQuestionTypeException;
import views.admin.questions.QuestionEditView;
import views.admin.questions.QuestionsListView;

public class QuestionController extends Controller {
  final Logger LOG = LoggerFactory.getLogger(this.getClass());

  private final QuestionService service;
  private final QuestionsListView listView;
  private final QuestionEditView editView;
  private final FormFactory formFactory;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public QuestionController(
      QuestionService service,
      QuestionsListView listView,
      QuestionEditView editView,
      FormFactory formFactory,
      HttpExecutionContext httpExecutionContext) {
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  public CompletionStage<Result> create(Request request) {
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                QuestionDefinition definition = questionForm.getBuilder().setVersion(1L).build();
                service.create(definition);
              } catch (UnsupportedQuestionTypeException e) {
                // I'm not sure why this would happen here, so we'll just log and redirect.
                LOG.info(e.toString());
              }
              return redirect(routes.QuestionController.index("table"));
            },
            httpExecutionContext.current());
  }

  public CompletionStage<Result> edit(Request request, String path) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              Optional<QuestionDefinition> definition = Optional.empty();
              try {
                definition = Optional.of(readOnlyService.getQuestionDefinition(path));
              } catch (InvalidPathException e) { // If the path doesn't exist, redirect to create.
                LOG.info(e.toString());
                return redirect(routes.QuestionController.create());
              }
              return ok(editView.render(request, definition));
            },
            httpExecutionContext.current());
  }

  public Result newOne(Request request) {
    return ok(editView.render(request, Optional.empty()));
  }

  public CompletionStage<Result> index(String renderAs) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              switch (renderAs) {
                case "list":
                  return ok(listView.renderAsList(readOnlyService.getAllQuestions()));
                case "table":
                  return ok(listView.renderAsTable(readOnlyService.getAllQuestions()));
                default:
                  return badRequest();
              }
            },
            httpExecutionContext.current());
  }

  // TODO: Implement update question.
  // https://github.com/seattle-uat/universal-application-tool/issues/103
  public CompletionStage<Result> update(Request request, Long id) {
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    try {
      QuestionDefinition definition = questionForm.getBuilder().setId(0L).setVersion(1L).build();
      service.update(definition);
    } catch (UnsupportedQuestionTypeException e) {
      // I'm not sure why this would happen here, so we'll just log and redirect.
      LOG.info(e.toString());
    } catch (UnsupportedOperationException e) {
      // This is expected for now until we implement update on QuestionService.
    }
    return CompletableFuture.completedFuture(redirect(routes.QuestionController.index("table")));
  }
}

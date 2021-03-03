package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import forms.QuestionForm;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.question.InvalidPathException;
import services.question.InvalidUpdateException;
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

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> create(Request request) {
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              String exception = "";
              try {
                QuestionDefinition definition = questionForm.getBuilder().setVersion(1L).build();
                boolean success = service.create(definition).isPresent();
                if (!success) {
                  exception =
                      String.format(
                          "create failed: this is most likely you specify an invalid path %s",
                          definition.getPath());
                }
              } catch (UnsupportedQuestionTypeException e) {
                exception = e.toString();
                LOG.info(exception);
              }
              return withException(redirect(routes.QuestionController.index("table")), exception);
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> edit(Request request, String path) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                QuestionDefinition definition = readOnlyService.getQuestionDefinition(path);
                return ok(editView.renderEditQuestionForm(request, definition));
              } catch (InvalidPathException e) { // If the path doesn't exist, redirect to newOne.
                LOG.info(e.toString());
                return redirect(routes.QuestionController.newOne());
              }
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result newOne(Request request) {
    return ok(editView.renderNewQuestionForm(request));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> index(Request request, String renderAs) {
    Optional<String> maybeFlash = request.flash().get("exception");
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              switch (renderAs) {
                case "list":
                  return ok(listView.renderAsList(readOnlyService.getAllQuestions(), maybeFlash));
                case "table":
                  return ok(listView.renderAsTable(readOnlyService.getAllQuestions(), maybeFlash));
                default:
                  return badRequest();
              }
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> update(Request request, Long id) {
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    String exception = "";
    try {
      QuestionDefinition definition = questionForm.getBuilder().setId(id).setVersion(1L).build();
      service.update(definition);
    } catch (UnsupportedQuestionTypeException e) {
      exception = e.toString();
      LOG.info(exception);
    } catch (InvalidUpdateException e) {
      exception = e.toString();
      LOG.info(exception);
    }
    return CompletableFuture.completedFuture(
        withException(redirect(routes.QuestionController.index("table")), exception));
  }

  private Result withException(Result result, String exception) {
    if (!exception.isEmpty()) {
      return result.flashing("exception", exception);
    }
    return result;
  }
}

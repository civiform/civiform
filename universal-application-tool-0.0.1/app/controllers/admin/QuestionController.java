package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import forms.QuestionForm;
import java.util.Optional;
import java.util.StringJoiner;
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
import services.ErrorAnd;
import services.question.InvalidQuestionTypeException;
import services.question.InvalidUpdateException;
import services.question.QuestionDefinition;
import services.question.QuestionNotFoundException;
import services.question.QuestionService;
import services.question.QuestionServiceError;
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
              try {
                QuestionDefinition definition = questionForm.getBuilder().setVersion(1L).build();
                ErrorAnd<QuestionDefinition, QuestionServiceError> result =
                    service.create(definition);
                if (result.isError()) {
                  StringJoiner messageJoiner = new StringJoiner(". ", "", ".");
                  for (QuestionServiceError e : result.getErrors()) {
                    messageJoiner.add(e.message());
                  }
                  String errorMessage = messageJoiner.toString();
                  return ok(editView.renderNewQuestionForm(request, questionForm, errorMessage));
                }
              } catch (UnsupportedQuestionTypeException e) {
                // These are valid question types, but are not fully supported yet.
                String errorMessage = e.toString();
                return ok(editView.renderNewQuestionForm(request, questionForm, errorMessage));
              } catch (InvalidQuestionTypeException e) {
                // These are unrecognized invalid question types.
                return badRequest(e.toString());
              }
              String successMessage =
                  String.format("question %s created", questionForm.getQuestionPath().path());
              return withMessage(
                  redirect(routes.QuestionController.index("table")), successMessage);
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> edit(Request request, Long id) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                QuestionDefinition definition = readOnlyService.getQuestionDefinition(id);
                return ok(editView.renderEditQuestionForm(request, definition));
              } catch (QuestionNotFoundException e) {
                return badRequest(e.toString());
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
    Optional<String> maybeFlash = request.flash().get("message");
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
                  return badRequest(
                      String.format(
                          "unrecognized renderAs: '%s', accepted values include 'list' and 'table'",
                          renderAs));
              }
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, Long id) {
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    try {
      QuestionDefinition definition = questionForm.getBuilder().build();
      ErrorAnd<QuestionDefinition, QuestionServiceError> result = service.update(definition);
      if (result.isError()) {
        StringJoiner messageJoiner = new StringJoiner(". ", "", ".");
        for (QuestionServiceError e : result.getErrors()) {
          messageJoiner.add(e.message());
        }
        String errorMessage = messageJoiner.toString();
        return ok(editView.renderEditQuestionForm(request, questionForm, errorMessage));
      }
    } catch (UnsupportedQuestionTypeException e) {
      // These are valid question types, but are not fully supported yet.
      String errorMessage = e.toString();
      return ok(editView.renderEditQuestionForm(request, questionForm, errorMessage));
    } catch (InvalidQuestionTypeException e) {
      // These are unrecognized invalid question types.
      return badRequest(e.toString());
    } catch (InvalidUpdateException e) {
      // Ill-formed update request
      return badRequest(e.toString());
    }
    String successMessage = String.format("question %s updated", questionForm.getQuestionPath().path());
    return withMessage(redirect(routes.QuestionController.index("table")), successMessage);
  }

  private Result withMessage(Result result, String message) {
    if (!message.isEmpty()) {
      return result.flashing("message", message);
    }
    return result;
  }
}

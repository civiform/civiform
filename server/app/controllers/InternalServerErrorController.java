package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import views.errors.InternalServerError;

/** Controller for handling server errors. */
public class InternalServerErrorController extends Controller {
  private final Provider<InternalServerError> internalServerErrorPageProvider;
  private final MessagesApi messagesApi;

  @Inject
  public InternalServerErrorController(
      Provider<InternalServerError> internalServerErrorPageProvider, MessagesApi messagesApi) {
    this.internalServerErrorPageProvider = checkNotNull(internalServerErrorPageProvider);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public CompletionStage<Result> index(Http.Request request, String exceptionId) {
    return CompletableFuture.completedFuture(
        Results.internalServerError(
            internalServerErrorPageProvider
                .get()
                .render(request, messagesApi.preferred(request), exceptionId)));
  }
}

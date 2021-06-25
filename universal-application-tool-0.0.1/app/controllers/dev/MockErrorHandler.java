package controllers.dev;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import com.google.inject.Inject;
import play.i18n.MessagesApi;

import views.errors.NotFound;

public class MockErrorHandler extends Controller {
  private final NotFound notFoundPage;
  private final MessagesApi messagesApi;

  @Inject
  public MockErrorHandler(NotFound notFoundPage, MessagesApi messagesApi) {
    this.notFoundPage=notFoundPage;
    this.messagesApi=messagesApi;
  }

  public Result notFound(Http.Request request) {
    return ok(notFoundPage.render(request, messagesApi.preferred(request))); 
  }
}

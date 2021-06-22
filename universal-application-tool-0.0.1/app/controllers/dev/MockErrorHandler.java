package controllers.dev;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import com.google.inject.Inject;

import views.errors.NotFound;

public class MockErrorHandler extends Controller {
  private final NotFound notFoundPage;

  @Inject
  public MockErrorHandler(NotFound notFoundPage) {
    this.notFoundPage=notFoundPage;
  }

  public Result notFound(Http.Request request) {
    return ok(notFoundPage.render()); 
  }
}

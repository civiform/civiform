package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.support.UnsupportedBrowserPageView;

public final class SupportController extends Controller {
  private final UnsupportedBrowserPageView unsupportedBrowserPageView;

  @Inject
  public SupportController(UnsupportedBrowserPageView unsupportedBrowserPageView) {
    this.unsupportedBrowserPageView = checkNotNull(unsupportedBrowserPageView);
  }

  public Result handleUnsupportedBrowser(Http.Request request) {
    return ok(unsupportedBrowserPageView.render(request, new UnsupportedBrowserPageView.Model()))
        .as(Http.MimeTypes.HTML);
  }
}

package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.support.UnsupportedBrowserView;

public final class SupportController extends Controller {
  private final UnsupportedBrowserView unsupportedBrowserView;

  @Inject
  public SupportController(UnsupportedBrowserView unsupportedBrowserView) {
    this.unsupportedBrowserView = checkNotNull(unsupportedBrowserView);
  }

  public Result handleUnsupportedBrowser(Http.Request request) {
    return ok(unsupportedBrowserView.render(request));
  }
}

package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.support.UnconfirmedIdcsEmailBugView;
import views.support.UnsupportedBrowserView;

public class SupportController extends Controller {
  private static final Logger logger = LoggerFactory.getLogger(SupportController.class);

  private final UnconfirmedIdcsEmailBugView unconfirmedIdcsEmailBugView;
  private final UnsupportedBrowserView unsupportedBrowserView;

  @Inject
  public SupportController(
      UnconfirmedIdcsEmailBugView unconfirmedIdcsEmailBugView,
      UnsupportedBrowserView unsupportedBrowserView) {
    this.unconfirmedIdcsEmailBugView = checkNotNull(unconfirmedIdcsEmailBugView);
    this.unsupportedBrowserView = checkNotNull(unsupportedBrowserView);
  }

  public Result handleUnconfirmedIdcsEmail(Http.Request request) {
    logger.info("UnconfirmedIdcsEmail-Support-Page: " + request.remoteAddress());

    return ok(unconfirmedIdcsEmailBugView.render());
  }

  public Result handleUnsupportedBrowser() {

    return ok(unsupportedBrowserView.render());
  }
}

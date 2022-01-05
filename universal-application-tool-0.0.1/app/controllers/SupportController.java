package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.support.UnconfirmedIdcsEmailBugView;

public class SupportController extends Controller {
  private static Logger LOG = LoggerFactory.getLogger(SupportController.class);

  private final UnconfirmedIdcsEmailBugView unconfirmedIdcsEmailBugView;

  @Inject
  public SupportController(UnconfirmedIdcsEmailBugView unconfirmedIdcsEmailBugView) {
    this.unconfirmedIdcsEmailBugView = checkNotNull(unconfirmedIdcsEmailBugView);
  }

  public Result handleUnconfirmedIdcsEmail(Http.Request request) {
    LOG.info("UnconfirmedIdcsEmail-Support-Page: " + request.remoteAddress());

    return ok(unconfirmedIdcsEmailBugView.render());
  }
}

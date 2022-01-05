package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.support.UnconfirmedIdcsEmailBugView;

public class SupportController extends Controller {
  private final UnconfirmedIdcsEmailBugView unconfirmedIdcsEmailBugView;

  @Inject
  public SupportController(UnconfirmedIdcsEmailBugView unconfirmedIdcsEmailBugView) {
    this.unconfirmedIdcsEmailBugView = checkNotNull(unconfirmedIdcsEmailBugView);
  }

  public Result handleUnconfirmedIdcsEmail(Http.Request request) {
    // TODO: log info about the user? maybe IP address along with a grep-able identifier to see how
    // many different
    //       IP addresses reach this endpoint?
    return ok(unconfirmedIdcsEmailBugView.render());
  }
}

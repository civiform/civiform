package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;
import views.J2HtmlDemo;

public class J2HtmlDemoController extends Controller {

  private final J2HtmlDemo view;

  @Inject
  public J2HtmlDemoController(J2HtmlDemo view) {
    this.view = checkNotNull(view);
  }

  public Result index() {
    return ok(view.render("Let's get started!"));
  }
}

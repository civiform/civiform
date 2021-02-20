package controllers;

import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.play.java.Secure;
import play.mvc.*;
import views.LoginForm;
import views.html.*;

/** This controller contains an action to handle HTTP requests to the application's home page. */
public class HomeController extends Controller {

  private final AssetsFinder assetsFinder;
  private final Config config;
  private final LoginForm form;

  @Inject
  public HomeController(AssetsFinder assetsFinder, Config config, LoginForm form) {
    this.assetsFinder = assetsFinder;
    this.config = config;
    this.form = form;
  }

  /**
   * An action that renders an HTML page with a welcome message. The configuration in the <code>
   * routes</code> file means that this method will be called when the application receives a <code>
   * GET</code> request with a path of <code>/</code>.
   */
  public Result index() {
    return ok(index.render("Your new application is ready.", assetsFinder));
  }

  public Result loginForm(Http.Request request, Optional<String> message)
      throws TechnicalException {
    return ok(this.form.render(request, message));
  }

  @Secure
  public Result secureIndex() {
    return ok(index.render("You are logged in.", assetsFinder));
  }
}

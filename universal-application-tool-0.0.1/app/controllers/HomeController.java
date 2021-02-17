package controllers;

import javax.inject.Inject;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.play.java.Secure;
import play.mvc.*;
import views.html.*;

/** This controller contains an action to handle HTTP requests to the application's home page. */
public class HomeController extends Controller {

  private final AssetsFinder assetsFinder;
  private final Config config;

  @Inject
  public HomeController(AssetsFinder assetsFinder, Config config) {
    this.assetsFinder = assetsFinder;
    this.config = config;
  }

  /**
   * An action that renders an HTML page with a welcome message. The configuration in the <code>
   * routes</code> file means that this method will be called when the application receives a <code>
   * GET</code> request with a path of <code>/</code>.
   */
  public Result index() {
    return ok(index.render("Your new application is ready.", assetsFinder));
  }

  public Result loginForm(Http.Request request) throws TechnicalException {
    FormClient formClient = (FormClient) config.getClients().findClient("FormClient").get();
    return ok(views.html.loginForm.render(formClient.getCallbackUrl(), request));
  }

  @Secure
  public Result secureIndex() {
    return ok(index.render("You are logged in.", assetsFinder));
  }
}

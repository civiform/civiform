package controllers.admin.tools;

import auth.Authorizers;
import com.google.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.admin.tools.UrlCheckerCommand;
import views.admin.tools.UrlCheckerView;
import views.admin.tools.UrlCheckerViewModel;

@Slf4j
public class AdminToolsController extends Controller {
  private final FormFactory formFactory;
  private final WSClient wsClient;
  private final UrlCheckerView urlCheckerView;

  @Inject
  public AdminToolsController(
      FormFactory formFactory, WSClient wsClient, UrlCheckerView urlCheckerView) {
    this.formFactory = formFactory;
    this.wsClient = wsClient;
    this.urlCheckerView = urlCheckerView;
  }

  /** Show url checker form */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result urlTester(Http.Request request) {
    return ok(urlCheckerView.render(request, new UrlCheckerViewModel())).as(Http.MimeTypes.HTML);
  }

  /** Checks if a url is valid and reachable and returns info on the status */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxTestUrl(Http.Request request) {
    var form = formFactory.form(UrlCheckerCommand.class).bindFromRequest(request);

    if (form.hasErrors()) {
      var msg =
          form.errors().stream().map(ValidationError::message).collect(Collectors.joining("<br/>"));
      return CompletableFuture.completedFuture(ok(msg).as(Http.MimeTypes.HTML));
    }

    var command = form.get();

    if (!isValidUrl(command.getUrl())) {
      return CompletableFuture.completedFuture(ok("Invalid url").as(Http.MimeTypes.HTML));
    }

    return wsClient
        .url(command.getUrl())
        .setMethod("HEAD")
        .setRequestTimeout(Duration.ofSeconds(10))
        .setFollowRedirects(true)
        .execute()
        .thenApply(
            response ->
                ok("[%d] %s".formatted(response.getStatus(), response.getStatusText()))
                    .as(Http.MimeTypes.HTML))
        .exceptionally(throwable -> ok("Error: " + throwable.getMessage()).as(Http.MimeTypes.HTML));
  }

  /** Return true if string is valid absolute url */
  private boolean isValidUrl(String url) {
    try {
      if (!new URI(url).isAbsolute()) {
        return false;
      }
    } catch (URISyntaxException e) {
      return false;
    }

    return true;
  }
}

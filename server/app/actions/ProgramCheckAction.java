package actions;

import controllers.routes;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;

public class ProgramCheckAction extends Action.Simple {
  private static final Logger logger = LoggerFactory.getLogger(ProgramCheckAction.class);

  @Override
  public CompletionStage<Result> call(Http.Request req) {
    logger.error("*********************************************************");
    logger.error("*               ProgramCheckAction");

    Optional<String> programIdOptional = getParameterValue(req, "programId");
    logger.error(
        String.format("* Program ID: %s - URI: %s", programIdOptional.orElse("None"), req.uri()));

    logger.error("*********************************************************");
    logger.info("");

    // Change 1716 to some ID you have in your database
    // It will redirect that program home, but let you into others
    if (programIdOptional.orElse("").equals("1716")) {
      return CompletableFuture.completedFuture(redirect(routes.HomeController.index()));
    }

    return delegate.call(req);
  }

  // FROM: https://github.com/playframework/playframework/issues/2983#issuecomment-456375489
  // There's an open issue to product more built in support for this, but it's still in the works
  // For now you'll have to the ID like this
  public static Optional<String> getParameterValue(Http.Request req, String parameterName) {

    String routePattern = req.attrs().get(Router.Attrs.HANDLER_DEF).path();

    RouteExtractor routeExtractor = new RouteExtractor(routePattern, req.path());
    Map<String, String> extract = routeExtractor.extract();
    String result = null;
    if (extract.containsKey(parameterName)) {
      result = extract.get(parameterName);
    }
    return Optional.ofNullable(result);
  }
}

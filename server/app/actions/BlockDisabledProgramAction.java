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
import models.DisplayMode;

public class BlockDisabledProgramAction extends Action.Simple{
    @Override
    public CompletionStage<Result> call(Http.Request request) {
        Optional<String> programIdOptional = getParameterValue(request, "programId");
        if (programIdOptional.orElse("").equals("1716")) {
            return CompletableFuture.completedFuture(redirect(routes.HomeController.index()));
        }
        return delegate.call(request);
    }

    // FROM: https://github.com/playframework/playframework/issues/2983#issuecomment-456375489
    // There's an open issue to product more built in support for this, but it's still in the works
    // For now you'll have to the ID like this
    public static Optional<String> getParameterValue(Http.Request request, String parameterName) {

        String routePattern = request.attrs().get(Router.Attrs.HANDLER_DEF).path();

        RouteExtractor routeExtractor = new RouteExtractor(routePattern, request.path());
        Map<String, String> extract = routeExtractor.extract();
        String result = null;
        if (extract.containsKey(parameterName)) {
        result = extract.get(parameterName);
        }
        return Optional.ofNullable(result);
    }
}

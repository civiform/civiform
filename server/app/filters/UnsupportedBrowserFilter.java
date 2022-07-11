package filters;

import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Results;
import play.api.routing.HandlerDef;
import play.routing.Router;

public class UnsupportedBrowserFilter extends EssentialFilter {
  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
      request -> {
        if (request.getHeaders().get("user-agent").get().contains("Trident/7.0; rv:11.0")) {
          HandlerDef handlerDef = request.attrs().get(Router.Attrs.HANDLER_DEF);
          if (!"controllers.Assets".equals(handlerDef.controller()) && !handlerDef.method().equals("handleUnsupportedBrowser")) {
            return play.libs.streams.Accumulator.done(Results.redirect(controllers.routes.SupportController.handleUnsupportedBrowser()));
          }
        }
        return next.apply(request);
      });
  }
}

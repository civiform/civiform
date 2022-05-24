package auth;

import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.http.PlayHttpActionAdapter;
import play.mvc.Result;
import play.mvc.Results;

/**
 * We want to handle unauthorized requests to the API differently from requests to UI endpoints
 * since there is not a UX consideration for redirecting them to the login page. The action adapter
 * allows pac4j-consuming code to intervene and change the default behavior of handling a given HTTP
 * result. The default {@link PlayHttpActionAdapter} does not provide a way of customizing behavior
 * beyond mapping status codes to {@link Result} objects, so this custom adapter provides the
 * behavior we'd like.
 */
public final class CiviFormHttpActionAdapter extends PlayHttpActionAdapter {

  private static final String API_URL_PATH_PREFIX = "/api/";

  @Override
  public Result adapt(final HttpAction action, final WebContext context) {
    if (isUnauthorizedApiRequest(action, context)) {
      return ((PlayWebContext) context).supplementResponse(Results.unauthorized());
    }

    return super.adapt(action, context);
  }

  private static boolean isUnauthorizedApiRequest(HttpAction action, WebContext context) {
    return action.getCode() == HttpConstants.UNAUTHORIZED
        && context.getPath().startsWith(API_URL_PATH_PREFIX);
  }
}

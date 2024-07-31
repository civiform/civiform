package views;

import j2html.tags.specialized.ScriptTag;
import java.util.List;
import play.mvc.Http.RequestHeader;
import views.html.helper.CSPNonce;

public final class CspUtil {
  /** Apply the CSP nonce from the given request to the given scriptTag */
  public static ScriptTag applyCsp(RequestHeader request, ScriptTag scriptTag) {
    String nonce = CSPNonce.apply(request.asScala());
    return scriptTag.attr("nonce", nonce);
  }

  /** Apply the CSP nonce from the given request to the given scriptTags */
  public static List<ScriptTag> applyCsp(RequestHeader request, List<ScriptTag> scriptTags) {
    return scriptTags.stream().map(scriptTag -> applyCsp(request, scriptTag)).toList();
  }
}

package views;

import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.ScriptTag;
import java.util.List;
import play.mvc.Http.RequestHeader;
import views.html.helper.CSPNonce;

public final class CspUtil {
  /** Get the CSP nonce from the given request */
  public static String getNonce(RequestHeader request) {
    return CSPNonce.apply(request.asScala());
  }

  /** Apply the CSP nonce from the given request to the given scriptTag */
  public static ScriptTag applyCsp(RequestHeader request, ScriptTag scriptTag) {
    return scriptTag.attr("nonce", getNonce(request));
  }

  /** Apply the CSP nonce from the given request to the given scriptTag */
  public static LinkTag applyCsp(RequestHeader request, LinkTag styleTag) {
    return styleTag.attr("nonce", getNonce(request));
  }

  /** Apply the CSP nonce from the given request to the given scriptTags */
  public static List<ScriptTag> applyCsp(RequestHeader request, List<ScriptTag> scriptTags) {
    return scriptTags.stream().map(scriptTag -> applyCsp(request, scriptTag)).toList();
  }

  /** Apply the CSP nonce from the given request to the given style tags */
  public static List<LinkTag> applyCspToStyles(RequestHeader request, List<LinkTag> styleTags) {
    return styleTags.stream().map(styleTag -> applyCsp(request, styleTag)).toList();
  }
}

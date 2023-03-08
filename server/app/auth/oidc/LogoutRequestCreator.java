package auth.oidc;

import com.google.common.collect.ImmutableMap;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pac4j.core.exception.TechnicalException;

public class LogoutRequestCreator {

  public static LogoutRequest createLogoutRequest(
      URI endSessionEndpoint,
      String postLogoutRedirectParam,
      String targetUrl,
      ImmutableMap<String, String> extraParams,
      State state) {
    try {
      Map<String, List<String>> params = new HashMap<>();

      params.put(postLogoutRedirectParam, List.of(new URI(targetUrl).toString()));

      if (state != null) {
        params.put("state", List.of(state.getValue()));
      }

      for (Map.Entry<String, String> extraParam : extraParams.entrySet()) {
        params.putIfAbsent(extraParam.getKey(), new ArrayList<>());
        params.get(extraParam.getKey()).add(extraParam.getValue());
      }

      return LogoutRequest.parse(endSessionEndpoint, params);

    } catch (URISyntaxException | ParseException e) {
      throw new TechnicalException(e);
    }
  }
}

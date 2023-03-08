package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import java.net.URI;
import org.junit.Test;

public class LogoutRequestCreatorTest {

  @Test
  public void toUri_simple() throws Exception {
    LogoutRequest request =
        LogoutRequestCreator.createLogoutRequest(
            /* endSessionEndpoint = */ new URI("https://auth.com/logout"),
            /* postLogoutRedirectParam = */ "post_logout_redirect_uri",
            /* targetUrl = */ "https://civiform.com/",
            /* extraParams= */ ImmutableMap.of(),
            /* state= */ null);
    assertThat(request.toURI().toString())
        .isEqualTo(
            "https://auth.com/logout?post_logout_redirect_uri=https%3A%2F%2Fciviform.com%2F");
  }

  @Test
  public void toUri_extraParams() throws Exception {
    LogoutRequest request =
        LogoutRequestCreator.createLogoutRequest(
            /* endSessionEndpoint = */ new URI("https://auth.com/logout"),
            /* postLogoutRedirectParam = */ "post_logout_redirect_uri",
            /* targetUrl = */ "https://civiform.com/",
            /* extraParams = */ ImmutableMap.of("client_id", "12345"),
            /* state= */ null);
    assertThat(request.toURI().toString())
        .isEqualTo(
            "https://auth.com/logout?post_logout_redirect_uri=https%3A%2F%2Fciviform.com%2F&client_id=12345");
  }

  @Test
  public void toUri_withState() throws Exception {
    LogoutRequest request =
        LogoutRequestCreator.createLogoutRequest(
            /* endSessionEndpoint = */ new URI("https://auth.com/logout"),
            /* postLogoutRedirectParam = */ "post_logout_redirect_uri",
            /* targetUrl = */ "https://civiform.com/",
            /* extraParams = */ ImmutableMap.of(),
            new State("stateValue"));
    assertThat(request.toURI().toString())
        .isEqualTo(
            "https://auth.com/logout?post_logout_redirect_uri=https%3A%2F%2Fciviform.com%2F&state=stateValue");
  }
}

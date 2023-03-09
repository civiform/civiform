package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import java.net.URI;
import java.util.Optional;
import org.junit.Test;

public class CustomOidcLogoutRequestTest {

  @Test
  public void toUri_simple() throws Exception {
    LogoutRequest request =
        new CustomOidcLogoutRequest(
            new URI("https://auth.com/logout"),
            "post_logout_redirect_uri",
            /* postLogoutRedirectURI= */ new URI("https://civiform.com/"),
            /* clientId = */ Optional.empty(),
            /* state= */ null);
    assertThat(request.toURI().toString())
        .isEqualTo(
            "https://auth.com/logout?post_logout_redirect_uri=https%3A%2F%2Fciviform.com%2F");
  }

  @Test
  public void toUri_withClientId() throws Exception {
    LogoutRequest request =
        new CustomOidcLogoutRequest(
            new URI("https://auth.com/logout"),
            "post_logout_redirect_uri",
            /* postLogoutRedirectURI= */ new URI("https://civiform.com/"),
            /* clientId = */ Optional.of("12345"),
            /* state= */ null);
    assertThat(request.toURI().toString())
        .isEqualTo(
            "https://auth.com/logout?post_logout_redirect_uri=https%3A%2F%2Fciviform.com%2F&client_id=12345");
  }

  @Test
  public void toUri_withState() throws Exception {
    LogoutRequest request =
        new CustomOidcLogoutRequest(
            new URI("https://auth.com/logout"),
            "post_logout_redirect_uri",
            /* postLogoutRedirectURI= */ new URI("https://civiform.com/"),
            /* clientId = */ Optional.empty(),
            new State("stateValue"));
    assertThat(request.toURI().toString())
        .isEqualTo(
            "https://auth.com/logout?post_logout_redirect_uri=https%3A%2F%2Fciviform.com%2F&state=stateValue");
  }

  @Test
  public void toUri_withFragmentAndNoParams() throws Exception {
    // This specific test is to imitate one of auth setups where we need to
    // use non-standard logout url containing fragment and ensure that final
    // url doesn't contain any extra url params.
    LogoutRequest request =
        new CustomOidcLogoutRequest(
            new URI("https://auth.com/logout#fragmentHere"),
            "",
            /* postLogoutRedirectURI= */ new URI("https://civiform.com/"),
            /* clientId = */ Optional.empty(),
            /* state= */ null);
    assertThat(request.toURI().toString()).isEqualTo("https://auth.com/logout#fragmentHere");
  }
}

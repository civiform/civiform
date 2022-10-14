package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class AuthIdentityProviderNameTest {

  @Test
  public void fromConfig_returnsDisabledWhenEmpty() {
    assertThat(AuthIdentityProviderName.fromConfig(ConfigFactory.empty()))
        .isEqualTo(AuthIdentityProviderName.IDCS_APPLICANT);
  }

  @Test
  public void fromConfig_returnsValidValue() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                AuthIdentityProviderName.AUTH_APPLICANT_CONFIG_PATH,
                AuthIdentityProviderName.GENERIC_OIDC_APPLICANT.getString()));
    assertThat(AuthIdentityProviderName.fromConfig(config))
        .isEqualTo(AuthIdentityProviderName.GENERIC_OIDC_APPLICANT);
  }

  @Test
  public void fromConfig_throwsErrorWithInvalidName() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(AuthIdentityProviderName.AUTH_APPLICANT_CONFIG_PATH, "bla-bla-bla"));
    assertThatThrownBy(() -> AuthIdentityProviderName.fromConfig(config))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

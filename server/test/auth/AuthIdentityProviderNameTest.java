package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class AuthIdentityProviderNameTest {

  @Test
  public void fromConfig_applicantReturnsDisabledWhenEmpty() {
    assertThat(AuthIdentityProviderName.applicantIdentityProviderfromConfig(ConfigFactory.empty()))
        .isEqualTo(AuthIdentityProviderName.IDCS_APPLICANT);
  }

  @Test
  public void fromConfig_applicantReturnsValidValue() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                AuthIdentityProviderName.AUTH_APPLICANT_CONFIG_PATH,
                AuthIdentityProviderName.GENERIC_OIDC_APPLICANT.getValue()));
    assertThat(AuthIdentityProviderName.applicantIdentityProviderfromConfig(config))
        .isEqualTo(AuthIdentityProviderName.GENERIC_OIDC_APPLICANT);
  }

  @Test
  public void fromConfig_applicantThrowsErrorWithInvalidName() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(AuthIdentityProviderName.AUTH_APPLICANT_CONFIG_PATH, "bla-bla-bla"));
    assertThatThrownBy(() -> AuthIdentityProviderName.applicantIdentityProviderfromConfig(config))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fromConfig_adminReturnsDisabledWhenEmpty() {
    assertThat(AuthIdentityProviderName.adminIdentityProviderfromConfig(ConfigFactory.empty()))
        .isEqualTo(AuthIdentityProviderName.ADFS_ADMIN);
  }

  @Test
  public void fromConfig_adminReturnsValidValue() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                AuthIdentityProviderName.AUTH_ADMIN_CONFIG_PATH,
                AuthIdentityProviderName.GENERIC_OIDC_ADMIN.getValue()));
    assertThat(AuthIdentityProviderName.adminIdentityProviderfromConfig(config))
        .isEqualTo(AuthIdentityProviderName.GENERIC_OIDC_ADMIN);
  }

  @Test
  public void fromConfig_adminThrowsErrorWithInvalidName() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(AuthIdentityProviderName.AUTH_ADMIN_CONFIG_PATH, "bla-bla-bla"));
    assertThatThrownBy(() -> AuthIdentityProviderName.adminIdentityProviderfromConfig(config))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

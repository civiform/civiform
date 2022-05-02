package auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ApiKeyGrantsTest {

  private static final String PROGRAM_1_SLUG = "program-1";
  private static final String PROGRAM_2_SLUG = "program-2";
  private ApiKeyGrants apiKeyGrants;

  @Before
  public void setUp() {
    apiKeyGrants = new ApiKeyGrants();
  }

  @Test
  public void hasProgramPermission_readAllowsRead() {
    apiKeyGrants.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);

    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isFalse();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isTrue();
  }

  @Test
  public void hasProgramPermission_writeAllowsWrite() {
    apiKeyGrants.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE);

    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isTrue();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
  }

  @Test
  public void hasProgramPermission_doesntConfuseSlugs() {
    apiKeyGrants.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE);
    apiKeyGrants.grantProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.READ);

    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isTrue();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.WRITE))
        .isFalse();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.READ))
        .isTrue();
  }

  @Test
  public void hasProgramPermission_allPermissionsAreFalse() {
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isFalse();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
  }

  @Test
  public void revokeProgramPermission_removesThePermission() {
    apiKeyGrants.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);
    apiKeyGrants.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE);

    apiKeyGrants.revokeProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);

    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isTrue();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
  }

  @Test
  public void revokeAllProgramPermissions_revokesAllPermissions() {
    apiKeyGrants.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);
    apiKeyGrants.grantProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.READ);
    apiKeyGrants.grantProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.WRITE);

    apiKeyGrants.revokeAllProgramPermissions();

    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
    assertThat(apiKeyGrants.hasProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.WRITE))
        .isFalse();
  }
}

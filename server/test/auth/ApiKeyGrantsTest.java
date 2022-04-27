package auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ApiKeyGrantsTest {

  private static final String PROGRAM_1_SLUG = "program-1";
  private static final String PROGRAM_2_SLUG = "program-2";
  private ApiKeyGrants subject;

  @Before
  public void setUp() {
    subject = new ApiKeyGrants();
  }

  @Test
  public void hasProgramPermission_readAllowsRead() {
    subject.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);

    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isFalse();
    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ)).isTrue();
  }

  @Test
  public void hasProgramPermission_writeAllowsWrite() {
    subject.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE);

    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isTrue();
    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
  }

  @Test
  public void revokeProgramPermission_removesThePermission() {
    subject.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);
    subject.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE);

    subject.revokeProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);

    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.WRITE))
        .isTrue();
    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
  }

  @Test
  public void revokeAllProgramPermissions_revokesAllPermissions() {
    subject.grantProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ);
    subject.grantProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.READ);
    subject.grantProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.WRITE);

    subject.revokeAllProgramPermissions();

    assertThat(subject.hasProgramPermission(PROGRAM_1_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
    assertThat(subject.hasProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.READ))
        .isFalse();
    assertThat(subject.hasProgramPermission(PROGRAM_2_SLUG, ApiKeyGrants.Permission.WRITE))
        .isFalse();
  }
}

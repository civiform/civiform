package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import repository.WithPostgresContainer;

public class TrustedIntermediaryGroupTest extends WithPostgresContainer {
  @Test
  public void testTIOrg() {
    Account ti = new Account();
    ti.save();
    Account applicant = new Account();
    applicant.save();

    TrustedIntermediaryGroup tiGroup = new TrustedIntermediaryGroup("org", "an org");
    tiGroup.save();

    ti.setMemberOfGroup(tiGroup);
    ti.save();
    applicant.setManagedByGroup(tiGroup);
    applicant.save();

    ti.refresh();
    assertThat(ti.getMemberOfGroup().isPresent()).isTrue();
    assertThat(ti.getManagedByGroup().isPresent()).isFalse();

    assertThat(applicant.getMemberOfGroup().isPresent()).isFalse();
    assertThat(applicant.getManagedByGroup().isPresent()).isTrue();

    tiGroup.refresh();
    assertThat(tiGroup.getTrustedIntermediaries()).contains(ti);
    assertThat(tiGroup.getManagedAccounts()).contains(applicant);
  }
}

package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import repository.ResetPostgres;

public class TrustedIntermediaryGroupModelTest extends ResetPostgres {
  @Test
  public void testTIOrg() {
    AccountModel ti = new AccountModel();
    ti.save();
    AccountModel applicant = new AccountModel();
    applicant.save();

    TrustedIntermediaryGroupModel tiGroup = new TrustedIntermediaryGroupModel("org", "an org");
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
    assertThat(tiGroup.getTrustedIntermediariesSize()).isEqualTo(1);
    assertThat(tiGroup.getManagedAccountsSize()).isEqualTo(1);
  }
}

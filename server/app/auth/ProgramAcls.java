package auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import models.AccountModel;

public final class ProgramAcls {
  @JsonProperty("tiProgramViewAcls")
  private Set<Long> tiProgramViewAcls;

  public ProgramAcls() {
    this.tiProgramViewAcls = new HashSet<>();
  }

  @JsonCreator
  public ProgramAcls(@Nullable @JsonProperty("tiProgramViewAcls") Set<Long> tiProgramViewAcls) {
    if (tiProgramViewAcls == null) {
      this.tiProgramViewAcls = new HashSet<>();
    } else {
      this.tiProgramViewAcls = tiProgramViewAcls;
    }
  }

  public ImmutableSet<Long> getTiProgramViewAcls() {
    return ImmutableSet.copyOf(tiProgramViewAcls);
  }

  public boolean hasProgramViewPermission(CiviFormProfile civiFormProfile) {
    // only a TI profile holder can access the program
    if (civiFormProfile.isTrustedIntermediary()) {
      AccountModel currentAccount = civiFormProfile.getAccount().join();
      // If the TI is applying on behalf of a client, then the currentAccount will be the
      // client account. Hence, we should check if the TIOrg Id present in getManagedByGroup() is
      // part of tiProgramViewAcls list.
      // If the TI is applying as themselves (for their personal needs or testing), then the
      // currentAccount will be their account.
      // Hence, we should check if the TIOrg Id present in getMemberOfGroup() is part of
      // tiProgramViewAcls list.
      // The Program should be displayed in either case.
      if (currentAccount.getMemberOfGroup().isPresent()) {
        return tiProgramViewAcls.contains(currentAccount.getMemberOfGroup().get().id);
      }
      if (currentAccount.getManagedByGroup().isPresent()) {
        return tiProgramViewAcls.contains(currentAccount.getManagedByGroup().get().id);
      }
      return false;
    }
    return false;
  }
}

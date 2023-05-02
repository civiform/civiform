package auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import javax.annotation.Nullable;
import models.Account;
import services.program.ProgramDefinition;
public class ProgramAcls {
  @JsonProperty("programViewAcls")
  private HashSet<String> programViewAcls;

  public ProgramAcls() {
    this.programViewAcls = new HashSet<>();
  }
  @JsonCreator
  public ProgramAcls(
    @Nullable @JsonProperty("programViewAcls") HashSet<String> programViewAcls) {
    if (programViewAcls == null) {
      this.programViewAcls = new HashSet<>();
    } else {
      this.programViewAcls = programViewAcls;
    }
  }
  public ImmutableSet<String> getProgramViewAcls() {
    return ImmutableSet.copyOf(programViewAcls);
  }
  public ProgramAcls addTiToProgram(Account tiAccount) {
    this.programViewAcls.add(tiAccount.id.toString());
    return this;
  }

  public boolean hasProgramViewPermission(Account account) {
    return programViewAcls.contains(account.getManagedByGroup().get().id.toString());
  }
}

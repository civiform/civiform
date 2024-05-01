package auth;

import static org.assertj.core.api.Assertions.assertThat;

import models.AccountModel;
import org.junit.Test;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class StoredFileAclsTest extends ResetPostgres {

  private final ProgramDefinition programOne =
      ProgramBuilder.newDraftProgram("program-one").buildDefinition();
  private final ProgramDefinition programTwo =
      ProgramBuilder.newDraftProgram("program-two").buildDefinition();

  @Test
  public void hasProgramReadPermission_emptyPermissions() {
    var acls = new StoredFileAcls();
    var account =
        new AccountModel().addAdministeredProgram(programOne).addAdministeredProgram(programTwo);

    assertThat(acls.hasProgramReadPermission(account)).isFalse();
  }

  @Test
  public void hasProgramReadPermission_hasPermission() {
    var acls = new StoredFileAcls().addProgramToReaders(programOne);
    var account = new AccountModel().addAdministeredProgram(programOne);

    assertThat(acls.hasProgramReadPermission(account)).isTrue();
  }

  @Test
  public void hasProgramReadPermission_doesNotHavePermission() {
    var acls = new StoredFileAcls().addProgramToReaders(programOne);
    var account = new AccountModel().addAdministeredProgram(programTwo);

    assertThat(acls.hasProgramReadPermission(account)).isFalse();
  }
}

package auth;

import static org.assertj.core.api.Assertions.assertThat;

import models.Account;
import org.junit.Test;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class StoredFileAclsTest extends ResetPostgres {

  private ProgramDefinition programOne =
      ProgramBuilder.newDraftProgram("program-one").buildDefinition();
  private ProgramDefinition programTwo =
      ProgramBuilder.newDraftProgram("program-two").buildDefinition();

  @Test
  public void hasProgramReadPermission_emptyPermissions() {
    var acls = new StoredFileAcls();
    var account =
        new Account().addAdministeredProgram(programOne).addAdministeredProgram(programTwo);

    assertThat(acls.hasProgramReadPermission(account)).isFalse();
  }

  @Test
  public void hasProgramReadPermission_hasPermission() {
    var acls = new StoredFileAcls().addProgramToReaders(programOne);
    var account = new Account().addAdministeredProgram(programOne);

    assertThat(acls.hasProgramReadPermission(account)).isTrue();
  }

  @Test
  public void hasProgramReadPermission_doesNotHavePermission() {
    var acls = new StoredFileAcls().addProgramToReaders(programOne);
    var account = new Account().addAdministeredProgram(programTwo);

    assertThat(acls.hasProgramReadPermission(account)).isFalse();
  }
}

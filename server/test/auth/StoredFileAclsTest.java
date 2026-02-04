package auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class StoredFileAclsTest extends ResetPostgres {

  private ProgramDefinition programOne;
  private ProgramDefinition programTwo;

  @Before
  public void setup() {
    // These need to be initialized here rather than at the class level as
    // there's a negative reflection interaction with @NamedParameters if
    // they are at the class level.
    programOne = ProgramBuilder.newDraftProgram("program-one").buildDefinition();
    programTwo = ProgramBuilder.newDraftProgram("program-two").buildDefinition();
  }

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
  public void hasProgramReadPermission_differentProgram_noPermission() {
    var acls = new StoredFileAcls().addProgramToReaders(programOne);
    var account = new AccountModel().addAdministeredProgram(programTwo);

    assertThat(acls.hasProgramReadPermission(account)).isFalse();
  }

  @Test
  @Parameters(named = "programReadData")
  public void hasProgramReadPermission_deserialize(String json, Boolean hasAccess)
      throws JsonProcessingException {

    var account = new AccountModel().addAdministeredProgram(programOne);

    ObjectMapper mapper = new ObjectMapper();
    var acl = mapper.readValue(json, StoredFileAcls.class);
    assertThat(acl.hasProgramReadPermission(account)).isEqualTo(hasAccess);
  }

  // NamedParameters is necessary in Junit4 because the json contains commas
  // which is also @Parameters argument delimiter.
  /** Test data for an account that administers 'program-one'. */
  @NamedParameters("programReadData")
  private static Object[] programReadData() {
    return new Object[][] {
      // {json, hasAccess}
      {"{}", false},
      {
        """
        {"programReadAcls": []}"
        """,
        false,
      },
      {
        """
        {"programReadAcls": ["unAcldProgram1", "unAcldProgram2"]}
        """,
        false
      },
      {
        """
        {"programReadAcls": ["program-one"]}
        """,
        true
      },
      {
        """
        {"programReadAcls": ["unAcldProgram", "program-one"]}
        """,
        true
      }
    };
  }
}

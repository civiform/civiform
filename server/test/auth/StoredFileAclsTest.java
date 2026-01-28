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
  private static final String ACLD_PROGRAM_NAME1 = "program-one";
  private static final long ACLD_APPLICANT_ID = 100;

  private ProgramDefinition programOne;
  private ProgramDefinition programTwo;

  @Before
  public void setup() {
    // These need to be initialized here rather than at the class level as
    // there's a negative reflection interaction with @NamedParameters if
    // they are at the class level.
    programOne = ProgramBuilder.newDraftProgram(ACLD_PROGRAM_NAME1).buildDefinition();
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
        {"programReadAcls": ["%s"]}
        """
            .formatted(ACLD_PROGRAM_NAME1),
        true
      },
      {
        """
        {"programReadAcls": ["unAcldProgram", "%s"]}
        """
            .formatted(ACLD_PROGRAM_NAME1),
        true
      },
      // Combined with other acls.
      {
        """
        {"programReadAcls": ["%s"],
        "applicantReadAcls": [1111111]}
        """
            .formatted(ACLD_PROGRAM_NAME1),
        true
      },
      {
        """
        {"programReadAcls": [],
        "applicantReadAcls": [1111111]}
        """,
        false
      },
    };
  }

  @Test
  @Parameters(named = "applicantReadData")
  public void hasApplicantReadPermission_deserialize(String json, Boolean hasAccess)
      throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    var acl = mapper.readValue(json, StoredFileAcls.class);
    assertThat(acl.hasApplicantReadPermission(ACLD_APPLICANT_ID)).isEqualTo(hasAccess);
  }

  // NamedParameters is necessary in Junit4 because the json contains commas
  // which is also @Parameters argument delimiter.
  /**
   * Test data for an account with applicant ACLs.
   */
  @NamedParameters("applicantReadData")
  private static Object[] applicantReadData() {
    return new Object[][] {
      // {json, hasAccess}
      // Empty json data.
      {"{}", false},
      {
        """
        {"applicantReadAcls": []}"
        """,
        false,
      },
      // Different applicant ids, by way of large numbers.
      {
        """
        {"applicantReadAcls": [800000, 900000]}
        """,
        false
      },
      {
        """
        {"applicantReadAcls": [%d]}
        """
            .formatted(ACLD_APPLICANT_ID),
        true
      },
      // Authorized with another applicant.
      {
        """
        {"applicantReadAcls": [900000, %d]}
        """
            .formatted(ACLD_APPLICANT_ID),
        true
      },
      // Combined with other acls.
      {
        """
        {"programReadAcls": ["program-one"],
        "applicantReadAcls": [%d]}
        """
            .formatted(ACLD_APPLICANT_ID),
        true
      },
      {
        """
        {"programReadAcls": ["program-one"],
        "applicantReadAcls": []}
        """,
        false
      },
    };
  }
}

package services.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import models.AccountModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.CiviFormError;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class RoleServiceTest extends ResetPostgres {

  private AccountRepository accountRepository;
  private RoleService service;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    service = instanceOf(RoleService.class);
  }

  @Test
  public void makeProgramAdmins_allPromoted() throws ProgramNotFoundException {
    String email1 = "fake@email.com";
    String email2 = "fake2.com";
    AccountModel account1 = new AccountModel();
    account1.setEmailAddress(email1);
    account1.save();
    AccountModel account2 = new AccountModel();
    account2.setEmailAddress(email2);
    account2.save();

    String programName = "test program";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    Optional<CiviFormError> result =
        service.makeProgramAdmins(program.id, ImmutableSet.of(email1, email2));

    assertThat(result).isEmpty();

    account1 = accountRepository.lookupAccountByEmail(email1).get();
    account2 = accountRepository.lookupAccountByEmail(email2).get();

    assertThat(account1.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(account2.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void makeProgramAdmins_emailsAreCaseSensitive() throws ProgramNotFoundException {
    String emailUpperCase = "Fake.Person@email.com";
    String emailLowerCase = "fake.person@email.com";
    AccountModel account = new AccountModel();
    account.setEmailAddress(emailUpperCase);
    account.save();

    String programName = "test program";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    // Make the lower case email a program admin.
    Optional<CiviFormError> lowerCaseResult =
        service.makeProgramAdmins(program.id, ImmutableSet.of(emailLowerCase));

    assertThat(lowerCaseResult)
        .isEqualTo(
            Optional.of(
                CiviFormError.of(
                    String.format(
                        "Cannot add %s as a Program Admin because they do not have an admin"
                            + " account. Have the user log in as admin on the home page, then they"
                            + " can be added as a Program Admin. ",
                        emailLowerCase))));

    // Lookup the upper case account. They do not have permission to any programs.
    account = accountRepository.lookupAccountByEmail(emailUpperCase).get();
    assertThat(account.getAdministeredProgramNames()).isEmpty();

    // Now make the upper case Email a program admin.
    Optional<CiviFormError> result =
        service.makeProgramAdmins(program.id, ImmutableSet.of(emailUpperCase));
    assertThat(result).isEmpty();

    // Lookup the upper case account. They now have permissions to the program.
    account = accountRepository.lookupAccountByEmail(emailUpperCase).get();
    assertThat(account.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void makeProgramAdmins_emptyList_returnsEmptyOptional() throws ProgramNotFoundException {
    assertThat(service.makeProgramAdmins(1L, ImmutableSet.of())).isEmpty();
  }

  @Test
  public void makeProgramAdmins_listOfBlankEmails_returnsEmptyOptional()
      throws ProgramNotFoundException {
    assertThat(service.makeProgramAdmins(1L, ImmutableSet.of(" ", "", "    "))).isEmpty();
  }

  @Test
  public void makeProgramAdmins_programNotFound_throwsException() {
    assertThatThrownBy(() -> service.makeProgramAdmins(1234L, ImmutableSet.of("email@email.com")))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void makeProgramAdmins_emailHasNoAccountReturnsError() throws ProgramNotFoundException {
    String email = "admin_does_not_exist@email.com";

    String programName = "test program";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    Optional<CiviFormError> lowerCaseResult =
        service.makeProgramAdmins(program.id, ImmutableSet.of(email));

    assertThat(lowerCaseResult)
        .isEqualTo(
            Optional.of(
                CiviFormError.of(
                    String.format(
                        "Cannot add %s as a Program Admin because they do not have an admin"
                            + " account. Have the user log in as admin on the home page, then they"
                            + " can be added as a Program Admin. ",
                        email))));
  }

  @Test
  public void makeProgramAdmins_manyEmailsHaveNoAccountReturnsError()
      throws ProgramNotFoundException {
    String email1 = "first_admin_does_not_exist@email.com";
    String email2 = "second_admin_does_not_exist@email.com";

    String programName = "test program";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    Optional<CiviFormError> lowerCaseResult =
        service.makeProgramAdmins(program.id, ImmutableSet.of(email1, email2));

    assertThat(lowerCaseResult)
        .isEqualTo(
            Optional.of(
                CiviFormError.of(
                    String.format(
                        "Cannot add %1$s as a Program Admin because they do not have an admin"
                            + " account. Have the user log in as admin on the home page, then they"
                            + " can be added as a Program Admin. Cannot add %2$s as a Program"
                            + " Admin because they do not have an admin account. Have the user log"
                            + " in as admin on the home page, then they can be added as a Program"
                            + " Admin. ",
                        email1, email2))));
  }

  @Test
  public void removeProgramAdmins_succeeds() throws ProgramNotFoundException {
    String programName = "to remove";
    ProgramDefinition toRemove = ProgramBuilder.newDraftProgram(programName).buildDefinition();
    String extraName = "extra";
    ProgramDefinition extra = ProgramBuilder.newDraftProgram(extraName).buildDefinition();

    AccountModel one = new AccountModel();
    String emailOne = "one@test.com";
    one.setEmailAddress(emailOne);
    one.addAdministeredProgram(toRemove);
    one.save();

    AccountModel two = new AccountModel();
    String emailTwo = "two@test.com";
    two.setEmailAddress(emailTwo);
    two.addAdministeredProgram(toRemove);
    two.addAdministeredProgram(extra);
    two.save();

    assertThat(one.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(two.getAdministeredProgramNames()).containsOnly(programName, extraName);

    service.removeProgramAdmins(toRemove.id(), ImmutableSet.of(emailOne, emailTwo));

    assertThat(accountRepository.lookupAccountByEmail(emailOne).get().getAdministeredProgramNames())
        .isEmpty();
    assertThat(accountRepository.lookupAccountByEmail(emailTwo).get().getAdministeredProgramNames())
        .containsOnly(extraName);
  }

  @Test
  public void removeProgramAdmins_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> service.removeProgramAdmins(1234L, ImmutableSet.of("test")))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void makeProgramAdmins_blockGlobalAdmin() throws ProgramNotFoundException {
    String globalAdminEmail = "global@admin";
    AccountModel globalAdmin = new AccountModel();
    globalAdmin.setEmailAddress(globalAdminEmail);
    globalAdmin.setGlobalAdmin(true);
    globalAdmin.save();

    String programName = "test program";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    assertThat(service.makeProgramAdmins(program.id, ImmutableSet.of(globalAdminEmail)))
        .isEqualTo(
            Optional.of(
                CiviFormError.of(
                    String.format(
                        "The following are already CiviForm admins and could not be added as"
                            + " program admins: %s",
                        globalAdminEmail))));
  }
}

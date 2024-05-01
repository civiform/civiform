package services.role;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import auth.Role;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import models.AccountModel;
import models.ProgramModel;
import repository.AccountRepository;
import services.CiviFormError;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** A service for reading and updating data related to system roles. */
public final class RoleService {

  private final ProgramService programService;
  private final AccountRepository accountRepository;

  @Inject
  public RoleService(ProgramService programRepository, AccountRepository accountRepository) {
    this.programService = programRepository;
    this.accountRepository = accountRepository;
  }

  /**
   * Get a set of {@link AccountModel}s that have the role {@link Role#ROLE_CIVIFORM_ADMIN}.
   *
   * @return an {@link ImmutableSet} of {@link AccountModel}s that are CiviForm admins.
   */
  public ImmutableSet<AccountModel> getGlobalAdmins() {
    return accountRepository.getGlobalAdmins();
  }

  /**
   * Promotes the set of accounts (identified by email) to the role of {@link
   * Role#ROLE_PROGRAM_ADMIN} for the given program. If an account is currently a {@link
   * Role#ROLE_CIVIFORM_ADMIN}, they will not be promoted, since CiviForm admins cannot be program
   * admins. Instead, we return a {@link CiviFormError} listing the admin accounts that could not be
   * promoted to program admins.
   *
   * @param programId the ID of the {@link ProgramModel} these accounts administer
   * @param accountEmails a {@link ImmutableSet} of account emails to make program admins
   * @return {@link Optional#empty()} if all accounts were promoted to program admins, or an {@link
   *     Optional} of a {@link CiviFormError} listing the accounts that could not be promoted to
   *     program admin
   */
  public Optional<CiviFormError> makeProgramAdmins(
      long programId, ImmutableSet<String> accountEmails) throws ProgramNotFoundException {
    if (accountEmails.isEmpty() || accountEmails.stream().allMatch(String::isBlank)) {
      return Optional.empty();
    }

    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    // Filter out CiviForm admins from the list of emails - a CiviForm admin cannot be a program
    // admin.
    ImmutableSet<String> globalAdminEmails =
        getGlobalAdmins().stream()
            .map(AccountModel::getEmailAddress)
            .filter(address -> !Strings.isNullOrEmpty(address))
            .collect(toImmutableSet());
    ImmutableSet.Builder<String> invalidEmailBuilder = ImmutableSet.builder();
    String errorMessageString = "";

    for (String email : accountEmails) {
      if (globalAdminEmails.contains(email)) {
        invalidEmailBuilder.add(email);
      } else {
        Optional<CiviFormError> maybeError =
            accountRepository.addAdministeredProgram(email, program);

        // Concatenate error messages.
        if (maybeError.isPresent()) {
          errorMessageString += maybeError.get().message() + " ";
        }
      }
    }

    String allErrorMessages = "";
    ImmutableSet<String> invalidEmails = invalidEmailBuilder.build();

    // If there were any errors, return the error message.
    if (!errorMessageString.isEmpty()) {
      allErrorMessages = errorMessageString;
    }
    if (!invalidEmails.isEmpty()) {
      allErrorMessages +=
          String.format(
              "The following are already CiviForm admins and could not be added as"
                  + " program admins: %s",
              Joiner.on(", ").join(invalidEmails));
    }
    if (!allErrorMessages.isEmpty()) {
      return Optional.of(CiviFormError.of(allErrorMessages));
    } else {
      return Optional.empty();
    }
  }

  /**
   * For each account (identified by email), remove the given program from the list of programs that
   * account administers. If an account does not administer the given program, do nothing.
   *
   * @param programId the ID of the program to remove
   * @param accountEmails a list of account emails of program admins for the given program
   * @throws ProgramNotFoundException if the given program does not exist
   */
  public void removeProgramAdmins(long programId, ImmutableSet<String> accountEmails)
      throws ProgramNotFoundException {
    if (!accountEmails.isEmpty()) {
      ProgramDefinition program = programService.getFullProgramDefinition(programId);
      accountEmails.forEach(email -> accountRepository.removeAdministeredProgram(email, program));
    }
  }
}

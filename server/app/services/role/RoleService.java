package services.role;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import annotations.FeatureFlags.AllowGlobalAdminsBeProgramAdmins;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import repository.UserRepository;
import services.CiviFormError;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** A service for reading and updating data related to system roles. */
public class RoleService {

  private final ProgramService programService;
  private final UserRepository userRepository;
  private final boolean allowGlobalAdmins;

  @Inject
  public RoleService(
      ProgramService programRepository,
      UserRepository userRepository,
      @AllowGlobalAdminsBeProgramAdmins boolean allowGlobalAdmins) {
    this.programService = programRepository;
    this.userRepository = userRepository;
    this.allowGlobalAdmins = allowGlobalAdmins;
  }

  /**
   * Get a set of {@link Account}s that have the role {@link auth.Roles#ROLE_CIVIFORM_ADMIN}.
   *
   * @return an {@link ImmutableSet} of {@link Account}s that are CiviForm admins.
   */
  public ImmutableSet<Account> getGlobalAdmins() {
    return userRepository.getGlobalAdmins();
  }

  /**
   * Promotes the set of accounts (identified by email) to the role of {@link
   * auth.Roles#ROLE_PROGRAM_ADMIN} for the given program. If an account is currently a {@link
   * auth.Roles#ROLE_CIVIFORM_ADMIN}, they will not be promoted, since CiviForm admins cannot be
   * program admins. Instead, we return a {@link CiviFormError} listing the admin accounts that
   * could not be promoted to program admins. If {@link ALLOW_GLOBAL_ADMINS_BE_PROGRAM_ADMINS} is
   * set to True Global admins can be promoted to Program admins.
   *
   * @param programId the ID of the {@link models.Program} these accounts administer
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

    ProgramDefinition program = programService.getProgramDefinition(programId);
    ImmutableSet<String> globalAdminEmails =
        allowGlobalAdmins
            ? ImmutableSet.of()
            : getGlobalAdmins().stream()
                .map(Account::getEmailAddress)
                .filter(address -> !Strings.isNullOrEmpty(address))
                .collect(toImmutableSet());
    ImmutableSet.Builder<String> invalidEmailBuilder = ImmutableSet.builder();
    String errorMessageString = "";

    for (String email : accountEmails) {
      if (globalAdminEmails.contains(email)) {
        invalidEmailBuilder.add(email);
      } else {
        Optional<CiviFormError> maybeError = userRepository.addAdministeredProgram(email, program);

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
      ProgramDefinition program = programService.getProgramDefinition(programId);
      accountEmails.forEach(email -> userRepository.removeAdministeredProgram(email, program));
    }
  }
}

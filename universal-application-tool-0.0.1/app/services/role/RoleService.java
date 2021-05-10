package services.role;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import javax.inject.Inject;
import models.Account;
import repository.UserRepository;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** A service for reading and updating data related to system roles. */
public class RoleService {

  private final ProgramService programService;
  private final UserRepository userRepository;

  @Inject
  public RoleService(ProgramService programRepository, UserRepository userRepository) {
    this.programService = programRepository;
    this.userRepository = userRepository;
  }

  /**
   * Get a set of {@link Account}s that have the role {@link auth.Roles#ROLE_UAT_ADMIN}.
   *
   * @return an {@link ImmutableSet} of {@link Account}s that are UAT admins.
   */
  public ImmutableSet<Account> getUatAdmins() {
    // TODO(cdanzi): implement this method
    return ImmutableSet.of();
  }

  /**
   * Promotes the set of accounts (identified by email) to the role of {@link
   * auth.Roles#ROLE_PROGRAM_ADMIN} for the given program. If an account is currently a {@link
   * auth.Roles#ROLE_UAT_ADMIN}, they will not be promoted, since UAT admins cannot be program
   * admins.
   *
   * @param programId the ID of the {@link models.Program} these accounts administer
   * @param accountEmails a {@link ImmutableSet} of account emails to make program admins
   */
  public void makeProgramAdmins(long programId, ImmutableSet<String> accountEmails)
      throws ProgramNotFoundException {
    ProgramDefinition program = programService.getProgramDefinition(programId);
    // Filter out UAT admins from the list of emails - a UAT admin cannot be a program admin.
    Sets.difference(
            accountEmails,
            getUatAdmins().stream().map(Account::getEmailAddress).collect(toImmutableSet()))
        .forEach(email -> userRepository.addAdministeredProgram(email, program));
  }
}

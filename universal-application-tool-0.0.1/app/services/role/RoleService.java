package services.role;

import com.google.common.collect.ImmutableSet;
import models.Account;
import services.program.ProgramNotFoundException;

/** A service for reading and updating data related to system roles. */
public interface RoleService {

  /**
   * Get a set of {@link Account}s that have the role {@link auth.Roles#ROLE_UAT_ADMIN}.
   *
   * @return an {@link ImmutableSet} of {@link Account}s that are UAT admins.
   */
  ImmutableSet<Account> getUatAdmins();

  /**
   * Promotes the set of accounts (identified by email) to the role of {@link
   * auth.Roles#ROLE_PROGRAM_ADMIN} for the given program. If an account is currently a {@link
   * auth.Roles#ROLE_UAT_ADMIN}, they will not be promoted, since UAT admins cannot be program
   * admins.
   *
   * @param programId the ID of the {@link models.Program} these accounts administer
   * @param accountEmails a {@link ImmutableSet} of account emails to make program admins
   */
  void makeProgramAdmins(long programId, ImmutableSet<String> accountEmails)
      throws ProgramNotFoundException;
}

package services.role;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import javax.inject.Inject;
import models.Account;
import repository.UserRepository;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

public class RoleServiceImpl implements RoleService {

  private final ProgramService programService;
  private final UserRepository userRepository;

  @Inject
  public RoleServiceImpl(ProgramService programRepository, UserRepository userRepository) {
    this.programService = programRepository;
    this.userRepository = userRepository;
  }

  @Override
  public ImmutableSet<Account> getUatAdmins() {
    // TODO(cdanzi): implement this method
    return ImmutableSet.of();
  }

  @Override
  public void makeProgramAdmins(long programId, ImmutableSet<String> accountEmails)
      throws ProgramNotFoundException {
    ProgramDefinition program = programService.getProgramDefinition(programId);
    // Filter out UAT admins from the list of emails - a UAT admin cannot be a program admin.
    Sets.difference(accountEmails, getUatAdmins())
        .forEach(email -> userRepository.addAdministeredProgram(email, program));
  }
}

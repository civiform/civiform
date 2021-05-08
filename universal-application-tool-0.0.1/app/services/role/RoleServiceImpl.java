package services.role;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import play.libs.concurrent.HttpExecutionContext;
import repository.UserRepository;
import services.program.ProgramService;

public class RoleServiceImpl implements RoleService {

  private final ProgramService programService;
  private final UserRepository userRepository;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public RoleServiceImpl(
      ProgramService programRepository,
      UserRepository userRepository,
      HttpExecutionContext httpExecutionContext) {
    this.programService = programRepository;
    this.userRepository = userRepository;
    this.httpExecutionContext = httpExecutionContext;
  }

  @Override
  public void makeProgramAdmins(long programId, ImmutableList<String> accountEmails) {
    programService
        .getProgramDefinitionAsync(programId)
        .thenApplyAsync(
            program -> {
              accountEmails.forEach(email -> userRepository.addAdministeredProgram(email, program));
              return null;
            },
            httpExecutionContext.current());
  }
}

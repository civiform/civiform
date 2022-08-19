package repository;

import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import services.application.ApplicationEventDetails;

public class ApplicationEventRepositoryTest extends ResetPostgres {

  private ApplicationEventRepository repo;

  @Before
  public void setUp() {
    repo = instanceOf(ApplicationEventRepository.class);
  }

  @Test
  public void insert() {
    /*Program program = resourceCreator.insertActiveProgram("Program");
    Applicant applicant = resourceCreator.insertApplicant();
    Application application = resourceCreator.insertActiveApplication(applicant, program);

    ApplicationEventDetails details = ApplicationEventDetails.builder().build();
    ApplicationEvent event = ApplicationEvent.create(application,
      ApplicationEventDetails.Type.STATUS_CHANGE, details);

     */
  }

}

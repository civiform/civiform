package views.errors;

import org.junit.Before;
import views.applicant.ApplicantLayout;

public class NotFoundTest {

  NotFound notFoundPage;

  @Inject ApplicantLayout layout;

  @Before
  public void setUp() {
    ApplicantLayout layout = instanceOf(ApplicantLayout.class);
  }
}

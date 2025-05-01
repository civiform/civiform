package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;

public class ProgramCardsSectionParamsFactoryTest extends ResetPostgres {
  private CiviFormProfile testProfile;
  private CiviFormProfileData testProfileData;

  @Before
  public void setup() {
    testProfile = mock(CiviFormProfile.class);
    testProfileData = mock(CiviFormProfileData.class);
    when(testProfile.isTrustedIntermediary()).thenReturn(true);
    when(testProfile.getProfileData()).thenReturn(testProfileData);
    when(testProfileData.containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME))
        .thenReturn(false);
  }

  @Test
  public void getActionUrl_returnsProgramOverviewUrlWhenNotActiveOrDraft() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ false,
            // empty lifecycle stage means this is their first time filling out this application
            /* lifeCycleStage= */ Optional.empty(),
            /* applicantId= */ Optional.empty(),
            /* profile= */ Optional.empty());
    assertThat(url).isEqualTo("/programs/fake-program");
  }

  @Test
  public void getActionUrl_returnsProgramOverviewUrlWithApplicantIdWhenPresent() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ false,
            // empty lifecycle stage means this is their first time filling out this application
            /* lifeCycleStage= */ Optional.empty(),
            /* applicantId= */ Optional.of(1L),
            /* profile= */ Optional.of(testProfile));
    assertThat(url).isEqualTo("/applicants/1/programs/fake-program");
  }

  @Test
  public void getActionUrl_returnsEditUrlWhenDraft() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ false,
            Optional.of(
                LifecycleStage.DRAFT), // draft lifecyle stage means they have an in progress draft
            /* applicantId= */ Optional.empty(),
            /* profile= */ Optional.empty());
    assertThat(url).isEqualTo("/programs/1/edit");
  }

  @Test
  public void getActionUrl_returnsEditUrlWithApplicantIdWhenPresent() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ false,
            Optional.of(
                LifecycleStage.DRAFT), // draft lifecyle stage means they have an in progress draft
            /* applicantId= */ Optional.of(1L),
            /* profile= */ Optional.of(testProfile));
    assertThat(url).isEqualTo("/applicants/1/programs/1/edit");
  }

  @Test
  public void getActionUrl_returnsReviewUrlWhenActive() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ false,
            Optional.of(
                LifecycleStage
                    .ACTIVE), // active lifecycle stage means they have submitted the application
            /* applicantId= */ Optional.empty(),
            /* profile= */ Optional.empty());
    assertThat(url).isEqualTo("/programs/1/review");
  }

  @Test
  public void getActionUrl_returnsReviewUrlWithApplicantIdWhenPresent() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ false,
            Optional.of(
                LifecycleStage
                    .ACTIVE), // active lifecycle stage means they have submitted the application
            /* applicantId= */ Optional.of(1L),
            /* profile= */ Optional.of(testProfile));
    assertThat(url).isEqualTo("/applicants/1/programs/1/review");
  }

  @Test
  public void getActionUrl_returnsEditUrlWhenCommonIntake() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ true,
            // empty lifecycle stage means this is their first time filling out this application
            /* lifeCycleStage= */ Optional.empty(),
            /* applicantId= */ Optional.empty(),
            /* profile= */ Optional.empty());
    assertThat(url).isEqualTo("/programs/1/edit");
  }

  @Test
  public void getActionUrl_returnsEditUrlWhenCommonIntakeWithApplicantIdWhenPresent() {
    ApplicantRoutes applicantRoutes = new ApplicantRoutes();
    String url =
        ProgramCardsSectionParamsFactory.getActionUrl(
            applicantRoutes,
            /* programId= */ 1L,
            /* programSlug= */ "fake-program",
            /* isCommonIntakeForm= */ true,
            // empty lifecycle stage means this is their first time filling out this application
            /* lifeCycleStage= */ Optional.empty(),
            /* applicantId= */ Optional.of(1L),
            /* profile= */ Optional.of(testProfile));
    assertThat(url).isEqualTo("/applicants/1/programs/1/edit");
  }
}

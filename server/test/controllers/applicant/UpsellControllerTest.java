package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;


import auth.ProfileFactory;
import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import java.time.Instant;
import models.ApplicantModel;
import models.ApplicationModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.applications.ApplicationService;
import services.applications.PdfExporterService;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.upsell.ApplicantPreScreenerUpsellView;
import views.applicant.upsell.ApplicantUpsellView;

public class UpsellControllerTest extends WithMockedProfiles {

  public static final Instant FAKE_SUBMIT_TIME = Instant.parse("2024-01-01T01:00:00.00Z");

  private UpsellController subject;
  private SettingsManifest settingsManifest;

  @Before
  public void setUp() {
    resetDatabase();
    settingsManifest = mock(SettingsManifest.class);
    subject =
        new UpsellController(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ApplicationService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(ApplicantUpsellView.class),
            instanceOf(ApplicantPreScreenerUpsellView.class),
            instanceOf(MessagesApi.class),
            instanceOf(PdfExporterService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            settingsManifest,
            instanceOf(MonitoringMetricCounters.class));
  }

  @Test
  public void considerRegister_redirectsToUpsellViewForDefaultProgramType() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());
    application.setSubmitTimeForTest(FAKE_SUBMIT_TIME);
    String redirectLocation = "someUrl";

    Request request = fakeRequestBuilder().build();
    Result result =
        subject
            .considerRegister(
                request,
                applicant.id,
                String.valueOf(programDefinition.id()),
                application.id,
                redirectLocation,
                application.getSubmitTime().toString())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Application confirmation");
    assertThat(contentAsString(result)).contains("Create an account");
  }

  @Test
  public void considerRegister_whenProgramSlugUrlsEnabled_withProgramId_redirectsHome() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());
    application.setSubmitTimeForTest(FAKE_SUBMIT_TIME);
    String redirectLocation = "someUrl";

    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .considerRegister(
                request,
                applicant.id,
                String.valueOf(programDefinition.id()),
                application.id,
                redirectLocation,
                application.getSubmitTime().toString())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().get()).isEqualTo("/");
  }

  @Test
  public void
      considerRegister_whenProgramSlugUrlsEnabled_redirectsToUpsellViewForDefaultProgramType() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());
    application.setSubmitTimeForTest(FAKE_SUBMIT_TIME);
    String redirectLocation = "someUrl";

    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .considerRegister(
                request,
                applicant.id,
                programDefinition.slug(),
                application.id,
                redirectLocation,
                application.getSubmitTime().toString())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Application confirmation");
    assertThat(contentAsString(result)).contains("Create an account");
  }

  @Test
  public void
      considerRegister_whenProgramSlugUrlsEnabled_redirectsToUpsellViewForPrescreenerProgramType() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActivePreScreenerForm("test-program").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());
    application.setSubmitTimeForTest(FAKE_SUBMIT_TIME);
    String redirectLocation = "someUrl";

    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .considerRegister(
                request,
                applicant.id,
                programDefinition.slug(),
                application.id,
                redirectLocation,
                application.getSubmitTime().toString())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Application confirmation");
    assertThat(contentAsString(result)).contains("Create an account");
  }

  @Test
  public void download_authenticatedApplicant() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          subject
              .download(fakeRequest(), application.id, applicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void download_authenticatedTI() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    profileFactory.createFakeTrustedIntermediary();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(managedApplicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          subject
              .download(fakeRequest(), application.id, managedApplicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void download_unauthorizedTI() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel unmanagedApplicant = createApplicant();
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    profileFactory.createFakeTrustedIntermediary();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(managedApplicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          subject
              .download(fakeRequest(), application.id, unmanagedApplicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void download_invalidApplicantID() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());

    Result result;
    try {
      result = subject.download(fakeRequest(), application.id, 0).toCompletableFuture().join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void download_invalidApplicationID() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());

    Result result;
    try {
      result = subject.download(fakeRequest(), 0, applicant.id).toCompletableFuture().join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}

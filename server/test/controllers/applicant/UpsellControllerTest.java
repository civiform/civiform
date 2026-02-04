package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileFactory;
import controllers.WithMockedProfiles;
import java.time.Instant;
import models.ApplicantModel;
import models.ApplicationModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class UpsellControllerTest extends WithMockedProfiles {

  public static final Instant FAKE_SUBMIT_TIME = Instant.parse("2024-01-01T01:00:00.00Z");

  @Before
  public void setUp() {
    resetDatabase();
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

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "true").build();
    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                request,
                applicant.id,
                programDefinition.id(),
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
          instanceOf(UpsellController.class)
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
          instanceOf(UpsellController.class)
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
          instanceOf(UpsellController.class)
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
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), application.id, 0)
              .toCompletableFuture()
              .join();
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
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), 0, applicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}

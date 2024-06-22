package actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import auth.ProfileUtils;
import controllers.applicant.routes;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.mockito.Mockito;
import play.api.routing.HandlerDef;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.routing.Router;
import play.test.Helpers;
import play.test.WithApplication;
import repository.ApplicationRepository;
import repository.ProgramRepository;
import scala.jdk.javaapi.CollectionConverters;

public class ProgramFastForwardApplicationActionTest extends WithApplication {
  private final long applicantId = 500L;
  private final long currentProgramId = 1L;
  private final long latestProgramId = 3L;

  @Test
  public void continue_to_next_action_when_program_is_already_the_latest_version() {
    // Setup mocks
    ProfileUtils profileUtilsMock = mock(ProfileUtils.class);
    ProgramRepository programRepositoryMock = mock(ProgramRepository.class);
    ApplicationRepository applicationRepositoryMock = mock(ApplicationRepository.class);
    Action.Simple nextActionInChainMock = mock(Action.Simple.class);

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format("/programs/%d/blocks/2/edit", currentProgramId));

    // Set up mocked return values
    when(profileUtilsMock.getApplicantId(request)).thenReturn(Optional.of(applicantId));
    when(programRepositoryMock.getMostRecentActiveProgramId(any(Long.class)))
        .thenReturn(currentProgramId);

    // The action to test
    ProgramFastForwardApplicationAction action =
        new ProgramFastForwardApplicationAction(
            profileUtilsMock, programRepositoryMock, applicationRepositoryMock);

    // Configure a fake secondary action to serve as a stand in for the the last action in the chain
    // (i.e. controller)
    when(nextActionInChainMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = nextActionInChainMock;

    // Result from running the action
    Result result = action.call(request).toCompletableFuture().join();

    // All the assertions and verifications
    assertThat(result).isNull();

    // This is the main reason for this action and must have run given the inputs for this test
    verify(applicationRepositoryMock, Mockito.times(0))
        .updateDraftApplicationProgram(any(Long.class), any(Long.class));
  }

  @Test
  public void successful_update_and_redirect_with_applicant_id_from_profile() {
    // Setup mocks
    ProfileUtils profileUtilsMock = mock(ProfileUtils.class);
    ProgramRepository programRepositoryMock = mock(ProgramRepository.class);
    ApplicationRepository applicationRepositoryMock = mock(ApplicationRepository.class);

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format("/programs/%d/blocks/2/edit", currentProgramId));

    // Set up mocked return values
    when(profileUtilsMock.getApplicantId(request)).thenReturn(Optional.of(applicantId));
    when(programRepositoryMock.getMostRecentActiveProgramId(any(Long.class)))
        .thenReturn(latestProgramId);

    // The action to test
    ProgramFastForwardApplicationAction action =
        new ProgramFastForwardApplicationAction(
            profileUtilsMock, programRepositoryMock, applicationRepositoryMock);

    // Result from running the action
    Result result = action.call(request).toCompletableFuture().join();

    // All the assertions and verifications
    assertThat(result).isNotNull();
    assertThat(result.redirectLocation().isPresent()).isTrue();
    assertThat(result.redirectLocation().get())
        .isEqualTo(routes.ApplicantProgramReviewController.review(latestProgramId).url());

    // This is the main reason for this action and must have run given the inputs for this test
    verify(applicationRepositoryMock, Mockito.times(1))
        .updateDraftApplicationProgram(any(Long.class), any(Long.class));
  }

  @Test
  public void successful_update_and_redirect_with_applicant_id_from_route() {
    // Setup mocks
    ProfileUtils profileUtilsMock = mock(ProfileUtils.class);
    ProgramRepository programRepositoryMock = mock(ProgramRepository.class);
    ApplicationRepository applicationRepositoryMock = mock(ApplicationRepository.class);

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/applicant/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format(
                "/programs/%d/applicant/%d/blocks/2/edit", currentProgramId, applicantId));

    // Set up mocked return values
    when(profileUtilsMock.getApplicantId(request)).thenReturn(Optional.empty());
    when(programRepositoryMock.getMostRecentActiveProgramId(any(Long.class)))
        .thenReturn(latestProgramId);

    // The action to test
    ProgramFastForwardApplicationAction action =
        new ProgramFastForwardApplicationAction(
            profileUtilsMock, programRepositoryMock, applicationRepositoryMock);

    // Result from running the action
    Result result = action.call(request).toCompletableFuture().join();

    // All the assertions and verifications
    assertThat(result).isNotNull();
    assertThat(result.redirectLocation().isPresent()).isTrue();
    assertThat(result.redirectLocation().get())
        .isEqualTo(routes.ApplicantProgramReviewController.review(latestProgramId).url());

    // This is the main reason for this action and must have run given the inputs for this test
    verify(applicationRepositoryMock, Mockito.times(1))
        .updateDraftApplicationProgram(any(Long.class), any(Long.class));
  }

  @Test
  public void failed_due_to_missing_program_id_in_route() {
    // Setup mocks
    ProfileUtils profileUtilsMock = mock(ProfileUtils.class);
    ProgramRepository programRepositoryMock = mock(ProgramRepository.class);
    ApplicationRepository applicationRepositoryMock = mock(ApplicationRepository.class);

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest("/programs/blocks/$blockId<[^/]+>/edit", "/programs/blocks/2/edit");

    // Set up mocked return values
    when(profileUtilsMock.getApplicantId(request)).thenReturn(Optional.empty());
    when(programRepositoryMock.getMostRecentActiveProgramId(any(Long.class)))
        .thenReturn(latestProgramId);

    // The action to test
    ProgramFastForwardApplicationAction action =
        new ProgramFastForwardApplicationAction(
            profileUtilsMock, programRepositoryMock, applicationRepositoryMock);

    // Result from running the action
    assertThatThrownBy(() -> action.call(request).toCompletableFuture().join())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("programId");
  }

  @Test
  public void failed_due_to_missing_applicant_id() {
    // Setup mocks
    ProfileUtils profileUtilsMock = mock(ProfileUtils.class);
    ProgramRepository programRepositoryMock = mock(ProgramRepository.class);
    ApplicationRepository applicationRepositoryMock = mock(ApplicationRepository.class);

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/applicant/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format("/programs/%d/applicant/a/blocks/2/edit", currentProgramId));

    // Set up mocked return values
    when(profileUtilsMock.getApplicantId(request)).thenReturn(Optional.empty());
    when(programRepositoryMock.getMostRecentActiveProgramId(any(Long.class)))
        .thenReturn(latestProgramId);

    // The action to test
    ProgramFastForwardApplicationAction action =
        new ProgramFastForwardApplicationAction(
            profileUtilsMock, programRepositoryMock, applicationRepositoryMock);

    // Result from running the action
    assertThatThrownBy(() -> action.call(request).toCompletableFuture().join())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("applicantId");
  }

  /**
   * Create a {@link Request} object with customized route information
   *
   * @param routePattern is the pattern for the routes
   * @param path is the current URI route
   * @return {@link Request}
   */
  private Request createFakeRequest(String routePattern, String path) {
    HandlerDef handlerDef =
        new HandlerDef(
            getClass().getClassLoader(),
            "router",
            "controllers.MyFakeController",
            "index",
            CollectionConverters.asScala(Collections.<Class<?>>emptyList()).toSeq(),
            "GET",
            routePattern,
            "",
            CollectionConverters.asScala(Collections.<String>emptyList()).toSeq());

    return Helpers.fakeRequest("GET", path).build().addAttr(Router.Attrs.HANDLER_DEF, handlerDef);
  }
}

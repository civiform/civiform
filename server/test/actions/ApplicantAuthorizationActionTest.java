package actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import models.ApplicantModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.api.routing.HandlerDef;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.routing.Router;
import play.test.Helpers;
import repository.VersionRepository;
import scala.jdk.javaapi.CollectionConverters;
import support.ProgramBuilder;

public class ApplicantAuthorizationActionTest extends WithMockedProfiles {
  @Before
  public void setUpWithFreshApplicants() {
    resetDatabase();
  }

  @Test
  public void when_applicant_and_program_auth_pass_then_moves_to_next_step_in_the_chain() {
    // Setup mocks
    Action.Simple nextActionInChainMock = mock(Action.Simple.class);
    ApplicantModel applicant = createApplicantWithMockedProfile();

    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/applicant/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format(
                "/programs/%d/applicant/%d/blocks/2/edit", activeProgram.id, applicant.id));

    // The action to test
    ApplicantAuthorizationAction action = instanceOf(ApplicantAuthorizationAction.class);

    // Configure a fake secondary action to serve as a stand in for the last action in the chain
    // (i.e. controller)
    when(nextActionInChainMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = nextActionInChainMock;

    // Result from running the action
    Result result = action.call(request).toCompletableFuture().join();

    // All the assertions and verifications
    assertThat(result).isNull();
  }

  @Test
  public void when_applicant_auth_passes_and_program_auth_fails_redirects_to_home()
      throws ExecutionException, InterruptedException {
    // Setup mocks
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/applicants/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format(
                "/programs/%d/applicants/%d/blocks/2/edit", draftProgram.id, applicant.id));

    // The action to test
    ApplicantAuthorizationAction action = instanceOf(ApplicantAuthorizationAction.class);

    // Result from running the action
    Result result = action.call(request).toCompletableFuture().get();

    // All the assertions and verifications
    assertThat(result).isNotNull();
    assertThat(result.redirectLocation().isPresent()).isTrue();
    assertThat(result.redirectLocation().get())
        .isEqualTo(controllers.routes.HomeController.index().url());
  }

  @Test
  public void when_applicant_auth_fails_redirect_home()
      throws ExecutionException, InterruptedException {
    // Setup mocks
    long applicantId = 20000L;
    long programId = 100000L;

    CiviFormProfile profile = mock(CiviFormProfile.class);
    when(profile.checkAuthorization(applicantId))
        .thenReturn(
            CompletableFuture.runAsync(
                () -> {
                  throw new SecurityException();
                }));

    ProfileUtils profileUtils = Mockito.mock(ProfileUtils.class);
    when(profileUtils.getApplicantId(any(Http.Request.class))).thenReturn(Optional.of(applicantId));
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(profile));

    // Setup fakeRequest and configure it to use the specified route pattern
    Request request =
        createFakeRequest(
            "/programs/$programId<[^/]+>/applicants/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit",
            String.format("/programs/%d/applicants/%d/blocks/2/edit", programId, applicantId));

    // The action to test
    ApplicantAuthorizationAction action =
        new ApplicantAuthorizationAction(profileUtils, instanceOf(VersionRepository.class));

    // Result from running the action
    Result result = action.call(request).toCompletableFuture().get();

    // All the assertions and verifications
    assertThat(result).isNotNull();
    assertThat(result.redirectLocation().isPresent()).isTrue();
    assertThat(result.redirectLocation().get())
        .isEqualTo(controllers.routes.HomeController.index().url());
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

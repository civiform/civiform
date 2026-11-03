package actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static support.FakeRequestBuilder.createHandlerDef;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import play.routing.Router;

public class ApplicantAccessActionTest {

  private static final String ROUTE_PATTERN =
      "/applicants/$applicantId<[^/]+>/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/hx/selectFileForUpload";

  private ProfileUtils profileUtils;
  private CiviFormProfile profile;
  private Action.Simple delegate;
  private ApplicantAccessAction action;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    profile = mock(CiviFormProfile.class);
    delegate = mock(Action.Simple.class);
    action = new ApplicantAccessAction(profileUtils);
    action.delegate = delegate;
  }

  private Request requestForApplicant(long applicantId) {
    return fakeRequestBuilder()
        .location(
            "POST", "/applicants/" + applicantId + "/programs/3/blocks/1/hx/selectFileForUpload")
        .build()
        .addAttr(
            Router.Attrs.HANDLER_DEF, createHandlerDef(getClass().getClassLoader(), ROUTE_PATTERN));
  }

  @Test
  public void call_userMayAccessApplicant_callsDelegate() {
    Request request = requestForApplicant(42);
    when(profileUtils.currentUserProfile(request)).thenReturn(profile);
    when(profile.checkAuthorization(42L)).thenReturn(CompletableFuture.completedFuture(null));
    when(delegate.call(request)).thenReturn(CompletableFuture.completedFuture(Results.ok()));

    Result result = action.call(request).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    verify(profile).checkAuthorization(42L);
  }

  @Test
  public void call_userMayNotAccessApplicant_returnsUnauthorizedWithoutCallingDelegate() {
    Request request = requestForApplicant(42);
    when(profileUtils.currentUserProfile(request)).thenReturn(profile);
    when(profile.checkAuthorization(anyLong()))
        .thenReturn(
            CompletableFuture.failedFuture(new CompletionException(new SecurityException())));

    Result result = action.call(request).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
    verifyNoInteractions(delegate);
  }

  @Test
  public void call_authorizationCheckFailsUnexpectedly_propagatesTheFailure() {
    Request request = requestForApplicant(42);
    when(profileUtils.currentUserProfile(request)).thenReturn(profile);
    when(profile.checkAuthorization(anyLong()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));

    assertThatThrownBy(() -> action.call(request).toCompletableFuture().join())
        .hasRootCauseInstanceOf(IllegalStateException.class);
    verifyNoInteractions(delegate);
  }
}

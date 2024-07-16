package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class CiviFormProfileFilterTest extends WithApplication {
  @Test
  public void testProfileIsCreatedForUserRoute() throws Exception {
    ProfileUtils profileUtils = instanceOf(ProfileUtils.class);
    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, profileUtils);

    Http.RequestBuilder request = fakeRequestBuilder();

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();

    // Since the session for the request did not have a profile, we expect to be
    // redirected to the GuestClient to get a user profile. The original URL should
    // be stored in the "redirectTo" session key.
    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/callback?client_name=GuestClient");
    assertThat(result.session().get("redirectTo")).hasValue("/");
  }

  @Test
  public void testProfileIsNotCreatedForNonUserRoute() throws Exception {
    ProfileUtils profileUtils = instanceOf(ProfileUtils.class);
    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, profileUtils);

    // This is not a user-facing request.
    Http.RequestBuilder request = fakeRequest("GET", "/playIndex");

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();

    // Since the request was for a non-user route, we should not get redirected to
    // the GuestClient.
    assertThat(result.status()).isEqualTo(200);
  }
}

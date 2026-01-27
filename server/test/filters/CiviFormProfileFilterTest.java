package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;
import play.test.WithApplication;

public class CiviFormProfileFilterTest extends WithApplication {

  private ProfileUtils profileUtils;
  private Provider<Router> routerProvider;

  @Before
  public void setUp() {
    profileUtils = instanceOf(ProfileUtils.class);
    routerProvider = () -> instanceOf(Router.class);
  }

  @Test
  public void testProfileIsCreatedForUserRoute() throws Exception {
    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, profileUtils, routerProvider);
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1/review");

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
    assertThat(result.session().get("redirectTo")).hasValue("/programs/1/review");
  }

  @Test
  public void testProfileIsNotCreatedForNonUserRoute() throws Exception {
    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, profileUtils, routerProvider);

    // This is not a user-facing request.
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/playIndex");

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

  @Test
  public void testProfileIsNotCreatedForOptionalProfileRoute() throws Exception {
    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, profileUtils, routerProvider);

    // This route may have a profile, but doesn't require one.
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs");

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();

    // Since the request was for a route that may or may not have a profile, we should not get
    // redirected to the GuestClient.
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testProfileIsNotCreatedFor404s() throws Exception {
    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, profileUtils, routerProvider);

    // This route doesn't exist and would result in a 404
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/badroute");

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();

    // Since the request was for a route that doesn't exist, we should not get redirected to
    // the GuestClient.
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testExistingProfile_passedToDownstreamFilters() throws Exception {
    CiviFormProfile mockProfile = mock(CiviFormProfile.class);
    ProfileUtils mockProfileUtils = mock(ProfileUtils.class);
    Http.RequestHeader request =
        fakeRequestBuilder().method("GET").uri("/programs/1/review").build();
    when(mockProfileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));

    CiviFormProfileFilter filter = new CiviFormProfileFilter(mat, mockProfileUtils, routerProvider);

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              // Verify profile is passed via request attributes
              Optional<CiviFormProfile> profileAttr =
                  header.attrs().getOptional(ValidAccountFilter.PROFILE_ATTRIBUTE_KEY);
              if (profileAttr.isPresent() && profileAttr.get() == mockProfile) {
                return CompletableFuture.completedFuture(play.mvc.Results.ok("Profile passed"));
              }
              return CompletableFuture.completedFuture(
                  play.mvc.Results.badRequest("Profile not passed"));
            },
            request);

    Result result = stage.toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }
}

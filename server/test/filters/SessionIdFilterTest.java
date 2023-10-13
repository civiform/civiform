package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.settings.SettingDescription;
import services.settings.SettingMode;
import services.settings.SettingType;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;

public class SessionIdFilterTest extends WithApplication {

  private SettingsManifest createSettingsManifest(boolean oidcLogoutEnabled) {
    return new SettingsManifest(
        ImmutableMap.of(
            "section1",
            SettingsSection.create(
                "section1",
                "description1",
                ImmutableList.of(),
                ImmutableList.of(
                    SettingDescription.create(
                        "ENHANCED_OIDC_LOGOUT_ENABLED",
                        "Enhanced OIDC Logout logic enabled",
                        false,
                        SettingType.BOOLEAN,
                        SettingMode.ADMIN_WRITEABLE)))),
        ConfigFactory.parseMap(ImmutableMap.of("enhanced_oidc_logout_enabled", oidcLogoutEnabled)));
  }

  @Test
  public void testSessionIdIsCreatedForNonExcludedRoute() throws Exception {
    SettingsManifest settingsManifest = createSettingsManifest(true);
    SessionIdFilter filter = new SessionIdFilter(mat, () -> settingsManifest);

    // The request has no session id.
    Http.RequestBuilder request = fakeRequest();
    assertThat(request.session().containsKey(SessionIdFilter.SESSION_ID)).isFalse();

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();
    assertThat(result.session().get(SessionIdFilter.SESSION_ID)).isNotEmpty();
  }

  @Test
  public void testSessionIdIsNotCreatedForExcludedRoute() throws Exception {
    SettingsManifest settingsManifest = createSettingsManifest(true);
    SessionIdFilter filter = new SessionIdFilter(mat, () -> settingsManifest);

    // The request is for an API route and has no session id.
    Http.RequestBuilder request = fakeRequest("GET", "/api/v1/admin");
    assertThat(request.session().containsKey(SessionIdFilter.SESSION_ID)).isFalse();

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();
    // The session has no session id. In fact, it has no session.
    assertThat(result.session()).isNull();
  }

  @Test
  public void testSessionIdIsNotCreatedWhenDisabled() throws Exception {
    SettingsManifest settingsManifest = createSettingsManifest(false);
    SessionIdFilter filter = new SessionIdFilter(mat, () -> settingsManifest);

    // The request has no session id.
    Http.RequestBuilder request = fakeRequest();
    assertThat(request.session().containsKey(SessionIdFilter.SESSION_ID)).isFalse();

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();
    // The session has no session id. In fact, it has no session.
    assertThat(result.session()).isNull();
  }
}

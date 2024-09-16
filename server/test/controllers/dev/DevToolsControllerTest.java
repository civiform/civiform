package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;

import controllers.FlashKey;
import java.util.Optional;
import models.VersionModel;
import org.junit.After;
import org.junit.Test;
import play.Application;
import play.Mode;
import play.cache.NamedCacheImpl;
import play.cache.SyncCacheApi;
import play.inject.BindingKey;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.VersionRepository;

public class DevToolsControllerTest {

  private Optional<Application> maybeApp = Optional.empty();
  private DevToolsController controller;
  private VersionRepository versionRepo;
  private SyncCacheApi programsByVersionCache;

  @After
  public void stopApplication() {
    if (maybeApp.isPresent()) {
      Helpers.stop(maybeApp.get());
      maybeApp = Optional.empty();
    }
  }

  @Test
  public void seedAndClearDatabase_displaysCorrectInformation() {
    setupControllerInMode(Mode.DEV);
    controller.clear();
    // Navigate to index before seeding - should not have the fake program.
    Result result = controller.index(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Seed the fake data.
    result = controller.seedPrograms();
    assertThat(result.redirectLocation()).hasValue(routes.DevToolsController.index().url());
    assertThat(result.flash().get(FlashKey.SUCCESS)).hasValue("The database has been seeded");
    result = controller.index(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Go to seed data display page.
    result = controller.data(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("comprehensive-sample-program");

    // Clear the data.
    result = controller.clear();
    assertThat(result.redirectLocation()).hasValue(routes.DevToolsController.index().url());
    assertThat(result.flash().get(FlashKey.SUCCESS)).hasValue("The database has been cleared");
    result = controller.index(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");
  }

  @Test
  public void seedAndClearCache_clearsCache() {
    setupControllerInMode(Mode.DEV);
    controller.clear();
    // Ensure the cache is clear at the beginning of the test
    assertThat(
            programsByVersionCache
                .get(String.valueOf(versionRepo.getActiveVersion().id))
                .isPresent())
        .isFalse();

    // Navigate to index before seeding - should not have the fake program.
    Result result = controller.index(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Seed the fake data.
    result = controller.seedPrograms();
    assertThat(result.redirectLocation()).hasValue(routes.DevToolsController.index().url());
    assertThat(result.flash().get(FlashKey.SUCCESS)).hasValue("The database has been seeded");
    result = controller.index(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Load the data, which sets the cache and ensure it is present
    VersionModel activeVersion = versionRepo.getActiveVersion();
    String cacheKey = String.valueOf(activeVersion.id);
    assertThat(programsByVersionCache.get(cacheKey).isPresent()).isTrue();

    // Clear the data and ensure the cache is cleared
    controller.clearCache();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(programsByVersionCache.get(cacheKey).isPresent()).isFalse();
  }

  private void setupControllerInMode(Mode mode) {
    maybeApp =
        Optional.of(
            new GuiceApplicationBuilder()
                .in(mode)
                .configure("version_cache_enabled", true)
                .build());
    controller = maybeApp.get().injector().instanceOf(DevToolsController.class);
    BindingKey<SyncCacheApi> versionProgramsKey =
        new BindingKey<>(SyncCacheApi.class).qualifiedWith(new NamedCacheImpl("version-programs"));
    programsByVersionCache = maybeApp.get().injector().instanceOf(versionProgramsKey.asScala());
    versionRepo = maybeApp.get().injector().instanceOf(VersionRepository.class);
  }
}

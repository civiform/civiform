package controllers.dev.seeding;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestNew;

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

public class DevDatabaseSeedControllerTest {

  private Optional<Application> maybeApp = Optional.empty();
  private DevDatabaseSeedController controller;
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
    Result result = controller.index(addCSRFToken(fakeRequest()).build());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Seed the fake data.
    result = controller.seedPrograms();
    assertThat(result.redirectLocation()).hasValue(routes.DevDatabaseSeedController.index().url());
    assertThat(result.flash().get(FlashKey.SUCCESS)).hasValue("The database has been seeded");
    result = controller.index(addCSRFToken(fakeRequest()).build());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Go to seed data display page.
    result = controller.data(addCSRFToken(fakeRequest()).build());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("comprehensive-sample-program");

    // Clear the data.
    result = controller.clear();
    assertThat(result.redirectLocation()).hasValue(routes.DevDatabaseSeedController.index().url());
    assertThat(result.flash().get(FlashKey.SUCCESS)).hasValue("The database has been cleared");
    result = controller.index(addCSRFToken(fakeRequest()).build());
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
    Result result = controller.index(addCSRFToken(fakeRequest()).build());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("comprehensive-sample-program");

    // Seed the fake data.
    result = controller.seedPrograms();
    assertThat(result.redirectLocation()).hasValue(routes.DevDatabaseSeedController.index().url());
    assertThat(result.flash().get(FlashKey.SUCCESS)).hasValue("The database has been seeded");
    result = controller.index(addCSRFToken(fakeRequest()).build());
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

  @Test
  public void index_inNonDevMode_returnsNotFound() {
    setupControllerInMode(Mode.TEST);
    Result result = controller.index(fakeRequestNew());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void data_inNonDevMode_returnsNotFound() {

    setupControllerInMode(Mode.TEST);
    Result result = controller.data(fakeRequestNew());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void seedPrograms_inNonDevMode_returnsNotFound() {
    setupControllerInMode(Mode.TEST);
    Result result = controller.seedPrograms();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void clear_inNonDevMode_returnsNotFound() {
    setupControllerInMode(Mode.TEST);
    Result result = controller.clear();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  private void setupControllerInMode(Mode mode) {
    maybeApp =
        Optional.of(
            new GuiceApplicationBuilder()
                .in(mode)
                .configure("version_cache_enabled", true)
                .build());
    controller = maybeApp.get().injector().instanceOf(DevDatabaseSeedController.class);
    BindingKey<SyncCacheApi> versionProgramsKey =
        new BindingKey<>(SyncCacheApi.class).qualifiedWith(new NamedCacheImpl("version-programs"));
    programsByVersionCache = maybeApp.get().injector().instanceOf(versionProgramsKey.asScala());
    versionRepo = maybeApp.get().injector().instanceOf(VersionRepository.class);
  }
}

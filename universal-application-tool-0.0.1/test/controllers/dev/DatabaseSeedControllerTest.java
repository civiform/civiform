package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import org.junit.Before;
import org.junit.Test;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Result;
import repository.WithPostgresContainer;
import support.TestConstants;

public class DatabaseSeedControllerTest extends WithPostgresContainer {

  private DatabaseSeedController controller;

  @Before
  public void setupController() {
    controller = createControllerInMode(Mode.DEV);
  }

  @Test
  public void seedAndClearDatabase_displaysCorrectInformation() {
    // Navigate to index before seeding - should not have the fake program.
    Result result = controller.index(addCSRFToken(fakeRequest()).build());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("Mock program");

    // Seed the fake data.
    result = controller.seed();
    assertThat(result.redirectLocation()).hasValue(routes.DatabaseSeedController.index().url());
    assertThat(result.flash().get("success")).hasValue("The database has been seeded");

    // Clear the data.
    result = controller.clear();
    assertThat(result.redirectLocation()).hasValue(routes.DatabaseSeedController.index().url());
    assertThat(result.flash().get("success")).hasValue("The database has been cleared");
  }

  @Test
  public void index_inNonDevMode_returnsNotFound() {
    DatabaseSeedController controller = createControllerInMode(Mode.TEST);
    Result result = controller.index(fakeRequest().build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void seed_inNonDevMode_returnsNotFound() {
    DatabaseSeedController controller = createControllerInMode(Mode.TEST);
    Result result = controller.seed();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void clear_inNonDevMode_returnsNotFound() {
    DatabaseSeedController controller = createControllerInMode(Mode.TEST);
    Result result = controller.clear();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  private DatabaseSeedController createControllerInMode(Mode mode) {
    return new GuiceApplicationBuilder()
        .in(mode)
        .build()
        .injector()
        .instanceOf(DatabaseSeedController.class);
  }
}

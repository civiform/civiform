package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeRequest;

import org.junit.Before;
import org.junit.Test;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import repository.ResetPostgres;

public class FeatureFlagOverrideControllerTest extends ResetPostgres {
  private FeatureFlagOverrideController controller;

  @Before
  public void setupController() {
    controller = createControllerInMode(Mode.DEV);
  }

  @Test
  public void disable_nonDevMode_fails() {
    controller = createControllerInMode(Mode.TEST);
    var result = controller.disable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void disable() {
    var result = controller.disable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void enable_nonDevMode_fails() {
    controller = createControllerInMode(Mode.TEST);
    var result = controller.enable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void enable() {
    var result = controller.enable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(OK);
  }

  private FeatureFlagOverrideController createControllerInMode(Mode mode) {
    return new GuiceApplicationBuilder()
        .in(mode)
        .build()
        .injector()
        .instanceOf(FeatureFlagOverrideController.class);
  }
}

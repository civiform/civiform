package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeRequest;

import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import play.Application;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

public class FeatureFlagOverrideControllerTest {
  private Optional<Application> maybeApp = Optional.empty();
  private FeatureFlagOverrideController controller;

  @After
  public void stopApplication() {
    if (maybeApp.isPresent()) {
      Helpers.stop(maybeApp.get());
      maybeApp = Optional.empty();
    }
  }

  @Test
  public void disable_nonDevMode_fails() {
    setupControllerInMode(Mode.TEST);
    var result = controller.disable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void disable() {
    setupControllerInMode(Mode.DEV);
    var result = controller.disable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void enable_nonDevMode_fails() {
    setupControllerInMode(Mode.TEST);
    var result = controller.enable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void enable() {
    setupControllerInMode(Mode.DEV);
    var result = controller.enable(fakeRequest().build(), "flag");
    assertThat(result.status()).isEqualTo(OK);
  }

  private void setupControllerInMode(Mode mode) {
    maybeApp = Optional.of(new GuiceApplicationBuilder().in(mode).build());
    controller = maybeApp.get().injector().instanceOf(FeatureFlagOverrideController.class);
  }
}

package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.settings.SettingsService;

public class FeatureFlagOverrideControllerTest extends ResetPostgres {

  private static final String FLAG_NAME = "ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS";
  private FeatureFlagOverrideController controller;

  private SettingsService settingsService;

  @Before
  public void setUp() {
    settingsService = instanceOf(SettingsService.class);
    controller = instanceOf(FeatureFlagOverrideController.class);
  }

  @Test
  public void disable() {
    // Setup
    controller = new FeatureFlagOverrideController(instanceOf(SettingsService.class));

    // Execute
    var result = controller.disable(fakeRequest(), FLAG_NAME);

    // Verify
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(getSettings().get(FLAG_NAME)).isEqualTo("false");
  }

  @Test
  public void enable() {
    // Setup
    controller = new FeatureFlagOverrideController(instanceOf(SettingsService.class));

    // Execute
    var result = controller.enable(fakeRequest(), FLAG_NAME);

    // Verify
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(getSettings().get(FLAG_NAME)).isEqualTo("true");
  }

  private ImmutableMap<String, String> getSettings() {
    return settingsService.loadSettings().toCompletableFuture().join().get();
  }
}

package actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.settings.SettingsManifest;

public class DemoModeDisabledActionTest extends ResetPostgres {

  private SettingsManifest mockSettingsManifest;

  @Before
  public void setUp() {
    mockSettingsManifest = mock(SettingsManifest.class);
  }

  @Test
  public void testDemoModeEnabled_proceedsToAction() {
    Request request = fakeRequestBuilder().build();
    when(mockSettingsManifest.getStagingDisableDemoModeLogins()).thenReturn(false);

    DemoModeDisabledAction action = new DemoModeDisabledAction(mockSettingsManifest);

    // Set up a mock for the delegate action
    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;
    Result result = action.call(request).toCompletableFuture().join();

    // Verify that the delegate action was called
    assertNull(result);
  }

  @Test
  public void testDemoModeDisabled_redirectsToHomePage() {
    Request request = fakeRequestBuilder().build();
    when(mockSettingsManifest.getStagingDisableDemoModeLogins()).thenReturn(true);

    DemoModeDisabledAction action = new DemoModeDisabledAction(mockSettingsManifest);

    Result result = action.call(request).toCompletableFuture().join();
    assertEquals(result.redirectLocation().get(), controllers.routes.HomeController.index().url());
  }
}

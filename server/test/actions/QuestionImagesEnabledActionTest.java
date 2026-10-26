package actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import services.settings.SettingsManifest;

public class QuestionImagesEnabledActionTest {

  private SettingsManifest settingsManifest;
  private Action.Simple delegate;
  private QuestionImagesEnabledAction action;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    delegate = mock(Action.Simple.class);
    action = new QuestionImagesEnabledAction(settingsManifest);
    action.delegate = delegate;
  }

  @Test
  public void call_featureEnabled_callsDelegate() {
    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getImagesInQuestionFeatureEnabled(request)).thenReturn(true);
    when(delegate.call(request)).thenReturn(CompletableFuture.completedFuture(Results.ok()));

    Result result = action.call(request).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void call_featureDisabled_returnsNotFoundWithoutCallingDelegate() {
    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getImagesInQuestionFeatureEnabled(request)).thenReturn(false);

    Result result = action.call(request).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    verifyNoInteractions(delegate);
  }
}

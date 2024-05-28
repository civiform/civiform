package actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import controllers.routes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import models.DisplayMode;
import models.ProgramModel;
import org.junit.Test;
import play.inject.Injector;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;
import repository.VersionRepository;
import services.program.BlockDefinition;
import services.program.ProgramService;
import services.program.ProgramType;

public class BlockDisabledProgramActionTest extends WithApplication {
  private ProgramService ps;
  private static Injector injector;
  private static final BlockDefinition EMPTY_FIRST_BLOCK =
      BlockDefinition.builder()
          .setId(1)
          .setName("Screen 1")
          .setDescription("Screen 1 description")
          .build();

  @Test
  public void testProgramSlugIsNotAvailable() {
    ProgramService programService = instanceOf(ProgramService.class);
    BlockDisabledProgramAction action = new BlockDisabledProgramAction(programService);
    Request request = Helpers.fakeRequest().build();

    // Set up a mock for the delegate action
    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;
    Result result = action.call(request).toCompletableFuture().join();

    // Verify that the delegate action was called
    assertEquals(null, result);
  }

  @Test
  public void testDisabledProgram() {
    ProgramService programService = instanceOf(ProgramService.class);
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    BlockDisabledProgramAction action = new BlockDisabledProgramAction(programService);

    Map<String, String> flashData = new HashMap<>();
    flashData.put("redirected-from-program-slug", "disabledprogram1");
    Request request = Helpers.fakeRequest().flash(flashData).build();
    ProgramModel program =
        new ProgramModel(
            /* adminName */ "disabledprogram1",
            /* adminDescription */ "description for a disabled program ",
            /* defaultDisplayName */ "disabled program",
            /* defaultDisplayDescription */ "description",
            /* defaultConfirmationMessage */ "",
            /* externalLink */ "",
            /* displayMode */ DisplayMode.DISABLED.getValue(),
            /* blockDefinitions */ ImmutableList.of(EMPTY_FIRST_BLOCK),
            /* associatedVersion */ versionRepository.getActiveVersion(),
            /* programType */ ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            /* ProgramAcls */ new ProgramAcls());
    program.save();

    Result result = action.call(request).toCompletableFuture().join();
    assertEquals(result.redirectLocation().get(), routes.HomeController.index().url());
  }

  @Test
  public void testNonDisabledProgram() {
    ProgramService programService = instanceOf(ProgramService.class);
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    BlockDisabledProgramAction action = new BlockDisabledProgramAction(programService);
    Map<String, String> flashData = new HashMap<>();
    flashData.put("redirected-from-program-slug", "publicprogram1");
    Request request = Helpers.fakeRequest().flash(flashData).build();
    ProgramModel program =
        new ProgramModel(
            /* adminName */ "publicprogram1",
            /* adminDescription */ "description for a public visibile program",
            /* defaultDisplayName */ "public program",
            /* defaultDisplayDescription */ "description",
            /* defaultConfirmationMessage */ "",
            /* externalLink */ "",
            /* displayMode */ DisplayMode.PUBLIC.getValue(),
            /* blockDefinitions */ ImmutableList.of(EMPTY_FIRST_BLOCK),
            /* associatedVersion */ versionRepository.getActiveVersion(),
            /* programType */ ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            /* ProgramAcls */ new ProgramAcls());
    program.save();

    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;

    Result result = action.call(request).toCompletableFuture().join();
    assertEquals(null, result);
  }
}

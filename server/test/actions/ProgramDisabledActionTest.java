package actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.createHandlerDef;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import controllers.FlashKey;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import models.ApplicationStep;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.VersionModel;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.routing.Router;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramService;
import services.program.ProgramType;
import services.settings.SettingsManifest;

public class ProgramDisabledActionTest extends ResetPostgres {
  private ProgramModel createProgram(DisplayMode displayMode, LifecycleStage lifecycleStage) {
    BlockDefinition emptyFirstBlock =
        BlockDefinition.builder()
            .setId(1)
            .setName("Screen 1")
            .setDescription("Screen 1 description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen 1"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen 1 description"))
            .build();

    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    VersionModel version =
        lifecycleStage == LifecycleStage.DRAFT
            ? versionRepository.getDraftVersionOrCreate()
            : versionRepository.getActiveVersion();

    ProgramModel program =
        new ProgramModel(
            /* adminName */ String.format("%s-program1", displayMode),
            /* adminDescription */ "admin-description",
            /* defaultDisplayName */ "admin-name",
            /* defaultDisplayDescription */ "description",
            /* defaultShortDescription */ "short description",
            /* defaultConfirmationMessage */ "",
            /* externalLink */ "",
            /* displayMode */ displayMode.getValue(),
            /* notificationPreferences */ ImmutableList.of(),
            /* blockDefinitions */ ImmutableList.of(emptyFirstBlock),
            /* associatedVersion */ version,
            /* programType */ ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            /* ProgramAcls */ new ProgramAcls(),
            /* categories */ ImmutableList.of(),
            /* applicationSteps */ ImmutableList.of(new ApplicationStep("title", "description")));
    program.save();

    return program;
  }

  // Fast Forward Disabled
  @Test
  public void testProgramSlugIsNotAvailable_whenFastForwardIsDisabled() {
    Request request = fakeRequestBuilder().build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(false);

    ProgramService programService = instanceOf(ProgramService.class);
    ProgramDisabledAction action = new ProgramDisabledAction(programService, mockSettingsManifest);

    // Set up a mock for the delegate action
    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;
    Result result = action.call(request).toCompletableFuture().join();

    // Verify that the delegate action was called
    assertNull(result);
  }

  @Test
  public void testProgramOnlyHasDraftVersion_whenFastForwardIsDisabled() {
    ProgramModel program = createProgram(DisplayMode.DISABLED, LifecycleStage.DRAFT);

    Request request =
        fakeRequestBuilder()
            .flash(Map.of(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, program.getSlug()))
            .build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(false);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    // Set up a mock for the delegate action
    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;
    Result result = action.call(request).toCompletableFuture().join();

    // Verify that the delegate action was called
    assertNull(result);
  }

  @Test
  public void testDisabledProgramFromFlashKey_whenFastForwardIsDisabled() {
    ProgramModel program = createProgram(DisplayMode.DISABLED, LifecycleStage.ACTIVE);

    Request request =
        fakeRequestBuilder()
            .flash(Map.of(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, program.getSlug()))
            .build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(false);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    Result result = action.call(request).toCompletableFuture().join();
    assertEquals(
        result.redirectLocation().get(),
        controllers.applicant.routes.ApplicantProgramsController.showInfoDisabledProgram(
                program.getSlug())
            .url());
  }

  @Test
  public void testDisabledProgramFromUriPathProgramId_whenFastForwardIsDisabled() {
    ProgramModel program = createProgram(DisplayMode.PUBLIC, LifecycleStage.ACTIVE);

    var routePattern =
        "/programs/$programId<[^/]+>/applicant/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    var path = String.format("/programs/%d/applicant/a/blocks/2/edit", program.id);

    Request request =
        fakeRequestBuilder()
            .location("GET", path)
            .build()
            .addAttr(
                Router.Attrs.HANDLER_DEF,
                createHandlerDef(getClass().getClassLoader(), routePattern));

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(false);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;

    Result result = action.call(request).toCompletableFuture().join();

    assertNull(result);
  }

  @Test
  public void testNonDisabledProgram_whenFastForwardIsDisabled() {
    ProgramModel program = createProgram(DisplayMode.PUBLIC, LifecycleStage.ACTIVE);

    Request request =
        fakeRequestBuilder()
            .flash(Map.of(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, program.getSlug()))
            .build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(false);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;

    Result result = action.call(request).toCompletableFuture().join();

    assertNull(result);
  }

  // Fast Forward Enabled
  @Test
  public void testProgramSlugIsNotAvailable_whenFastForwardIsEnabled() {
    Request request = fakeRequestBuilder().build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(true);

    ProgramService programService = instanceOf(ProgramService.class);
    ProgramDisabledAction action = new ProgramDisabledAction(programService, mockSettingsManifest);

    // Set up a mock for the delegate action
    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;
    Result result = action.call(request).toCompletableFuture().join();

    // Verify that the delegate action was called
    assertNull(result);
  }

  @Test
  public void testProgramOnlyHasDraftVersion_whenFastForwardIsEnabled() {
    ProgramModel program = createProgram(DisplayMode.DISABLED, LifecycleStage.DRAFT);

    Request request =
        fakeRequestBuilder()
            .flash(Map.of(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, program.getSlug()))
            .build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(true);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    // Set up a mock for the delegate action
    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;
    Result result = action.call(request).toCompletableFuture().join();

    // Verify that the delegate action was called
    assertNull(result);
  }

  @Test
  public void testDisabledProgramFromFlashKey_whenFastForwardIsEnabled() {
    ProgramModel program = createProgram(DisplayMode.DISABLED, LifecycleStage.ACTIVE);

    Request request =
        fakeRequestBuilder()
            .flash(Map.of(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, program.getSlug()))
            .build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(true);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    Result result = action.call(request).toCompletableFuture().join();
    assertEquals(
        result.redirectLocation().get(),
        controllers.applicant.routes.ApplicantProgramsController.showInfoDisabledProgram(
                program.getSlug())
            .url());
  }

  @Test
  public void testDisabledProgramFromUriPathProgramId_whenFastForwardIsEnabled() {
    ProgramModel program = createProgram(DisplayMode.DISABLED, LifecycleStage.ACTIVE);

    var routePattern =
        "/programs/$programId<[^/]+>/applicant/$applicantId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    var path = String.format("/programs/%d/applicant/a/blocks/2/edit", program.id);

    Request request =
        fakeRequestBuilder()
            .location("GET", path)
            .build()
            .addAttr(
                Router.Attrs.HANDLER_DEF,
                createHandlerDef(getClass().getClassLoader(), routePattern));

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(true);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    Result result = action.call(request).toCompletableFuture().join();
    assertEquals(
        result.redirectLocation().get(),
        controllers.applicant.routes.ApplicantProgramsController.showInfoDisabledProgram(
                program.getSlug())
            .url());
  }

  @Test
  public void testNonDisabledProgramFromFlashKey_whenFastForwardIsEnabled() {
    ProgramModel program = createProgram(DisplayMode.PUBLIC, LifecycleStage.ACTIVE);

    Request request =
        fakeRequestBuilder()
            .flash(Map.of(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, program.getSlug()))
            .build();

    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getFastforwardEnabled(request)).thenReturn(true);

    ProgramDisabledAction action =
        new ProgramDisabledAction(instanceOf(ProgramService.class), mockSettingsManifest);

    Action.Simple delegateMock = mock(Action.Simple.class);
    when(delegateMock.call(request)).thenReturn(CompletableFuture.completedFuture(null));
    action.delegate = delegateMock;

    Result result = action.call(request).toCompletableFuture().join();

    assertNull(result);
  }
}

package controllers.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.docs.ApiDocsService;
import services.program.ProgramType;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.docs.ApiDocsPageView;
import views.shared.LayoutDeps;

public class ApiDocsControllerTest extends ResetPostgres {

  private static final String PROGRAM_OR_VERSION_NOT_FOUND_ERROR = "No programs found";

  private ApiDocsController controller;

  @Before
  public void setUp() {
    resetTables();

    controller =
        new ApiDocsController(
            instanceOf(SettingsManifest.class),
            new ApiDocsPageView(layoutDepsWithMockProfile()),
            instanceOf(ApiDocsService.class),
            instanceOf(MessagesApi.class));

    // Create one program in the system
    ProgramBuilder.newActiveProgram("Test Program 1").buildDefinition();
  }

  @Test
  public void index_redirectsToArbitraryProgramActiveDocs() {
    Request request = fakeRequest();
    Result result = controller.index(request);

    // SEE_OTHER is the redirect code
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .isEqualTo(Optional.of("/docs/api/programs/test-program-1/active"));
  }

  @Test
  public void index_externalProgramOnly_notFound() {
    resetTables();
    ProgramBuilder.newActiveProgram("Test External Program 1")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Request request = fakeRequest();
    Result result = controller.index(request);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("No programs found");
  }

  @Test
  public void activeDocsForSlug_programExists() {
    Request request = fakeRequest();
    Result result = controller.activeDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void activeDocsForSlug_programDoesNotExist() {
    Request request = fakeRequest();
    Result result = controller.activeDocsForSlug(request, "non-existent-program");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void activeDocsForSlug_externalProgram_doesNotExist() {
    ProgramBuilder.newActiveProgram("Test External Program")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();
    Request request = fakeRequest();
    Result result = controller.activeDocsForSlug(request, "test-external-program");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void draftDocsForSlug_noDraftAvailable() {
    Request request = fakeRequest();
    Result result = controller.draftDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void draftDocsForSlug_draftAvailable() {
    ProgramBuilder.newDraftProgram("Test Program 1").buildDefinition();

    Request request = fakeRequest();
    Result result = controller.draftDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  /**
   * Builds a {@link LayoutDeps} whose {@link ProfileUtils} returns a mocked profile. The Thymeleaf
   * page views render the admin header from the current user's profile, which a fake request does
   * not carry.
   */
  private LayoutDeps layoutDepsWithMockProfile() {
    ProfileUtils profileUtils = mock(ProfileUtils.class);
    when(profileUtils.currentUserProfile(any())).thenReturn(mock(CiviFormProfile.class));

    LayoutDeps injectedLayoutDeps = instanceOf(LayoutDeps.class);
    return new LayoutDeps(
        injectedLayoutDeps.baseViewDeps(),
        injectedLayoutDeps.bundledAssetsFinder(),
        profileUtils,
        injectedLayoutDeps.deploymentType(),
        injectedLayoutDeps.clock());
  }
}

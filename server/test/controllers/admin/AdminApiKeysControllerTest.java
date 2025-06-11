package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Content;
import repository.ProgramRepository;
import repository.VersionRepository;
import repository.ResetPostgres;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import services.program.ProgramType;
import support.ProgramBuilder;
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOneView;

public class AdminApiKeysControllerTest extends ResetPostgres {

  private ProgramRepository programRepository;
  private VersionRepository versionRepository;
  private ApiKeyNewOneView apiKeyNewOneView;
  private ProfileUtils profileUtils;
  private FormFactory formFactory;
  private ProgramService programService;
  private AdminApiKeysController controller;

  @Before
  public void setUp() {
    resetTables();

    programRepository = mock(ProgramRepository.class);
    versionRepository = mock(VersionRepository.class);
    apiKeyNewOneView = mock(ApiKeyNewOneView.class);
    profileUtils = mock(ProfileUtils.class);
    formFactory = mock(FormFactory.class);
    programService = mock(ProgramService.class);

    ApiKeyService apiKeyService = mock(ApiKeyService.class);
    ApiKeyIndexView apiKeyIndexView = mock(ApiKeyIndexView.class);
    ApiKeyCredentialsView apiKeyCredentialsView = mock(ApiKeyCredentialsView.class);

    controller =
        new AdminApiKeysController(
            apiKeyService,
            apiKeyIndexView,
            apiKeyNewOneView,
            apiKeyCredentialsView,
            programService,
            formFactory,
            profileUtils,
            versionRepository,
            programRepository);
  }

  @Test
  public void newOne_rendersInternalProgramsOnly() {
    ProgramBuilder.newActiveProgram("Internal One").buildDefinition();
    ProgramBuilder.newActiveProgram("Internal Two").buildDefinition();
    ProgramBuilder.newActiveProgram("External Program")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    when(programRepository.getAllNonExternalProgramNames())
        .thenReturn(ImmutableSet.of("Internal One", "Internal Two"));

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("Mocked content body");
    when(apiKeyNewOneView.render(any(Http.Request.class), eq(ImmutableSet.of("Internal One", "Internal Two"))))
        .thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).render(any(Http.Request.class), eq(ImmutableSet.of("Internal One", "Internal Two")));
    verify(apiKeyNewOneView, never()).renderNoPrograms(any());
  }

  @Test
  public void newOne_rendersNoProgramsIfAllAreExternal() {
    ProgramBuilder.newActiveProgram("External 1")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();
    ProgramBuilder.newActiveProgram("External 2")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    when(programRepository.getAllNonExternalProgramNames()).thenReturn(ImmutableSet.of());

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("Mocked content body");
    when(apiKeyNewOneView.renderNoPrograms(any(Http.Request.class))).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).renderNoPrograms(any(Http.Request.class));
    verify(apiKeyNewOneView, never()).render(any(), any());
  }

  @Test
  public void newOne_rendersAllInternalIfNoneAreExternal() {
    ProgramBuilder.newActiveProgram("Alpha").buildDefinition();
    ProgramBuilder.newActiveProgram("Beta").buildDefinition();

    when(programRepository.getAllNonExternalProgramNames())
        .thenReturn(ImmutableSet.of("Alpha", "Beta"));

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("Mocked content body");
    when(apiKeyNewOneView.render(any(Http.Request.class), eq(ImmutableSet.of("Alpha", "Beta"))))
        .thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).render(any(Http.Request.class), eq(ImmutableSet.of("Alpha", "Beta")));
    verify(apiKeyNewOneView, never()).renderNoPrograms(any());
  }
}

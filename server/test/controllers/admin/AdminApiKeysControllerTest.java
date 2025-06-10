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
import play.Application;
import play.data.FormFactory;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import play.twirl.api.Content;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOneView;

public class AdminApiKeysControllerTest extends WithApplication {

  private ApiKeyService apiKeyService;
  private ApiKeyIndexView indexView;
  private ApiKeyNewOneView apiKeyNewOneView;
  private ApiKeyCredentialsView apiKeyCredentialsView;
  private ProgramService programService;
  private ProgramRepository programRepository;
  private FormFactory formFactory;
  private ProfileUtils profileUtils;
  private VersionRepository versionRepository;
  private AdminApiKeysController controller;

  @Override
  protected Application provideApplication() {
    return new GuiceApplicationBuilder().build();
  }

  @Before
  public void setUp() {

    apiKeyService = mock(ApiKeyService.class);
    indexView = mock(ApiKeyIndexView.class);
    apiKeyNewOneView = mock(ApiKeyNewOneView.class);
    apiKeyCredentialsView = mock(ApiKeyCredentialsView.class);
    programService = mock(ProgramService.class);
    programRepository = mock(ProgramRepository.class);
    formFactory = mock(FormFactory.class);
    profileUtils = mock(ProfileUtils.class);
    versionRepository = mock(VersionRepository.class);

    controller =
        new AdminApiKeysController(
            apiKeyService,
            indexView,
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
    ImmutableSet<String> internalPrograms = ImmutableSet.of("Internal One", "Internal Two");

    when(programRepository.getAllNonExternalProgramNames()).thenReturn(internalPrograms);

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("Mocked content body");

    when(apiKeyNewOneView.render(any(Http.Request.class), eq(internalPrograms)))
        .thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).render(any(Http.Request.class), eq(internalPrograms));
    verify(apiKeyNewOneView, never()).renderNoPrograms(any());
  }

  @Test
  public void newOne_rendersNoProgramsIfAllAreExternal() {
    when(programRepository.getAllNonExternalProgramNames()).thenReturn(ImmutableSet.of());

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("Mocked content body");

    when(apiKeyNewOneView.renderNoPrograms(any(Http.Request.class))).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).renderNoPrograms(any(Http.Request.class));
    verify(apiKeyNewOneView, never()).render(any(), any());
  }
}

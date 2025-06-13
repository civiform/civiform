package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Content;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import services.program.ProgramType;
import support.ProgramBuilder;
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOneView;

public class AdminApiKeysControllerTest extends ResetPostgres {

  private ApiKeyService apiKeyService;
  private ApiKeyIndexView apiKeyIndexView;
  private ApiKeyNewOneView apiKeyNewOneView;
  private ApiKeyCredentialsView apiKeyCredentialsView;
  private AdminApiKeysController controller;

  @Before
  public void setUp() {
    resetTables();

    apiKeyService = mock(ApiKeyService.class);
    apiKeyIndexView = mock(ApiKeyIndexView.class);
    apiKeyNewOneView = mock(ApiKeyNewOneView.class);
    apiKeyCredentialsView = mock(ApiKeyCredentialsView.class);

    controller =
        new AdminApiKeysController(
            apiKeyService,
            apiKeyIndexView,
            apiKeyNewOneView,
            apiKeyCredentialsView,
            instanceOf(ProgramService.class),
            instanceOf(play.data.FormFactory.class),
            instanceOf(auth.ProfileUtils.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class));
  }

  @Test
  public void newOne_rendersInternalProgramsOnly() {
    ProgramBuilder.newActiveProgram("Internal One").buildDefinition();
    ProgramBuilder.newActiveProgram("Internal Two").buildDefinition();
    ProgramBuilder.newActiveProgram("External Program")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("mocked content");
    when(apiKeyNewOneView.render(any(Http.Request.class), any())).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView)
        .render(any(Http.Request.class), eq(ImmutableSet.of("Internal One", "Internal Two")));
  }

  @Test
  public void newOne_rendersNoProgramsIfAllAreExternal() {
    ProgramBuilder.newActiveProgram("External 1")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();
    ProgramBuilder.newActiveProgram("External 2")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("mocked content");
    when(apiKeyNewOneView.renderNoPrograms(any(Http.Request.class))).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).renderNoPrograms(any(Http.Request.class));
  }

  @Test
  public void newOne_rendersAllInternalIfNoneAreExternal() {
    ProgramBuilder.newActiveProgram("Alpha").buildDefinition();
    ProgramBuilder.newActiveProgram("Beta").buildDefinition();

    Content mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("mocked content");
    when(apiKeyNewOneView.render(any(Http.Request.class), any())).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).render(any(Http.Request.class), eq(ImmutableSet.of("Alpha", "Beta")));
  }
}

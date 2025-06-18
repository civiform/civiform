package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
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

  private static final String INTERNAL_PROGRAM_ONE = "Internal One";
  private static final String INTERNAL_PROGRAM_TWO = "Internal Two";
  private static final String EXTERNAL_PROGRAM = "External Program";

  private ApiKeyNewOneView apiKeyNewOneView;
  private AdminApiKeysController controller;
  private ProfileUtils profileUtils;
  private Content mockContent;

  @Before
  public void setUp() {
    resetTables();

    apiKeyNewOneView = mock(ApiKeyNewOneView.class);
    profileUtils = mock(ProfileUtils.class);

    mockContent = mock(Content.class);
    when(mockContent.body()).thenReturn("mocked content");

    CiviFormProfile mockProfile = mock(CiviFormProfile.class);
    when(profileUtils.currentUserProfile(any())).thenReturn(mockProfile);

    controller =
        new AdminApiKeysController(
            instanceOf(ApiKeyService.class),
            instanceOf(ApiKeyIndexView.class),
            apiKeyNewOneView,
            instanceOf(ApiKeyCredentialsView.class),
            instanceOf(ProgramService.class),
            instanceOf(play.data.FormFactory.class),
            profileUtils,
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class));
  }

  @Test
  public void newOne_rendersInternalProgramsOnly() {
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_ONE).buildDefinition();
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_TWO).buildDefinition();
    ProgramBuilder.newActiveProgram(EXTERNAL_PROGRAM)
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    when(apiKeyNewOneView.render(any(Http.Request.class), any())).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView)
        .render(
            any(Http.Request.class),
            eq(ImmutableSet.of(INTERNAL_PROGRAM_ONE, INTERNAL_PROGRAM_TWO)));
  }

  @Test
  public void newOne_rendersNoProgramsIfAllAreExternal() {
    ProgramBuilder.newActiveProgram(EXTERNAL_PROGRAM)
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    when(apiKeyNewOneView.renderNoPrograms(any(Http.Request.class))).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView).renderNoPrograms(any(Http.Request.class));
  }

  @Test
  public void newOne_rendersAllInternalIfNoneAreExternal() {
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_ONE).buildDefinition();
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_TWO).buildDefinition();

    when(apiKeyNewOneView.render(any(Http.Request.class), any())).thenReturn(mockContent);

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    verify(apiKeyNewOneView)
        .render(
            any(Http.Request.class),
            eq(ImmutableSet.of(INTERNAL_PROGRAM_ONE, INTERNAL_PROGRAM_TWO)));
  }

  @Test
  public void create_withValidationErrors_rendersInternalProgramsOnly() {
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_ONE).buildDefinition();
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_TWO).buildDefinition();
    ProgramBuilder.newActiveProgram(EXTERNAL_PROGRAM)
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .method("POST")
            .bodyForm(
                ImmutableMap.of(
                    "name", "",
                    "expiration", "",
                    "subnet", "invalid",
                    "programSlugs", ""));

    when(apiKeyNewOneView.render(any(Http.Request.class), any(), any())).thenReturn(mockContent);

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(400);
    verify(apiKeyNewOneView)
        .render(
            any(Http.Request.class),
            eq(ImmutableSet.of(INTERNAL_PROGRAM_ONE, INTERNAL_PROGRAM_TWO)),
            any());
  }
}

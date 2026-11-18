package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.DateConverter;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import services.program.ProgramType;
import support.ProgramBuilder;
import views.admin.apikeys.ApiKeyCredentialsPageView;
import views.admin.apikeys.ApiKeyIndexPageView;
import views.admin.apikeys.ApiKeyNewOnePageView;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.ProgramCheckbox;

public class AdminApiKeysControllerTest extends ResetPostgres {

  private static final String INTERNAL_PROGRAM_ONE = "Internal One";
  private static final String INTERNAL_PROGRAM_TWO = "Internal Two";
  private static final String EXTERNAL_PROGRAM = "External Program";

  private ApiKeyNewOnePageView apiKeyNewOnePageView;
  private AdminApiKeysController controller;
  private ProfileUtils profileUtils;

  @Before
  public void setUp() {
    resetTables();

    apiKeyNewOnePageView = mock(ApiKeyNewOnePageView.class);
    profileUtils = mock(ProfileUtils.class);

    when(apiKeyNewOnePageView.render(any(Http.Request.class), any())).thenReturn("mocked content");

    CiviFormProfile mockProfile = mock(CiviFormProfile.class);
    when(profileUtils.currentUserProfile(any())).thenReturn(mockProfile);

    controller =
        new AdminApiKeysController(
            instanceOf(ApiKeyService.class),
            instanceOf(ApiKeyIndexPageView.class),
            apiKeyNewOnePageView,
            instanceOf(ApiKeyCredentialsPageView.class),
            instanceOf(ProgramService.class),
            instanceOf(play.data.FormFactory.class),
            profileUtils,
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class),
            instanceOf(DateConverter.class));
  }

  @Test
  public void newOne_rendersInternalProgramsOnly() {
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_ONE).buildDefinition();
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_TWO).buildDefinition();
    ProgramBuilder.newActiveProgram(EXTERNAL_PROGRAM)
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);

    ApiKeyNewOnePageViewModel model = captureNewOnePageModel();
    assertThat(model.isShowForm()).isTrue();
    assertThat(programLabels(model)).containsExactly(INTERNAL_PROGRAM_ONE, INTERNAL_PROGRAM_TWO);
  }

  @Test
  public void newOne_rendersNoProgramsIfAllAreExternal() {
    ProgramBuilder.newActiveProgram(EXTERNAL_PROGRAM)
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);

    ApiKeyNewOnePageViewModel model = captureNewOnePageModel();
    assertThat(model.isShowForm()).isFalse();
    assertThat(model.getProgramCheckboxes()).isEmpty();
  }

  @Test
  public void newOne_rendersAllInternalIfNoneAreExternal() {
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_ONE).buildDefinition();
    ProgramBuilder.newActiveProgram(INTERNAL_PROGRAM_TWO).buildDefinition();

    Result result = controller.newOne(fakeRequest().build());

    assertThat(result.status()).isEqualTo(200);
    assertThat(programLabels(captureNewOnePageModel()))
        .containsExactly(INTERNAL_PROGRAM_ONE, INTERNAL_PROGRAM_TWO);
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

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(400);

    ApiKeyNewOnePageViewModel model = captureNewOnePageModel();
    assertThat(model.isShowForm()).isTrue();
    assertThat(programLabels(model)).containsExactly(INTERNAL_PROGRAM_ONE, INTERNAL_PROGRAM_TWO);
  }

  private ApiKeyNewOnePageViewModel captureNewOnePageModel() {
    ArgumentCaptor<ApiKeyNewOnePageViewModel> captor =
        ArgumentCaptor.forClass(ApiKeyNewOnePageViewModel.class);
    verify(apiKeyNewOnePageView).render(any(Http.Request.class), captor.capture());
    return captor.getValue();
  }

  private static ImmutableList<String> programLabels(ApiKeyNewOnePageViewModel model) {
    return model.getProgramCheckboxes().stream()
        .map(ProgramCheckbox::getLabel)
        .collect(ImmutableList.toImmutableList());
  }
}

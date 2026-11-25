package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import support.cloud.FakePublicStorageClient;
import views.admin.programs.ProgramCardPreview;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramImagePageView;
import views.admin.programs.ProgramImageView;

/** Checks the upload size hint on the program image page, which needs a signed-in admin. */
public class AdminProgramImageControllerHintTest extends WithMockedProfiles {

  private AdminProgramImageController controller;

  @Before
  public void setUp() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();
    controller =
        new AdminProgramImageController(
            new FakePublicStorageClient(),
            instanceOf(ProgramService.class),
            instanceOf(ProgramImageView.class),
            instanceOf(ProgramImagePageView.class),
            instanceOf(ProgramCardPreview.class),
            instanceOf(SettingsManifest.class),
            instanceOf(RequestChecker.class),
            instanceOf(FormFactory.class),
            instanceOf(MessagesApi.class),
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_fileUploadImprovementsEnabled_hintShowsConfiguredMaxSize() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("FILE_UPLOAD_QUESTION_IMPROVEMENTS_ENABLED", "true")
                .method("GET")
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    // FakePublicStorageClient has a 6 MB limit.
    assertThat(contentAsString(result)).contains("The maximum size for image upload is 6MB.");
  }
}

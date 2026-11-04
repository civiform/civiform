package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import services.program.ProgramType;
import support.ProgramBuilder;
import views.admin.programs.ProgramEditStatus;

public class AdminProgramImageControllerExternalProgramTest extends WithMockedProfiles {
  private AdminProgramImageController controller;

  @Before
  public void setUp() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();
    controller = instanceOf(AdminProgramImageController.class);
  }

  @Test
  public void index_externalProgram_fileUploadImprovementsEnabled_rendersPage() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("external program")
            .withProgramType(ProgramType.EXTERNAL)
            .build();

    Result result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("FILE_UPLOAD_QUESTION_IMPROVEMENTS_ENABLED", "true")
                .method("GET")
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("external program");
  }
}

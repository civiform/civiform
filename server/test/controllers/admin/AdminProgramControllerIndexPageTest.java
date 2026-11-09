package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import support.ProgramBuilder;

/** Tests the program list pages rendered through Thymeleaf; the legacy path has its own tests. */
public class AdminProgramControllerIndexPageTest extends WithMockedProfiles {

  private AdminProgramController controller;

  @Before
  public void setUp() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();
    controller = instanceOf(AdminProgramController.class);
  }

  private static Request thymeleafRequest() {
    return fakeRequestBuilder()
        .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
        .build();
  }

  @Test
  public void index_thymeleafEnabled_rendersCardsAndPublishModals() {
    ProgramBuilder.newActiveProgram("active-program").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(thymeleafRequest());
    String page = contentAsString(result);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(page).contains("Program dashboard");
    assertThat(page).contains("active-program").contains("draft-program");
    assertThat(page).contains("id=\"download-demographics-csv-modal\"");
    assertThat(page).contains("id=\"publish-modal-draft-program\"");
    assertThat(page).contains("id=\"publish-all-programs-modal-button\"");
  }

  @Test
  public void index_thymeleafEnabledWithoutDrafts_omitsPublishAllButton() {
    ProgramBuilder.newActiveProgram("active-program").build();

    String page = contentAsString(controller.index(thymeleafRequest()));

    assertThat(page).doesNotContain("publish-all-programs-modal");
  }

  @Test
  public void index_thymeleafEnabledWithoutDisabledPrograms_omitsTabs() {
    ProgramBuilder.newActiveProgram("active-program").build();

    String page = contentAsString(controller.index(thymeleafRequest()));

    assertThat(page).doesNotContain(routes.AdminProgramController.indexDisabled().url());
  }

  @Test
  public void indexDisabled_thymeleafEnabled_rendersOnlyDisabledProgramsWithTabs() {
    ProgramBuilder.newActiveProgram("in-use-program").build();
    ProgramBuilder.newActiveProgram("disabled-program", DisplayMode.DISABLED).build();

    Result result = controller.indexDisabled(thymeleafRequest());
    String page = contentAsString(result);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(page).contains("disabled-program").doesNotContain("in-use-program");
    assertThat(page).contains(routes.AdminProgramController.indexDisabled().url());
  }
}

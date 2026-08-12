package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;
import controllers.WithMockedProfiles;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.migration.ProgramMigrationService;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.PartialView;
import views.admin.migration.AdminImportErrorListPartialViewModel;
import views.admin.migration.AdminImportErrorPartialViewModel;
import views.admin.migration.AdminImportPageView;
import views.admin.migration.AdminImportProgramDataPartialViewModel;
import views.admin.migration.AdminImportProgramSavedPartialViewModel;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;
import views.shared.BaseViewDeps;

/** Throwaway smoke test: renders the new Thymeleaf import templates with the flag enabled. */
public class TempImportSmokeTest extends WithMockedProfiles {
  private AdminImportController controller;

  @Before
  public void setUp() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();

    controller =
        new AdminImportController(
            instanceOf(AdminImportView.class),
            instanceOf(AdminImportViewPartial.class),
            instanceOf(AdminImportPageView.class),
            new PartialView<>(
                instanceOf(BaseViewDeps.class),
                TypeLiteral.get(AdminImportErrorPartialViewModel.class)),
            new PartialView<>(
                instanceOf(BaseViewDeps.class),
                TypeLiteral.get(AdminImportErrorListPartialViewModel.class)),
            new PartialView<>(
                instanceOf(BaseViewDeps.class),
                TypeLiteral.get(AdminImportProgramDataPartialViewModel.class)),
            new PartialView<>(
                instanceOf(BaseViewDeps.class),
                TypeLiteral.get(AdminImportProgramSavedPartialViewModel.class)),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramMigrationService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class),
            instanceOf(ProgramService.class),
            instanceOf(SettingsManifest.class));
  }

  @Test
  public void index_rendersThymeleafPage() {
    Result result =
        controller.index(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .build());

    assertThat(result.status()).isEqualTo(OK);
    String content = contentAsString(result);
    assertThat(content).contains("admin-import-header");
    assertThat(content).contains("Import an existing program");
    assertThat(content).contains("Paste the JSON file contents into this box.");
  }

  @Test
  public void hxImportProgram_malformedJson_rendersThymeleafErrorPartial() {
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", "{\"garbage\": true}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    String content = contentAsString(result);
    assertThat(content).contains("program-data");
    assertThat(content).contains("Error processing JSON");
    assertThat(content).contains("Try again");
  }

  @Test
  public void hxImportProgram_validJson_rendersThymeleafProgramData() {
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson", AdminImportControllerTest.PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    String content = contentAsString(result);
    assertThat(content).contains("Program preview");
    assertThat(content).contains("Minimal Sample Program");
    assertThat(content).contains("Screen 1");
    assertThat(content).contains("Please enter your first and last name");
    assertThat(content).contains("New Question");
  }

  @Test
  public void hxSaveProgram_savesAndRendersThymeleafSavedPartial() {
    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .addCiviFormSetting("ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED", "true")
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson", AdminImportControllerTest.PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    String content = contentAsString(result);
    assertThat(content).contains("Your program has been successfully imported");
    assertThat(content).contains("View program");
  }
}

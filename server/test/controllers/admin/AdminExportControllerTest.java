package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.CategoryModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.i18n.Lang;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.migration.ProgramMigrationService;
import services.program.ProgramService;
import services.question.QuestionService;
import support.ProgramBuilder;
import views.admin.migration.AdminExportView;

public class AdminExportControllerTest extends ResetPostgres {
  private AdminExportController controller;

  @Before
  public void setUp() {
    controller =
        new AdminExportController(
            instanceOf(AdminExportView.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramMigrationService.class),
            instanceOf(ProgramService.class),
            instanceOf(QuestionService.class),
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_migrationEnabled_ok_displaysJsonForTheSelectedProgram() {
    String draftProgramA = "a-program-draft";

    ProgramModel draftProgram = ProgramBuilder.newDraftProgram(draftProgramA).build();

    Result result = controller.index(fakeRequest(), draftProgram.id);
    String stringResult = contentAsString(result);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(stringResult).contains("Export a program");
    assertThat(stringResult)
        .contains("JSON export for " + draftProgram.getProgramDefinition().adminName());
    assertThat(stringResult).contains(draftProgramA);
  }

  @Test
  public void index_invalidProgramId_badRequest() {
    Result result = controller.index(fakeRequest(), Long.MAX_VALUE);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("ID " + Long.MAX_VALUE + " could not be found");
  }

  @Test
  public void index_validProgram_rendersJsonPreview() {
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("active-program-1").build();

    Result result = controller.index(fakeRequest(), activeProgram.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Copy JSON");
    assertThat(contentAsString(result)).contains("Download JSON");
  }

  @Test
  public void index_removesProgramCategories() {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel category = new CategoryModel(translations);
    category.save();

    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("active-program-1")
            .withCategories(ImmutableList.of(category))
            .build();
    Result result = controller.index(fakeRequest(), activeProgram.id);

    assertThat(contentAsString(result)).contains("&quot;categories&quot; : [ ]");
    assertThat(contentAsString(result)).doesNotContain("Health");
  }

  @Test
  public void downloadJson_downloadsJson() {
    String adminName = "fake-admin-name";

    Result result =
        controller.downloadJson(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", String.valueOf("")))
                .build(),
            adminName);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType().get()).isEqualTo("application/json");
    assertThat(result.header("Content-Disposition")).isPresent();
    assertThat(result.header("Content-Disposition").get())
        .startsWith(String.format("attachment; filename=\"%s-exported", adminName));
  }
}

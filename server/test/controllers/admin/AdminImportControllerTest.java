package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
<<<<<<< HEAD
import com.google.common.collect.ImmutableMap;
=======
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
>>>>>>> 3e779e27d (browser tests, some unit test)
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;

public class AdminImportControllerTest extends ResetPostgres {
  private static final String TEST_FILE_CONTENT =
      "{ \"id\" : 32, \"adminName\" : \"email-program\", \"adminDescription\" : \"\"}";

  private AdminImportController controller;
  private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

  @Before
  public void setUp() {
    controller =
        new AdminImportController(
            instanceOf(AdminImportView.class),
<<<<<<< HEAD
            instanceOf(AdminImportViewPartial.class),
            instanceOf(FormFactory.class),
=======
            instanceOf(MessagesApi.class),
            instanceOf(ObjectMapper.class),
>>>>>>> 3e779e27d (browser tests, some unit test)
            instanceOf(ProfileUtils.class),
            mockSettingsManifest,
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void index_migrationEnabled_ok() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import a program");
  }

  @Test
  public void hxImportProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.hxImportProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void hxImportProgram_noRequestBody_redirectsToIndex() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result = controller.hxImportProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminImportController.index().url());
  }

  @Test
  @Ignore
  public void hxImportProgram_migrationEnabled_resultHasTextContent() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programJson", TEST_FILE_CONTENT)))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(TEST_FILE_CONTENT);
  }
}

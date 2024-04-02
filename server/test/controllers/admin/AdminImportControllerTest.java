package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import auth.ProfileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminImportView;

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
  public void importProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.importProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void importProgram_noRequestBody_okWithError() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result = controller.importProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Request did not contain a file");
  }

  // TODO(#7087): Re-enable this test once https://github.com/orgs/playframework/discussions/12518
  // is answered.
  // I wrote this test based on the guidance in
  // https://www.playframework.com/documentation/2.9.x/JavaFileUpload#Testing-the-file-upload,
  // but the `request.body()` ends up being null when this test runs even though it's non-null when
  // testing on a server or in browser tests. Hopefully the Play Framework team answers that
  // discussion question and we can re-enable this test soon. The browser tests in
  // admin_program_migration.test.ts are passing and do exercise this endpoint, so we at least have
  // some test coverage there.
  @Test
  @Ignore
  public void importProgram_migrationEnabled_resultHasFileContent() throws IOException {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    File file = getFile();
    Http.MultipartFormData.Part<Source<ByteString, ?>> part =
        new Http.MultipartFormData.FilePart<>(
            "file",
            "test.json",
            Http.MimeTypes.JSON,
            FileIO.fromPath(file.toPath()),
            Files.size(file.toPath()));

    Http.RequestBuilder request =
        addCSRFToken(
            Helpers.fakeRequest()
                .uri(routes.AdminImportController.importProgram().url())
                .method("POST")
                .bodyRaw(
                    Collections.singletonList(part),
                    play.libs.Files.singletonTemporaryFileCreator(),
                    app.asScala().materializer()));

    Result result = controller.importProgram(request.build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(TEST_FILE_CONTENT);
  }

  private File getFile() {
    String filePath = "/tmp/output.json";
    try {
      FileWriter file = new FileWriter(filePath);
      file.write(TEST_FILE_CONTENT);
      file.close();
      return new File(filePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

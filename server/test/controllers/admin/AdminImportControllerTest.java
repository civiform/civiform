package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.api.test.Helpers.route;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import akka.stream.IOResult;
import akka.stream.javadsl.Source;

import akka.stream.javadsl.FileIO;
import akka.util.ByteString;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.data.FormFactory;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;
import play.test.Helpers;
import play.test.WithApplication;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.applicant.JsonPathProvider;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminExportView;
import views.admin.migration.AdminImportView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

public class AdminImportControllerTest extends WithApplication {
    private static final String TEST_FILE_CONTENT = "{  \"id\" : 32,  \"adminName\" : \"email-program\", \"adminDescription\" : \"\"}";

    private AdminImportController controller;
    private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

    // From https://github.com/playframework/playframework/blob/main/documentation/manual/working/javaGuide/main/upload/code/JavaFileUploadTest.java
    // Didn't do anything
    @Override
    protected Application provideApplication() {
        Router router = Router.empty();
        play.api.inject.guice.GuiceApplicationBuilder scalaBuilder =
                new play.api.inject.guice.GuiceApplicationBuilder().additionalRouter(router.asScala());
        return GuiceApplicationBuilder.fromScalaBuilder(scalaBuilder).build();
    }

    @Before
    public void setUp() {
        controller =
                new AdminImportController(
                        instanceOf(AdminImportView.class),
                        instanceOf(ProfileUtils.class),
                        mockSettingsManifest,
                        instanceOf(VersionRepository.class));
    }

    /*
    @Test
    public void index_migrationNotEnabled_notFound() {
        when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

        Result result = controller.index(addCSRFToken(fakeRequest()).build());

        assertThat(result.status()).isEqualTo(NOT_FOUND);
        assertThat(contentAsString(result)).contains("not enabled");
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
        assertThat(contentAsString(result)).contains("not enabled");
    }

    @Test
    public void importProgram_noRequestBody_okWithError() {
        when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

        Result result = controller.importProgram(addCSRFToken(fakeRequest()).build());

        assertThat(result.status()).isEqualTo(OK);
        assertThat(contentAsString(result)).contains("Request did not contain a file");
    }

     */

    @Test
    public void importProgram_migrationEnabled_resultHasFileContent() throws IOException {
        when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

        File file = getFile();
        // Test format from https://www.playframework.com/documentation/2.9.x/JavaFileUpload#Testing-the-file-upload.

        Http.MultipartFormData.Part<Source<ByteString, ?>> part =
                new Http.MultipartFormData.FilePart<>(
                        "file",
                        "test.json",
                        Http.MimeTypes.JSON,
                        FileIO.fromPath(file.toPath()),
                        Files.size(file.toPath()));

        Http.RequestBuilder request =
                addCSRFToken(Helpers.fakeRequest()
                        .uri(routes.AdminImportController.importProgram().url())
                        .method("POST")
                        .bodyRaw(
                                Collections.singletonList(part),
                                play.libs.Files.singletonTemporaryFileCreator(),
                                app.asScala().materializer()));




        System.err.println("body in test="  + request.body().asText());



        Result result =// Helpers.route(app, request);
                controller.importProgram(request.build());


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

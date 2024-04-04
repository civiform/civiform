package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Files;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.MessageKey;
import services.settings.SettingsManifest;
import views.admin.migration.AdminImportView;

/**
 * A controller for the import part of program migration (allowing admins to easily migrate programs
 * between different environments). This controller is responsible for reading a JSON file that
 * represents a program and turning it into a full-fledged {@link
 * services.program.ProgramDefinition}, including all blocks and questions. {@link
 * AdminExportController} is responsible for exporting an existing program into the JSON format that
 * will be read by this controller.
 *
 * <p>Typically, admins will export from one environment (e.g. staging) and import to another
 * environment (e.g. production).
 */
public class AdminImportController extends CiviFormController {
  /**
   * Maximum file size allowed for the file import in MB.
   *
   * <p>This likely could be even smaller.
   */
  public static final int MAX_FILE_SIZE_MB = 1;

  private static final Logger logger = LoggerFactory.getLogger(AdminImportController.class);

  private final AdminImportView adminImportView;
  private final MessagesApi messagesApi;

  private final SettingsManifest settingsManifest;

  @Inject
  public AdminImportController(
      AdminImportView adminImportView,
      MessagesApi messagesApi,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.messagesApi = checkNotNull(messagesApi);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }
    return ok(adminImportView.render(request, /* programData= */ Optional.empty()));
  }

  // TODO: Rename route to fit guidelines in doc
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result importProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }

    // See https://www.playframework.com/documentation/2.9.x/JavaFileUpload for how to parse files
    // in Play Framework.
    Http.MultipartFormData<Files.TemporaryFile> body = request.body().asMultipartFormData();
    if (body == null) {
      return ok(
          adminImportView.render(request, createErrorResult("Request did not contain a file.")));
    }

    // The key here must match the `<input type="file">` name set in {@link
    // views.admin.migration.AdminImportView}.
    Http.MultipartFormData.FilePart<Files.TemporaryFile> uploadedFile =
        body.getFile(/* key= */ "file");
    if (uploadedFile == null) {
      return ok(
          adminImportView.render(request, createErrorResult("Request did not contain a file.")));
    }
    if (uploadedFile.getFileSize() > MAX_FILE_SIZE_MB * 1024 * 1024) {
      String error =
          messagesApi
              .preferred(request)
              .at(MessageKey.FILEUPLOAD_VALIDATION_FILE_TOO_LARGE.getKeyName(), MAX_FILE_SIZE_MB);
      return ok(adminImportView.render(request, createErrorResult(error)));
    }

    // TODO(#7087): Enable automatic cleaning up of temporary files:
    // https://www.playframework.com/documentation/2.9.x/JavaFileUpload#Cleaning-up-temporary-files.
    Files.TemporaryFile uploadedFileRef = uploadedFile.getRef();

    // TODO(#7087): Should this path be defined in application.conf so different deployments can use
    // different values? Is /tmp a reasonable default?
    File file = new File("/tmp/imported_program.json");
    uploadedFileRef.copyTo(file, /* replace= */ true);

    JsonNode parsedJson;
    try {
      InputStream inputStream = new FileInputStream(file);
      parsedJson = Json.parse(inputStream);
    } catch (FileNotFoundException e) {
      return ok(
          adminImportView
              .renderFetchedProgramData(request, "Imported file could not be found.")
              .render());
    } catch (RuntimeException e) {
      return ok(
          adminImportView
              .renderFetchedProgramData(
                  request, "JSON file is incorrectly formatted: " + e.getMessage())
              .render());
    }

    boolean fileDeleted = file.delete();
    if (!fileDeleted) {
      logger.error(String.format("File [%s] was not able to be deleted", file.getAbsolutePath()));
    }

    return ok(
        adminImportView.renderFetchedProgramData(request, parsedJson.toPrettyString()).render());
  }

  private Optional<ErrorAnd<String, CiviFormError>> createErrorResult(String error) {
    return Optional.of(ErrorAnd.error(ImmutableSet.of(CiviFormError.of(error))));
  }
}

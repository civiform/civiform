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
import play.libs.Files;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
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
  private static final Logger logger = LoggerFactory.getLogger(AdminImportController.class);

  private final AdminImportView adminImportView;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminImportController(
      AdminImportView adminImportView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }
    return ok(adminImportView.render(request, /* programData= */ Optional.empty()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result importProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }

    System.out.println("form url encoded= " + request.body().asFormUrlEncoded());
    System.out.println("text= " + request.body().asText());

    Http.MultipartFormData<Files.TemporaryFile> body = request.body().asMultipartFormData();
    if (body == null){
      System.err.println("null body -> OK");
      return ok(
              adminImportView.render(request, Optional.of(createErrorResult("Request did not contain a file."))));
    }
    System.err.println("non-null body is:");
    System.err.println(body);
    Http.MultipartFormData.FilePart<Files.TemporaryFile> uploadedFile = body.getFile("file");

    if (uploadedFile == null) {
      return ok(
          adminImportView.render(request, Optional.of(createErrorResult("No file was uploaded."))));
    }

    // TODO(#7087): Don't parse the file if it's too large.
    // TODO(#7087): Enable the automatic cleaning up of temporary files:
    // https://www.playframework.com/documentation/2.9.x/JavaFileUpload#Cleaning-up-temporary-files.
    Files.TemporaryFile uploadedFileRef = uploadedFile.getRef();

    // TODO(#7087): Should this path be defined in application.conf so different deployments can use different values? Is /tmp a reasonable default?
    File file = new File("/tmp/imported_program.json");
    uploadedFileRef.copyTo(file, /* replace= */ true);

    JsonNode parsedJson;
    try {
      InputStream inputStream = new FileInputStream(file);
      parsedJson = Json.parse(inputStream);
    } catch (FileNotFoundException e) {
      return ok(
          adminImportView.render(request, Optional.of(createErrorResult("Could not parse file."))));
    }

    boolean fileDeleted = file.delete();
    if (!fileDeleted) {
      logger.error(String.format("File [%s] was not able to be deleted", file.getAbsolutePath()));
    }

    // TODO(#7087): Is there a way to redirect back to the index page (`/admin/import` URL) but have this result?
    // Right now, this keeps users on the `/admin/import/program` URL. And if they refresh the page, they get a warning from their browser asking if they want to resubmit the form.
    return ok(adminImportView.render(request, Optional.of(ErrorAnd.of(parsedJson.toString()))));
  }

  private ErrorAnd<String, CiviFormError> createErrorResult(String error) {
    return ErrorAnd.error(ImmutableSet.of(CiviFormError.of(error)));
  }
}

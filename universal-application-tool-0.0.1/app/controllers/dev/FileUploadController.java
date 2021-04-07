package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.io.FileInputStream;
import java.nio.file.Files;
import play.Environment;
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.AmazonS3Client;
import views.dev.FileUploadView;

/** Controller for interacting with S3 directly in dev mode. */
public class FileUploadController extends Controller {
  private final Environment environment;
  private final FileUploadView view;
  private final AmazonS3Client s3client;

  @Inject
  public FileUploadController(
      FileUploadView view, AmazonS3Client s3client, Environment environment) {
    this.view = checkNotNull(view);
    this.s3client = checkNotNull(s3client);
    this.environment = checkNotNull(environment);
  }

  public Result index(Request request) {
    if (environment.isDev()) {
      return ok(view.render(request));
    } else {
      return notFound();
    }
  }

  public Result upload(Request request) {
    if (environment.isDev()) {
      MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
      MultipartFormData.FilePart<TemporaryFile> data = body.getFile("filename");
      if (data != null) {
        String fileName = data.getFilename();
        long fileSize = data.getFileSize();
        String contentType = data.getContentType();
        TemporaryFile file = data.getRef();
        uploadToS3(file);
        return ok(
            String.format(
                "File uploaded: name: %s, size: %d, type: %s.", fileName, fileSize, contentType));
      } else {
        return badRequest("Missing file");
      }
    } else {
      return notFound();
    }
  }

  private void uploadToS3(TemporaryFile file) {
    try (FileInputStream fileInputStream = new FileInputStream(file.path().toFile())) {
      byte[] data = new byte[(int) Files.size(file.path())];
      fileInputStream.read(data);
      s3client.putObject("my_file", data);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }
}

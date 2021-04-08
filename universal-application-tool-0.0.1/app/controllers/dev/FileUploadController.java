package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Set;
import models.StoredFile;
import play.Environment;
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import views.dev.FileUploadView;

/** Controller for interacting with S3 directly in dev mode. */
public class FileUploadController extends Controller {
  private final Environment environment;
  private final FileUploadView view;
  private final StoredFileRepository storedFileRepository;

  @Inject
  public FileUploadController(
      FileUploadView view, StoredFileRepository storedFileRepository, Environment environment) {
    this.view = checkNotNull(view);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.environment = checkNotNull(environment);
  }

  public Result index(Request request) {
    if (!environment.isDev()) {
      return notFound();
    }
    Set<StoredFile> files =
        storedFileRepository.listWithPresignedUrl().toCompletableFuture().join();
    ImmutableList<StoredFile> fileList =
        files.stream()
            .sorted(Comparator.comparing(StoredFile::getName))
            .collect(ImmutableList.toImmutableList());
    return ok(view.render(request, fileList, request.flash().get("success")));
  }

  public Result create(Request request) {
    if (!environment.isDev()) {
      return notFound();
    }
    MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
    MultipartFormData.FilePart<TemporaryFile> data = body.getFile("filename");
    if (data == null) {
      return badRequest("Missing file");
    }
    String fileName = data.getFilename();
    long fileSize = data.getFileSize();
    String contentType = data.getContentType();
    TemporaryFile file = data.getRef();
    uploadToS3(fileName, file);
    String successMessage =
        String.format(
            "File uploaded: name: %s, size: %d, type: %s.", fileName, fileSize, contentType);
    return redirect(routes.FileUploadController.index().url()).flashing("success", successMessage);
  }

  private void uploadToS3(String name, TemporaryFile file) {
    StoredFile storedFile = new StoredFile();
    storedFile.setName(name);
    try (FileInputStream fileInputStream = new FileInputStream(file.path().toFile())) {
      byte[] data = new byte[(int) Files.size(file.path())];
      fileInputStream.read(data);
      storedFile.setContent(data);
      storedFileRepository.insert(storedFile);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }
}

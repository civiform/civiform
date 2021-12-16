package controllers.dev;

import static play.mvc.Results.redirect;

import java.util.Optional;
import models.StoredFile;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;

public interface CloudStorageStrategy {

  Result create(StoredFileRepository storedFileRepository, Request request);

}


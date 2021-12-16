package controllers.dev;


import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;

public interface CloudStorageStrategy {

  Result create(StoredFileRepository storedFileRepository, Request request);
}

package parsers;

import javax.inject.Inject;
import play.http.DefaultHttpErrorHandler;
import play.mvc.BodyParser;

/**
 * This is a custom HTTP request body parser for Program Migration that increases the allowable
 * limit for buffering. Program json is often quite large and we want to allow it to be processed
 * without increasing the buffer limit for all HTTP requests. All other requests use the default
 * Play buffer limit which is 100KB. For more information see:
 * https://www.playframework.com/documentation/3.0.x/JavaBodyParsers
 */
public class ProgramMigrationParser extends BodyParser.FormUrlEncoded {
  @Inject
  public ProgramMigrationParser(DefaultHttpErrorHandler errorHandler) {
    // Allows requests up to about 1MB
    super(1024 * 1024, errorHandler);
  }
}

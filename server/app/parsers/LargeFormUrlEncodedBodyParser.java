package parsers;

import javax.inject.Inject;
import play.http.DefaultHttpErrorHandler;
import play.mvc.BodyParser;

/**
 * This is a custom HTTP request body parser that increases the allowable limit for buffering to 1
 * MiB. All other requests use the default Play buffer limit which is 100KB. For more information
 * see: https://www.playframework.com/documentation/3.0.x/JavaBodyParsers
 */
public class LargeFormUrlEncodedBodyParser extends BodyParser.FormUrlEncoded {
  @Inject
  public LargeFormUrlEncodedBodyParser(DefaultHttpErrorHandler errorHandler) {
    super(1024 * 1024, errorHandler);
  }
}

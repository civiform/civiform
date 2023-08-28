package services.openApi.v2;

/**
 * https://swagger.io/specification/v2/#http-codes
 *
 * <p>The HTTP Status Codes are used by CiviForm's API responses
 */
public enum HttpStatusCode {
  OK("200"),
  BadRequest("400"),
  Unauthorized("401");

  private final String code;

  HttpStatusCode(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return code;
  }
}

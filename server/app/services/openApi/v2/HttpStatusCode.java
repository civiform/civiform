package services.openApi.v2;

/**
 * The HTTP Status Codes are used by CiviForm's API responses
 *
 * <p>https://swagger.io/specification/v2/#http-codes
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

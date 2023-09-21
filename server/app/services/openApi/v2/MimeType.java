package services.openApi.v2;

/** The available mime types to allow */
public enum MimeType {
  Json("application/json");

  private final String code;

  MimeType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}

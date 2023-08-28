package services.openApi.v2;

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

package views;

public enum JsBundle {
  ADMIN("admin.bundle"),
  APPLICANT("applicant.bundle");

  private final String jsPath;

  JsBundle(String jsPath) {
    this.jsPath = jsPath;
  }

  public String getJsPath() {
    return jsPath;
  }
}

package views;

/**
 * JS bundle is a bundled minified JS file containing client-side CiviForm code. Each page must
 * include only one JS bundle. Each bundle compiled from an entry point that imports all necessary
 * dependencies.
 */
public enum JsBundle {
  /**
   * Bundle used on admin pages: CiviForm admin and program admin pages.
   *
   * <p>Entry point is admin_entry_point.ts
   */
  ADMIN("admin.bundle"),

  /**
   * Bundle used on applicant pages: pages that applicants and trusted intermediaries see.
   *
   * <p>Entry point is applicant_entry_point.ts
   */
  APPLICANT("applicant.bundle");

  private final String jsPath;

  JsBundle(String jsPath) {
    this.jsPath = jsPath;
  }

  public String getJsPath() {
    return "dist/" + jsPath;
  }
}

package views;

/**
 * List of Thymeleaf layout template HTML files and the relative path from the {@code app/views}
 * folder.
 */
public enum LayoutTemplate {
  ADMIN_LAYOUT("admin/AdminLayout.html"),
  TRANSITIONAL_LAYOUT("admin/TransitionalLayout.html");

  private final String path;

  LayoutTemplate(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}

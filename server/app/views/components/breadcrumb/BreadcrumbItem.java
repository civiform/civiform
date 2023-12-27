package views.components.breadcrumb;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import views.components.Icons;

/** Represents a single item in a breadcrumb list. */
@AutoValue
public abstract class BreadcrumbItem {
  public static BreadcrumbItem create(String text, @Nullable String link, @Nullable Icons icon) {
    return new AutoValue_BreadcrumbItem(text, link, icon);
  }

  /** The text to display for this breadcrumb item. */
  public abstract String text();

  /**
   * An optional href link that represents this breadcrumb item. For example, an "Edit program ABC"
   * breadcrumb item should have an href that takes the user to a page where the admin can make
   * edits to program ABC.
   *
   * <p>If null, the breadcrumb item is rendered as plain text instead of as a link.
   */
  @Nullable
  public abstract String link();

  /**
   * An optional icon that can be rendered to the left of the breadcrumb text.
   *
   * <p>If null, no icon is rendered.
   */
  @Nullable
  public abstract Icons icon();
}

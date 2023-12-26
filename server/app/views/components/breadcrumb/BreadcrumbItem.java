package views.components.breadcrumb;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import views.components.Icons;

/** Represents a single item in a breadcrumb list. */
@AutoValue
public abstract class BreadcrumbItem {
  public static BreadcrumbItem create(String text, @Nullable String href, @Nullable Icons icon) {
    return new AutoValue_BreadcrumbItem(text, href, icon);
  }

  /** The text to display for this item in the breadcrumb. */
  public abstract String text();

  /**
   * An optional href link that represents this breadcrumb item. For example, an "Edit program ABC"
   * breadcrumb item should have an href that takes the user to
   * `/programs/{programId}/blocks/1/edit`.
   *
   * <p>If null, the breadcrumb item is *not* rendered as a link and is just rendered as plain text.
   */
  @Nullable
  public abstract String href();

  /**
   * An optional icon that can be rendered to the left of the breadcrumb text.
   *
   * <p>If null, no icon is rendered.
   */
  @Nullable
  public abstract Icons icon();
}

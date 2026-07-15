package views.shared;

import com.google.common.collect.ImmutableList;
import controllers.FlashKey;
import java.util.Locale;
import play.mvc.Http;
import services.AlertType;

/**
 * A single page-level alert rendered into the layout's alert container.
 *
 * @param type Alert severity, mapped to the matching USWDS alert variant
 * @param text Alert message text
 */
public record PageAlert(AlertType type, String text) {

  public static PageAlert error(String text) {
    return new PageAlert(AlertType.ERROR, text);
  }

  public static PageAlert success(String text) {
    return new PageAlert(AlertType.SUCCESS, text);
  }

  public static PageAlert warning(String text) {
    return new PageAlert(AlertType.WARNING, text);
  }

  public static PageAlert info(String text) {
    return new PageAlert(AlertType.INFO, text);
  }

  /** USWDS modifier class suffix, e.g. {@code error} for {@code usa-alert--error}. */
  public String typeClass() {
    return type.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Builds alerts from the standard {@link FlashKey#ERROR}, {@link FlashKey#WARNING}, and {@link
   * FlashKey#SUCCESS} flash keys so any page rendered after a redirect shows them without
   * per-page plumbing.
   */
  public static ImmutableList<PageAlert> fromFlash(Http.Flash flash) {
    ImmutableList.Builder<PageAlert> builder = ImmutableList.builder();
    flash.get(FlashKey.ERROR).ifPresent(text -> builder.add(error(text)));
    flash.get(FlashKey.WARNING).ifPresent(text -> builder.add(warning(text)));
    flash.get(FlashKey.SUCCESS).ifPresent(text -> builder.add(success(text)));
    return builder.build();
  }
}

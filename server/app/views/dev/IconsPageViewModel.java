package views.dev;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** View model for the dev icons page, one row per {@link views.components.Icons} value. */
@Data
@Builder
public class IconsPageViewModel implements BaseViewModel {
  private final ImmutableList<IconRow> icons;

  /**
   * One icon row.
   *
   * @param name the enum constant name
   * @param viewBox the SVG viewBox attribute value
   * @param path the SVG path data
   */
  public record IconRow(String name, String viewBox, String path) {}
}

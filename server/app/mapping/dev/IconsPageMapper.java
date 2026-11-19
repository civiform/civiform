package mapping.dev;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import views.components.Icons;
import views.dev.IconsPageViewModel;
import views.dev.IconsPageViewModel.IconRow;

/** Maps the {@link Icons} enum to the IconsPageViewModel for the dev icons page. */
public final class IconsPageMapper {

  /** Maps every icon, in enum declaration order, to a row. */
  public IconsPageViewModel map() {
    ImmutableList<IconRow> icons =
        Arrays.stream(Icons.values())
            .map(icon -> new IconRow(icon.name(), icon.getViewBox(), icon.path))
            .collect(ImmutableList.toImmutableList());

    return IconsPageViewModel.builder().icons(icons).build();
  }
}

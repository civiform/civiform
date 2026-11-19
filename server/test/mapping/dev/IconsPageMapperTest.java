package mapping.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import views.components.Icons;
import views.dev.IconsPageViewModel;
import views.dev.IconsPageViewModel.IconRow;

public final class IconsPageMapperTest {

  private IconsPageMapper mapper;

  @Before
  public void setup() {
    mapper = new IconsPageMapper();
  }

  @Test
  public void map_includesEveryIconInEnumOrder() {
    IconsPageViewModel result = mapper.map();

    assertThat(result.getIcons())
        .extracting(IconRow::name)
        .containsExactly(Arrays.stream(Icons.values()).map(Icons::name).toArray(String[]::new));
  }

  @Test
  public void map_sizeOnlyIcon_derivesSquareViewBox() {
    IconsPageViewModel result = mapper.map();

    assertThat(findRow(result, Icons.ADD).viewBox()).isEqualTo("0 0 20 20");
  }

  @Test
  public void map_explicitViewBoxIcon_keepsViewBox() {
    IconsPageViewModel result = mapper.map();

    assertThat(findRow(result, Icons.COG).viewBox()).isEqualTo("0 -960 960 960");
  }

  @Test
  public void map_setsPath() {
    IconsPageViewModel result = mapper.map();

    assertThat(findRow(result, Icons.ADD).path()).isEqualTo(Icons.ADD.path);
  }

  private static IconRow findRow(IconsPageViewModel model, Icons icon) {
    return model.getIcons().stream()
        .filter(row -> row.name().equals(icon.name()))
        .findFirst()
        .orElseThrow();
  }
}

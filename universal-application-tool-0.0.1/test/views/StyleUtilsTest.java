package views;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class StyleUtilsTest {

  @Test
  public void applyUtilityClass_singleReturnsExpected() {
    String expected = "hover:bg-blue-100";
    String result = StyleUtils.applyUtilityClass(StyleUtils.HOVER, Styles.BG_BLUE_100);

    assertEquals(expected, result);
  }

  @Test
  public void applyUtility_multiReturnsExpected() {
    String expected = "focus:outline-none focus:ring-2";
    String result =
        StyleUtils.applyUtilityClass(
            StyleUtils.FOCUS, ImmutableList.of(Styles.OUTLINE_NONE, Styles.RING_2));

    assertEquals(expected, result);
  }

  @Test
  public void focus_returnsExpected() {
    String expected = "focus:outline-none focus:ring-2";
    String result = StyleUtils.focus(ImmutableList.of(Styles.OUTLINE_NONE, Styles.RING_2));
    assertEquals(expected, result);

    expected = "focus:outline-none";
    result = StyleUtils.focus(Styles.OUTLINE_NONE);
    assertEquals(expected, result);
  }

  @Test
  public void hover_returnsExpected() {
    String expected = "hover:outline-none hover:ring-2";
    String result = StyleUtils.hover(ImmutableList.of(Styles.OUTLINE_NONE, Styles.RING_2));
    assertEquals(expected, result);

    expected = "hover:outline-none";
    result = StyleUtils.hover(Styles.OUTLINE_NONE);
    assertEquals(expected, result);
  }
}

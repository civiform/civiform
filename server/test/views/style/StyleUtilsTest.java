package views.style;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class StyleUtilsTest {

  @Test
  public void applyUtilityClass_singleReturnsExpected() {
    String expected = "hover:bg-blue-100";
    String result = StyleUtils.applyUtilityClass(StyleUtils.HOVER, "bg-blue-100");

    assertEquals(expected, result);
  }

  @Test
  public void applyUtility_multiReturnsExpected() {
    String expected = "focus:outline-none focus:ring-2";
    String result =
        StyleUtils.applyUtilityClass(
            StyleUtils.FOCUS, ImmutableList.of("outline-none", "ring-2"));

    assertEquals(expected, result);
  }

  @Test
  public void focus_returnsExpected() {
    String expected = "focus:outline-none focus:ring-2";
    String result = StyleUtils.focus(ImmutableList.of("outline-none", "ring-2"));
    assertEquals(expected, result);

    expected = "focus:outline-none";
    result = StyleUtils.focus("outline-none");
    assertEquals(expected, result);
  }

  @Test
  public void hover_returnsExpected() {
    String expected = "hover:outline-none hover:ring-2";
    String result = StyleUtils.hover(ImmutableList.of("outline-none", "ring-2"));
    assertEquals(expected, result);

    expected = "hover:outline-none";
    result = StyleUtils.hover("outline-none");
    assertEquals(expected, result);
  }

  @Test
  public void responsiveSmall_returnsExpected() {
    String expected = "sm:outline-none sm:ring-2";
    String result =
        StyleUtils.responsiveSmall(ImmutableList.of("outline-none", "ring-2"));
    assertEquals(expected, result);

    expected = "sm:outline-none";
    result = StyleUtils.responsiveSmall("outline-none");
    assertEquals(expected, result);
  }

  @Test
  public void responsiveMedium_returnsExpected() {
    String expected = "md:outline-none md:ring-2";
    String result =
        StyleUtils.responsiveMedium(ImmutableList.of("outline-none", "ring-2"));
    assertEquals(expected, result);

    expected = "md:outline-none";
    result = StyleUtils.responsiveMedium("outline-none");
    assertEquals(expected, result);
  }

  @Test
  public void responsiveLarge_returnsExpected() {
    String expected = "lg:outline-none lg:ring-2";
    String result =
        StyleUtils.responsiveLarge(ImmutableList.of("outline-none", "ring-2"));
    assertEquals(expected, result);

    expected = "lg:outline-none";
    result = StyleUtils.responsiveLarge("outline-none");
    assertEquals(expected, result);
  }
}

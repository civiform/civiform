package views.style;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class StyleUtilsTest {

  @Test
  public void applyUtilityClass_singleReturnsExpected() {
    String expected = "hover:bg-blue-100";
    String result = StyleUtils.applyUtilityClass(StyleUtils.HOVER, "bg-blue-100");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void applyUtility_multiReturnsExpected() {
    String expected = "focus:outline-none focus:ring-2";
    String result =
        StyleUtils.applyUtilityClass(StyleUtils.FOCUS, ImmutableList.of("outline-none", "ring-2"));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void focus_returnsExpected() {
    String expected = "focus:outline-none focus:ring-2";
    String result = StyleUtils.focus(ImmutableList.of("outline-none", "ring-2"));
    assertThat(result).isEqualTo(expected);

    expected = "focus:outline-none";
    result = StyleUtils.focus("outline-none");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void hover_returnsExpected() {
    String expected = "hover:outline-none hover:ring-2";
    String result = StyleUtils.hover(ImmutableList.of("outline-none", "ring-2"));
    assertThat(result).isEqualTo(expected);

    expected = "hover:outline-none";
    result = StyleUtils.hover("outline-none");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void responsiveSmall_returnsExpected() {
    String expected = "sm:outline-none sm:ring-2";
    String result = StyleUtils.responsiveSmall(ImmutableList.of("outline-none", "ring-2"));
    assertThat(result).isEqualTo(expected);

    expected = "sm:outline-none";
    result = StyleUtils.responsiveSmall("outline-none");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void responsiveMedium_returnsExpected() {
    String expected = "md:outline-none md:ring-2";
    String result = StyleUtils.responsiveMedium(ImmutableList.of("outline-none", "ring-2"));
    assertThat(result).isEqualTo(expected);

    expected = "md:outline-none";
    result = StyleUtils.responsiveMedium("outline-none");
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void responsiveLarge_returnsExpected() {
    String expected = "lg:outline-none lg:ring-2";
    String result = StyleUtils.responsiveLarge(ImmutableList.of("outline-none", "ring-2"));
    assertThat(result).isEqualTo(expected);

    expected = "lg:outline-none";
    result = StyleUtils.responsiveLarge("outline-none");
    assertThat(result).isEqualTo(expected);
  }
}

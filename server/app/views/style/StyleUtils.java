package views.style;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility class for applying styles. */
public final class StyleUtils {
  public static String EVEN = "even";
  public static String FOCUS = "focus";
  public static String FOCUS_WITHIN = "focus-within";
  public static String HOVER = "hover";
  public static String DISABLED = "disabled";

  public static String RESPONSIVE_SM = "sm";
  public static String RESPONSIVE_MD = "md";
  public static String RESPONSIVE_LG = "lg";
  public static String RESPONSIVE_XL = "xl";
  public static String RESPONSIVE_2XL = "2xl";
  public static String RESPONSIVE_3XL = "3xl";

  public static String applyUtilityClass(String utility, String... styles) {
    return applyUtilityClass(utility, Stream.of(styles));
  }

  public static String applyUtilityClass(String utility, ImmutableList<String> styles) {
    return applyUtilityClass(utility, styles.stream());
  }

  public static String applyUtilityClass(String utility, Stream<String> styles) {
    return styles.map(entry -> utility + ":" + entry).collect(Collectors.joining(" "));
  }

  public static String disabled(ImmutableList<String> styles) {
    return applyUtilityClass(DISABLED, styles);
  }

  public static String disabled(String... styles) {
    return applyUtilityClass(DISABLED, styles);
  }

  public static String even(ImmutableList<String> styles) {
    return applyUtilityClass(EVEN, styles);
  }

  public static String even(String... styles) {
    return applyUtilityClass(EVEN, styles);
  }

  public static String focus(ImmutableList<String> styles) {
    return applyUtilityClass(FOCUS, styles);
  }

  public static String focus(String... styles) {
    return applyUtilityClass(FOCUS, styles);
  }

  public static String focusWithin(ImmutableList<String> styles) {
    return applyUtilityClass(FOCUS_WITHIN, styles);
  }

  public static String focusWithin(String... styles) {
    return applyUtilityClass(FOCUS_WITHIN, styles);
  }

  public static String hover(String... styles) {
    return applyUtilityClass(HOVER, styles);
  }

  public static String hover(ImmutableList<String> styles) {
    return applyUtilityClass(HOVER, styles);
  }

  public static String responsiveSmall(String... styles) {
    return applyUtilityClass(RESPONSIVE_SM, styles);
  }

  public static String responsiveSmall(ImmutableList<String> styles) {
    return applyUtilityClass(RESPONSIVE_SM, styles);
  }

  public static String responsiveMedium(String... styles) {
    return applyUtilityClass(RESPONSIVE_MD, styles);
  }

  public static String responsiveMedium(ImmutableList<String> styles) {
    return applyUtilityClass(RESPONSIVE_MD, styles);
  }

  public static String responsiveLarge(String... styles) {
    return applyUtilityClass(RESPONSIVE_LG, styles);
  }

  public static String responsiveLarge(ImmutableList<String> styles) {
    return applyUtilityClass(RESPONSIVE_LG, styles);
  }

  public static String responsiveXLarge(String... styles) {
    return applyUtilityClass(RESPONSIVE_XL, styles);
  }

  public static String responsiveXLarge(ImmutableList<String> styles) {
    return applyUtilityClass(RESPONSIVE_XL, styles);
  }

  public static String responsive2XLarge(String... styles) {
    return applyUtilityClass(RESPONSIVE_2XL, styles);
  }

  public static String responsive3XLarge(String... styles) {
    return applyUtilityClass(RESPONSIVE_3XL, styles);
  }

  public static String responsive2XLarge(ImmutableList<String> styles) {
    return applyUtilityClass(RESPONSIVE_2XL, styles);
  }

  public static String joinStyles(String... styles) {
    return String.join(" ", styles);
  }

  public static String removeStyles(String style, String... stylesToRemove) {
    return Stream.of(stylesToRemove)
        .reduce(style, (acc, styleToRemove) -> acc.replace(styleToRemove, ""))
        .trim();
  }
}

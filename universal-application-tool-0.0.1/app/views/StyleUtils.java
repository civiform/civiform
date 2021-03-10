package views;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;

public class StyleUtils {
  public static String EVEN = "even";
  public static String FOCUS = "focus";
  public static String HOVER = "hover";

  public static String RESPONSIVE_SM = "sm";
  public static String RESPONSIVE_MD = "md";
  public static String RESPONSIVE_LG = "lg";
  public static String RESPONSIVE_XL = "xl";
  public static String RESPONSIVE_2XL = "2xl";

  public static String applyUtilityClass(String utility, String style) {
    return utility + ":" + style;
  }

  public static String applyUtilityClass(String utility, ImmutableList<String> styles) {
    return styles.stream().map(entry -> utility + ":" + entry).collect(Collectors.joining(" "));
  }

  public static String even(String style) {
    return applyUtilityClass(EVEN, style);
  }

  public static String even(ImmutableList<String> styles) {
    return applyUtilityClass(EVEN, styles);
  }

  public static String focus(String style) {
    return applyUtilityClass(FOCUS, style);
  }

  public static String focus(ImmutableList<String> styles) {
    return applyUtilityClass(FOCUS, styles);
  }

  public static String hover(String style) {
    return applyUtilityClass(HOVER, style);
  }

  public static String hover(ImmutableList<String> styles) {
    return applyUtilityClass(HOVER, styles);
  }
}

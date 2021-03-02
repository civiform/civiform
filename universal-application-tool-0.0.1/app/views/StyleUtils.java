package views;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;

public class StyleUtils {
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
}

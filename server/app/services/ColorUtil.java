package services;

public final class ColorUtil {

  /**
   * Returns true if the given color has a contrast ratio of at least 4.5:1 when compared to pure
   * white, following the w3 contrast ratio formula:
   * https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio.
   */
  public static boolean contrastsWithWhite(String hex) {
    double whiteLuminance = calculateLuminance("#FFFFFF");
    double chosenColorLuminance = calculateLuminance(hex);
    double contrastRatio = (whiteLuminance + 0.05) / (chosenColorLuminance + 0.05);
    return contrastRatio >= 4.5;
  }

  /**
   * Calculates the relative luminance for a given color, following the w3 formula:
   * https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
   */
  private static double calculateLuminance(String hex) {
    if (hex.length() < 7) {
      hex = convert3DigitHexTo6Digits(hex);
    }

    int redCode = Integer.valueOf(hex.substring(1, 3), 16);
    int greenCode = Integer.valueOf(hex.substring(3, 5), 16);
    int blueCode = Integer.valueOf(hex.substring(5, 7), 16);

    double rsrgb = getSRGB(redCode);
    double gsrgb = getSRGB(greenCode);
    double bsrgb = getSRGB(blueCode);

    return (rsrgb * 0.2126) + (gsrgb * 0.7152) + (bsrgb * 0.0722);
  }

  /** Returns the sRGB value for a given R,G,B value. */
  private static double getSRGB(double code) {
    double normalizedColorValue = code / 255.0;
    return normalizedColorValue > 0.04045
        ? Math.pow(((normalizedColorValue + 0.055) / 1.055), 2.4)
        : normalizedColorValue / 12.92;
  }

  /** Converts a 3-digit hex color code to a 6-digit one. */
  private static String convert3DigitHexTo6Digits(String hex) {
    char redCode = hex.charAt(1);
    char greenCode = hex.charAt(2);
    char blueCode = hex.charAt(3);

    return String.format(
        "#%s%s%s%s%s%s", redCode, redCode, greenCode, greenCode, blueCode, blueCode);
  }
}

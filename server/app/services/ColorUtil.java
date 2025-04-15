package services;

public class ColorUtil {
  public static boolean contrastsWithWhite(String hex) {
    double whiteLuminance = calculateLuminance("#FFFFFF");
    double chosenColorLuminance = calculateLuminance(hex);
    double contrastRatio = (whiteLuminance + 0.05) / (chosenColorLuminance + 0.05);
    return contrastRatio >= 4.5;
  }

  private static double calculateLuminance(String hex) {
    if (hex.length() < 7) {
      hex = convert3DigitHexTo6Digits(hex);
    }

    int redCode = Integer.valueOf(hex.substring(1, 3), 16);
    int greenCode = Integer.valueOf(hex.substring(3, 5), 16);
    int blueCode = Integer.valueOf(hex.substring(5, 7), 16);

    double redValue = getColorValue(redCode);
    double greenValue = getColorValue(greenCode);
    double blueValue = getColorValue(blueCode);

    return (redValue * 0.2126) + (greenValue * 0.7152) + (blueValue * 0.0722);
  }

  private static double getColorValue(double code) {
    double normalizedColorValue = code / 255.0;
    return normalizedColorValue > 0.04045
        ? Math.pow(((normalizedColorValue + 0.055) / 1.055), 2.4)
        : normalizedColorValue / 12.92;
  }

  private static String convert3DigitHexTo6Digits(String hex) {
    char redCode = hex.charAt(1);
    char greenCode = hex.charAt(2);
    char blueCode = hex.charAt(3);

    return String.format(
        "#%s%s%s%s%s%s", redCode, redCode, greenCode, greenCode, blueCode, blueCode);
  }
}

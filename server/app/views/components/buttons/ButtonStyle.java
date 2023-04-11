package views.components.buttons;

/**
 * Represents the style options available to the buttons. To keep buttons consistent across
 * CiviForm, we have a set of predetermined styles that can be used.
 */
public enum ButtonStyle {
  SOLID_BLUE(ButtonStyles.SOLID_BLUE),
  SOLID_WHITE(ButtonStyles.SOLID_WHITE),
  NONE("");

  private final String styles;

  public String getStyles() {
    return styles;
  }

  ButtonStyle(String styles) {
    this.styles = styles;
  }
}

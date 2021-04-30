package views.style;

public class AdminStyles {

  public static final String LANGUAGE_LINK_SELECTED =
      StyleUtils.joinStyles(
          ReferenceClasses.ADMIN_LANGUAGE_LINK,
          Styles.M_2,
          Styles.BORDER_BLUE_400,
          Styles.BORDER_B_2);

  public static final String LANGUAGE_LINK_NOT_SELECTED =
      StyleUtils.joinStyles(ReferenceClasses.ADMIN_LANGUAGE_LINK, Styles.M_2);
}

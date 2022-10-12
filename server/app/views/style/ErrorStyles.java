package views.style;

public final class ErrorStyles {
  public static final String H1_NOT_FOUND =
      StyleUtils.joinStyles(
          "text-2xl",
          "text-black",
          "font-bold",
          "text-center",
          "text-gray-800",
          "mt-28",
          "mb-10",
          StyleUtils.responsiveSmall("mt-40", "mb-5", "text-3xl"));

  public static final String P_DESCRIPTION =
      StyleUtils.joinStyles(
          "text-xl",
          "text-base",
          "text-black",
          "text-center",
          "text-gray-700",
          "mb-6",
          StyleUtils.responsiveSmall("mt-10"));

  public static final String P_MOBILE_INLINE =
      StyleUtils.joinStyles("inline", StyleUtils.responsiveSmall("block"));
}

package views.components;

import j2html.tags.ContainerTag;
import services.question.types.QuestionType;

/**
 * Class to hold constants for icons and provide methods for rendering SVG components. Most of these
 * icons are from https://fonts.google.com/icons, each one is commented with its icon name.
 */
public class Icons {

  // Place
  public static final String ADDRESS_SVG_PATH =
      "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38"
          + " 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z";
  public static final String ANNOTATION_SVG_PATH =
      "M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z";
  // Check Box
  public static final String CHECKBOX_SVG_PATH =
      "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0"
          + " 16H5V5h14v14zM17.99 9l-1.41-1.42-6.59 6.59-2.58-2.57-1.42 1.41 4 3.99z";
  // Payments
  public static final String CURRENCY_SVG_PATH =
      "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3"
          + " 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11"
          + " 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z";
  public static final String DATE_SVG_PATH =
      "M19 4h-1V3c0-.55-.45-1-1-1s-1 .45-1 1v1H8V3c0-.55-.45-1-1-1s-1 .45-1 1v1H5c-1.11"
          + " 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 15c0"
          + " .55-.45 1-1 1H6c-.55 0-1-.45-1-1V9h14v10zM7 11h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z";
  // Arrow Drop Down Circle
  public static final String DROPDOWN_SVG_PATH =
      "M12 4c4.41 0 8 3.59 8 8s-3.59 8-8 8-8-3.59-8-8 3.59-8 8-8m0-2C6.48 2 2 6.48 2 12s4.48 10 10"
          + " 10 10-4.48 10-10S17.52 2 12 2zm0 13l-4-4h8z";
  // Email
  public static final String EMAIL_SVG_PATH =
      "M22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6zm-2 0l-8"
          + " 5-8-5h16zm0 12H4V8l8 5 8-5v10z";
  // Forms Add On
  public static final String ENUMERATOR_SVG_PATH =
      "M19 6H7V4H19ZM7 10V8H19V10ZM7 14V12H13.65Q13.075 12.4 12.613 12.9Q12.15 13.4 11.8 14ZM7"
          + " 16H11.075Q11.025 16.25 11.012 16.488Q11 16.725 11 16.975Q11 17.25 11.025 17.5Q11.05"
          + " 17.75 11.1 18H7ZM16"
          + " 20.975V17.975H13V15.975H16V12.975H18V15.975H21V17.975H18V20.975ZM5 6H3V4H5ZM3"
          + " 10V8H5V10ZM3 14V12H5V14ZM3 16H5V18H3Z";
  // Upload
  public static final String FILEUPLOAD_SVG_PATH =
      "M11 16V7.85L8.4 10.45L7 9L12 4L17 9L15.6 10.45L13 7.85V16ZM6 20Q5.175 20 4.588 19.413Q4"
          + " 18.825 4 18V15H6V18Q6 18 6 18Q6 18 6 18H18Q18 18 18 18Q18 18 18 18V15H20V18Q20"
          + " 18.825 19.413 19.413Q18.825 20 18 20Z";
  // Badge
  public static final String ID_SVG_PATH =
      "M14 13.5H18V12H14ZM14 16.5H18V15H14ZM15 7H20Q20.825 7 21.413 7.587Q22 8.175 22 9V20Q22"
          + " 20.825 21.413 21.413Q20.825 22 20 22H4Q3.175 22 2.588 21.413Q2 20.825 2 20V9Q2 8.175"
          + " 2.588 7.587Q3.175 7 4 7H9V4Q9 3.175 9.588 2.587Q10.175 2 11 2H13Q13.825 2 14.413"
          + " 2.587Q15 3.175 15 4ZM11 9H13V4H11ZM12 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5Q12 14.5"
          + " 12 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5Q12 14.5 12"
          + " 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5Q12 14.5 12 14.5ZM9 15Q9.625 15"
          + " 10.062 14.562Q10.5 14.125 10.5 13.5Q10.5 12.875 10.062 12.438Q9.625 12 9 12Q8.375 12"
          + " 7.938 12.438Q7.5 12.875 7.5 13.5Q7.5 14.125 7.938 14.562Q8.375 15 9 15ZM6"
          + " 18H12V17.55Q12 17.125 11.762 16.762Q11.525 16.4 11.1 16.2Q10.6 15.975 10.088"
          + " 15.863Q9.575 15.75 9 15.75Q8.425 15.75 7.913 15.863Q7.4 15.975 6.9 16.2Q6.475 16.4"
          + " 6.238 16.762Q6 17.125 6 17.55ZM9 9H4Q4 9 4 9Q4 9 4 9V20Q4 20 4 20Q4 20 4 20H20Q20 20"
          + " 20 20Q20 20 20 20V9Q20 9 20 9Q20 9 20 9H15Q15 9.825 14.413 10.412Q13.825 11 13"
          + " 11H11Q10.175 11 9.588 10.412Q9 9.825 9 9Z";
  // Person
  public static final String NAME_SVG_PATH =
      "M12 12Q10.35 12 9.175 10.825Q8 9.65 8 8Q8 6.35 9.175 5.175Q10.35 4 12 4Q13.65 4 14.825"
          + " 5.175Q16 6.35 16 8Q16 9.65 14.825 10.825Q13.65 12 12 12ZM4 20V17.2Q4 16.35 4.438"
          + " 15.637Q4.875 14.925 5.6 14.55Q7.15 13.775 8.75 13.387Q10.35 13 12 13Q13.65 13 15.25"
          + " 13.387Q16.85 13.775 18.4 14.55Q19.125 14.925 19.562 15.637Q20 16.35 20 17.2V20ZM6"
          + " 18H18V17.2Q18 16.925 17.863 16.7Q17.725 16.475 17.5 16.35Q16.15 15.675 14.775"
          + " 15.337Q13.4 15 12 15Q10.6 15 9.225 15.337Q7.85 15.675 6.5 16.35Q6.275 16.475 6.138"
          + " 16.7Q6 16.925 6 17.2ZM12 10Q12.825 10 13.413 9.412Q14 8.825 14 8Q14 7.175 13.413"
          + " 6.588Q12.825 6 12 6Q11.175 6 10.588 6.588Q10 7.175 10 8Q10 8.825 10.588 9.412Q11.175"
          + " 10 12 10ZM12 8Q12 8 12 8Q12 8 12 8Q12 8 12 8Q12 8 12 8Q12 8 12 8Q12 8 12 8Q12 8 12"
          + " 8Q12 8 12 8ZM12 18Q12 18 12 18Q12 18 12 18Q12 18 12 18Q12 18 12 18Q12 18 12 18Q12 18"
          + " 12 18Q12 18 12 18Q12 18 12 18Z";
  // Numbers
  public static final String NUMBER_SVG_PATH =
      "M20.5,10L21,8h-4l1-4h-2l-1,4h-4l1-4h-2L9,8H5l-0.5,2h4l-1,4h-4L3,16h4l-1,4h2l1-4h4l-1,4h2l1-4h4l0.5-2h-4l1-4H20.5z"
          + " M13.5,14h-4l1-4h4L13.5,14z";
  public static final String PLUS_SVG_PATH =
      "M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z";
  public static final String SEARCH_SVG_PATH =
      "M55.146,51.887L41.588,37.786c3.486-4.144,5.396-9.358,5.396-14.786c0-12.682-10.318-23-23-23s-23,10.318-23,23"
          + "  s10.318,23,23,23c4.761,0,9.298-1.436,13.177-4.162l13.661,14.208c0.571,0.593,1.339,0.92,2.162,0.92"
          + "  c0.779,0,1.518-0.297,2.079-0.837C56.255,54.982,56.293,53.08,55.146,51.887z"
          + " M23.984,6c9.374,0,17,7.626,17,17s-7.626,17-17,17 s-17-7.626-17-17S14.61,6,23.984,6z";
  // Radio Button Checked
  public static final String RADIO_BUTTON_PATH =
      "M12 17Q14.075 17 15.538 15.537Q17 14.075 17 12Q17 9.925 15.538 8.462Q14.075 7 12 7Q9.925 7"
          + " 8.463 8.462Q7 9.925 7 12Q7 14.075 8.463 15.537Q9.925 17 12 17ZM12 22Q9.925 22 8.1"
          + " 21.212Q6.275 20.425 4.925 19.075Q3.575 17.725 2.788 15.9Q2 14.075 2 12Q2 9.925 2.788"
          + " 8.1Q3.575 6.275 4.925 4.925Q6.275 3.575 8.1 2.787Q9.925 2 12 2Q14.075 2 15.9"
          + " 2.787Q17.725 3.575 19.075 4.925Q20.425 6.275 21.212 8.1Q22 9.925 22 12Q22 14.075"
          + " 21.212 15.9Q20.425 17.725 19.075 19.075Q17.725 20.425 15.9 21.212Q14.075 22 12"
          + " 22ZM12 12Q12 12 12 12Q12 12 12 12Q12 12 12 12Q12 12 12 12Q12 12 12 12Q12 12 12 12Q12"
          + " 12 12 12Q12 12 12 12ZM12 20Q15.325 20 17.663 17.663Q20 15.325 20 12Q20 8.675 17.663"
          + " 6.337Q15.325 4 12 4Q8.675 4 6.338 6.337Q4 8.675 4 12Q4 15.325 6.338 17.663Q8.675 20"
          + " 12 20Z";
  // Notes
  public static final String TEXT_SVG_PATH = "M21 11.01L3 11v2h18zM3 16h12v2H3zM21 6H3v2.01L21 8z";
  public static final String UNKNOWN_SVG_PATH =
      "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0"
          + " 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4"
          + " 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5"
          + " 0-2.21-1.79-4-4-4z";
  public static final String TRASH_CAN_SVG_PATH =
      "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1"
          + " 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16";
  public static final String WARNING_SVG_PATH =
      "M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742"
          + " 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012"
          + " 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z";

  public static final String ACCORDION_BUTTON_PATH = "M19 9l-7 7-7-7";

  public static ContainerTag questionTypeSvg(QuestionType type, int size) {
    return questionTypeSvg(type, size, size);
  }

  public static ContainerTag questionTypeSvg(QuestionType type, int width, int height) {
    String iconPath = "";
    switch (type) {
      case ADDRESS:
        iconPath = Icons.ADDRESS_SVG_PATH;
        break;
      case CHECKBOX:
        iconPath = Icons.CHECKBOX_SVG_PATH;
        break;
      case CURRENCY:
        return svg(Icons.CURRENCY_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case DATE:
        iconPath = Icons.DATE_SVG_PATH;
        break;
      case DROPDOWN:
        iconPath = Icons.DROPDOWN_SVG_PATH;
        break;
      case EMAIL:
        iconPath = Icons.EMAIL_SVG_PATH;
        break;
      case FILEUPLOAD:
        iconPath = Icons.FILEUPLOAD_SVG_PATH;
        break;
      case ID:
        iconPath = Icons.ID_SVG_PATH;
        break;
      case NAME:
        iconPath = Icons.NAME_SVG_PATH;
        break;
      case NUMBER:
        iconPath = Icons.NUMBER_SVG_PATH;
        break;
      case RADIO_BUTTON:
        iconPath = Icons.RADIO_BUTTON_PATH;
        break;
      case ENUMERATOR:
        iconPath = Icons.ENUMERATOR_SVG_PATH;
        break;
      case STATIC:
        return svg(Icons.ANNOTATION_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case TEXT:
        iconPath = Icons.TEXT_SVG_PATH;
        break;
      default:
        iconPath = Icons.UNKNOWN_SVG_PATH;
    }
    return svg(iconPath, width, height);
  }

  public static ContainerTag svg(String pathString, int pixelSize) {
    return svg(pathString, pixelSize, pixelSize);
  }

  public static ContainerTag svg(String pathString, int width, int height) {
    return svg(pathString).attr("viewBox", String.format("0 0 %1$d %2$d", width, height));
  }

  private static ContainerTag svg(String pathString) {
    return svg().with(path(pathString));
  }

  private static ContainerTag svg() {
    return new ContainerTag("svg")
        .attr("xmlns", "http://www.w3.org/2000/svg")
        .attr("fill", "currentColor")
        .attr("stroke", "currentColor")
        .attr("stroke-width", "1%")
        .attr("aria-hidden", "true");
  }

  private static ContainerTag path(String pathString) {
    return new ContainerTag("path").attr("d", pathString);
  }
}

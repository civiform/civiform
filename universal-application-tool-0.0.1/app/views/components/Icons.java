package views.components;

import j2html.tags.ContainerTag;
import services.question.types.QuestionType;

/** Class to hold constants for icons and provide methods for rendering SVG components. */
public class Icons {
  public static final String ADDRESS_SVG_PATH =
      "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38"
          + " 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z";
  public static final String ANNOTATION_SVG_PATH =
      "M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z";
  // Check
  public static final String CHECKBOX_SVG_PATH = "M5 13l4 4L19 7";
  public static final String CURRENCY_SVG_PATH =
      "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3"
          + " 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11"
          + " 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z";
  public static final String DATE_SVG_PATH =
      "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z";
  // Menu
  public static final String DROPDOWN_SVG_PATH = "M4 6h16M4 10h16M4 14h16M4 18h16";
  public static final String EMAIL_SVG_PATH =
      "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2"
          + " 0 002 2z";
  public static final String ENUMERATOR_SVG_PATH =
      "M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2"
          + " 2 0 00-2 2v8a2 2 0 002 2z";
  // Upload
  public static final String FILEUPLOAD_SVG_PATH =
      "M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0"
          + " 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0"
          + " 01-1.414 0z";
  public static final String ID_SVG_PATH =
      "M12 11c0 3.517-1.009 6.799-2.753 9.571m-3.44-2.04l.054-.09A13.916 13.916 0 008 11a4 4 0 118"
          + "0c0 1.017-.07 2.019-.203 3m-2.118 6.844A21.88 21.88 0 0015.171 17m3.839"
          + "1.132c.645-2.266.99-4.659.99-7.132A8 8 0 008 4.07M3 15.364c.64-1.319 1-2.8 1-4.364"
          + "0-1.457.39-2.823 1.07-4";
  public static final String NAME_SVG_PATH =
      "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34"
          + " 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99"
          + " 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z";
  // Calculator
  public static final String NUMBER_SVG_PATH =
      "M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0"
          + " 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0"
          + " 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0"
          + " 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0"
          + " 100-2H13zM9 9a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zM7 8a1 1 0 000 2h.01a1 1 0"
          + " 000-2H7z";
  public static final String PLUS_SVG_PATH =
      "M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z";
  public static final String SEARCH_SVG_PATH =
      "M55.146,51.887L41.588,37.786c3.486-4.144,5.396-9.358,5.396-14.786c0-12.682-10.318-23-23-23s-23,10.318-23,23"
          + "  s10.318,23,23,23c4.761,0,9.298-1.436,13.177-4.162l13.661,14.208c0.571,0.593,1.339,0.92,2.162,0.92"
          + "  c0.779,0,1.518-0.297,2.079-0.837C56.255,54.982,56.293,53.08,55.146,51.887z"
          + " M23.984,6c9.374,0,17,7.626,17,17s-7.626,17-17,17 s-17-7.626-17-17S14.61,6,23.984,6z";
  // Custom: the circle from Stop plus the little circle from Cog
  public static final String RADIO_BUTTON_OUTER_SVG_PATH = "M21 12a9 9 0 11-18 0 9 9 0 0118 0z";
  public static final String RADIO_BUTTON_INNER_SVG_PATH = "M15 12a3 3 0 11-6 0 3 3 0 016 0z";
  public static final String TEXT_SVG_PATH =
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
        return svg(Icons.CHECKBOX_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case CURRENCY:
        return svg(Icons.CURRENCY_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case DATE:
        return svg(Icons.DATE_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case DROPDOWN:
        return svg(Icons.DROPDOWN_SVG_PATH, width, height)
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case EMAIL:
        return svg(Icons.EMAIL_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case FILEUPLOAD:
        return svg(Icons.FILEUPLOAD_SVG_PATH, width, height)
            .attr("fill-rule", "evenodd")
            .attr("clip-rule", "evenodd");
      case ID:
        return svg(Icons.ID_SVG_PATH, width, height);
      case NAME:
        iconPath = Icons.NAME_SVG_PATH;
        break;
      case NUMBER:
        return svg(Icons.NUMBER_SVG_PATH, width, height).attr("fill-rule", "evenodd");
      case RADIO_BUTTON:
        return svg(Icons.RADIO_BUTTON_OUTER_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2")
            .with(path(Icons.RADIO_BUTTON_INNER_SVG_PATH));
      case ENUMERATOR:
        iconPath = Icons.ENUMERATOR_SVG_PATH;
        return svg(iconPath, width, height).attr("fill", "transparent").attr("stroke-width", "2");
      case STATIC:
        return svg(Icons.ANNOTATION_SVG_PATH, width, height)
            .attr("fill", "none")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round")
            .attr("stroke-width", "2");
      case TEXT:
      default:
        iconPath = Icons.TEXT_SVG_PATH;
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

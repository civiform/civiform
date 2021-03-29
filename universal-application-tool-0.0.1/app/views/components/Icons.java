package views.components;

import j2html.tags.ContainerTag;
import services.question.QuestionType;

public class Icons {

  // Copied from heroicons.com

  public static final String LOGIN_BANNER_PATH =
      "M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001"
          + "4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988"
          + "3.988 0 01-1.564-.317z";

  public static final String ADDRESS_SVG_PATH =
      "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38"
          + " 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z";
  // View-list
  public static final String DROPDOWN_SVG_PATH = "M4 6h16M4 10h16M4 14h16M4 18h16";
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
  public static final String SEARCH_SVG_PATH =
      "M55.146,51.887L41.588,37.786c3.486-4.144,5.396-9.358,5.396-14.786c0-12.682-10.318-23-23-23s-23,10.318-23,23"
          + "  s10.318,23,23,23c4.761,0,9.298-1.436,13.177-4.162l13.661,14.208c0.571,0.593,1.339,0.92,2.162,0.92"
          + "  c0.779,0,1.518-0.297,2.079-0.837C56.255,54.982,56.293,53.08,55.146,51.887z"
          + " M23.984,6c9.374,0,17,7.626,17,17s-7.626,17-17,17 "
          + " s-17-7.626-17-17S14.61,6,23.984,6z";
  public static final String REPEATER_SVG_PATH =
      "M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2"
          + " 2 0 00-2 2v8a2 2 0 002 2z";
  public static final String TEXT_SVG_PATH =
      "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0"
          + " 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4"
          + " 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5"
          + " 0-2.21-1.79-4-4-4z";

  public static ContainerTag questionTypeSvg(QuestionType type, int size) {
    return questionTypeSvg(type, size, size);
  }

  public static ContainerTag questionTypeSvg(QuestionType type, int width, int height) {
    String iconPath = "";
    switch (type) {
      case ADDRESS:
        iconPath = Icons.ADDRESS_SVG_PATH;
        break;
      case DROPDOWN:
        return svg(Icons.DROPDOWN_SVG_PATH, width, height)
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round");
      case NAME:
        iconPath = Icons.NAME_SVG_PATH;
        break;
      case NUMBER:
        return svg(Icons.NUMBER_SVG_PATH, width, height).attr("fill-rule", "evenodd");
      case REPEATER:
        iconPath = Icons.REPEATER_SVG_PATH;
        return svg(iconPath, width, height).attr("fill", "transparent").attr("stroke-width", "2");
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

  public static ContainerTag svg(String pathString) {
    return new ContainerTag("svg")
        .attr("xmlns", "http://www.w3.org/2000/svg")
        .attr("fill", "currentColor")
        .attr("stroke", "currentColor")
        .attr("stroke-width", "1%")
        .attr("aria-hidden", "true")
        .with(
            new ContainerTag("path")
                .attr("xmlns", "http://www.w3.org/2000/svg")
                .attr("d", pathString));
  }
}

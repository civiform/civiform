package services.export;

import com.google.common.collect.ImmutableList;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;
import services.geo.ServiceAreaInclusion;

final class ExportFormatUtils {
  // 8 decimal places of lat/long is millimeter precision, so we round at 8 and always use a
  // period as the decimal seperator.
  private static DecimalFormat latLongFormatter =
      new DecimalFormat("#.########", new DecimalFormatSymbols(Locale.US));

  private static NumberFormat integerAnswerFormatter =
      new DecimalFormat("0", new DecimalFormatSymbols(Locale.US));

  /**
   * Format a latitude or longitude coordinate as a string, rounding to 8 decimal places (millimeter
   * precision) and stripping trailing zeros after the decimal.
   */
  static String formatLatOrLongAsString(Double coordinate) {
    return latLongFormatter.format(coordinate);
  }

  /** Format a number answer as a string. */
  static String formatNumberAnswer(Long answer) {
    return integerAnswerFormatter.format(answer);
  }

  /** Format a fileKey as an admin ACLed URL */
  static String formatFileUrlForAdmin(String baseUrl, String fileKey) {
    return baseUrl
        + controllers.routes.FileController.acledAdminShow(
                URLEncoder.encode(fileKey, StandardCharsets.UTF_8))
            .url();
  }

  /** Takes a list of {@link ServiceAreaInclusion}s and transforms them into a delimited string */
  // TODO(#7134): Only here for api backwards compatibility. Long term remove.
  static String serializeServiceArea(
      ImmutableList<ServiceAreaInclusion> serviceAreaInclusionGroup) {
    return serviceAreaInclusionGroup.stream()
        .map(
            (area) ->
                area.getServiceAreaId()
                    + "_"
                    + area.getState().getSerializationFormat()
                    + "_"
                    + area.getTimeStamp())
        .collect(Collectors.joining(","));
  }
}

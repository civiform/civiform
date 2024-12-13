package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

public class ExportFormatUtilsTest {
  @Test
  public void formatNumberAnswer_integer() {
    assertThat(ExportFormatUtils.formatNumberAnswer(5L)).isEqualTo("5");
  }

  @Test
  public void formatNumberAnswer_thousands() {
    assertThat(ExportFormatUtils.formatNumberAnswer(5000L)).isEqualTo("5000");
  }

  @Test
  public void formatNumberAnswer_negativeInteger() {
    assertThat(ExportFormatUtils.formatNumberAnswer(-5L)).isEqualTo("-5");
  }

  @Test
  public void formatLatOrLongAsString_wholeNumber() {
    assertThat(ExportFormatUtils.formatLatOrLongAsString(5.0)).isEqualTo("5");
  }

  @Test
  public void formatLatOrLongAsString_thousands() {
    assertThat(ExportFormatUtils.formatLatOrLongAsString(5555.0)).isEqualTo("5555");
  }

  @Test
  public void formatLatOrLongAsString_decimals() {
    assertThat(ExportFormatUtils.formatLatOrLongAsString(5.555)).isEqualTo("5.555");
  }

  @Test
  public void formatLatOrLongAsString_moreThanEightDecimals() {
    assertThat(ExportFormatUtils.formatLatOrLongAsString(5.5555555555)).isEqualTo("5.55555556");
  }

  @Test
  public void formatFileUrlForAdmin_buildsUrl() {
    assertThat(ExportFormatUtils.formatFileUrlForAdmin("https://my-civiform.com", "my-file-key"))
        .isEqualTo("https://my-civiform.com/admin/applicant-files/my-file-key");
  }

  @Test
  public void formatFileUrlForAdmin_nonUrlSafeKey() {
    assertThat(ExportFormatUtils.formatFileUrlForAdmin("https://my-civiform.com", "my_unsafe<key"))
        .isEqualTo("https://my-civiform.com/admin/applicant-files/my_unsafe%253Ckey");
  }

  @Test
  public void serializeServiceArea_multipleServiceAreas() {
    var serviceAreaInclusions =
        ImmutableList.of(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("cityvilleTownship")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(1709069741L)
                .build(),
            ServiceAreaInclusion.builder()
                .setServiceAreaId("portland")
                .setState(ServiceAreaState.NOT_IN_AREA)
                .setTimeStamp(1709069741L)
                .build());

    assertThat(ExportFormatUtils.serializeServiceArea(serviceAreaInclusions))
        .isEqualTo("cityvilleTownship_InArea_1709069741,portland_NotInArea_1709069741");
  }
}

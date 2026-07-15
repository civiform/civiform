package views.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.FlashKey;
import org.junit.Test;
import play.mvc.Http;
import services.AlertType;

public class PageAlertTest {

  @Test
  public void typeClass_isLowercaseUswdsModifier() {
    assertThat(PageAlert.error("oops").typeClass()).isEqualTo("error");
    assertThat(PageAlert.success("yay").typeClass()).isEqualTo("success");
    assertThat(PageAlert.warning("hmm").typeClass()).isEqualTo("warning");
    assertThat(PageAlert.info("fyi").typeClass()).isEqualTo("info");
  }

  @Test
  public void factories_setMatchingAlertType() {
    assertThat(PageAlert.error("oops").type()).isEqualTo(AlertType.ERROR);
    assertThat(PageAlert.success("yay").type()).isEqualTo(AlertType.SUCCESS);
    assertThat(PageAlert.warning("hmm").type()).isEqualTo(AlertType.WARNING);
    assertThat(PageAlert.info("fyi").type()).isEqualTo(AlertType.INFO);
  }

  @Test
  public void fromFlash_emptyFlash_returnsNoAlerts() {
    Http.Flash flash = new Http.Flash(ImmutableMap.of());

    assertThat(PageAlert.fromFlash(flash)).isEmpty();
  }

  @Test
  public void fromFlash_mapsStandardKeysToAlerts() {
    Http.Flash flash =
        new Http.Flash(
            ImmutableMap.of(
                FlashKey.ERROR, "it broke",
                FlashKey.WARNING, "be careful",
                FlashKey.SUCCESS, "it worked"));

    assertThat(PageAlert.fromFlash(flash))
        .containsExactly(
            PageAlert.error("it broke"),
            PageAlert.warning("be careful"),
            PageAlert.success("it worked"));
  }

  @Test
  public void fromFlash_ignoresUnrelatedKeys() {
    Http.Flash flash =
        new Http.Flash(
            ImmutableMap.of(
                FlashKey.SUCCESS, "it worked",
                FlashKey.CONCURRENT_UPDATE, "not a page alert"));

    assertThat(PageAlert.fromFlash(flash))
        .containsExactly(PageAlert.success("it worked"));
  }

  @Test
  public void fromFlash_returnsImmutableList() {
    Http.Flash flash = new Http.Flash(ImmutableMap.of(FlashKey.SUCCESS, "it worked"));

    assertThat(PageAlert.fromFlash(flash)).isInstanceOf(ImmutableList.class);
  }
}

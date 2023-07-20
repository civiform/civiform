package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.text;
import static org.assertj.core.api.Assertions.assertThat;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAMS_INDEX_LOGIN_PROMPT;

import org.junit.Test;

public class ModalTest {

  private static final Modal.Builder MODAL_BUILDER =
      Modal.builder()
          .setModalId("modal-id")
          .setContent(div().with(text("content")))
          .setModalTitle("Modal Title");

  @Test
  public void buildsModal_withIdTitleAndContent() {
    Modal modal = MODAL_BUILDER.build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("<div id=\"modal-id\"");
    assertThat(rendered).contains("<div>content</div>");
    assertThat(rendered).contains("Modal Title");
  }

  @Test
  public void buildsModal_withSetWidth() {
    Modal modal = MODAL_BUILDER.setWidth(Modal.Width.FOURTH).build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("w-1/4");
  }

  @Test
  public void buildsModal_withRepeatOpenBehavior_alwaysShow() {
    Modal modal =
        MODAL_BUILDER.setRepeatOpenBehavior(Modal.RepeatOpenBehavior.alwaysShow()).build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).doesNotContain("only-show-once-group");
  }

  @Test
  public void buildsModal_withRepeatOpenBehavior_onlyShowOnceNoBypassUrl() {
    Modal modal =
        MODAL_BUILDER
            .setRepeatOpenBehavior(
                Modal.RepeatOpenBehavior.showOnlyOnce(PROGRAMS_INDEX_LOGIN_PROMPT))
            .build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("only-show-once-group=\"programs_index_login_prompt\"");
    assertThat(rendered).doesNotContain("bypass-url");
  }

  @Test
  public void buildsModal_withRepeatOpenBehavior_onlyShowOnceWithBypassUrl() {
    Modal modal =
        MODAL_BUILDER
            .setRepeatOpenBehavior(
                Modal.RepeatOpenBehavior.showOnlyOnce(
                    PROGRAMS_INDEX_LOGIN_PROMPT, "https://bypassurl.com"))
            .build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("only-show-once-group=\"programs_index_login_prompt\"");
    assertThat(rendered).contains("bypass-url=\"https://bypassurl.com\"");
  }
}

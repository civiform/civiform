package views.components;

import static j2html.TagCreator.div;
import static j2html.TagCreator.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static play.api.test.Helpers.stubLangs;
import static play.test.Helpers.stubMessagesApi;
import static scala.collection.JavaConverters.asScalaIteratorConverter;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAMS_INDEX_LOGIN_PROMPT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import services.MessageKey;

public class ModalTest {

  private static final Modal.Builder MODAL_BUILDER =
      Modal.builder()
          .setModalId("modal-id")
          .setLocation(Modal.Location.ADMIN_FACING)
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

  @Test
  public void buildsModal_adminFacingNoMessages_usesDefaultClose() {
    Modal modal =
        Modal.builder()
            .setModalId("modal-id")
            .setLocation(Modal.Location.ADMIN_FACING)
            .setContent(div().with(text("content")))
            .setModalTitle("Modal Title")
            .build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("aria-label=\"Close\"");
  }

  @Test
  public void buildsModal_debugNoMessages_usesDefaultClose() {
    Modal modal =
        Modal.builder()
            .setModalId("modal-id")
            .setLocation(Modal.Location.DEBUG)
            .setContent(div().with(text("content")))
            .setModalTitle("Modal Title")
            .build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("aria-label=\"Close\"");
  }

  @Test
  public void buildsModal_applicantFacingNoMessages_throwsException() {

    Modal.Builder modalBuilder =
        Modal.builder()
            .setModalId("modal-id")
            .setLocation(Modal.Location.APPLICANT_FACING)
            .setContent(div().with(text("content")))
            .setModalTitle("Modal Title");

    assertThatThrownBy(modalBuilder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void buildsModal_applicantFacingHasMessages_usesCloseFromMessage() {
    Lang lang = Lang.forCode("es-US");

    // Note: There's a bit of an API mismatch in Play, where [stubMessagesApi] expects
    // classes in the play.i18n package but [stubLangs] expects classes in the
    // play.api.i18n package, which is why the same Lang is created twice.
    Langs allLangs =
        new Langs(
            stubLangs(
                asScalaIteratorConverter(
                        ImmutableList.of(play.api.i18n.Lang.get("es-US").get()).iterator())
                    .asScala()
                    .toSeq()));

    MessagesApi messagesApi =
        stubMessagesApi(
            ImmutableMap.of(
                lang.code(), ImmutableMap.of(MessageKey.BUTTON_CLOSE.getKeyName(), "es-US-Close")),
            allLangs);
    Modal modal =
        Modal.builder()
            .setModalId("modal-id")
            .setLocation(Modal.Location.APPLICANT_FACING)
            .setContent(div().with(text("content")))
            .setModalTitle("Modal Title")
            .setMessages(messagesApi.preferred(ImmutableList.of(lang)))
            .build();
    String rendered = modal.getContainerTag().render();

    assertThat(rendered).contains("aria-label=\"es-US-Close\"");
  }
}

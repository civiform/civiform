package views;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.specialized.ButtonTag;
import org.junit.Test;
import views.components.buttons.Button;
import views.components.buttons.ButtonAction;
import views.components.buttons.ButtonStyle;

public class BaseHtmlViewTest {

  @Test
  public void submitButton_rendersAFormSubmitButton() {
    ButtonTag result =
        Button.builder()
            .setText("text contents")
            .setStyle(ButtonStyle.SOLID_BLUE)
            .setButtonAction(ButtonAction.ofSubmit())
            .build();

    assertThat(result.render()).isEqualTo("<button type=\"submit\">text contents</button>");
  }
}

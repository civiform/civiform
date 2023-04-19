package views.components.buttons;

import static j2html.TagCreator.button;

import com.google.auto.value.AutoValue;
import j2html.tags.specialized.ButtonTag;
import java.util.Optional;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

@AutoValue
public abstract class Button {

  // The text of the button.
  public abstract String text();

  // The style of the button, based on a predetermined set of available styles.
  public abstract ButtonStyle style();

  // What to do when the button is clicked.
  public abstract ButtonAction buttonAction();

  // The id of the button.
  public abstract Optional<String> id();

  // Custom classes to be added to the button. Should be used sparingly;
  // significant changes to CiviForm buttons should be made to ButtonStyles.java instead.
  public abstract Optional<String[]> customClasses();

  // Forces us to set text at compile-time.
  public interface RequiredText {
    RequiredStyle setText(String text);
  }

  // Forces us to set style at compile-time.
  public interface RequiredStyle {
    RequiredButtonAction setStyle(ButtonStyle style);
  }

  // Forces us to set button action at compile-time.
  public interface RequiredButtonAction {
    Builder setButtonAction(ButtonAction buttonAction);
  }

  public static Button.RequiredText builder() {
    return new AutoValue_Button.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements RequiredStyle, RequiredText, RequiredButtonAction {

    public abstract Builder setId(String id);

    public abstract Builder setCustomClasses(String... customClasses);

    // Effectively private. This is the build method that AutoValue will generate
    // an implementation for.
    abstract Button autoBuild();

    public ButtonTag build() {
      Button internalButton = autoBuild();
      ButtonTag button =
          button()
              .withClasses(
                  // Join the canonical button styles from ButtonStyles.java with custom classes
                  // the client requests.
                  StyleUtils.joinStyles(
                      internalButton.style().getStyles(),
                      StyleUtils.joinStyles(internalButton.customClasses().orElse(new String[0]))))
              .withText(internalButton.text());

      internalButton.id().ifPresent(button::withId);

      // Add attributes or types to the ButtonTag based on the client's ButtonAction choice.
      switch (internalButton.buttonAction().action()) {
        case REDIRECT:
          button.attr("data-redirect-to", internalButton.buttonAction().redirect());
          break;
        case SUBMIT_WITH_FORM_ID:
          button.withType("submit").withForm(internalButton.buttonAction().submitWithFormId());
          break;
        case SUBMIT_WITH_FORM_ACTION:
          button
              .withType("submit")
              .withFormaction(internalButton.buttonAction().submitWithFormAction());
          break;
        case SUBMIT:
          button.withType("submit");
          break;
        case CLOSE_MODAL:
          button.withClass(ReferenceClasses.MODAL_CLOSE);
          break;
        case NONE:
          break;
      }

      return button;
    }
  }
}

package views.components.buttons;

import com.google.auto.value.AutoOneOf;

/**
 * Represents the action that should be taken when the end user clicks a button. As an AutoOneOf,
 * exactly one option from the enum will be selected at a time.
 */
@AutoOneOf(ButtonAction.Action.class)
public abstract class ButtonAction {
  public enum Action {
    REDIRECT,
    SUBMIT_WITH_FORM_ID,
    SUBMIT
  }

  public abstract Action action();

  public static ButtonAction ofRedirect(String redirectUrl) {
    return AutoOneOf_ButtonAction.redirect(redirectUrl);
  }

  public static ButtonAction ofSubmitWithFormId(String formId) {
    return AutoOneOf_ButtonAction.submitWithFormId(formId);
  }

  public static ButtonAction ofSubmit() {
    return AutoOneOf_ButtonAction.submit();
  }

  abstract String redirect();

  abstract String submitWithFormId();

  abstract void submit();
}

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
    SUBMIT_WITH_FORM_ACTION,
    SUBMIT,
    CLOSE_MODAL,
    NONE
  }

  public abstract Action action();

  public static ButtonAction ofRedirect(String redirectUrl) {
    return AutoOneOf_ButtonAction.redirect(redirectUrl);
  }

  public static ButtonAction ofSubmitWithFormId(String formId) {
    return AutoOneOf_ButtonAction.submitWithFormId(formId);
  }

  public static ButtonAction ofSubmitWithFormAction(String formAction) {
    return AutoOneOf_ButtonAction.submitWithFormAction(formAction);
  }

  public static ButtonAction ofSubmit() {
    return AutoOneOf_ButtonAction.submit();
  }

  public static ButtonAction ofCloseModal() {
    return AutoOneOf_ButtonAction.closeModal();
  }

  public static ButtonAction ofNone() {
    return AutoOneOf_ButtonAction.none();
  }

  abstract String redirect();

  abstract String submitWithFormId();

  abstract String submitWithFormAction();

  abstract void submit();

  abstract void closeModal();

  abstract void none();
}

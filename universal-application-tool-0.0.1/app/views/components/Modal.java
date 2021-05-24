package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;

import com.google.auto.value.AutoValue;
import j2html.tags.Tag;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

@AutoValue
public abstract class Modal {
  public abstract String modalId();

  public abstract String displayName();

  public abstract Tag modal();

  public static Modal create(String modalId, String displayName, Tag modal) {
    return new AutoValue_Modal(modalId, displayName, modal);
  }

  public Tag getContainerTag() {
    return div(modal()).withId(modalId()).withClasses(ReferenceClasses.MODAL, BaseStyles.MODAL);
  }

  public Tag getButton() {
    return button(modalId() + "-button", displayName()).withClasses(BaseStyles.MODAL_BUTTON);
  }
}

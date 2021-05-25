package views.components;

import static j2html.TagCreator.div;
import static views.BaseHtmlView.button;

import com.google.auto.value.AutoValue;
import j2html.tags.Tag;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

@AutoValue
public abstract class Modal {
  public abstract String modalId();

  public abstract String displayName();

  public abstract Tag content();

  public static Modal create(String modalId, String displayName, Tag content) {
    return new AutoValue_Modal(modalId, displayName, content);
  }

  public Tag getContainerTag() {
    return div()
        .withId(modalId())
        .withClasses(ReferenceClasses.MODAL, BaseStyles.MODAL)
        .with(getModalHeader())
        .with(getContent());
  }

  public Tag getButton() {
    return button(modalId() + "-button", displayName()).withClasses(BaseStyles.MODAL_BUTTON);
  }

  private Tag getContent() {
    return div(content()).withClasses(BaseStyles.MODAL_CONTENT);
  }

  private Tag getModalHeader() {
    return div()
      .withClasses(BaseStyles.MODAL_HEADER)
        .with(div(displayName()).withClasses(Styles.TEXT_LG))
    .with(div("x").withId(modalId() + "-close").withClasses(BaseStyles.MODAL_CLOSE_BUTTON));
  }
}

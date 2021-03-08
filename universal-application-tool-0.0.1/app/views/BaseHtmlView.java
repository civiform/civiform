package views;

import static j2html.TagCreator.br;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import play.mvc.Http;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.html.helper.CSRF;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {

  public Tag renderHeader(String headerText) {
    return h1(headerText);
  }

  protected DomContent textInputWithLabel(
      String labelValue, String inputId, Optional<String> value) {
    FieldWithLabel field = FieldWithLabel.createInputWithLabel(inputId).setLabelText(labelValue);
    if (value.isPresent()) {
      field.setValue(value.get());
    }
    return field.getContainer();
  }

  public DomContent textInputWithLabel(
      String labelValue, String inputId, String value) {
    Optional<String> optionalValue = Optional.ofNullable(value).filter(s -> !s.trim().isEmpty());

    return textInputWithLabel(labelValue, inputId, optionalValue);
  }

  public DomContent textAreaWithLabel(
      String labelValue, String inputId, Optional<String> value) {
    FieldWithLabel field = FieldWithLabel.createTextAreaWithLabel(inputId).setLabelText(labelValue);
    if (value.isPresent()) {
      field.setValue(value.get());
    }
    return field.getContainer();
  }

  public DomContent textAreaWithLabel(
      String labelValue, String inputId, String value) {
    Optional<String> optionalValue = Optional.ofNullable(value).filter(s -> !s.trim().isEmpty());

    return textAreaWithLabel(labelValue, inputId, optionalValue);
  }

  protected Tag checkboxInputWithLabel(
      String labelText, String inputId, String inputName, String inputValue) {
    return label()
        .with(
            input().withType("checkbox").withName(inputName).withValue(inputValue).withId(inputId),
            text(labelText));
  }

  protected Tag passwordField(String id, String fieldName, String labelText) {
    return label()
        .with(text(labelText), input().withType("password").withName(fieldName).withId(id))
        .attr(Attr.FOR, fieldName);
  }

  protected Tag passwordField(String fieldName, String labelText) {
    return label()
        .with(text(labelText), input().withType("password").withName(fieldName))
        .attr(Attr.FOR, fieldName);
  }

  protected Tag button(String textContents) {
    return TagCreator.button(text(textContents)).withType("button");
  }

  protected Tag button(String id, String textContents) {
    return button(textContents).withId(id);
  }

  protected Tag submitButton(String textContents) {
    return TagCreator.button(text(textContents)).withType("submit");
  }

  protected Tag submitButton(String id, String textContents) {
    return submitButton(textContents).withId(id);
  }

  protected Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text).attr("onclick", String.format("window.location = '%s';", redirectUrl));
  }

  public ImmutableList<DomContent> formSelect(
      String labelValue,
      String selectId,
      ImmutableList<SimpleEntry<String, String>> options,
      String selectedValue) {
    SelectWithLabel selectWithLabel = new SelectWithLabel(selectId)
      .setLabelText(labelValue)
      .setOptions(options)
      .setValue(selectedValue);
    return ImmutableList.of(selectWithLabel.getContainer());
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all UAT forms.
   */
  protected Tag makeCsrfTokenInputTag(Http.Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private String getCsrfToken(Http.Request request) {
    return CSRF.getToken(request.asScala()).value();
  }
}

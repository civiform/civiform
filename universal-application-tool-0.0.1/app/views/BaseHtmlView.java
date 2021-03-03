package views;

import static j2html.TagCreator.br;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;
import static j2html.TagCreator.text;
import static j2html.TagCreator.textarea;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import play.mvc.Http;
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

  protected ImmutableList<DomContent> textInputWithLabel(
      String labelValue, String inputId, Optional<String> value) {
    Tag labelTag = label(labelValue).attr(Attr.FOR, inputId);
    Tag inputTag = input().withType("text").withId(inputId).withName(inputId);
    if (value.isPresent()) {
      inputTag.withValue(value.get());
    }

    return ImmutableList.of(labelTag, br(), inputTag, br(), br());
  }

  public ImmutableList<DomContent> textInputWithLabel(
      String labelValue, String inputId, String value) {
    Optional<String> optionalValue = Optional.ofNullable(value).filter(s -> !s.trim().isEmpty());

    return textInputWithLabel(labelValue, inputId, optionalValue);
  }

  public ImmutableList<DomContent> textAreaWithLabel(
      String labelValue, String inputId, Optional<String> value) {
    Tag labelTag = label(labelValue).attr(Attr.FOR, inputId);
    Tag textAreaTag = textarea(value.orElse("")).withType("text").withId(inputId).withName(inputId);

    return ImmutableList.of(labelTag, br(), textAreaTag, br(), br());
  }

  public ImmutableList<DomContent> textAreaWithLabel(
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

  protected Tag textField(String fieldName, String labelText) {
    return label()
        .with(text(labelText), input().withType("text").withName(fieldName))
        .attr(Attr.FOR, fieldName);
  }

  protected Tag textField(String id, String fieldName, String labelText) {
    return label(text(labelText), input().withType("text").withName(fieldName).withId(id))
        .attr(Attr.FOR, fieldName);
  }

  protected Tag textFieldWithValue(String fieldName, String labelText, String placeholder) {
    return label(
            text(labelText), input().withType("text").withName(fieldName).withValue(placeholder))
        .attr(Attr.FOR, fieldName);
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
    Tag labelTag = label(labelValue).attr(Attr.FOR, selectId);
    ContainerTag selectTag = select().withId(selectId).withName(selectId);

    for (SimpleEntry<String, String> option : options) {
      Tag optionTag = option(option.getKey()).withValue(option.getValue());
      if (option.getValue().equals(selectedValue)) {
        optionTag.attr(Attr.SELECTED);
      }
      selectTag.with(optionTag);
    }

    return ImmutableList.of(labelTag, br(), selectTag, br(), br());
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

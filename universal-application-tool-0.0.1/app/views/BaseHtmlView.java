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
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
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

  protected ImmutableList<DomContent> inputWithLabel(
      String labelValue, String inputId, Optional<String> value) {
    Tag labelTag = label(labelValue).attr("for", inputId);
    Tag inputTag = input().withType("text").withId(inputId).withName(inputId);
    if (value.isPresent()) {
      inputTag.withValue(value.get());
    }
    return ImmutableList.of(labelTag, br(), inputTag, br(), br());
  }

  public ImmutableList<DomContent> textAreaWithLabel(
      String labelValue, String inputId, Optional<String> value) {
    Tag labelTag = label(labelValue).attr("for", inputId);
    Tag textAreaTag = textarea(value.orElse("")).withType("text").withId(inputId).withName(inputId);
    return ImmutableList.of(labelTag, br(), textAreaTag, br(), br());
  }

  public ImmutableList<DomContent> formSelect(
      String labelValue,
      String selectId,
      String[] optionLabels,
      String[] optionValues,
      String selectedValue) {
    Tag labelTag = label(labelValue).attr("for", selectId);
    ContainerTag selectTag = select().withId(selectId).withName(selectId);
    for (int i = 0; i < optionLabels.length && i < optionValues.length; i++) {
      Tag optionTag = option(optionLabels[i]).withValue(optionValues[i]);
      if (optionValues[i].equals(selectedValue)) {
        optionTag.attr("selected");
      }
      selectTag.with(optionTag);
    }
    return ImmutableList.of(labelTag, br(), selectTag, br(), br());
  }

  protected Tag textField(String fieldName, String labelText) {
    return label(text(labelText), input().withType("text").withName(fieldName))
        .attr("for", fieldName);
  }

  protected Tag textField(String id, String fieldName, String labelText) {
    return label(text(labelText), input().withType("text").withName(fieldName).withId(id))
        .attr("for", fieldName);
  }

  protected Tag textFieldWithValue(String fieldName, String labelText, String placeholder) {
    return label(
            text(labelText), input().withType("text").withName(fieldName).withValue(placeholder))
        .attr("for", fieldName);
  }

  protected Tag passwordField(String id, String fieldName, String labelText) {
    return label(text(labelText), input().withType("password").withName(fieldName).withId(id))
        .attr("for", fieldName);
  }

  protected Tag passwordField(String fieldName, String labelText) {
    return label(text(labelText), input().withType("password").withName(fieldName))
        .attr("for", fieldName);
  }

  protected Tag button(String id, String text) {
    return button(text).withId(id);
  }

  protected Tag button(String text) {
    return TagCreator.button(text(text)).withType("button");
  }

  protected Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text).attr("onclick", String.format("window.location = '%s';", redirectUrl));
  }

  protected Tag submitButton(String textContents) {
    return input().withType("submit").withValue(textContents);
  }

  protected Tag submitButton(String id, String textContents) {
    return input().withType("submit").withId(id).withValue(textContents);
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

package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;
import play.data.validation.Constraints;

public class TextAreaElementTagModelProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new TextAreaElementTagModelProcessor(prefix));
  }

  @Builder
  public record Model(
      String id,
      String name,
      String value,
      String label,
      String placeholder,
      String helpText,
      String validationMessage,
      String isValid,
      String required,
      String readonly,
      String disabled,
      String size,
      String minLength,
      String maxLength,
      String markdownEnabled,
      String validationClass,
      String validationField) {}

  public static class ConstraintModel {
    @Constraints.Required private String description;

    public ConstraintModel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return this.description;
    }

    public String getClassName() {
      return this.getClass().getName();
    }
  }

  @Test
  public void basic_textarea_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder().id("description").name("description").label("Description").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="description">Description</label>
<span id="error-message-description" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="description" name="description"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="description"
            name="description"
            label="Description" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="description">Description</label>
<span id="error-message-description" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="description" name="description"></textarea>
</div>
""");
  }

  @Test
  public void disabled_textarea_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder().id("comments").name("comments").label("Comments").disabled("true").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:disabled="${model.disabled()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="comments">Comments</label>
<span id="error-message-comments" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="comments" name="comments" disabled="disabled"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="comments"
            name="comments"
            label="Comments"
            disabled="true" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="comments">Comments</label>
<span id="error-message-comments" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="comments" name="comments" disabled="disabled"></textarea>
</div>
""");
  }

  @Test
  public void readonly_textarea_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder().id("notes").name("notes").label("Notes").readonly("true").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:readonly="${model.readonly()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="notes">Notes</label>
<span id="error-message-notes" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="notes" name="notes" readonly="readonly"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="notes"
            name="notes"
            label="Notes"
            readonly="true" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="notes">Notes</label>
<span id="error-message-notes" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="notes" name="notes" readonly="readonly"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_value_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("comments")
            .name("comments")
            .label("Comments")
            .value("Initial text content")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:value="${model.value()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="comments">Comments</label>
<span id="error-message-comments" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="comments" name="comments">Initial text content</textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="comments"
            name="comments"
            label="Comments"
            value="Initial text content" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="comments">Comments</label>
<span id="error-message-comments" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="comments" name="comments">Initial text content</textarea>
</div>
""");
  }

  @Test
  public void textarea_with_placeholder_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("notes")
            .name("notes")
            .label("Notes")
            .placeholder("Enter your notes here")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:placeholder="${model.placeholder()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="notes">Notes</label>
<span id="error-message-notes" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="notes" name="notes" placeholder="Enter your notes here"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="notes"
            name="notes"
            label="Notes"
            placeholder="Enter your notes here" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="notes">Notes</label>
<span id="error-message-notes" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="notes" name="notes" placeholder="Enter your notes here"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_help_text_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("bio")
            .name("bio")
            .label("Biography")
            .helpText("Please provide a brief biography")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="bio">Biography</label>
<div id="help-text-bio" class="usa-hint">Please provide a brief biography</div>
<span id="error-message-bio" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="bio" name="bio" aria-describedby="help-text-bio"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="bio"
            name="bio"
            label="Biography"
            help-text="Please provide a brief biography" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="bio">Biography</label>
<div id="help-text-bio" class="usa-hint">Please provide a brief biography</div>
<span id="error-message-bio" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="bio" name="bio" aria-describedby="help-text-bio"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_validation_error_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("message")
            .name("message")
            .label("Message")
            .isValid("false")
            .validationMessage("Message is required")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}" />
        """,
        """
<div class="usa-form-group usa-form-group--error">
<label class="usa-label" for="message">Message</label>
<span id="error-message-message" class="usa-error-message" role="alert">Message is required</span>
<textarea class="usa-textarea" id="message" name="message" aria-describedby="error-message-message" aria-invalid="true"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="message"
            name="message"
            label="Message"
            is-valid="false"
            validation-message="Message is required" />
        """,
        """
<div class="usa-form-group usa-form-group--error">
<label class="usa-label" for="message">Message</label>
<span id="error-message-message" class="usa-error-message" role="alert">Message is required</span>
<textarea class="usa-textarea" id="message" name="message" aria-describedby="error-message-message" aria-invalid="true"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_error_and_help_text() {
    assertHtml(
        Model.builder()
            .id("feedback")
            .name("feedback")
            .label("Feedback")
            .helpText("Tell us what you think")
            .isValid("false")
            .validationMessage("Feedback cannot be empty")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}" />
        """,
        """
<div class="usa-form-group usa-form-group--error">
<label class="usa-label" for="feedback">Feedback</label>
<div id="help-text-feedback" class="usa-hint">Tell us what you think</div>
<span id="error-message-feedback" class="usa-error-message" role="alert">Feedback cannot be empty</span>
<textarea class="usa-textarea" id="feedback" name="feedback" aria-describedby="error-message-feedback help-text-feedback" aria-invalid="true"></textarea>
</div>
""");
  }

  @Test
  public void required_textarea_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder().id("terms").name("terms").label("Terms").required("true").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:required="${model.required()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="terms">Terms</label>
<span id="error-message-terms" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="terms" name="terms" required="required"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="terms"
            name="terms"
            label="Terms"
            required="true" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="terms">Terms</label>
<span id="error-message-terms" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="terms" name="terms" required="required"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_size_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder().id("summary").name("summary").label("Summary").size("small").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:size="${model.size()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="summary">Summary</label>
<span id="error-message-summary" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea usa-textarea--small" id="summary" name="summary"></textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="summary"
            name="summary"
            label="Summary"
            size="small" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="summary">Summary</label>
<span id="error-message-summary" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea usa-textarea--small" id="summary" name="summary"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_medium_size() {
    assertHtml(
        Model.builder()
            .id("description")
            .name("description")
            .label("Description")
            .size("medium")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:size="${model.size()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="description">Description</label>
<span id="error-message-description" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea usa-textarea--medium" id="description" name="description"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_all_attributes_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("feedback")
            .name("feedback")
            .label("Feedback")
            .value("Initial feedback")
            .placeholder("Enter your feedback")
            .helpText("Please be specific")
            .required("true")
            .size("md")
            .minLength("1")
            .maxLength("10")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:value="${model.value()}"
            th:placeholder="${model.placeholder()}"
            th:help-text="${model.helpText()}"
            th:required="${model.required()}"
            th:size="${model.size()}"
            th:minlength="${model.minLength()}"
            th:maxlength="${model.maxLength()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="feedback">Feedback</label>
<div id="help-text-feedback" class="usa-hint">Please be specific</div>
<span id="error-message-feedback" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea usa-textarea--md" id="feedback" name="feedback" placeholder="Enter your feedback" required="required" aria-describedby="help-text-feedback" minlength="1" maxlength="10">Initial feedback</textarea>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:textarea
            id="feedback"
            name="feedback"
            label="Feedback"
            value="Initial feedback"
            placeholder="Enter your feedback"
            help-text="Please be specific"
            required="true"
            size="md"
            minlength="1"
            maxlength="10" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="feedback">Feedback</label>
<div id="help-text-feedback" class="usa-hint">Please be specific</div>
<span id="error-message-feedback" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea usa-textarea--md" id="feedback" name="feedback" placeholder="Enter your feedback" required="required" aria-describedby="help-text-feedback" minlength="1" maxlength="10">Initial feedback</textarea>
</div>
""");
  }

  @Test
  public void textarea_with_data_attributes_plain_and_thymeleaf() {
    // Plain data attributes
    assertHtml(
        Model.builder().id("tracking").name("tracking").label("Tracking").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            data-testid="tracking-textarea"
            data-analytics="track-me" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="tracking">Tracking</label>
<span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="tracking" name="tracking" data-testid="tracking-textarea" data-analytics="track-me"></textarea>
</div>
""");

    // Thymeleaf data attributes
    assertHtml(
        Model.builder().id("tracking").name("tracking").label("Tracking").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:data-testid="${model.id()}"
            th:data-analytics="${model.name()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="tracking">Tracking</label>
<span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="tracking" name="tracking" data-testid="tracking" data-analytics="tracking"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_aria_attributes_plain_and_thymeleaf() {
    // Plain aria attributes
    assertHtml(
        Model.builder().id("comments").name("comments").label("Comments").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            aria-label="Comment box" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="comments">Comments</label>
<span id="error-message-comments" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="comments" name="comments" aria-label="Comment box"></textarea>
</div>
""");

    // Thymeleaf aria attributes
    assertHtml(
        Model.builder().id("comments").name("comments").label("Comments").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:aria-label="${model.label()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="comments">Comments</label>
<span id="error-message-comments" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="comments" name="comments" aria-label="Comments"></textarea>
</div>
""");
  }

  @Test
  public void textarea_with_complex_name() {
    assertHtml(
        Model.builder()
            .id("complex")
            .name("user[profile][bio]")
            .label("Biography")
            .isValid("true")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="complex">Biography</label>
<span id="error-message-user-profile-bio" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="complex" name="user[profile][bio]"></textarea>
</div>
""");
  }

  @Test
  public void mixed_thymeleaf_and_plain_attributes() {
    assertHtml(
        Model.builder().id("mixed").name("mixedTextarea").label("Mixed Textarea").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            name="mixedTextarea"
            label="Mixed Textarea" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="mixed">Mixed Textarea</label>
<span id="error-message-mixedTextarea" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="mixed" name="mixedTextarea"></textarea>
</div>
""");
  }

  @Test
  public void basic_textarea_databound_validation() {
    assertHtml(
        new ConstraintModel("person description"),
        """
        <cf:textarea
            id="description"
            name="description"
            label="Description"
            th:validation-class="${model.getClassName()}"
            validation-field="description"/>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="description">Description</label>
<span id="error-message-description" class="usa-error-message" role="alert" hidden="hidden"></span>
<textarea class="usa-textarea" id="description" name="description" data-required-message="??error.required_en_US??" required="required"></textarea>
</div>
""");
  }

  @Test
  public void missing_required_attributes_throw_exceptions() {
    // Missing id
    assertException(
        """
        <cf:textarea
            name="test"
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Blank id
    assertException(
        """
        <cf:textarea
            id=""
            name="test"
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Missing name
    assertException(
        """
        <cf:textarea
            id="test"
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Blank name
    assertException(
        """
        <cf:textarea
            id="test"
            name=""
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Missing label
    assertException(
        """
        <cf:textarea
            id="test"
            name="test" />
        """,
        IllegalStateException.class);

    // Blank label
    assertException(
        """
        <cf:textarea
            id="test"
            name="test"
            label="" />
        """,
        IllegalStateException.class);
  }

  @Test
  public void invalid_attribute_values_throw_exceptions() {
    // Invalid size
    assertException(
        """
        <cf:textarea
            id="test"
            name="test"
            label="Test"
            size="invalid-size" />
        """,
        IllegalStateException.class);

    // Validation class without field
    assertException(
        Model.builder().validationClass("com.example.MyClass").build(),
        """
        <cf:textarea
            id="test"
            name="test"
            label="Test"
            th:validation-class="${model.validationClass()}" />
        """,
        IllegalStateException.class);

    // Validation field without class
    assertException(
        Model.builder().validationField("myField").build(),
        """
        <cf:textarea
            id="test"
            name="test"
            label="Test"
            th:validation-field="${model.validationField()}" />
        """,
        IllegalStateException.class);
  }

  @Test
  public void null_thymeleaf_attributes_throw_exceptions() {
    // Null id
    assertException(
        Model.builder().id(null).name("test").label("Test").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        IllegalStateException.class);

    // Null name
    assertException(
        Model.builder().id("test").name(null).label("Test").build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        IllegalStateException.class);

    // Null label
    assertException(
        Model.builder().id("test").name("test").label(null).build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        IllegalStateException.class);
  }

  @Test
  public void whitespace_only_attributes_throw_exceptions() {
    // Whitespace only id
    assertException(
        """
        <cf:textarea
            id="   "
            name="test"
            label="Test" />
        """,
        IllegalStateException.class);

    // Whitespace only name
    assertException(
        """
        <cf:textarea
            id="test"
            name="   "
            label="Test" />
        """,
        IllegalStateException.class);

    // Whitespace only label
    assertException(
        """
        <cf:textarea
            id="test"
            name="test"
            label="   " />
        """,
        IllegalStateException.class);
  }

  @Test
  public void textarea_with_name_containing_spaces_throws() {
    assertException(
        Model.builder()
            .id("spaces")
            .name("text area")
            .label("Text Area")
            .validationMessage("Required")
            .build(),
        """
        <cf:textarea
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}" />
        """,
        IllegalStateException.class);
  }
}

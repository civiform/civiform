package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;

public class CheckboxElementTagModelProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new CheckboxElementTagModelProcessor(prefix));
  }

  @Builder
  public record Model(
      String id,
      String name,
      String label,
      String helpText,
      String validationMessage,
      String isValid,
      String required,
      String tiled,
      String columns,
      String validationClass,
      String validationField) {}

  @Test
  public void basic_checkbox() {
    assertHtml(
        // model
        Model.builder().id("notifications").name("notifications").label("Notifications").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="email" value="email" label="Email notifications"/>
          <item id="sms" value="sms" label="SMS notifications"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Notifications</legend>
<span id="error-message-notifications" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="email" name="notifications" value="email"/>
    <label class="usa-checkbox__label" for="email">Email notifications</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="sms" name="notifications" value="sms"/>
    <label class="usa-checkbox__label" for="sms">SMS notifications</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void basic_checkbox_with_plain_attributes() {
    assertHtml(
        // template
        """
        <cf:checkbox
            id="notifications"
            name="notifications"
            label="Notifications">
          <item id="email" value="email" label="Email notifications"/>
          <item id="sms" value="sms" label="SMS notifications"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Notifications</legend>
<span id="error-message-notifications" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="email" name="notifications" value="email"/>
    <label class="usa-checkbox__label" for="email">Email notifications</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="sms" name="notifications" value="sms"/>
    <label class="usa-checkbox__label" for="sms">SMS notifications</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_help_text() {
    assertHtml(
        // model
        Model.builder()
            .id("preferences")
            .name("preferences")
            .label("Preferences")
            .helpText("Select your preferred notification methods")
            .build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}">
          <item id="email" value="email" label="Email"/>
          <item id="push" value="push" label="Push"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Preferences</legend>
<div id="help-text-preferences" class="usa-hint">Select your preferred notification methods</div>
<span id="error-message-preferences" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="email" name="preferences" value="email"/>
    <label class="usa-checkbox__label" for="email">Email</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="push" name="preferences" value="push"/>
    <label class="usa-checkbox__label" for="push">Push</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_help_text_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:checkbox
            id="preferences"
            name="preferences"
            label="Preferences"
            help-text="Select your preferred notification methods">
          <item id="email" value="email" label="Email"/>
          <item id="push" value="push" label="Push"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Preferences</legend>
<div id="help-text-preferences" class="usa-hint">Select your preferred notification methods</div>
<span id="error-message-preferences" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="email" name="preferences" value="email"/>
    <label class="usa-checkbox__label" for="email">Email</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="push" name="preferences" value="push"/>
    <label class="usa-checkbox__label" for="push">Push</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_validation_error() {
    assertHtml(
        // model
        Model.builder()
            .id("terms")
            .name("terms")
            .label("Terms and Conditions")
            .isValid("false")
            .validationMessage("You must accept the terms")
            .build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <item id="accept" value="accept" label="I accept the terms and conditions"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group usa-form-group--error">
<fieldset class="usa-fieldset" data-required-message="You must accept the terms">
<legend class="usa-legend">Terms and Conditions</legend>
<span id="error-message-terms" class="usa-error-message" role="alert">You must accept the terms</span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile usa-checkbox__input--error" id="accept" name="terms" value="accept" aria-describedby="error-message-terms" aria-invalid="true"/>
    <label class="usa-checkbox__label" for="accept">I accept the terms and conditions</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_validation_error_plain_attributes() {
    assertHtml(
        // template
        """
        <cf:checkbox
            id="terms"
            name="terms"
            label="Terms and Conditions"
            is-valid="false"
            validation-message="You must accept the terms">
          <item id="accept" value="accept" label="I accept the terms and conditions"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group usa-form-group--error">
<fieldset class="usa-fieldset" data-required-message="You must accept the terms">
<legend class="usa-legend">Terms and Conditions</legend>
<span id="error-message-terms" class="usa-error-message" role="alert">You must accept the terms</span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile usa-checkbox__input--error" id="accept" name="terms" value="accept" aria-describedby="error-message-terms" aria-invalid="true"/>
    <label class="usa-checkbox__label" for="accept">I accept the terms and conditions</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_error_and_help_text() {
    assertHtml(
        // model
        Model.builder()
            .id("skills")
            .name("skills")
            .label("Skills")
            .helpText("Select at least one skill")
            .isValid("false")
            .validationMessage("Please select at least one skill")
            .build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <item id="java" value="java" label="Java"/>
          <item id="python" value="python" label="Python"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group usa-form-group--error">
<fieldset class="usa-fieldset" data-required-message="Please select at least one skill">
<legend class="usa-legend">Skills</legend>
<div id="help-text-skills" class="usa-hint">Select at least one skill</div>
<span id="error-message-skills" class="usa-error-message" role="alert">Please select at least one skill</span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile usa-checkbox__input--error" id="java" name="skills" value="java" aria-describedby="error-message-skills" aria-invalid="true"/>
    <label class="usa-checkbox__label" for="java">Java</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile usa-checkbox__input--error" id="python" name="skills" value="python" aria-describedby="error-message-skills" aria-invalid="true"/>
    <label class="usa-checkbox__label" for="python">Python</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void required_checkbox() {
    assertHtml(
        // model
        Model.builder()
            .id("agreement")
            .name("agreement")
            .label("Agreement")
            .required("true")
            .build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:required="${model.required()}">
          <item id="agree" value="agree" label="I agree"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Agreement</legend>
<span id="error-message-agreement" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="agree" name="agreement" value="agree" required="required"/>
    <label class="usa-checkbox__label" for="agree">I agree</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void required_checkbox_boolean_attribute() {
    assertHtml(
        // template
        """
        <cf:checkbox
            id="agreement"
            name="agreement"
            label="Agreement"
            required="true">
          <item id="agree" value="agree" label="I agree"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Agreement</legend>
<span id="error-message-agreement" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="agree" name="agreement" value="agree" required="required"/>
    <label class="usa-checkbox__label" for="agree">I agree</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_selected_item() {
    assertHtml(
        // model
        Model.builder().id("colors").name("colors").label("Favorite Colors").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="red" value="red" label="Red" selected="true"/>
          <item id="blue" value="blue" label="Blue"/>
          <item id="green" value="green" label="Green" selected="true"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Favorite Colors</legend>
<span id="error-message-colors" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="red" name="colors" value="red" checked="checked"/>
    <label class="usa-checkbox__label" for="red">Red</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="blue" name="colors" value="blue"/>
    <label class="usa-checkbox__label" for="blue">Blue</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="green" name="colors" value="green" checked="checked"/>
    <label class="usa-checkbox__label" for="green">Green</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_disabled_item() {
    assertHtml(
        // model
        Model.builder().id("features").name("features").label("Features").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="feature1" value="feature1" label="Feature 1"/>
          <item id="feature2" value="feature2" label="Feature 2" disabled="true"/>
          <item id="feature3" value="feature3" label="Feature 3"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Features</legend>
<span id="error-message-features" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="feature1" name="features" value="feature1"/>
    <label class="usa-checkbox__label" for="feature1">Feature 1</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="feature2" name="features" value="feature2" disabled="disabled"/>
    <label class="usa-checkbox__label" for="feature2">Feature 2</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="feature3" name="features" value="feature3"/>
    <label class="usa-checkbox__label" for="feature3">Feature 3</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_item_descriptions() {
    assertHtml(
        // model
        Model.builder().id("plans").name("plans").label("Select Plans").build(),
        // template
        """
<cf:checkbox
  th:id="${model.id()}"
  th:name="${model.name()}"
  th:label="${model.label()}">
<item id="basic" value="basic" label="Basic Plan" description="$10/month - Essential features"/>
<item id="pro" value="pro" label="Pro Plan" description="$20/month - All features included"/>
</cf:checkbox>
""",
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Select Plans</legend>
<span id="error-message-plans" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="basic" name="plans" value="basic"/>
    <label class="usa-checkbox__label" for="basic">Basic Plan<span class="usa-checkbox__label-description">$10/month - Essential features</span></label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="pro" name="plans" value="pro"/>
    <label class="usa-checkbox__label" for="pro">Pro Plan<span class="usa-checkbox__label-description">$20/month - All features included</span></label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_not_tiled() {
    assertHtml(
        // model
        Model.builder().id("options").name("options").label("Options").tiled("false").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:tiled="${model.tiled()}">
          <item id="option1" value="option1" label="Option 1"/>
          <item id="option2" value="option2" label="Option 2"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Options</legend>
<span id="error-message-options" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input" id="option1" name="options" value="option1"/>
    <label class="usa-checkbox__label" for="option1">Option 1</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input" id="option2" name="options" value="option2"/>
    <label class="usa-checkbox__label" for="option2">Option 2</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_columns() {
    assertHtml(
        // model
        Model.builder().id("interests").name("interests").label("Interests").columns("2").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:columns="${model.columns()}">
          <item id="sports" value="sports" label="Sports"/>
          <item id="music" value="music" label="Music"/>
          <item id="art" value="art" label="Art"/>
          <item id="tech" value="tech" label="Technology"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Interests</legend>
<span id="error-message-interests" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group checkbox-group-col-2">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="sports" name="interests" value="sports"/>
    <label class="usa-checkbox__label" for="sports">Sports</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="music" name="interests" value="music"/>
    <label class="usa-checkbox__label" for="music">Music</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="art" name="interests" value="art"/>
    <label class="usa-checkbox__label" for="art">Art</label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="tech" name="interests" value="tech"/>
    <label class="usa-checkbox__label" for="tech">Technology</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_thymeleaf_item_attributes() {
    assertHtml(
        // model
        Model.builder().id("items").name("items").label("Items").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item th:id="${model.id()}" th:value="${model.id()}" th:label="${model.label()}"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Items</legend>
<span id="error-message-items" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
      <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="items" name="items" value="items"/>
      <label class="usa-checkbox__label" for="items">Items</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_data_attributes() {
    assertHtml(
        // model
        Model.builder().id("tracking").name("tracking").label("Tracking").build(),
        // template
        """
<cf:checkbox
  th:id="${model.id()}"
  th:name="${model.name()}"
  th:label="${model.label()}">
<item id="track1" value="track1" label="Option 1" data-testid="tracking-checkbox" data-analytics="track-me"/>
</cf:checkbox>
""",
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Tracking</legend>
<span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="track1" name="tracking" value="track1" data-testid="tracking-checkbox" data-analytics="track-me"/>
    <label class="usa-checkbox__label" for="track1">Option 1</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_aria_attributes() {
    assertHtml(
        // model
        Model.builder().id("accessible").name("accessible").label("Accessible").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="option1" value="option1" label="Option 1" aria-label="First option"/>
        </cf:checkbox>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Accessible</legend>
<span id="error-message-accessible" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="option1" name="accessible" value="option1" aria-label="First option"/>
    <label class="usa-checkbox__label" for="option1">Option 1</label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_with_all_attributes() {
    assertHtml(
        // model
        Model.builder()
            .id("complete")
            .name("complete")
            .label("Complete Example")
            .helpText("Select all that apply")
            .required("true")
            .tiled("true")
            .columns("2")
            .build(),
        // template
        """
<cf:checkbox
    th:id="${model.id()}"
    th:name="${model.name()}"
    th:label="${model.label()}"
    th:help-text="${model.helpText()}"
    th:required="${model.required()}"
    th:tiled="${model.tiled()}"
    th:columns="${model.columns()}">
  <item id="opt1" value="opt1" label="Option 1" description="First option" selected="true"/>
  <item id="opt2" value="opt2" label="Option 2" description="Second option"/>
  <item id="opt3" value="opt3" label="Option 3" description="Third option" disabled="true"/>
</cf:checkbox>
""",
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Complete Example</legend>
<div id="help-text-complete" class="usa-hint">Select all that apply</div>
<span id="error-message-complete" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="checkbox-group checkbox-group-col-2">
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="opt1" name="complete" value="opt1" checked="checked" required="required"/>
    <label class="usa-checkbox__label" for="opt1">Option 1<span class="usa-checkbox__label-description">First option</span></label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="opt2" name="complete" value="opt2" required="required"/>
    <label class="usa-checkbox__label" for="opt2">Option 2<span class="usa-checkbox__label-description">Second option</span></label>
  </div>
  <div class="usa-checkbox">
    <input type="checkbox" class="usa-checkbox__input usa-checkbox__input--tile" id="opt3" name="complete" value="opt3" disabled="disabled" required="required"/>
    <label class="usa-checkbox__label" for="opt3">Option 3<span class="usa-checkbox__label-description">Third option</span></label>
  </div>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void checkbox_without_id_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            name="test"
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_blank_id_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id=""
            name="test"
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_without_name_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_blank_name_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name=""
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_without_label_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name="test">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_blank_label_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name="test"
            label="">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_null_id_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().id(null).name("test").label("Test").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_null_name_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().id("test").name(null).label("Test").build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_null_label_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().id("test").name("test").label(null).build(),
        // template
        """
        <cf:checkbox
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_whitespace_only_id_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="   "
            name="test"
            label="Test">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_whitespace_only_name_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name="   "
            label="Test">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_with_whitespace_only_label_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name="test"
            label="   ">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_item_without_value_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name="test"
            label="Test">
          <item id="item1" label="Item 1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void checkbox_item_without_label_throws() {
    assertException(
        // template
        """
        <cf:checkbox
            id="test"
            name="test"
            label="Test">
          <item id="item1" value="item1"/>
        </cf:checkbox>
        """,
        // expected
        IllegalStateException.class);
  }
}

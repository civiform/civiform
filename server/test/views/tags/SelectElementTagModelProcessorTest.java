package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;
import play.data.validation.Constraints;

public class SelectElementTagModelProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new SelectElementTagModelProcessor(prefix));
  }

  @Builder
  public record Model(
      String id,
      String name,
      String value,
      String label,
      String helpText,
      String validationMessage,
      String isValid,
      String required,
      String readonly,
      String disabled,
      String size,
      String validationClass,
      String validationField) {}

  public static class ConstraintModel {
    @Constraints.Required private String status;

    public ConstraintModel(String status) {
      this.status = status;
    }

    public String getStatus() {
      return this.status;
    }

    public String getClassName() {
      return this.getClass().getName();
    }
  }

  @Test
  public void basic_select_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder().id("country").name("country").label("Country").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <option value="">Select a country</option>
          <option value="us">United States</option>
          <option value="ca">Canada</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="country" name="country">
  <option value="">Select a country</option>
  <option value="us">United States</option>
  <option value="ca">Canada</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="country"
            name="country"
            label="Country">
          <option value="">Select a country</option>
          <option value="us">United States</option>
          <option value="ca">Canada</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="country" name="country">
  <option value="">Select a country</option>
  <option value="us">United States</option>
  <option value="ca">Canada</option>
</select>
</div>
""");
  }

  @Test
  public void disabled_select_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder().id("country").name("country").label("Country").disabled("true").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:disabled="${model.disabled()}">
          <option value="">Select a country</option>
          <option value="us">United States</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="country" name="country" disabled="disabled">
  <option value="">Select a country</option>
  <option value="us">United States</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="country"
            name="country"
            label="Country"
            disabled="true">
          <option value="">Select a country</option>
          <option value="us">United States</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="country" name="country" disabled="disabled">
  <option value="">Select a country</option>
  <option value="us">United States</option>
</select>
</div>
""");
  }

  @Test
  public void readonly_select_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder().id("country").name("country").label("Country").readonly("true").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:readonly="${model.readonly()}">
          <option value="">Select a country</option>
          <option value="us">United States</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="country" name="country" readonly="readonly">
  <option value="">Select a country</option>
  <option value="us">United States</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="country"
            name="country"
            label="Country"
            readonly="true">
          <option value="">Select a country</option>
          <option value="us">United States</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="country" name="country" readonly="readonly">
  <option value="">Select a country</option>
  <option value="us">United States</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_help_text_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("state")
            .name("state")
            .label("State")
            .helpText("Select your state of residence")
            .build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}">
          <option value="">Select a state</option>
          <option value="ny">New York</option>
          <option value="ca">California</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="state">State</label>
<div id="help-text-state" class="usa-hint">Select your state of residence</div>
<span id="error-message-state" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="state" name="state" aria-describedby="help-text-state">
  <option value="">Select a state</option>
  <option value="ny">New York</option>
  <option value="ca">California</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="state"
            name="state"
            label="State"
            help-text="Select your state of residence">
          <option value="">Select a state</option>
          <option value="ny">New York</option>
          <option value="ca">California</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="state">State</label>
<div id="help-text-state" class="usa-hint">Select your state of residence</div>
<span id="error-message-state" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="state" name="state" aria-describedby="help-text-state">
  <option value="">Select a state</option>
  <option value="ny">New York</option>
  <option value="ca">California</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_validation_error_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("category")
            .name("category")
            .label("Category")
            .isValid("false")
            .validationMessage("Please select a category")
            .build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <option value="">Select a category</option>
          <option value="tech">Technology</option>
          <option value="health">Health</option>
        </cf:select>
        """,
        """
<div class="usa-form-group usa-form-group--error">
<label class="usa-label" for="category">Category</label>
<span id="error-message-category" class="usa-error-message" role="alert">Please select a category</span>
<select class="usa-select" id="category" name="category" aria-describedby="error-message-category" aria-invalid="true">
  <option value="">Select a category</option>
  <option value="tech">Technology</option>
  <option value="health">Health</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="category"
            name="category"
            label="Category"
            is-valid="false"
            validation-message="Please select a category">
          <option value="">Select a category</option>
          <option value="tech">Technology</option>
          <option value="health">Health</option>
        </cf:select>
        """,
        """
<div class="usa-form-group usa-form-group--error">
<label class="usa-label" for="category">Category</label>
<span id="error-message-category" class="usa-error-message" role="alert">Please select a category</span>
<select class="usa-select" id="category" name="category" aria-describedby="error-message-category" aria-invalid="true">
  <option value="">Select a category</option>
  <option value="tech">Technology</option>
  <option value="health">Health</option>
</select>
</div>
""");
  }

  @Test
  public void required_select_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder().id("terms").name("terms").label("Accept Terms").required("true").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:required="${model.required()}">
          <option value="">Select</option>
          <option value="yes">Yes</option>
          <option value="no">No</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="terms">Accept Terms</label>
<span id="error-message-terms" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="terms" name="terms" required="required">
  <option value="">Select</option>
  <option value="yes">Yes</option>
  <option value="no">No</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="terms"
            name="terms"
            label="Accept Terms"
            required="true">
          <option value="">Select</option>
          <option value="yes">Yes</option>
          <option value="no">No</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="terms">Accept Terms</label>
<span id="error-message-terms" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="terms" name="terms" required="required">
  <option value="">Select</option>
  <option value="yes">Yes</option>
  <option value="no">No</option>
</select>
</div>
""");
  }

  @Test
  public void select_sizes_2xs_small_medium() {
    // 2xs size
    assertHtml(
        Model.builder().id("tiny").name("tiny").label("Tiny").size("2xs").build(),
        """
        <cf:select th:id="${model.id()}" th:name="${model.name()}"
                  th:label="${model.label()}" th:size="${model.size()}">
          <option value="">Select</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="tiny">Tiny</label>
<span id="error-message-tiny" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select usa-input--2xs" id="tiny" name="tiny">
  <option value="">Select</option>
</select>
</div>
""");

    // Small size
    assertHtml(
        Model.builder().id("state").name("state").label("State").size("small").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:size="${model.size()}">
          <option value="">Select state</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="state">State</label>
<span id="error-message-state" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select usa-input--small" id="state" name="state">
  <option value="">Select state</option>
</select>
</div>
""");

    // Medium size
    assertHtml(
        Model.builder().id("country").name("country").label("Country").size("medium").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:size="${model.size()}">
          <option value="">Select country</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="country">Country</label>
<span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select usa-input--medium" id="country" name="country">
  <option value="">Select country</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_all_attributes_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("language")
            .name("language")
            .label("Language")
            .helpText("Select your preferred language")
            .required("true")
            .size("md")
            .build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}"
            th:required="${model.required()}"
            th:size="${model.size()}">
          <option value="">Select language</option>
          <option value="en">English</option>
          <option value="es">Spanish</option>
          <option value="fr">French</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="language">Language</label>
<div id="help-text-language" class="usa-hint">Select your preferred language</div>
<span id="error-message-language" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select usa-input--md" id="language" name="language" required="required" aria-describedby="help-text-language">
  <option value="">Select language</option>
  <option value="en">English</option>
  <option value="es">Spanish</option>
  <option value="fr">French</option>
</select>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:select
            id="language"
            name="language"
            label="Language"
            help-text="Select your preferred language"
            required="true"
            size="md">
          <option value="">Select language</option>
          <option value="en">English</option>
          <option value="es">Spanish</option>
          <option value="fr">French</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="language">Language</label>
<div id="help-text-language" class="usa-hint">Select your preferred language</div>
<span id="error-message-language" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select usa-input--md" id="language" name="language" required="required" aria-describedby="help-text-language">
  <option value="">Select language</option>
  <option value="en">English</option>
  <option value="es">Spanish</option>
  <option value="fr">French</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_error_and_help_text() {
    assertHtml(
        Model.builder()
            .id("priority")
            .name("priority")
            .label("Priority")
            .helpText("Select the priority level")
            .isValid("false")
            .validationMessage("Priority is required")
            .build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <option value="">Select priority</option>
          <option value="high">High</option>
          <option value="medium">Medium</option>
          <option value="low">Low</option>
        </cf:select>
        """,
        """
<div class="usa-form-group usa-form-group--error">
<label class="usa-label" for="priority">Priority</label>
<div id="help-text-priority" class="usa-hint">Select the priority level</div>
<span id="error-message-priority" class="usa-error-message" role="alert">Priority is required</span>
<select class="usa-select" id="priority" name="priority" aria-describedby="error-message-priority help-text-priority" aria-invalid="true">
  <option value="">Select priority</option>
  <option value="high">High</option>
  <option value="medium">Medium</option>
  <option value="low">Low</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_data_attributes_plain_and_thymeleaf() {
    // Plain data attributes
    assertHtml(
        Model.builder().id("tracking").name("tracking").label("Tracking").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            data-testid="tracking-select"
            data-analytics="track-me">
          <option value="">Select</option>
          <option value="option1">Option 1</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="tracking">Tracking</label>
<span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="tracking" name="tracking" data-testid="tracking-select" data-analytics="track-me">
  <option value="">Select</option>
  <option value="option1">Option 1</option>
</select>
</div>
""");

    // Thymeleaf data attributes
    assertHtml(
        Model.builder().id("tracking").name("tracking").label("Tracking").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:data-testid="${model.id()}"
            th:data-analytics="${model.name()}">
          <option value="">Select</option>
          <option value="option1">Option 1</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="tracking">Tracking</label>
<span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="tracking" name="tracking" data-testid="tracking" data-analytics="tracking">
  <option value="">Select</option>
  <option value="option1">Option 1</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_aria_attributes_plain_and_thymeleaf() {
    // Plain aria attributes
    assertHtml(
        Model.builder().id("role").name("role").label("Role").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            aria-label="Select role">
          <option value="">Select role</option>
          <option value="admin">Admin</option>
          <option value="user">User</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="role">Role</label>
<span id="error-message-role" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="role" name="role" aria-label="Select role">
  <option value="">Select role</option>
  <option value="admin">Admin</option>
  <option value="user">User</option>
</select>
</div>
""");

    // Thymeleaf aria attributes
    assertHtml(
        Model.builder().id("role").name("role").label("Role").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:aria-label="${model.label()}">
          <option value="">Select role</option>
          <option value="admin">Admin</option>
          <option value="user">User</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="role">Role</label>
<span id="error-message-role" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="role" name="role" aria-label="Role">
  <option value="">Select role</option>
  <option value="admin">Admin</option>
  <option value="user">User</option>
</select>
</div>
""");
  }

  @Test
  public void select_with_optgroups() {
    assertHtml(
        Model.builder().id("vehicle").name("vehicle").label("Vehicle Type").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <option value="">Select vehicle</option>
          <optgroup label="Cars">
            <option value="sedan">Sedan</option>
            <option value="suv">SUV</option>
          </optgroup>
          <optgroup label="Trucks">
            <option value="pickup">Pickup</option>
            <option value="van">Van</option>
          </optgroup>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="vehicle">Vehicle Type</label>
<span id="error-message-vehicle" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="vehicle" name="vehicle">
  <option value="">Select vehicle</option>
  <optgroup label="Cars">
    <option value="sedan">Sedan</option>
    <option value="suv">SUV</option>
  </optgroup>
  <optgroup label="Trucks">
    <option value="pickup">Pickup</option>
    <option value="van">Van</option>
  </optgroup>
</select>
</div>
""");
  }

  @Test
  public void select_with_complex_name() {
    assertHtml(
        Model.builder()
            .id("complex")
            .name("user[profile][country]")
            .label("Country")
            .isValid("true")
            .build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}">
          <option value="">Select country</option>
          <option value="us">United States</option>
          <option value="uk">United Kingdom</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="complex">Country</label>
<span id="error-message-user-profile-country" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="complex" name="user[profile][country]">
  <option value="">Select country</option>
  <option value="us">United States</option>
  <option value="uk">United Kingdom</option>
</select>
</div>
""");
  }

  @Test
  public void mixed_thymeleaf_and_plain_attributes() {
    assertHtml(
        Model.builder().id("mixed").name("mixedSelect").label("Mixed Select").build(),
        """
        <cf:select
            th:id="${model.id()}"
            name="mixedSelect"
            label="Mixed Select">
          <option value="">Select</option>
          <option value="option1">Option 1</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="mixed">Mixed Select</label>
<span id="error-message-mixedSelect" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="mixed" name="mixedSelect">
  <option value="">Select</option>
  <option value="option1">Option 1</option>
</select>
</div>
""");
  }

  @Test
  public void basic_select_databound_validation() {
    assertHtml(
        new ConstraintModel("active"),
        """
        <cf:select
            id="status"
            name="status"
            label="Status"
            th:validation-class="${model.getClassName()}"
            validation-field="status">
          <option value="">Select status</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
        </cf:select>
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="status">Status</label>
<span id="error-message-status" class="usa-error-message" role="alert" hidden="hidden"></span>
<select class="usa-select" id="status" name="status" data-required-message="??error.required_en_US??" required="required">
  <option value="">Select status</option>
  <option value="active">Active</option>
  <option value="inactive">Inactive</option>
</select>
</div>
""");
  }

  @Test
  public void missing_required_attributes_throw_exceptions() {
    // Missing id
    assertException(
        """
        <cf:select
            name="test"
            label="Test Label">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Blank id
    assertException(
        """
        <cf:select
            id=""
            name="test"
            label="Test Label">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Missing name
    assertException(
        """
        <cf:select
            id="test"
            label="Test Label">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Blank name
    assertException(
        """
        <cf:select
            id="test"
            name=""
            label="Test Label">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Missing label
    assertException(
        """
        <cf:select
            id="test"
            name="test">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Blank label
    assertException(
        """
        <cf:select
            id="test"
            name="test"
            label="">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);
  }

  @Test
  public void invalid_attribute_values_throw_exceptions() {
    // Invalid size
    assertException(
        """
        <cf:select
            id="test"
            name="test"
            label="Test"
            size="invalid-size">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Validation class without field
    assertException(
        Model.builder().validationClass("com.example.MyClass").build(),
        """
        <cf:select
            id="test"
            name="test"
            label="Test"
            th:validation-class="${model.validationClass()}">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Validation field without class
    assertException(
        Model.builder().validationField("myField").build(),
        """
        <cf:select
            id="test"
            name="test"
            label="Test"
            th:validation-field="${model.validationField()}">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);
  }

  @Test
  public void null_thymeleaf_attributes_throw_exceptions() {
    // Null id
    assertException(
        Model.builder().id(null).name("test").label("Test").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Null name
    assertException(
        Model.builder().id("test").name(null).label("Test").build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Null label
    assertException(
        Model.builder().id("test").name("test").label(null).build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);
  }

  @Test
  public void whitespace_only_attributes_throw_exceptions() {
    // Whitespace only id
    assertException(
        """
        <cf:select
            id="   "
            name="test"
            label="Test">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Whitespace only name
    assertException(
        """
        <cf:select
            id="test"
            name="   "
            label="Test">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);

    // Whitespace only label
    assertException(
        """
        <cf:select
            id="test"
            name="test"
            label="   ">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);
  }

  @Test
  public void select_with_name_containing_spaces_throws() {
    assertException(
        Model.builder()
            .id("spaces")
            .name("select name")
            .label("Select")
            .validationMessage("Required")
            .build(),
        """
        <cf:select
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <option value="">Select</option>
        </cf:select>
        """,
        IllegalStateException.class);
  }
}

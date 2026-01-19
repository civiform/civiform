package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;
import play.data.validation.Constraints;

public class InputElementTagModelProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new InputElementTagModelProcessor(prefix));
  }

  @Builder
  public record Model(
      String id,
      String name,
      String value,
      String label,
      String type,
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
      String pattern,
      String validationClass,
      String validationField) {}

  public static class ConstraintModel {
    @Constraints.Required private String name;

    public ConstraintModel(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public String getClassName() {
      return this.getClass().getName();
    }
  }

  @Test
  public void basic_text_input_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder().id("firstName").name("firstName").label("First Name").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="firstName">First Name</label>
  <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="firstName" name="firstName"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="firstName"
            name="firstName"
            label="First Name" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="firstName">First Name</label>
  <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="firstName" name="firstName"/>
</div>
""");
  }

  @Test
  public void disabled_input_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder()
            .id("firstName")
            .name("firstName")
            .label("First Name")
            .disabled("true")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:disabled="${model.disabled()}" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="firstName">First Name</label>
<span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
<input type="text" class="usa-input" id="firstName" name="firstName" disabled="disabled"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="firstName"
            name="firstName"
            label="First Name"
            disabled="true" />
        """,
        """
<div class="usa-form-group">
<label class="usa-label" for="firstName">First Name</label>
<span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
<input type="text" class="usa-input" id="firstName" name="firstName" disabled="disabled"/>
</div>
""");
  }

  @Test
  public void readonly_input_with_thymeleaf_and_plain_attributes() {
    // With Thymeleaf attributes
    assertHtml(
        Model.builder()
            .id("firstName")
            .name("firstName")
            .label("First Name")
            .readonly("true")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:readonly="${model.readonly()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="firstName">First Name</label>
  <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="firstName" name="firstName" readonly="readonly"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="firstName"
            name="firstName"
            label="First Name"
            readonly="true" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="firstName">First Name</label>
  <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="firstName" name="firstName" readonly="readonly"/>
</div>
""");
  }

  @Test
  public void input_with_value_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("email")
            .name("email")
            .label("Email Address")
            .value("test@example.com")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:value="${model.value()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="email">Email Address</label>
  <span id="error-message-email" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="email" name="email" value="test@example.com"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="email"
            name="email"
            label="Email Address"
            value="test@example.com" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="email">Email Address</label>
  <span id="error-message-email" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="email" name="email" value="test@example.com"/>
</div>
""");
  }

  @Test
  public void input_with_placeholder_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("phone")
            .name("phone")
            .label("Phone Number")
            .placeholder("(555) 555-5555")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:placeholder="${model.placeholder()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="phone">Phone Number</label>
  <span id="error-message-phone" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="phone" name="phone" placeholder="(555) 555-5555"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="phone"
            name="phone"
            label="Phone Number"
            placeholder="(555) 555-5555" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="phone">Phone Number</label>
  <span id="error-message-phone" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="phone" name="phone" placeholder="(555) 555-5555"/>
</div>
""");
  }

  @Test
  public void input_with_help_text_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("username")
            .name("username")
            .label("Username")
            .helpText("Choose a unique username")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="username">Username</label>
  <div id="help-text-username" class="usa-hint">Choose a unique username</div>
  <span id="error-message-username" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="username" name="username" aria-describedby="help-text-username"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="username"
            name="username"
            label="Username"
            help-text="Choose a unique username" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="username">Username</label>
  <div id="help-text-username" class="usa-hint">Choose a unique username</div>
  <span id="error-message-username" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="username" name="username" aria-describedby="help-text-username"/>
</div>
""");
  }

  @Test
  public void input_with_validation_error_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("email")
            .name("email")
            .label("Email Address")
            .isValid("false")
            .validationMessage("Please enter a valid email address")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}" />
        """,
        """
<div class="usa-form-group usa-form-group--error">
  <label class="usa-label" for="email">Email Address</label>
  <span id="error-message-email" class="usa-error-message" role="alert">Please enter a valid email address</span>
  <input type="text" class="usa-input" id="email" name="email" aria-describedby="error-message-email" aria-invalid="true"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="email"
            name="email"
            label="Email Address"
            is-valid="false"
            validation-message="Please enter a valid email address" />
        """,
        """
<div class="usa-form-group usa-form-group--error">
  <label class="usa-label" for="email">Email Address</label>
  <span id="error-message-email" class="usa-error-message" role="alert">Please enter a valid email address</span>
  <input type="text" class="usa-input" id="email" name="email" aria-describedby="error-message-email" aria-invalid="true"/>
</div>
""");
  }

  @Test
  public void required_input_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder().id("password").name("password").label("Password").required("true").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:required="${model.required()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="password">Password</label>
  <span id="error-message-password" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="password" name="password" required="required"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="password"
            name="password"
            label="Password"
            required="true" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="password">Password</label>
  <span id="error-message-password" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="password" name="password" required="required"/>
</div>
""");
  }

  @Test
  public void input_types_date_email_password_number_tel_url() {
    // Date type
    assertHtml(
        Model.builder()
            .id("birthdate")
            .name("birthdate")
            .label("Date of Birth")
            .type("date")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:type="${model.type()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="birthdate">Date of Birth</label>
  <span id="error-message-birthdate" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="date" class="usa-input" id="birthdate" name="birthdate"/>
</div>
""");

    // Email type
    assertHtml(
        Model.builder().id("email").name("email").label("Email").type("email").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:type="${model.type()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="email">Email</label>
  <span id="error-message-email" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="email" class="usa-input" id="email" name="email"/>
</div>
""");

    // Password type
    assertHtml(
        Model.builder()
            .id("password")
            .name("password")
            .label("Password")
            .type("password")
            .required("true")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:type="${model.type()}"
            th:required="${model.required()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="password">Password</label>
  <span id="error-message-password" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="password" class="usa-input" id="password" name="password" required="required"/>
</div>
""");

    // Number type
    assertHtml(
        Model.builder()
            .id("quantity")
            .name("quantity")
            .label("Quantity")
            .type("number")
            .value("1")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:type="${model.type()}"
            th:value="${model.value()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="quantity">Quantity</label>
  <span id="error-message-quantity" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="number" class="usa-input" id="quantity" name="quantity" value="1"/>
</div>
""");

    // Tel type
    assertHtml(
        Model.builder()
            .id("phone")
            .name("phone")
            .label("Phone")
            .type("tel")
            .placeholder("123-456-7890")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:type="${model.type()}"
            th:placeholder="${model.placeholder()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="phone">Phone</label>
  <span id="error-message-phone" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="tel" class="usa-input" id="phone" name="phone" placeholder="123-456-7890"/>
</div>
""");

    // URL type
    assertHtml(
        Model.builder()
            .id("website")
            .name("website")
            .label("Website")
            .type("url")
            .placeholder("https://example.com")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:type="${model.type()}"
            th:placeholder="${model.placeholder()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="website">Website</label>
  <span id="error-message-website" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="url" class="usa-input" id="website" name="website" placeholder="https://example.com"/>
</div>
""");
  }

  @Test
  public void input_sizes_2xs_small_medium() {
    // 2xs size
    assertHtml(
        Model.builder().id("tiny").name("tiny").label("Tiny").size("2xs").build(),
        """
        <cf:input th:id="${model.id()}" th:name="${model.name()}"
                  th:label="${model.label()}" th:size="${model.size()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="tiny">Tiny</label>
  <span id="error-message-tiny" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input usa-input--2xs" id="tiny" name="tiny"/>
</div>
""");

    // Small size
    assertHtml(
        Model.builder().id("zipcode").name("zipcode").label("ZIP Code").size("small").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:size="${model.size()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="zipcode">ZIP Code</label>
  <span id="error-message-zipcode" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input usa-input--small" id="zipcode" name="zipcode"/>
</div>
""");

    // Medium size
    assertHtml(
        Model.builder().id("city").name("city").label("City").size("medium").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:size="${model.size()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="city">City</label>
  <span id="error-message-city" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input usa-input--medium" id="city" name="city"/>
</div>
""");
  }

  @Test
  public void input_with_all_attributes_thymeleaf_and_plain() {
    // With Thymeleaf
    assertHtml(
        Model.builder()
            .id("fullName")
            .name("fullName")
            .label("Full Name")
            .value("John Doe")
            .placeholder("Enter your full name")
            .helpText("First and last name")
            .type("text")
            .required("true")
            .size("md")
            .minLength("2")
            .maxLength("100")
            .pattern("[A-Z]")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:value="${model.value()}"
            th:placeholder="${model.placeholder()}"
            th:help-text="${model.helpText()}"
            th:type="${model.type()}"
            th:required="${model.required()}"
            th:size="${model.size()}"
            th:minlength="${model.minLength()}"
            th:maxlength="${model.maxLength()}"
            th:pattern="${model.pattern()}"/>
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="fullName">Full Name</label>
  <div id="help-text-fullName" class="usa-hint">First and last name</div>
  <span id="error-message-fullName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input usa-input--md" id="fullName" name="fullName" value="John Doe" placeholder="Enter your full name" required="required" aria-describedby="help-text-fullName" minlength="2" maxlength="100" pattern="[A-Z]"/>
</div>
""");

    // With plain attributes
    assertHtml(
        """
        <cf:input
            id="fullName"
            name="fullName"
            label="Full Name"
            value="John Doe"
            placeholder="Enter your full name"
            help-text="First and last name"
            type="text"
            required="true"
            size="md"
            minlength="2"
            maxlength="100"
            pattern="[A-Z]"/>
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="fullName">Full Name</label>
  <div id="help-text-fullName" class="usa-hint">First and last name</div>
  <span id="error-message-fullName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input usa-input--md" id="fullName" name="fullName" value="John Doe" placeholder="Enter your full name" required="required" aria-describedby="help-text-fullName" minlength="2" maxlength="100" pattern="[A-Z]"/>
</div>
""");
  }

  @Test
  public void input_with_error_and_help_text() {
    assertHtml(
        Model.builder()
            .id("age")
            .name("age")
            .label("Age")
            .helpText("Must be 18 or older")
            .isValid("false")
            .validationMessage("Age must be at least 18")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}" />
        """,
        """
<div class="usa-form-group usa-form-group--error">
  <label class="usa-label" for="age">Age</label>
  <div id="help-text-age" class="usa-hint">Must be 18 or older</div>
  <span id="error-message-age" class="usa-error-message" role="alert">Age must be at least 18</span>
  <input type="text" class="usa-input" id="age" name="age" aria-describedby="error-message-age help-text-age" aria-invalid="true"/>
</div>
""");
  }

  @Test
  public void input_with_data_attributes_plain_and_thymeleaf() {
    // Plain data attributes
    assertHtml(
        Model.builder().id("tracking").name("tracking").label("Tracking ID").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            data-testid="tracking-input"
            data-analytics="track-me" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="tracking">Tracking ID</label>
  <span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="tracking" name="tracking" data-testid="tracking-input" data-analytics="track-me"/>
</div>
""");

    // Thymeleaf data attributes
    assertHtml(
        Model.builder().id("tracking").name("tracking").label("Tracking ID").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:data-testid="${model.id()}"
            th:data-analytics="${model.name()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="tracking">Tracking ID</label>
  <span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="tracking" name="tracking" data-testid="tracking" data-analytics="tracking"/>
</div>
""");
  }

  @Test
  public void input_with_aria_attributes_plain_and_thymeleaf() {
    // Plain aria attributes
    assertHtml(
        Model.builder().id("search").name("search").label("Search").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            aria-label="Search the site" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="search">Search</label>
  <span id="error-message-search" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="search" name="search" aria-label="Search the site"/>
</div>
""");

    // Thymeleaf aria attributes
    assertHtml(
        Model.builder().id("search").name("search").label("Search").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:aria-label="${model.label()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="search">Search</label>
  <span id="error-message-search" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="search" name="search" aria-label="Search"/>
</div>
""");
  }

  @Test
  public void input_with_complex_name() {
    assertHtml(
        Model.builder()
            .id("complex")
            .name("user[profile][email]")
            .label("Email")
            .isValid("true")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="complex">Email</label>
  <span id="error-message-user-profile-email" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="complex" name="user[profile][email]"/>
</div>
""");
  }

  @Test
  public void mixed_thymeleaf_and_plain_attributes() {
    assertHtml(
        Model.builder().id("mixed").name("mixedInput").label("Mixed Input").build(),
        """
        <cf:input
            th:id="${model.id()}"
            name="mixedInput"
            label="Mixed Input" />
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="mixed">Mixed Input</label>
  <span id="error-message-mixedInput" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="mixed" name="mixedInput"/>
</div>
""");
  }

  @Test
  public void basic_text_input_databound_validation() {
    assertHtml(
        new ConstraintModel("person name"),
        """
        <cf:input
            id="firstName"
            name="firstName"
            label="First Name"
            th:validation-class="${model.getClassName()}"
            validation-field="name"/>
        """,
        """
<div class="usa-form-group">
  <label class="usa-label" for="firstName">First Name</label>
  <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
  <input type="text" class="usa-input" id="firstName" name="firstName" data-required-message="??error.required_en_US??" required="required"/>
</div>
""");
  }

  @Test
  public void missing_required_attributes_throw_exceptions() {
    // Missing id
    assertException(
        """
        <cf:input
            name="test"
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Blank id
    assertException(
        """
        <cf:input
            id=""
            name="test"
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Missing name
    assertException(
        """
        <cf:input
            id="test"
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Blank name
    assertException(
        """
        <cf:input
            id="test"
            name=""
            label="Test Label" />
        """,
        IllegalStateException.class);

    // Missing label
    assertException(
        """
        <cf:input
            id="test"
            name="test" />
        """,
        IllegalStateException.class);

    // Blank label
    assertException(
        """
        <cf:input
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
        <cf:input
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
        <cf:input
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
        <cf:input
            id="test"
            name="test"
            label="Test"
            th:validation-field="${model.validationField()}" />
        """,
        IllegalStateException.class);

    // Invalid minLength
    assertException(
        Model.builder().minLength("myField").build(),
        """
        <cf:input
            id="test"
            name="test"
            label="Test"
            th:minLength="${model.minLength()}" />
        """,
        IllegalStateException.class);

    // Invalid maxLength
    assertException(
        Model.builder().maxLength("myField").build(),
        """
        <cf:input
            id="test"
            name="test"
            label="Test"
            th:maxLength="${model.maxLength()}" />
        """,
        IllegalStateException.class);
  }

  @Test
  public void null_thymeleaf_attributes_throw_exceptions() {
    // Null id
    assertException(
        Model.builder().id(null).name("test").label("Test").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        IllegalStateException.class);

    // Null name
    assertException(
        Model.builder().id("test").name(null).label("Test").build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}" />
        """,
        IllegalStateException.class);

    // Null label
    assertException(
        Model.builder().id("test").name("test").label(null).build(),
        """
        <cf:input
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
        <cf:input
            id="   "
            name="test"
            label="Test" />
        """,
        IllegalStateException.class);

    // Whitespace only name
    assertException(
        """
        <cf:input
            id="test"
            name="   "
            label="Test" />
        """,
        IllegalStateException.class);

    // Whitespace only label
    assertException(
        """
        <cf:input
            id="test"
            name="test"
            label="   " />
        """,
        IllegalStateException.class);
  }

  @Test
  public void input_with_name_containing_spaces_throws() {
    assertException(
        Model.builder()
            .id("spaces")
            .name("first name")
            .label("First Name")
            .validationMessage("Required")
            .build(),
        """
        <cf:input
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}" />
        """,
        IllegalStateException.class);
  }
}

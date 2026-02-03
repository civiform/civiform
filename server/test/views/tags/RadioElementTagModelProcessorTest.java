package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;

public class RadioElementTagModelProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new RadioElementTagModelProcessor(prefix));
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
  public void basic_radio() {
    assertHtml(
        // model
        Model.builder().id("contact").name("contact").label("Contact Method").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="email" value="email" label="Email"/>
          <item id="phone" value="phone" label="Phone"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Contact Method</legend>
<span id="error-message-contact" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="email" name="contact" value="email"/>
  <label class="usa-radio__label" for="email">Email</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="phone" name="contact" value="phone"/>
  <label class="usa-radio__label" for="phone">Phone</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void basic_radio_with_plain_attributes() {
    assertHtml(
        // template
        """
        <cf:radio
            id="contact"
            name="contact"
            label="Contact Method">
          <item id="email" value="email" label="Email"/>
          <item id="phone" value="phone" label="Phone"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Contact Method</legend>
<span id="error-message-contact" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="email" name="contact" value="email"/>
  <label class="usa-radio__label" for="email">Email</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="phone" name="contact" value="phone"/>
  <label class="usa-radio__label" for="phone">Phone</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_help_text() {
    assertHtml(
        // model
        Model.builder()
            .id("shipping")
            .name("shipping")
            .label("Shipping Method")
            .helpText("Select your preferred shipping method")
            .build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}">
          <item id="standard" value="standard" label="Standard"/>
          <item id="express" value="express" label="Express"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Shipping Method</legend>
<div id="help-text-shipping" class="usa-hint">Select your preferred shipping method</div>
<span id="error-message-shipping" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="standard" name="shipping" value="standard"/>
  <label class="usa-radio__label" for="standard">Standard</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="express" name="shipping" value="express"/>
  <label class="usa-radio__label" for="express">Express</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_help_text_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:radio
            id="shipping"
            name="shipping"
            label="Shipping Method"
            help-text="Select your preferred shipping method">
          <item id="standard" value="standard" label="Standard"/>
          <item id="express" value="express" label="Express"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Shipping Method</legend>
<div id="help-text-shipping" class="usa-hint">Select your preferred shipping method</div>
<span id="error-message-shipping" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="standard" name="shipping" value="standard"/>
  <label class="usa-radio__label" for="standard">Standard</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="express" name="shipping" value="express"/>
  <label class="usa-radio__label" for="express">Express</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_validation_error() {
    assertHtml(
        // model
        Model.builder()
            .id("gender")
            .name("gender")
            .label("Gender")
            .isValid("false")
            .validationMessage("Please select a gender")
            .build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <item id="male" value="male" label="Male"/>
          <item id="female" value="female" label="Female"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group usa-form-group--error">
<fieldset class="usa-fieldset" data-required-message="Please select a gender">
<legend class="usa-legend">Gender</legend>
<span id="error-message-gender" class="usa-error-message" role="alert">Please select a gender</span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile usa-radio__input--error" id="male" name="gender" value="male" aria-describedby="error-message-gender" aria-invalid="true"/>
  <label class="usa-radio__label" for="male">Male</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile usa-radio__input--error" id="female" name="gender" value="female" aria-describedby="error-message-gender" aria-invalid="true"/>
  <label class="usa-radio__label" for="female">Female</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_validation_error_plain_attributes() {
    assertHtml(
        // template
        """
        <cf:radio
            id="gender"
            name="gender"
            label="Gender"
            is-valid="false"
            validation-message="Please select a gender">
          <item id="male" value="male" label="Male"/>
          <item id="female" value="female" label="Female"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group usa-form-group--error">
<fieldset class="usa-fieldset" data-required-message="Please select a gender">
<legend class="usa-legend">Gender</legend>
<span id="error-message-gender" class="usa-error-message" role="alert">Please select a gender</span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile usa-radio__input--error" id="male" name="gender" value="male" aria-describedby="error-message-gender" aria-invalid="true"/>
  <label class="usa-radio__label" for="male">Male</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile usa-radio__input--error" id="female" name="gender" value="female" aria-describedby="error-message-gender" aria-invalid="true"/>
  <label class="usa-radio__label" for="female">Female</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_error_and_help_text() {
    assertHtml(
        // model
        Model.builder()
            .id("subscription")
            .name("subscription")
            .label("Subscription Plan")
            .helpText("Choose your subscription plan")
            .isValid("false")
            .validationMessage("Please select a subscription plan")
            .build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:help-text="${model.helpText()}"
            th:is-valid="${model.isValid()}"
            th:validation-message="${model.validationMessage()}">
          <item id="free" value="free" label="Free"/>
          <item id="premium" value="premium" label="Premium"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group usa-form-group--error">
<fieldset class="usa-fieldset" data-required-message="Please select a subscription plan">
<legend class="usa-legend">Subscription Plan</legend>
<div id="help-text-subscription" class="usa-hint">Choose your subscription plan</div>
<span id="error-message-subscription" class="usa-error-message" role="alert">Please select a subscription plan</span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile usa-radio__input--error" id="free" name="subscription" value="free" aria-describedby="error-message-subscription" aria-invalid="true"/>
  <label class="usa-radio__label" for="free">Free</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile usa-radio__input--error" id="premium" name="subscription" value="premium" aria-describedby="error-message-subscription" aria-invalid="true"/>
  <label class="usa-radio__label" for="premium">Premium</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void required_radio() {
    assertHtml(
        // model
        Model.builder().id("terms").name("terms").label("Accept Terms").required("true").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:required="${model.required()}">
          <item id="yes" value="yes" label="Yes"/>
          <item id="no" value="no" label="No"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Accept Terms</legend>
<span id="error-message-terms" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="yes" name="terms" value="yes" required="required"/>
  <label class="usa-radio__label" for="yes">Yes</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="no" name="terms" value="no" required="required"/>
  <label class="usa-radio__label" for="no">No</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void required_radio_boolean_attribute() {
    assertHtml(
        // template
        """
        <cf:radio
            id="terms"
            name="terms"
            label="Accept Terms"
            required="true">
          <item id="yes" value="yes" label="Yes"/>
          <item id="no" value="no" label="No"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Accept Terms</legend>
<span id="error-message-terms" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="yes" name="terms" value="yes" required="required"/>
  <label class="usa-radio__label" for="yes">Yes</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="no" name="terms" value="no" required="required"/>
  <label class="usa-radio__label" for="no">No</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_selected_item() {
    assertHtml(
        // model
        Model.builder().id("size").name("size").label("Size").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="small" value="small" label="Small"/>
          <item id="medium" value="medium" label="Medium" selected="true"/>
          <item id="large" value="large" label="Large"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Size</legend>
<span id="error-message-size" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="small" name="size" value="small"/>
  <label class="usa-radio__label" for="small">Small</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="medium" name="size" value="medium" checked="checked"/>
  <label class="usa-radio__label" for="medium">Medium</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="large" name="size" value="large"/>
  <label class="usa-radio__label" for="large">Large</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_disabled_item() {
    assertHtml(
        // model
        Model.builder().id("payment").name("payment").label("Payment Method").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="credit" value="credit" label="Credit Card"/>
          <item id="debit" value="debit" label="Debit Card" disabled="true"/>
          <item id="paypal" value="paypal" label="PayPal"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Payment Method</legend>
<span id="error-message-payment" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="credit" name="payment" value="credit"/>
  <label class="usa-radio__label" for="credit">Credit Card</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="debit" name="payment" value="debit" disabled="disabled"/>
  <label class="usa-radio__label" for="debit">Debit Card</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="paypal" name="payment" value="paypal"/>
  <label class="usa-radio__label" for="paypal">PayPal</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_item_descriptions() {
    assertHtml(
        // model
        Model.builder().id("tier").name("tier").label("Membership Tier").build(),
        // template
        """
<cf:radio
  th:id="${model.id()}"
  th:name="${model.name()}"
  th:label="${model.label()}">
<item id="bronze" value="bronze" label="Bronze" description="$5/month - Basic features"/>
<item id="silver" value="silver" label="Silver" description="$10/month - Enhanced features"/>
<item id="gold" value="gold" label="Gold" description="$20/month - All features"/>
</cf:radio>
""",
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Membership Tier</legend>
<span id="error-message-tier" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="bronze" name="tier" value="bronze"/>
  <label class="usa-radio__label" for="bronze">Bronze<span class="usa-radio__label-description">$5/month - Basic features</span></label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="silver" name="tier" value="silver"/>
  <label class="usa-radio__label" for="silver">Silver<span class="usa-radio__label-description">$10/month - Enhanced features</span></label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="gold" name="tier" value="gold"/>
  <label class="usa-radio__label" for="gold">Gold<span class="usa-radio__label-description">$20/month - All features</span></label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_not_tiled() {
    assertHtml(
        // model
        Model.builder().id("choice").name("choice").label("Choice").tiled("false").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}"
            th:tiled="${model.tiled()}">
          <item id="option1" value="option1" label="Option 1"/>
          <item id="option2" value="option2" label="Option 2"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Choice</legend>
<span id="error-message-choice" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input" id="option1" name="choice" value="option1"/>
  <label class="usa-radio__label" for="option1">Option 1</label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input" id="option2" name="choice" value="option2"/>
  <label class="usa-radio__label" for="option2">Option 2</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_thymeleaf_item_attributes() {
    assertHtml(
        // model
        Model.builder().id("items").name("items").label("Items").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item th:id="${model.id()}" th:value="${model.id()}" th:label="${model.label()}"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Items</legend>
<span id="error-message-items" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="items" name="items" value="items"/>
  <label class="usa-radio__label" for="items">Items</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_data_attributes() {
    assertHtml(
        // model
        Model.builder().id("tracking").name("tracking").label("Tracking").build(),
        // template
        """
<cf:radio
  th:id="${model.id()}"
  th:name="${model.name()}"
  th:label="${model.label()}">
<item id="track1" value="track1" label="Option 1" data-testid="tracking-radio" data-analytics="track-me"/>
</cf:radio>
""",
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Tracking</legend>
<span id="error-message-tracking" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="track1" name="tracking" value="track1" data-testid="tracking-radio" data-analytics="track-me"/>
  <label class="usa-radio__label" for="track1">Option 1</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_aria_attributes() {
    assertHtml(
        // model
        Model.builder().id("accessible").name("accessible").label("Accessible").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="option1" value="option1" label="Option 1" aria-label="First option"/>
        </cf:radio>
        """,
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Accessible</legend>
<span id="error-message-accessible" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="option1" name="accessible" value="option1" aria-label="First option"/>
  <label class="usa-radio__label" for="option1">Option 1</label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_with_all_attributes() {
    assertHtml(
        // model
        Model.builder()
            .id("complete")
            .name("complete")
            .label("Complete Example")
            .helpText("Select one option")
            .required("true")
            .tiled("true")
            .build(),
        // template
        """
<cf:radio
    th:id="${model.id()}"
    th:name="${model.name()}"
    th:label="${model.label()}"
    th:help-text="${model.helpText()}"
    th:required="${model.required()}"
    th:tiled="${model.tiled()}">
  <item id="opt1" value="opt1" label="Option 1" description="First option" selected="true"/>
  <item id="opt2" value="opt2" label="Option 2" description="Second option"/>
  <item id="opt3" value="opt3" label="Option 3" description="Third option" disabled="true"/>
</cf:radio>
""",
        // expected
        """
<div class="usa-form-group">
<fieldset class="usa-fieldset">
<legend class="usa-legend">Complete Example</legend>
<div id="help-text-complete" class="usa-hint">Select one option</div>
<span id="error-message-complete" class="usa-error-message" role="alert" hidden="hidden"></span>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="opt1" name="complete" value="opt1" checked="checked" required="required"/>
  <label class="usa-radio__label" for="opt1">Option 1<span class="usa-radio__label-description">First option</span></label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="opt2" name="complete" value="opt2" required="required"/>
  <label class="usa-radio__label" for="opt2">Option 2<span class="usa-radio__label-description">Second option</span></label>
</div>
<div class="usa-radio">
  <input type="radio" class="usa-radio__input usa-radio__input--tile" id="opt3" name="complete" value="opt3" disabled="disabled" required="required"/>
  <label class="usa-radio__label" for="opt3">Option 3<span class="usa-radio__label-description">Third option</span></label>
</div>
</fieldset>
</div>
""");
  }

  @Test
  public void radio_without_id_throws() {
    assertException(
        // template
        """
        <cf:radio
            name="test"
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_blank_id_throws() {
    assertException(
        // template
        """
        <cf:radio
            id=""
            name="test"
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_without_name_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_blank_name_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name=""
            label="Test Label">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_without_label_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name="test">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_blank_label_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name="test"
            label="">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_null_id_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().id(null).name("test").label("Test").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_null_name_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().id("test").name(null).label("Test").build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_null_label_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().id("test").name("test").label(null).build(),
        // template
        """
        <cf:radio
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:label="${model.label()}">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_whitespace_only_id_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="   "
            name="test"
            label="Test">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_whitespace_only_name_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name="   "
            label="Test">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_with_whitespace_only_label_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name="test"
            label="   ">
          <item id="item1" value="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_item_without_value_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name="test"
            label="Test">
          <item id="item1" label="Item 1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void radio_item_without_label_throws() {
    assertException(
        // template
        """
        <cf:radio
            id="test"
            name="test"
            label="Test">
          <item id="item1" value="item1"/>
        </cf:radio>
        """,
        // expected
        IllegalStateException.class);
  }
}

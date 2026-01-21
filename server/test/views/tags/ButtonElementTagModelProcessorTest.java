package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;

public class ButtonElementTagModelProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new ButtonElementTagModelProcessor(prefix));
  }

  @Builder
  public record Model(
      String id,
      String name,
      String value,
      String text,
      String type,
      String variant,
      String size,
      String disabled,
      String inverse) {}

  @Test
  public void basic_button() {
    assertHtml(
        // model
        Model.builder().text("Click Me").build(),
        // template
        """
        <cf:button th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button">Click Me</button>
""");
  }

  @Test
  public void basic_button_with_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button text="Click Me" />
        """,
        // expected
        """
<button type="button" class="usa-button">Click Me</button>
""");
  }

  @Test
  public void button_with_nested_content() {
    assertHtml(
        // template
        """
        <cf:button><strong>Bold</strong>Text</cf:button>
        """,
        // expected
        """
<button type="button" class="usa-button"><strong>Bold</strong>Text</button>
""");
  }

  @Test
  public void button_with_id() {
    assertHtml(
        // model
        Model.builder().id("submitBtn").text("Submit").build(),
        // template
        """
        <cf:button th:id="${model.id()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button" id="submitBtn">Submit</button>
""");
  }

  @Test
  public void button_with_id_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button id="submitBtn" text="Submit" />
        """,
        // expected
        """
<button type="button" class="usa-button" id="submitBtn">Submit</button>
""");
  }

  @Test
  public void button_with_name_and_value() {
    assertHtml(
        // model
        Model.builder().name("action").value("save").text("Save").build(),
        // template
        """
        <cf:button
            th:name="${model.name()}"
            th:value="${model.value()}"
            th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button" name="action" value="save">Save</button>
""");
  }

  @Test
  public void button_with_name_and_value_plain_attributes() {
    assertHtml(
        // template
        """
        <cf:button
            name="action"
            value="save"
            text="Save" />
        """,
        // expected
        """
<button type="button" class="usa-button" name="action" value="save">Save</button>
""");
  }

  @Test
  public void button_type_submit() {
    assertHtml(
        // model
        Model.builder().type("submit").text("Submit Form").build(),
        // template
        """
        <cf:button th:type="${model.type()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="submit" class="usa-button">Submit Form</button>
""");
  }

  @Test
  public void button_type_submit_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button type="submit" text="Submit Form" />
        """,
        // expected
        """
<button type="submit" class="usa-button">Submit Form</button>
""");
  }

  @Test
  public void button_type_reset() {
    assertHtml(
        // model
        Model.builder().type("reset").text("Reset Form").build(),
        // template
        """
        <cf:button th:type="${model.type()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="reset" class="usa-button">Reset Form</button>
""");
  }

  @Test
  public void button_variant_secondary() {
    assertHtml(
        // model
        Model.builder().variant("secondary").text("Secondary").build(),
        // template
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--secondary">Secondary</button>
""");
  }

  @Test
  public void button_variant_secondary_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button variant="secondary" text="Secondary" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--secondary">Secondary</button>
""");
  }

  @Test
  public void button_variant_outline() {
    assertHtml(
        // model
        Model.builder().variant("outline").text("Outline").build(),
        // template
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--outline">Outline</button>
""");
  }

  @Test
  public void button_variant_accent_cool() {
    assertHtml(
        // model
        Model.builder().variant("accent-cool").text("Accent Cool").build(),
        // template
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--accent-cool">Accent Cool</button>
""");
  }

  @Test
  public void button_variant_accent_warm() {
    assertHtml(
        // model
        Model.builder().variant("accent-warm").text("Accent Warm").build(),
        // template
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--accent-warm">Accent Warm</button>
""");
  }

  @Test
  public void button_variant_base() {
    assertHtml(
        // model
        Model.builder().variant("base").text("Base").build(),
        // template
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--base">Base</button>
""");
  }

  @Test
  public void button_variant_unstyled() {
    assertHtml(
        // model
        Model.builder().variant("unstyled").text("Unstyled").build(),
        // template
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--unstyled">Unstyled</button>
""");
  }

  @Test
  public void button_size_big() {
    assertHtml(
        // model
        Model.builder().size("big").text("Big Button").build(),
        // template
        """
        <cf:button th:size="${model.size()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--big">Big Button</button>
""");
  }

  @Test
  public void button_size_big_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button size="big" text="Big Button" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--big">Big Button</button>
""");
  }

  @Test
  public void button_disabled() {
    assertHtml(
        // model
        Model.builder().disabled("true").text("Disabled").build(),
        // template
        """
        <cf:button th:disabled="${model.disabled()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button" disabled="disabled">Disabled</button>
""");
  }

  @Test
  public void button_disabled_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button disabled="true" text="Disabled" />
        """,
        // expected
        """
<button type="button" class="usa-button" disabled="disabled">Disabled</button>
""");
  }

  @Test
  public void button_inverse() {
    assertHtml(
        // model
        Model.builder().inverse("true").text("Inverse").build(),
        // template
        """
        <cf:button th:inverse="${model.inverse()}" th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--inverse">Inverse</button>
""");
  }

  @Test
  public void button_inverse_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:button inverse="true" text="Inverse" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--inverse">Inverse</button>
""");
  }

  @Test
  public void button_with_data_attributes() {
    assertHtml(
        // model
        Model.builder().text("Track Me").build(),
        // template
        """
        <cf:button
            th:text="${model.text()}"
            data-testid="submit-button"
            data-analytics="track-click" />
        """,
        // expected
        """
<button type="button" class="usa-button" data-testid="submit-button" data-analytics="track-click">Track Me</button>
""");
  }

  @Test
  public void button_with_thymeleaf_data_attributes() {
    assertHtml(
        // model
        Model.builder().id("myButton").text("Button").build(),
        // template
        """
        <cf:button
            th:id="${model.id()}"
            th:text="${model.text()}"
            th:data-testid="${model.id()}"
            th:data-target="'#' + ${model.id()}" />
        """,
        // expected
        """
<button type="button" class="usa-button" id="myButton" data-testid="myButton" data-target="#myButton">Button</button>
""");
  }

  @Test
  public void button_with_aria_attributes() {
    assertHtml(
        // model
        Model.builder().text("Close").build(),
        // template
        """
        <cf:button
            th:text="${model.text()}"
            aria-label="Close dialog" />
        """,
        // expected
        """
<button type="button" class="usa-button" aria-label="Close dialog">Close</button>
""");
  }

  @Test
  public void button_with_thymeleaf_aria_attributes() {
    assertHtml(
        // model
        Model.builder().text("Close").build(),
        // template
        """
        <cf:button
            th:text="${model.text()}"
            th:aria-label="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button" aria-label="Close">Close</button>
""");
  }

  @Test
  public void button_with_all_attributes() {
    assertHtml(
        // model
        Model.builder()
            .id("actionBtn")
            .name("action")
            .value("submit")
            .text("Submit Form")
            .type("submit")
            .variant("secondary")
            .size("big")
            .inverse("true")
            .build(),
        // template
        """
        <cf:button
            th:id="${model.id()}"
            th:name="${model.name()}"
            th:value="${model.value()}"
            th:text="${model.text()}"
            th:type="${model.type()}"
            th:variant="${model.variant()}"
            th:size="${model.size()}"
            th:inverse="${model.inverse()}" />
        """,
        // expected
        """
<button type="submit" class="usa-button usa-button--secondary usa-button--big usa-button--inverse" id="actionBtn" name="action" value="submit">Submit Form</button>
""");
  }

  @Test
  public void button_with_all_attributes_plain() {
    assertHtml(
        // template
        """
        <cf:button
            id="actionBtn"
            name="action"
            value="submit"
            text="Submit Form"
            type="submit"
            variant="secondary"
            size="big"
            inverse="true" />
        """,
        // expected
        """
<button type="submit" class="usa-button usa-button--secondary usa-button--big usa-button--inverse" id="actionBtn" name="action" value="submit">Submit Form</button>
""");
  }

  @Test
  public void button_secondary_outline_big() {
    assertHtml(
        // model
        Model.builder().variant("outline").size("big").text("Large Outline Button").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:size="${model.size()}"
            th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--outline usa-button--big">Large Outline Button</button>
""");
  }

  @Test
  public void button_submit_with_disabled() {
    assertHtml(
        // model
        Model.builder().type("submit").disabled("true").text("Submit").build(),
        // template
        """
        <cf:button
            th:type="${model.type()}"
            th:disabled="${model.disabled()}"
            th:text="${model.text()}" />
        """,
        // expected
        """
<button type="submit" class="usa-button" disabled="disabled">Submit</button>
""");
  }

  @Test
  public void button_inverse_with_variant() {
    assertHtml(
        // model
        Model.builder().variant("outline").inverse("true").text("Inverse Outline").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:inverse="${model.inverse()}"
            th:text="${model.text()}" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--outline usa-button--inverse">Inverse Outline</button>
""");
  }

  @Test
  public void mixed_thymeleaf_and_plain_attributes() {
    assertHtml(
        // model
        Model.builder().id("mixed").text("Mixed").build(),
        // template
        """
        <cf:button
            th:id="${model.id()}"
            text="Mixed"
            variant="secondary" />
        """,
        // expected
        """
<button type="button" class="usa-button usa-button--secondary" id="mixed">Mixed</button>
""");
  }

  @Test
  public void button_with_icon_nested_content() {
    assertHtml(
        // template
        """
<cf:button variant="outline"><svg class="usa-icon" aria-hidden="true"><use href="#close"></use></svg>Close</cf:button>
""",
        // expected
        """
<button type="button" class="usa-button usa-button--outline"><svg class="usa-icon" aria-hidden="true"><use href="#close"></use></svg>Close</button>
""");
  }

  @Test
  public void button_text_takes_precedence_over_nested_content() {
    assertHtml(
        // model
        Model.builder().text("Text Attribute").build(),
        // template
        """
        <cf:button th:text="${model.text()}">
          This nested content should be ignored
        </cf:button>
        """,
        // expected
        """
<button type="button" class="usa-button">Text Attribute</button>
""");
  }

  @Test
  public void button_empty_with_no_text_or_content() {
    assertHtml(
        // template
        """
        <cf:button />
        """,
        // expected
        """
<button type="button" class="usa-button"></button>
""");
  }

  @Test
  public void button_whitespace_text_preserved() {
    assertHtml(
        // template
        """
        <cf:button text="  Spaced  " />
        """,
        // expected
        """
<button type="button" class="usa-button">  Spaced  </button>
""");
  }

  @Test
  public void button_with_invalid_size_throws() {
    assertException(
        // model
        Model.builder().size("small").text("Button").build(),
        // template
        """
        <cf:button
            th:size="${model.size()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_size_medium_throws() {
    assertException(
        // model
        Model.builder().size("medium").text("Button").build(),
        // template
        """
        <cf:button
            th:size="${model.size()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_size_large_throws() {
    assertException(
        // model
        Model.builder().size("large").text("Button").build(),
        // template
        """
        <cf:button
            th:size="${model.size()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_variant_throws() {
    assertException(
        // model
        Model.builder().variant("primary").text("Button").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_variant_danger_throws() {
    assertException(
        // model
        Model.builder().variant("danger").text("Button").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_variant_success_throws() {
    assertException(
        // model
        Model.builder().variant("success").text("Button").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_variant_warning_throws() {
    assertException(
        // model
        Model.builder().variant("warning").text("Button").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_variant_info_throws() {
    assertException(
        // model
        Model.builder().variant("info").text("Button").build(),
        // template
        """
        <cf:button
            th:variant="${model.variant()}"
            th:text="${model.text()}" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_size_plain_attribute_throws() {
    assertException(
        // template
        """
        <cf:button
            size="small"
            text="Button" />
        """,
        // expected
        IllegalStateException.class);
  }

  @Test
  public void button_with_invalid_variant_plain_attribute_throws() {
    assertException(
        // template
        """
        <cf:button
            variant="primary"
            text="Button" />
        """,
        // expected
        IllegalStateException.class);
  }
}

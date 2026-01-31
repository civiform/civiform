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
  public void button_with_all_attributes() {
    // With Thymeleaf
    assertHtml(
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
        """
<button type="submit" class="usa-button usa-button--secondary usa-button--big usa-button--inverse" id="actionBtn" name="action" value="submit">Submit Form</button>
""");

    // With plain attributes
    assertHtml(
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
        """
<button type="submit" class="usa-button usa-button--secondary usa-button--big usa-button--inverse" id="actionBtn" name="action" value="submit">Submit Form</button>
""");
  }

  @Test
  public void button_type_submit() {
    // With Thymeleaf
    assertHtml(
        Model.builder().type("submit").text("Submit Form").build(),
        """
        <cf:button th:type="${model.type()}" th:text="${model.text()}" />
        """,
        """
        <button type="submit" class="usa-button">Submit Form</button>
        """);

    // With plain attributes
    assertHtml(
        """
        <cf:button type="submit" text="Submit Form" />
        """,
        """
        <button type="submit" class="usa-button">Submit Form</button>
        """);
  }

  @Test
  public void button_variant_secondary() {
    // With Thymeleaf
    assertHtml(
        Model.builder().variant("secondary").text("Secondary").build(),
        """
        <cf:button th:variant="${model.variant()}" th:text="${model.text()}" />
        """,
        """
        <button type="button" class="usa-button usa-button--secondary">Secondary</button>
        """);

    // With plain attributes
    assertHtml(
        """
        <cf:button variant="secondary" text="Secondary" />
        """,
        """
        <button type="button" class="usa-button usa-button--secondary">Secondary</button>
        """);
  }

  @Test
  public void button_size_big() {
    // With Thymeleaf
    assertHtml(
        Model.builder().size("big").text("Big Button").build(),
        """
        <cf:button th:size="${model.size()}" th:text="${model.text()}" />
        """,
        """
        <button type="button" class="usa-button usa-button--big">Big Button</button>
        """);

    // With plain attributes
    assertHtml(
        """
        <cf:button size="big" text="Big Button" />
        """,
        """
        <button type="button" class="usa-button usa-button--big">Big Button</button>
        """);
  }

  @Test
  public void button_with_data_attributes() {
    // Plain data attributes
    assertHtml(
        """
        <cf:button
            text="Track Me"
            data-testid="submit-button"
            data-analytics="track-click" />
        """,
        """
<button type="button" class="usa-button" data-testid="submit-button" data-analytics="track-click">Track Me</button>
""");

    // Thymeleaf data attributes
    assertHtml(
        Model.builder().id("myButton").text("Button").build(),
        """
        <cf:button
            th:id="${model.id()}"
            th:text="${model.text()}"
            th:data-testid="${model.id()}"
            th:data-target="'#' + ${model.id()}" />
        """,
        """
<button type="button" class="usa-button" id="myButton" data-testid="myButton" data-target="#myButton">Button</button>
""");
  }

  @Test
  public void button_with_aria_attributes() {
    // Plain aria attributes
    assertHtml(
        Model.builder().text("Close").build(),
        """
        <cf:button
            th:text="${model.text()}"
            aria-label="Close dialog" />
        """,
        """
        <button type="button" class="usa-button" aria-label="Close dialog">Close</button>
        """);

    // Thymeleaf aria attributes
    assertHtml(
        Model.builder().text("Close").build(),
        """
        <cf:button
            th:text="${model.text()}"
            th:aria-label="${model.text()}" />
        """,
        """
        <button type="button" class="usa-button" aria-label="Close">Close</button>
        """);
  }

  @Test
  public void button_secondary_outline_big() {
    assertHtml(
        Model.builder().variant("outline").size("big").text("Large Outline Button").build(),
        """
        <cf:button
            th:variant="${model.variant()}"
            th:size="${model.size()}"
            th:text="${model.text()}" />
        """,
        """
<button type="button" class="usa-button usa-button--outline usa-button--big">Large Outline Button</button>
""");
  }

  @Test
  public void button_submit_with_disabled() {
    // With Thymeleaf
    assertHtml(
        Model.builder().disabled("true").text("Disabled").build(),
        """
        <cf:button th:disabled="${model.disabled()}" th:text="${model.text()}" />
        """,
        """
        <button type="button" class="usa-button" disabled="disabled">Disabled</button>
        """);

    // With plain attributes
    assertHtml(
        """
        <cf:button disabled="true" text="Disabled" />
        """,
        """
        <button type="button" class="usa-button" disabled="disabled">Disabled</button>
        """);
  }

  @Test
  public void button_inverse_with_variant() {
    assertHtml(
        Model.builder().variant("outline").inverse("true").text("Inverse Outline").build(),
        """
        <cf:button
            th:variant="${model.variant()}"
            th:inverse="${model.inverse()}"
            th:text="${model.text()}" />
        """,
        """
<button type="button" class="usa-button usa-button--outline usa-button--inverse">Inverse Outline</button>
""");
  }

  @Test
  public void button_with_icon_nested_content() {
    assertHtml(
        """
        <cf:button variant="outline"><svg class="usa-icon" aria-hidden="true">
          <use href="#close"></use></svg>Close
        </cf:button>
        """,
        """
        <button type="button" class="usa-button usa-button--outline">
          <svg class="usa-icon" aria-hidden="true"><use href="#close"></use></svg>Close
        </button>
        """);
  }

  @Test
  public void button_empty_with_no_text_or_content() {
    assertHtml(
        """
        <cf:button />
        """,
        """
        <button type="button" class="usa-button"></button>
        """);
  }

  @Test
  public void invalid_size_attribute_values_throw_exceptions() {
    assertException(
        Model.builder().size("small").text("Button").build(),
        """
        <cf:button
            th:size="${model.size()}"
            th:text="${model.text()}" />
        """,
        IllegalStateException.class);
  }

  @Test
  public void invalid_variant_attribute_values_throw_exceptions() {
    assertException(
        Model.builder().variant("primary").text("Button").build(),
        """
        <cf:button
            th:variant="${model.variant()}"
            th:text="${model.text()}" />
        """,
        IllegalStateException.class);
  }

  @Test
  public void invalid_type_attribute_values_throw_exceptions() {
    assertException(
        Model.builder().type("primary").text("Button").build(),
        """
        <cf:button
            th:type="${model.type()}"
            th:text="${model.text()}" />
        """,
        IllegalStateException.class);
  }
}

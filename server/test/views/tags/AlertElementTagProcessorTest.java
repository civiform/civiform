package views.tags;

import java.util.Set;
import lombok.Builder;
import org.junit.Test;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.processor.IProcessor;

public class AlertElementTagProcessorTest extends BaseElementTagModelProcessorTest {
  @Override
  protected Set<IProcessor> getTestProcessors(String prefix) {
    return Set.of(new AlertElementTagModelProcessor(prefix));
  }

  @Test
  public void addda() {
    assertHtml(
        // template
        """
        <cf:alert type="info">
            <h2>Lorem Ipsum</h2>
            <text>Lorem Ipsum</text>
        </cf:alert>
        """,
        // expected
        """
        <div class="usa-alert usa-alert--info">
            <div class="usa-alert__body">
                <h2 class="usa-alert__heading">Lorem Ipsum</h2>
                <p class="usa-alert__text">Lorem Ipsum</p>
            </div>
        </div>
        """);
  }

  @Test
  public void adddb() {
    assertHtml(
        // template
        """
        <cf:alert type="success">
            <h2>Lorem Ipsum</h2>
            <content>
                <p class="usa-alert__text">Lorem Ipsum</p>
                <ul class="usa-list">
                    <li>one</li>
                    <li>two</li>
                    <li>three</li>
                </ul>
                <p class="usa-alert__text">Lorem Ipsum</p>
                <button class="usa-button">click</button>
                <p class="usa-alert__text">Lorem Ipsum</p>
            </content>
        </cf:alert>
        """,
        // expected
        """
        <div class="usa-alert usa-alert--success">
            <div class="usa-alert__body">
                <h2 class="usa-alert__heading">Lorem Ipsum</h2>
                <p class="usa-alert__text">Lorem Ipsum</p>
                <ul class="usa-list">
                    <li>one</li>
                    <li>two</li>
                    <li>three</li>
                </ul>
                <p class="usa-alert__text">Lorem Ipsum</p>
                <button class="usa-button">click</button>
                <p class="usa-alert__text">Lorem Ipsum</p>
            </div>
        </div>
        """);
  }

  @Builder
  public record Model(String type, String slim, String noIcon) {}

  @Test
  public void basic_alert_with_text() {
    assertHtml(
        // template
        """
        <cf:alert>
          <text>This is an alert message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <p class="usa-alert__text">This is an alert message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_type_success() {
    assertHtml(
        // model
        Model.builder().type("success").build(),
        // template
        """
        <cf:alert th:type="${model.type()}">
          <text>Success message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--success">
<div class="usa-alert__body">
  <p class="usa-alert__text">Success message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_type_success_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:alert type="success">
          <text>Success message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--success">
<div class="usa-alert__body">
  <p class="usa-alert__text">Success message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_type_warning() {
    assertHtml(
        // model
        Model.builder().type("warning").build(),
        // template
        """
        <cf:alert th:type="${model.type()}">
          <text>Warning message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--warning">
<div class="usa-alert__body">
  <p class="usa-alert__text">Warning message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_type_error() {
    assertHtml(
        // model
        Model.builder().type("error").build(),
        // template
        """
        <cf:alert th:type="${model.type()}">
          <text>Error message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--error">
<div class="usa-alert__body">
  <p class="usa-alert__text">Error message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_type_info() {
    assertHtml(
        // model
        Model.builder().type("info").build(),
        // template
        """
        <cf:alert th:type="${model.type()}">
          <text>Info message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <p class="usa-alert__text">Info message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_heading_h2() {
    assertHtml(
        // template
        """
        <cf:alert type="success">
          <h2>Success Heading</h2>
          <text>Success message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--success">
<div class="usa-alert__body">
  <h2 class="usa-alert__heading">Success Heading</h2>
  <p class="usa-alert__text">Success message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_heading_h3() {
    assertHtml(
        // template
        """
        <cf:alert type="info">
          <h3>Information</h3>
          <text>Info message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <h3 class="usa-alert__heading">Information</h3>
  <p class="usa-alert__text">Info message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_heading_h4() {
    assertHtml(
        // template
        """
        <cf:alert type="warning">
          <h4>Warning Title</h4>
          <text>Warning message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--warning">
<div class="usa-alert__body">
  <h4 class="usa-alert__heading">Warning Title</h4>
  <p class="usa-alert__text">Warning message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_heading_existing_class() {
    assertHtml(
        // template
        """
        <cf:alert type="success">
          <h2 class="custom-class">Success Heading</h2>
          <text>Success message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--success">
<div class="usa-alert__body">
  <h2 class="custom-class usa-alert__heading">Success Heading</h2>
  <p class="usa-alert__text">Success message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_slim() {
    assertHtml(
        // model
        Model.builder().slim("true").build(),
        // template
        """
        <cf:alert th:slim="${model.slim()}">
          <text>Slim alert</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info usa-alert--slim">
<div class="usa-alert__body">
  <p class="usa-alert__text">Slim alert</p>
</div>
</div>
""");
  }

  @Test
  public void alert_slim_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:alert slim="true">
          <text>Slim alert</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info usa-alert--slim">
<div class="usa-alert__body">
  <p class="usa-alert__text">Slim alert</p>
</div>
</div>
""");
  }

  @Test
  public void alert_no_icon() {
    assertHtml(
        // model
        Model.builder().noIcon("true").build(),
        // template
        """
        <cf:alert th:no-icon="${model.noIcon()}">
          <text>Alert without icon</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info usa-alert--no-icon">
<div class="usa-alert__body">
  <p class="usa-alert__text">Alert without icon</p>
</div>
</div>
""");
  }

  @Test
  public void alert_no_icon_plain_attribute() {
    assertHtml(
        // template
        """
        <cf:alert no-icon="true">
          <text>Alert without icon</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info usa-alert--no-icon">
<div class="usa-alert__body">
  <p class="usa-alert__text">Alert without icon</p>
</div>
</div>
""");
  }

  @Test
  public void alert_slim_and_no_icon() {
    assertHtml(
        // model
        Model.builder().slim("true").noIcon("true").build(),
        // template
        """
        <cf:alert th:slim="${model.slim()}" th:no-icon="${model.noIcon()}">
          <text>Slim alert without icon</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info usa-alert--slim usa-alert--no-icon">
<div class="usa-alert__body">
  <p class="usa-alert__text">Slim alert without icon</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_content() {
    assertHtml(
        // template
        """
        <cf:alert type="success">
          <h2>Success</h2>
          <content>
            <p>This is a paragraph with <strong>bold text</strong>.</p>
            <ul>
              <li>Item one</li>
              <li>Item two</li>
            </ul>
          </content>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--success">
<div class="usa-alert__body">
  <h2 class="usa-alert__heading">Success</h2>
  <p>This is a paragraph with <strong>bold text</strong>.</p>
  <ul>
    <li>Item one</li>
    <li>Item two</li>
  </ul>
</div>
</div>
""");
  }

  @Test
  public void alert_with_content_no_heading() {
    assertHtml(
        // template
        """
        <cf:alert type="info">
          <content>
            <p>First paragraph.</p>
            <p>Second paragraph with <a href="#">a link</a>.</p>
          </content>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <p>First paragraph.</p>
  <p>Second paragraph with <a href="#">a link</a>.</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_all_attributes() {
    assertHtml(
        // model
        Model.builder().type("warning").slim("true").noIcon("true").build(),
        // template
        """
        <cf:alert
            th:type="${model.type()}"
            th:slim="${model.slim()}"
            th:no-icon="${model.noIcon()}">
          <text>Complete alert</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--warning usa-alert--slim usa-alert--no-icon">
<div class="usa-alert__body">
  <p class="usa-alert__text">Complete alert</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_all_attributes_plain() {
    assertHtml(
        // template
        """
        <cf:alert
            type="warning"
            slim="true"
            no-icon="true">
          <text>Complete alert</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--warning usa-alert--slim usa-alert--no-icon">
<div class="usa-alert__body">
  <p class="usa-alert__text">Complete alert</p>
</div>
</div>
""");
  }

  @Test
  public void alert_text_only_no_wrapper() {
    assertHtml(
        // template
        """
        <cf:alert>
          <text>Simple text message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <p class="usa-alert__text">Simple text message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_heading_only() {
    assertHtml(
        // template
        """
        <cf:alert>
          <h2>Just a heading</h2>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <h2 class="usa-alert__heading">Just a heading</h2>
</div>
</div>
""");
  }

  @Test
  public void alert_empty() {
    assertHtml(
        // template
        """
        <cf:alert />
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
</div>
</div>
""");
  }

  @Test
  public void alert_content_with_nested_html() {
    assertHtml(
        // template
        """
<cf:alert type="error">
  <h3>Error Details</h3>
  <content>
    <p>An error occurred:</p>
    <ol>
      <li>Check your input</li>
      <li>Try again</li>
    </ol>
    <p>Contact <a href="mailto:support@example.com">support</a> if the problem persists.</p>
  </content>
</cf:alert>
""",
        // expected
        """
<div class="usa-alert usa-alert--error">
<div class="usa-alert__body">
  <h3 class="usa-alert__heading">Error Details</h3>
  <p>An error occurred:</p>
  <ol>
    <li>Check your input</li>
    <li>Try again</li>
  </ol>
  <p>Contact <a href="mailto:support@example.com">support</a> if the problem persists.</p>
</div>
</div>
""");
  }

  @Test
  public void alert_mixed_thymeleaf_and_plain_attributes() {
    assertHtml(
        // model
        Model.builder().type("success").build(),
        // template
        """
        <cf:alert
            th:type="${model.type()}"
            slim="true">
          <text>Mixed attributes</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--success usa-alert--slim">
<div class="usa-alert__body">
  <p class="usa-alert__text">Mixed attributes</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_h1_heading() {
    assertHtml(
        // template
        """
        <cf:alert>
          <h1>Main Title</h1>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <h1 class="usa-alert__heading">Main Title</h1>
  <p class="usa-alert__text">Message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_h5_heading() {
    assertHtml(
        // template
        """
        <cf:alert>
          <h5>Small Heading</h5>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <h5 class="usa-alert__heading">Small Heading</h5>
  <p class="usa-alert__text">Message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_with_h6_heading() {
    assertHtml(
        // template
        """
        <cf:alert>
          <h6>Smallest Heading</h6>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        """
<div class="usa-alert usa-alert--info">
<div class="usa-alert__body">
  <h6 class="usa-alert__heading">Smallest Heading</h6>
  <p class="usa-alert__text">Message</p>
</div>
</div>
""");
  }

  @Test
  public void alert_slim_with_heading_throws() {
    assertException(
        // template
        """
        <cf:alert slim="true">
          <h2>Heading</h2>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_slim_with_heading_thymeleaf_throws() {
    assertException(
        // model
        Model.builder().slim("true").build(),
        // template
        """
        <cf:alert th:slim="${model.slim()}">
          <h2>Heading</h2>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_with_both_text_and_content_throws() {
    assertException(
        // template
        """
        <cf:alert>
          <text>Text element</text>
          <content>
            <p>Content element</p>
          </content>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_heading_not_first_element_throws() {
    assertException(
        // template
        """
        <cf:alert>
          <text>Text first</text>
          <h2>Heading second</h2>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_heading_after_content_throws() {
    assertException(
        // template
        """
        <cf:alert>
          <content>
            <p>Content first</p>
          </content>
          <h2>Heading after content</h2>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_slim_with_h1_throws() {
    assertException(
        // template
        """
        <cf:alert slim="true">
          <h1>Title</h1>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_slim_with_h3_throws() {
    assertException(
        // template
        """
        <cf:alert slim="true">
          <h3>Title</h3>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_slim_with_h4_throws() {
    assertException(
        // template
        """
        <cf:alert slim="true">
          <h4>Title</h4>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_slim_with_h5_throws() {
    assertException(
        // template
        """
        <cf:alert slim="true">
          <h5>Title</h5>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }

  @Test
  public void alert_slim_with_h6_throws() {
    assertException(
        // template
        """
        <cf:alert slim="true">
          <h6>Title</h6>
          <text>Message</text>
        </cf:alert>
        """,
        // expected
        TemplateProcessingException.class);
  }
}

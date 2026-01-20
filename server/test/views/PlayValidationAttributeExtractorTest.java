package views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import org.junit.Test;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;

public class PlayValidationAttributeExtractorTest {

  // Test model classes with getters
  public static class UserModel {
    @Required(message = "Username is required")
    @MinLength(value = 3, message = "Username must be at least 3 characters")
    @MaxLength(value = 20, message = "Username cannot exceed 20 characters")
    private String username;

    @Required
    @Email(message = "Please enter a valid email")
    private String email;

    @Pattern(value = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Min(value = 18, message = "Must be at least 18 years old")
    @Max(value = 120, message = "Age cannot exceed 120")
    private Integer age;

    private String noValidation;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public String getNoValidation() {
      return noValidation;
    }

    public void setNoValidation(String noValidation) {
      this.noValidation = noValidation;
    }
  }

  public static class RequiredOnlyModel {
    @Required private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

  public static class CustomMessageModel {
    @Required(message = "custom.required.message")
    @MinLength(value = 5, message = "custom.minlength.message")
    private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

  public static class EmptyModel {
    private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

  public static class MultipleValidationsModel {
    @Required(message = "Field is required")
    @MinLength(value = 2, message = "Too short")
    @MaxLength(value = 10, message = "Too long")
    @Pattern(value = "[a-z]+", message = "Lowercase only")
    private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

  public static class NumericValidationsModel {
    @Min(value = 0, message = "Cannot be negative")
    @Max(value = 100, message = "Cannot exceed 100")
    private Integer score;

    public Integer getScore() {
      return score;
    }

    public void setScore(Integer score) {
      this.score = score;
    }
  }

  // Tests
  @Test
  public void testRequiredAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(RequiredOnlyModel.class, "field");

    assertThat(attrs).containsKey("required").containsEntry("required", "required");
  }

  @Test
  public void testRequiredWithCustomMessage() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "username");

    assertThat(attrs)
        .containsEntry("required", "required")
        .containsEntry("data-required-message", "Username is required");
  }

  @Test
  public void testMinLengthAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "username");

    assertThat(attrs)
        .containsEntry("minlength", "3")
        .containsEntry("data-minlength-message", "Username must be at least 3 characters");
  }

  @Test
  public void testMaxLengthAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "username");

    assertThat(attrs)
        .containsEntry("maxlength", "20")
        .containsEntry("data-maxlength-message", "Username cannot exceed 20 characters");
  }

  @Test
  public void testEmailAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "email");

    assertThat(attrs)
        .doesNotContainKey("type")
        .containsEntry("data-email-message", "Please enter a valid email");
  }

  @Test
  public void testPatternAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "phone");

    assertThat(attrs)
        .containsEntry("pattern", "^[0-9]{10}$")
        .containsEntry("data-pattern-message", "Phone must be 10 digits");
  }

  @Test
  public void testMinAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "age");

    assertThat(attrs)
        .containsEntry("min", "18")
        .containsEntry("data-min-message", "Must be at least 18 years old");
  }

  @Test
  public void testMaxAnnotation() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "age");

    assertThat(attrs)
        .containsEntry("max", "120")
        .containsEntry("data-max-message", "Age cannot exceed 120");
  }

  @Test
  public void testFieldWithNoValidations() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "noValidation");

    assertThat(attrs).isEmpty();
  }

  @Test
  public void testNonExistentField() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "nonExistentField");

    assertThat(attrs).isEmpty();
  }

  @Test
  public void testMultipleValidations() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(MultipleValidationsModel.class, "field");

    assertThat(attrs)
        .hasSize(8)
        .containsKeys("required", "minlength", "maxlength", "pattern")
        .containsKeys("data-required-message", "data-minlength-message", "data-maxlength-message")
        .containsEntry("required", "required")
        .containsEntry("data-pattern-message", "Lowercase only")
        .containsEntry("minlength", "2")
        .containsEntry("maxlength", "10")
        .containsEntry("pattern", "[a-z]+")
        .containsEntry("data-required-message", "Field is required")
        .containsEntry("data-minlength-message", "Too short")
        .containsEntry("data-maxlength-message", "Too long");
  }

  @Test
  public void testRequiredWithoutCustomMessage() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(RequiredOnlyModel.class, "field");

    assertThat(attrs)
        .containsKey("required")
        .containsKey("data-required-message")
        .containsEntry("data-required-message", "error.required");
  }

  @Test
  public void testGetAttributesAsString() {
    String attrsString =
        PlayValidationAttributeExtractor.getAttributesAsString(CustomMessageModel.class, "field");

    assertThat(attrsString)
        .contains("required=\"required\"")
        .contains("minlength=\"5\"")
        .contains("data-required-message=\"custom.required.message\"")
        .contains("data-minlength-message=\"custom.minlength.message\"");
  }

  @Test
  public void testGetAttributesAsStringEmpty() {
    String attrsString =
        PlayValidationAttributeExtractor.getAttributesAsString(EmptyModel.class, "field");

    assertThat(attrsString).isEmpty();
  }

  @Test
  public void testNumericValidations() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(NumericValidationsModel.class, "score");

    assertThat(attrs)
        .hasSize(4)
        .containsEntry("min", "0")
        .containsEntry("max", "100")
        .containsEntry("data-min-message", "Cannot be negative")
        .containsEntry("data-max-message", "Cannot exceed 100");
  }

  @Test
  public void testAttributeStringFormat() {
    String attrsString =
        PlayValidationAttributeExtractor.getAttributesAsString(RequiredOnlyModel.class, "field");

    assertThat(attrsString).doesNotEndWith(" ").matches(".*=\".*\".*");
  }

  @Test
  public void testUsernameFieldContainsAllValidations() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "username");

    assertThat(attrs)
        .hasSize(6)
        .containsOnly(
            entry("required", "required"),
            entry("data-required-message", "Username is required"),
            entry("minlength", "3"),
            entry("data-minlength-message", "Username must be at least 3 characters"),
            entry("maxlength", "20"),
            entry("data-maxlength-message", "Username cannot exceed 20 characters"));
  }

  @Test
  public void testEmailFieldContainsOnlyEmailAndRequiredValidations() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(UserModel.class, "email");

    assertThat(attrs)
        .hasSize(3)
        .containsOnly(
            entry("required", "required"),
            entry("data-required-message", "error.required"),
            entry("data-email-message", "Please enter a valid email"));
  }

  @Test
  public void testPatternMessageIsPreserved() {
    Map<String, String> attrs =
        PlayValidationAttributeExtractor.getHtmlAttributes(MultipleValidationsModel.class, "field");

    assertThat(attrs).extractingByKey("data-pattern-message").isEqualTo("Lowercase only");
  }

  @Test
  public void testAttributesAsStringContainsNoExtraWhitespace() {
    String attrsString =
        PlayValidationAttributeExtractor.getAttributesAsString(CustomMessageModel.class, "field");

    assertThat(attrsString)
        .doesNotStartWith(" ")
        .doesNotEndWith(" ")
        .doesNotContain("  "); // no double spaces
  }
}

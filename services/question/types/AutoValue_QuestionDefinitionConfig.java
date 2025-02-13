package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import javax.annotation.processing.Generated;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_QuestionDefinitionConfig extends QuestionDefinitionConfig {

  private final String name;

  private final String description;

  private final LocalizedStrings questionText;

  private final Optional<LocalizedStrings> questionHelpTextInternal;

  private final Optional<QuestionDefinition.ValidationPredicates> validationPredicates;

  private final OptionalLong id;

  private final Optional<Long> enumeratorId;

  private final Optional<Instant> lastModifiedTime;

  private final boolean universal;

  private final ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags;

  private AutoValue_QuestionDefinitionConfig(
      String name,
      String description,
      LocalizedStrings questionText,
      Optional<LocalizedStrings> questionHelpTextInternal,
      Optional<QuestionDefinition.ValidationPredicates> validationPredicates,
      OptionalLong id,
      Optional<Long> enumeratorId,
      Optional<Instant> lastModifiedTime,
      boolean universal,
      ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags) {
    this.name = name;
    this.description = description;
    this.questionText = questionText;
    this.questionHelpTextInternal = questionHelpTextInternal;
    this.validationPredicates = validationPredicates;
    this.id = id;
    this.enumeratorId = enumeratorId;
    this.lastModifiedTime = lastModifiedTime;
    this.universal = universal;
    this.primaryApplicantInfoTags = primaryApplicantInfoTags;
  }

  @JsonProperty("name")
  @Override
  String name() {
    return name;
  }

  @JsonProperty("description")
  @Override
  String description() {
    return description;
  }

  @JsonProperty("questionText")
  @Override
  LocalizedStrings questionText() {
    return questionText;
  }

  @JsonProperty("questionHelpText")
  @Override
  Optional<LocalizedStrings> questionHelpTextInternal() {
    return questionHelpTextInternal;
  }

  @JsonProperty("validationPredicates")
  @Override
  Optional<QuestionDefinition.ValidationPredicates> validationPredicates() {
    return validationPredicates;
  }

  @JsonProperty("id")
  @Override
  OptionalLong id() {
    return id;
  }

  @JsonProperty("enumeratorId")
  @Override
  Optional<Long> enumeratorId() {
    return enumeratorId;
  }

  @JsonIgnore
  @Override
  Optional<Instant> lastModifiedTime() {
    return lastModifiedTime;
  }

  @JsonProperty("universal")
  @Override
  boolean universal() {
    return universal;
  }

  @JsonProperty("primaryApplicantInfoTags")
  @Override
  ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags() {
    return primaryApplicantInfoTags;
  }

  @Override
  public String toString() {
    return "QuestionDefinitionConfig{"
        + "name="
        + name
        + ", "
        + "description="
        + description
        + ", "
        + "questionText="
        + questionText
        + ", "
        + "questionHelpTextInternal="
        + questionHelpTextInternal
        + ", "
        + "validationPredicates="
        + validationPredicates
        + ", "
        + "id="
        + id
        + ", "
        + "enumeratorId="
        + enumeratorId
        + ", "
        + "lastModifiedTime="
        + lastModifiedTime
        + ", "
        + "universal="
        + universal
        + ", "
        + "primaryApplicantInfoTags="
        + primaryApplicantInfoTags
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof QuestionDefinitionConfig) {
      QuestionDefinitionConfig that = (QuestionDefinitionConfig) o;
      return this.name.equals(that.name())
          && this.description.equals(that.description())
          && this.questionText.equals(that.questionText())
          && this.questionHelpTextInternal.equals(that.questionHelpTextInternal())
          && this.validationPredicates.equals(that.validationPredicates())
          && this.id.equals(that.id())
          && this.enumeratorId.equals(that.enumeratorId())
          && this.lastModifiedTime.equals(that.lastModifiedTime())
          && this.universal == that.universal()
          && this.primaryApplicantInfoTags.equals(that.primaryApplicantInfoTags());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= description.hashCode();
    h$ *= 1000003;
    h$ ^= questionText.hashCode();
    h$ *= 1000003;
    h$ ^= questionHelpTextInternal.hashCode();
    h$ *= 1000003;
    h$ ^= validationPredicates.hashCode();
    h$ *= 1000003;
    h$ ^= id.hashCode();
    h$ *= 1000003;
    h$ ^= enumeratorId.hashCode();
    h$ *= 1000003;
    h$ ^= lastModifiedTime.hashCode();
    h$ *= 1000003;
    h$ ^= universal ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= primaryApplicantInfoTags.hashCode();
    return h$;
  }

  @Override
  public QuestionDefinitionConfig.Builder toBuilder() {
    return new AutoValue_QuestionDefinitionConfig.Builder(this);
  }

  static final class Builder extends QuestionDefinitionConfig.Builder {
    private String name;
    private String description;
    private LocalizedStrings questionText;
    private Optional<LocalizedStrings> questionHelpTextInternal = Optional.empty();
    private Optional<QuestionDefinition.ValidationPredicates> validationPredicates =
        Optional.empty();
    private OptionalLong id = OptionalLong.empty();
    private Optional<Long> enumeratorId = Optional.empty();
    private Optional<Instant> lastModifiedTime = Optional.empty();
    private boolean universal;
    private ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags;
    private byte set$0;

    Builder() {}

    Builder(QuestionDefinitionConfig source) {
      this.name = source.name();
      this.description = source.description();
      this.questionText = source.questionText();
      this.questionHelpTextInternal = source.questionHelpTextInternal();
      this.validationPredicates = source.validationPredicates();
      this.id = source.id();
      this.enumeratorId = source.enumeratorId();
      this.lastModifiedTime = source.lastModifiedTime();
      this.universal = source.universal();
      this.primaryApplicantInfoTags = source.primaryApplicantInfoTags();
      set$0 = (byte) 1;
    }

    @Override
    public QuestionDefinitionConfig.Builder setName(String name) {
      if (name == null) {
        throw new NullPointerException("Null name");
      }
      this.name = name;
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setDescription(String description) {
      if (description == null) {
        throw new NullPointerException("Null description");
      }
      this.description = description;
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setQuestionText(LocalizedStrings questionText) {
      if (questionText == null) {
        throw new NullPointerException("Null questionText");
      }
      this.questionText = questionText;
      return this;
    }

    @Override
    QuestionDefinitionConfig.Builder setQuestionHelpTextInternal(
        LocalizedStrings questionHelpTextInternal) {
      this.questionHelpTextInternal = Optional.of(questionHelpTextInternal);
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setValidationPredicates(
        QuestionDefinition.ValidationPredicates validationPredicates) {
      this.validationPredicates = Optional.of(validationPredicates);
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setId(long id) {
      this.id = OptionalLong.of(id);
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setId(OptionalLong id) {
      if (id == null) {
        throw new NullPointerException("Null id");
      }
      this.id = id;
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setEnumeratorId(long enumeratorId) {
      this.enumeratorId = Optional.of(enumeratorId);
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setEnumeratorId(Optional<Long> enumeratorId) {
      if (enumeratorId == null) {
        throw new NullPointerException("Null enumeratorId");
      }
      this.enumeratorId = enumeratorId;
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setLastModifiedTime(Instant lastModifiedTime) {
      this.lastModifiedTime = Optional.of(lastModifiedTime);
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setLastModifiedTime(
        Optional<Instant> lastModifiedTime) {
      if (lastModifiedTime == null) {
        throw new NullPointerException("Null lastModifiedTime");
      }
      this.lastModifiedTime = lastModifiedTime;
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setUniversal(boolean universal) {
      this.universal = universal;
      set$0 |= (byte) 1;
      return this;
    }

    @Override
    public QuestionDefinitionConfig.Builder setPrimaryApplicantInfoTags(
        ImmutableSet<PrimaryApplicantInfoTag> primaryApplicantInfoTags) {
      if (primaryApplicantInfoTags == null) {
        throw new NullPointerException("Null primaryApplicantInfoTags");
      }
      this.primaryApplicantInfoTags = primaryApplicantInfoTags;
      return this;
    }

    @Override
    public QuestionDefinitionConfig build() {
      if (set$0 != 1
          || this.name == null
          || this.description == null
          || this.questionText == null
          || this.primaryApplicantInfoTags == null) {
        StringBuilder missing = new StringBuilder();
        if (this.name == null) {
          missing.append(" name");
        }
        if (this.description == null) {
          missing.append(" description");
        }
        if (this.questionText == null) {
          missing.append(" questionText");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" universal");
        }
        if (this.primaryApplicantInfoTags == null) {
          missing.append(" primaryApplicantInfoTags");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_QuestionDefinitionConfig(
          this.name,
          this.description,
          this.questionText,
          this.questionHelpTextInternal,
          this.validationPredicates,
          this.id,
          this.enumeratorId,
          this.lastModifiedTime,
          this.universal,
          this.primaryApplicantInfoTags);
    }
  }
}

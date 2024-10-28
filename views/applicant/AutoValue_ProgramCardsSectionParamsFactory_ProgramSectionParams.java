package views.applicant;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ProgramCardsSectionParamsFactory_ProgramSectionParams extends ProgramCardsSectionParamsFactory.ProgramSectionParams {

  private final ImmutableList<ProgramCardsSectionParamsFactory.ProgramCardParams> cards;

  private final Optional<String> title;

  private final ProgramCardsSectionParamsFactory.SectionType sectionType;

  private final Optional<String> id;

  private AutoValue_ProgramCardsSectionParamsFactory_ProgramSectionParams(
      ImmutableList<ProgramCardsSectionParamsFactory.ProgramCardParams> cards,
      Optional<String> title,
      ProgramCardsSectionParamsFactory.SectionType sectionType,
      Optional<String> id) {
    this.cards = cards;
    this.title = title;
    this.sectionType = sectionType;
    this.id = id;
  }

  @Override
  public ImmutableList<ProgramCardsSectionParamsFactory.ProgramCardParams> cards() {
    return cards;
  }

  @Override
  public Optional<String> title() {
    return title;
  }

  @Override
  public ProgramCardsSectionParamsFactory.SectionType sectionType() {
    return sectionType;
  }

  @Override
  public Optional<String> id() {
    return id;
  }

  @Override
  public String toString() {
    return "ProgramSectionParams{"
        + "cards=" + cards + ", "
        + "title=" + title + ", "
        + "sectionType=" + sectionType + ", "
        + "id=" + id
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ProgramCardsSectionParamsFactory.ProgramSectionParams) {
      ProgramCardsSectionParamsFactory.ProgramSectionParams that = (ProgramCardsSectionParamsFactory.ProgramSectionParams) o;
      return this.cards.equals(that.cards())
          && this.title.equals(that.title())
          && this.sectionType.equals(that.sectionType())
          && this.id.equals(that.id());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= cards.hashCode();
    h$ *= 1000003;
    h$ ^= title.hashCode();
    h$ *= 1000003;
    h$ ^= sectionType.hashCode();
    h$ *= 1000003;
    h$ ^= id.hashCode();
    return h$;
  }

  @Override
  public ProgramCardsSectionParamsFactory.ProgramSectionParams.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends ProgramCardsSectionParamsFactory.ProgramSectionParams.Builder {
    private ImmutableList<ProgramCardsSectionParamsFactory.ProgramCardParams> cards;
    private Optional<String> title = Optional.empty();
    private ProgramCardsSectionParamsFactory.SectionType sectionType;
    private Optional<String> id = Optional.empty();
    Builder() {
    }
    private Builder(ProgramCardsSectionParamsFactory.ProgramSectionParams source) {
      this.cards = source.cards();
      this.title = source.title();
      this.sectionType = source.sectionType();
      this.id = source.id();
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramSectionParams.Builder setCards(List<ProgramCardsSectionParamsFactory.ProgramCardParams> cards) {
      this.cards = ImmutableList.copyOf(cards);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramSectionParams.Builder setTitle(String title) {
      this.title = Optional.of(title);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramSectionParams.Builder setSectionType(ProgramCardsSectionParamsFactory.SectionType sectionType) {
      if (sectionType == null) {
        throw new NullPointerException("Null sectionType");
      }
      this.sectionType = sectionType;
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramSectionParams.Builder setId(String id) {
      this.id = Optional.of(id);
      return this;
    }
    @Override
    public ProgramCardsSectionParamsFactory.ProgramSectionParams build() {
      if (this.cards == null
          || this.sectionType == null) {
        StringBuilder missing = new StringBuilder();
        if (this.cards == null) {
          missing.append(" cards");
        }
        if (this.sectionType == null) {
          missing.append(" sectionType");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ProgramCardsSectionParamsFactory_ProgramSectionParams(
          this.cards,
          this.title,
          this.sectionType,
          this.id);
    }
  }

}

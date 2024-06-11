package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import play.data.validation.Constraints;
import services.LocalizedStrings;

/**
 * An EBean mapped entity representing a category of programs. For example, "Health" or "Education".
 *
 * <p>A program can have many categories and a category can be assigned to many programs. Each
 * category contains a {@code LocalizedStrings} object with the translations of its name.
 */
@Entity
@Table(name = "categories")
public class CategoryModel extends BaseModel {

  @WhenCreated private Instant createTime;

  @WhenModified private Instant lastModifiedTime;

  @DbJsonB @Constraints.Required private LocalizedStrings localizedName;

  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "categories")
  @JoinTable(
      name = "programs_categories",
      joinColumns = @JoinColumn(name = "categories_id"),
      inverseJoinColumns = @JoinColumn(name = "programs_id"))
  private List<ProgramModel> programs;

  public CategoryModel(ImmutableMap<Locale, String> translations) {
    this.localizedName = LocalizedStrings.create(translations);
    this.lifecycleStage = LifecycleStage.ACTIVE;
  }

  public Long getId() {
    return id;
  }

  public String getDefaultName() {
    return localizedName.getDefault();
  }

  @JsonIgnore
  public LocalizedStrings getLocalizedName() {
    return localizedName;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getLastModifiedTime() {
    return lastModifiedTime;
  }

  @JsonIgnore
  public LifecycleStage getLifecycleStage() {
    return lifecycleStage;
  }

  @JsonIgnore
  public ImmutableList<ProgramModel> getPrograms() {
    return ImmutableList.copyOf(programs);
  }
}

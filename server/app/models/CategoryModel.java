package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import play.data.validation.Constraints;
import services.LocalizedStrings;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "categories")
public class CategoryModel extends BaseModel {

  @WhenCreated private Instant createTime;

  @WhenModified private Instant lastModifiedTime;

  @DbJsonB
  @Constraints.Required
  private LocalizedStrings localizedName;

  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "categories")
  @JoinTable(
    name = "programs_categories",
    joinColumns = @JoinColumn(name = "categories_id"),
    inverseJoinColumns = @JoinColumn(name = "programs_id"))
  private List<ProgramModel> programs;

  public CategoryModel(String defaultName) {
    this.localizedName = LocalizedStrings.withDefaultValue(defaultName);
    this.lifecycleStage = LifecycleStage.ACTIVE;
  }

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
}

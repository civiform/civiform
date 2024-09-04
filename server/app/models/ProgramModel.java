package models;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProgramAcls;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Keep;
import io.ebean.annotation.DbArray;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;

/**
 * An EBean mapped class that stores configuration for a specific benefits program.
 *
 * <p>A program contains a list of {@code BlockDefinition}s, each of which contains {@code
 * ProgramQuestionDefinition}s that reference a given {@code Question}. All questions referenced by
 * a program must be present in every {@code Version} which contains the program.
 *
 * <p>When an application is submitted, an immutable snapshot of the applicant's answers for the
 * program application are stored for that program and applicant in an {@code Application}.
 *
 * <p>Programs that aren't updated between versions are associated with multiple versions. I.e. A
 * program that is not updated will be carried over to a new version.
 */
@Entity
@Table(name = "programs")
public class ProgramModel extends BaseModel {

  private ProgramDefinition programDefinition;

  /** Different versions of the same program are linked by their immutable name. */
  @Constraints.Required private String name;

  @Constraints.Required private String description;

  /** Link to external site for this program. */
  @Constraints.Required private String externalLink;

  /** The program's display mode. */
  @Constraints.Required private String displayMode;

  /** The notification preferences for this program */
  @Constraints.Required
  private @DbArray List<ProgramNotificationPreference> notificationPreferences;

  // Not required - will be autofilled if not present.
  private String slug;

  @DbJsonB private LocalizedStrings localizedName;

  @DbJsonB private ProgramAcls acls;

  @DbJsonB private LocalizedStrings localizedDescription;

  @DbJsonB private LocalizedStrings localizedConfirmationMessage;

  @Constraints.Required @DbJson private ImmutableList<BlockDefinition> blockDefinitions;

  /** When was this program created. */
  @WhenCreated private Instant createTime;

  /**
   * When was this program last modified. Also updates every time any of the joinTable tables are
   * modified, for example a new Version is published.
   */
  @WhenModified private Instant lastModifiedTime;

  @Constraints.Required private ProgramType programType;

  /**
   * If true, eligibility conditions should gate application submission for this program. If false,
   * ineligible applications should still be allowed to be submitted and tags that note that the
   * applicant may not qualify should be hidden.
   */
  @Constraints.Required private Boolean eligibilityIsGating;

  /**
   * A localized description of the summary image (used as alt text).
   *
   * <p>Note: If the program doesn't have a summary image, the field here will be null but the
   * corresponding field in {@link ProgramDefinition} will be {@code Optional.empty}. (Ebean doesn't
   * support optional fields, which is why it's null instead of Optional in this model.) Be sure to
   * convert between null and Optional when going between this model and {@link ProgramDefinition}.
   */
  @DbJsonB @Nullable private LocalizedStrings localizedSummaryImageDescription;

  /**
   * A key used to fetch the program's summary image from cloud storage.
   *
   * <p>Null if the program doesn't have a summary image. See note on {@code
   * localizedSummaryImageDescription} for null vs Optional.
   */
  @Nullable private String summaryImageFileKey;

  @ManyToMany(mappedBy = "programs")
  @JoinTable(
      name = "versions_programs",
      joinColumns = @JoinColumn(name = "programs_id"),
      inverseJoinColumns = @JoinColumn(name = "versions_id"))
  private List<VersionModel> versions;

  @Keep
  @OneToMany(mappedBy = "program")
  @OrderBy("id desc")
  private List<ApplicationModel> applications;

  @ManyToMany(mappedBy = "programs")
  @JoinTable(
      name = "programs_categories",
      joinColumns = @JoinColumn(name = "programs_id"),
      inverseJoinColumns = @JoinColumn(name = "categories_id"))
  private List<CategoryModel> categories;

  public ImmutableList<VersionModel> getVersions() {
    return ImmutableList.copyOf(versions);
  }

  public ImmutableList<CategoryModel> getCategories() {
    return ImmutableList.copyOf(categories);
  }

  /**
   * Gets the program definition from the database.
   *
   * <p>This should never be called directly, but instead called from {@link
   * repository.ProgramRepository#getShallowProgramDefinition}.
   */
  public ProgramDefinition getProgramDefinition() {
    return checkNotNull(this.programDefinition);
  }

  public ProgramModel(ProgramDefinition definition) {
    this(definition, Optional.empty());
  }

  public ProgramModel(ProgramDefinition definition, VersionModel version) {
    this(definition, Optional.of(version));
  }

  private ProgramModel(ProgramDefinition definition, Optional<VersionModel> version) {
    this.programDefinition = definition;
    this.id = definition.id();
    this.name = definition.adminName();
    this.description = definition.adminDescription();
    this.externalLink = definition.externalLink();
    this.localizedName = definition.localizedName();
    this.localizedDescription = definition.localizedDescription();
    this.localizedConfirmationMessage = definition.localizedConfirmationMessage();
    this.blockDefinitions = definition.blockDefinitions();
    this.displayMode = definition.displayMode().getValue();
    this.notificationPreferences = new ArrayList<>(definition.notificationPreferences());
    this.programType = definition.programType();
    this.eligibilityIsGating = definition.eligibilityIsGating();
    this.acls = definition.acls();
    this.categories = definition.categories();
    this.localizedSummaryImageDescription =
        definition.localizedSummaryImageDescription().orElse(null);
    this.summaryImageFileKey = definition.summaryImageFileKey().orElse(null);

    orderBlockDefinitionsBeforeUpdate();

    if (version.isPresent()) {
      this.versions.add(version.get());
    }
  }

  /**
   * Construct a new Program object with the given program name, description, and block definitions.
   * Includes program categories.
   */
  public ProgramModel(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription,
      String defaultConfirmationMessage,
      String externalLink,
      String displayMode,
      ImmutableList<ProgramNotificationPreference> notificationPreferences,
      ImmutableList<BlockDefinition> blockDefinitions,
      VersionModel associatedVersion,
      ProgramType programType,
      boolean eligibilityIsGating,
      ProgramAcls programAcls,
      ImmutableList<CategoryModel> categories) {
    this.name = adminName;
    this.description = adminDescription;
    // A program is always created with the default CiviForm locale first, then localized.
    this.localizedName = LocalizedStrings.withDefaultValue(defaultDisplayName);
    this.localizedDescription = LocalizedStrings.withDefaultValue(defaultDisplayDescription);
    this.localizedConfirmationMessage =
        LocalizedStrings.withDefaultValue(defaultConfirmationMessage);
    this.externalLink = externalLink;
    this.displayMode = displayMode;
    this.notificationPreferences = new ArrayList<>(notificationPreferences);
    this.blockDefinitions = blockDefinitions;
    this.versions.add(associatedVersion);
    this.programType = programType;
    this.eligibilityIsGating = eligibilityIsGating;
    this.acls = programAcls;
    this.categories = categories;
  }

  /** Populates column values from {@link ProgramDefinition} */
  @PreUpdate
  public void persistChangesToProgramDefinition() {
    id = programDefinition.id();
    name = programDefinition.adminName();
    externalLink = programDefinition.externalLink();
    description = programDefinition.adminDescription();
    localizedName = programDefinition.localizedName();
    localizedDescription = programDefinition.localizedDescription();
    localizedConfirmationMessage = programDefinition.localizedConfirmationMessage();
    blockDefinitions = programDefinition.blockDefinitions();
    slug = programDefinition.slug();
    displayMode = programDefinition.displayMode().getValue();
    notificationPreferences = new ArrayList<>(programDefinition.notificationPreferences());
    programType = programDefinition.programType();
    eligibilityIsGating = programDefinition.eligibilityIsGating();
    acls = programDefinition.acls();
    localizedSummaryImageDescription =
        programDefinition.localizedSummaryImageDescription().orElse(null);
    summaryImageFileKey = programDefinition.summaryImageFileKey().orElse(null);
    categories = programDefinition.categories();

    orderBlockDefinitionsBeforeUpdate();
  }

  /** Populates {@link ProgramDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadProgramDefinition() {
    ProgramDefinition.Builder builder =
        ProgramDefinition.builder()
            .setId(id)
            .setAdminName(name)
            .setAdminDescription(description)
            .setBlockDefinitions(blockDefinitions)
            .setLocalizedName(localizedName)
            .setLocalizedDescription(localizedDescription)
            .setExternalLink(externalLink)
            .setDisplayMode(DisplayMode.valueOf(displayMode))
            .setNotificationPreferences(ImmutableList.copyOf(notificationPreferences))
            .setCreateTime(createTime)
            .setLastModifiedTime(lastModifiedTime)
            .setProgramType(programType)
            .setEligibilityIsGating(eligibilityIsGating)
            .setAcls(acls)
            .setCategories(ImmutableList.copyOf(categories));

    setLocalizedConfirmationMessage(builder);
    setLocalizedSummaryImageDescription(builder);
    setSummaryImageFileKey(builder);
    this.programDefinition = builder.build();
  }

  private ProgramModel setLocalizedConfirmationMessage(ProgramDefinition.Builder builder) {
    if (localizedConfirmationMessage != null) {
      builder.setLocalizedConfirmationMessage(localizedConfirmationMessage);
    } else {
      builder.setLocalizedConfirmationMessage(LocalizedStrings.withEmptyDefault());
    }
    return this;
  }

  private void setLocalizedSummaryImageDescription(ProgramDefinition.Builder builder) {
    if (localizedSummaryImageDescription != null) {
      builder.setLocalizedSummaryImageDescription(Optional.of(localizedSummaryImageDescription));
    } else {
      // See docs on `this.localizedSummaryImageDescription` -- a null field here means an
      // Optional.empty field for the program definition.
      builder.setLocalizedSummaryImageDescription(Optional.empty());
    }
  }

  private void setSummaryImageFileKey(ProgramDefinition.Builder builder) {
    if (summaryImageFileKey != null) {
      builder.setSummaryImageFileKey(Optional.of(summaryImageFileKey));
    } else {
      // See docs on `this.summaryImageFileKey` -- a null field here means an
      // Optional.empty field for the program definition.
      builder.setSummaryImageFileKey(Optional.empty());
    }
  }

  public String getSlug() {
    if (Strings.isNullOrEmpty(this.slug)) {
      this.slug = this.programDefinition.slug();
    }
    return this.slug;
  }

  /**
   * See {@link ProgramDefinition#orderBlockDefinitions} for why we need to order blocks.
   *
   * <p>This is used in {@link PreUpdate} but cannot be used when reading from storage because
   * {@link QuestionDefinition}s may not be present in the {@link ProgramDefinition}'s {@link
   * BlockDefinition}'s {@link services.program.ProgramQuestionDefinition}s.
   */
  private void orderBlockDefinitionsBeforeUpdate() {
    try {
      programDefinition = checkNotNull(programDefinition).orderBlockDefinitions();
      blockDefinitions = programDefinition.blockDefinitions();
    } catch (NoSuchElementException e) {
      // We are not able to check block order if the question definitions have not been
      // added to the program question definitions. If we can't check order, we don't
      // really need to make sure they're ordered, so this is a no-op.
    }
  }
}

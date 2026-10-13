package views.admin.settings;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;
import views.components.ToastMessage.ToastType;

/** ViewModel for the CiviForm admin server settings page (Thymeleaf). */
@Data
@Builder
public final class AdminSettingsIndexPageViewModel implements BaseViewModel {

  private final String formActionUrl;

  // Top level settings sections, in the order the legacy page listed them. The
  // side nav and the form body both iterate this list.
  private final ImmutableList<Section> sections;

  // Flash toasts carried over from a save (success or warning). Empty on the
  // error re-render.
  private final ImmutableList<FlashToast> flashToasts;

  // Id of the error toast shown when the last save failed validation, or null
  // when the last render had no errors.
  private final String errorToastId;

  /**
   * Generates a random field id for fields without an explicit id (labels need an id to stay
   * associated with their inputs).
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }

  /** A settings section or subsection. Subsections nest arbitrarily deep. */
  @Data
  @Builder
  public static final class Section {
    private final String name;

    // Slugified section name. Used as the anchor target of the side nav link
    // and the id of the top level section heading; subsections are not linked.
    private final String slug;

    private final ImmutableList<Setting> settings;

    private final ImmutableList<Section> subsections;
  }

  /** A single displayable server setting. */
  @Data
  @Builder
  public static final class Setting {
    private final String variableName;

    // The setting description, rendered from markdown to HTML.
    private final String descriptionHtml;

    // Which input the setting renders as: BOOLEAN, STRING, ENUM or INT. String
    // and list-of-strings settings share the STRING input, as they did in
    // j2html.
    private final String inputTypeName;

    // Current display value. Empty string when unset, and null for an unset INT
    // (which renders no value attribute at all).
    private final String value;

    private final boolean readOnly;

    // BOOLEAN settings only: whether the "True" radio is selected.
    private final boolean checked;

    // ENUM settings only: the values the setting may be set to.
    private final ImmutableList<String> allowableValues;

    // Validation error from the last save attempt, or null. Only STRING and
    // BOOLEAN settings render errors, matching the legacy page.
    private final String errorMessage;
  }

  /** A toast message read out of the flash scope. */
  @Data
  @Builder
  public static final class FlashToast {
    private final String id;

    private final String message;

    private final ToastType type;
  }
}

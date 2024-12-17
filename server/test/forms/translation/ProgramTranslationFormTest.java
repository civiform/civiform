package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http.Request;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.LocalizationUpdate;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;

public class ProgramTranslationFormTest extends ResetPostgres {
  private ApplicationStatusesRepository repo;

  @Before
  public void setup() {
    repo = instanceOf(ApplicationStatusesRepository.class);
  }

  private static final ImmutableMap<String, String> REQUEST_DATA_WITH_TWO_TRANSLATIONS =
      ImmutableMap.<String, String>builder()
          .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
          .put(ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
          .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
          .put(ProgramTranslationForm.statusKeyToUpdateFieldName(0), "first status configured text")
          .put(ProgramTranslationForm.localizedStatusFieldName(0), "first status text")
          .put(ProgramTranslationForm.localizedEmailFieldName(0), "first status email")
          .put(
              ProgramTranslationForm.statusKeyToUpdateFieldName(1), "second status configured text")
          .put(ProgramTranslationForm.localizedStatusFieldName(1), "second status text")
          .put(ProgramTranslationForm.localizedEmailFieldName(1), "second status email")
          .build();

  private static final ImmutableMap<String, String> REQUEST_DATA_WITH_TWO_BLOCKS_TRANSLATED =
      ImmutableMap.<String, String>builder()
          .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
          .put(ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
          .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
          .put(ProgramTranslationForm.localizedScreenName(0), "first block name")
          .put(ProgramTranslationForm.localizedScreenDescription(0), "first block description")
          .put(ProgramTranslationForm.localizedScreenName(1), "second block name")
          .put(ProgramTranslationForm.localizedScreenDescription(1), "second block description")
          .build();

  private static final ImmutableMap<String, String>
      REQUEST_DATA_WITH_TWO_BLOCKS_AND_ELIGIBILITY_MSG_TRANSLATED =
          ImmutableMap.<String, String>builder()
              .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
              .put(ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
              .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
              .put(ProgramTranslationForm.localizedScreenName(0), "first block name")
              .put(ProgramTranslationForm.localizedScreenDescription(0), "first block description")
              .put(
                  ProgramTranslationForm.localizedEligibilityMessage(0),
                  "first eligibility message")
              .put(ProgramTranslationForm.localizedScreenName(1), "second block name")
              .put(ProgramTranslationForm.localizedScreenDescription(1), "second block description")
              .put(
                  ProgramTranslationForm.localizedEligibilityMessage(1),
                  "second eligibility message")
              .build();

  @Test
  public void bindFromRequest() throws Exception {
    Request request = fakeRequestBuilder().bodyForm(REQUEST_DATA_WITH_TWO_TRANSLATIONS).build();

    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 2,
            /* hasSummaryImageDescription= */ false,
            blockIds);
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setLocalizedShortDescription("short description")
                .setLocalizedConfirmationMessage("")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("first status configured text")
                            .setLocalizedStatusText(Optional.of("first status text"))
                            .setLocalizedEmailBody(Optional.of("first status email"))
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("second status configured text")
                            .setLocalizedStatusText(Optional.of("second status text"))
                            .setLocalizedEmailBody(Optional.of("second status email"))
                            .build()))
                .setScreens(ImmutableList.of())
                .build());
  }

  @Test
  public void bindFromRequest_extraStatusesInFormBodyBeyondSpecifiedAreIgnored() throws Exception {
    Request request = fakeRequestBuilder().bodyForm(REQUEST_DATA_WITH_TWO_TRANSLATIONS).build();

    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 1,
            /* hasSummaryImageDescription= */ false,
            blockIds);
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setLocalizedShortDescription("short description")
                .setLocalizedConfirmationMessage("")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("first status configured text")
                            .setLocalizedStatusText(Optional.of("first status text"))
                            .setLocalizedEmailBody(Optional.of("first status email"))
                            .build()))
                .setScreens(ImmutableList.of())
                .build());
  }

  @Test
  public void bindFromRequest_missingStatusesInFormBodyAreOmmitted() throws Exception {
    Request request = fakeRequestBuilder().bodyForm(REQUEST_DATA_WITH_TWO_TRANSLATIONS).build();

    ImmutableList<Long> blockIds = ImmutableList.of();

    // While parsing the form, it's expected for there to be 3 distinct statuses. When there are
    // only 2 statuses provided in the request body, attempting to parse a 3rd should not throw
    // an error and just return a list of 2 updates.
    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 3,
            /* hasSummaryImageDescription= */ false,
            blockIds);
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setLocalizedShortDescription("short description")
                .setLocalizedConfirmationMessage("")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("first status configured text")
                            .setLocalizedStatusText(Optional.of("first status text"))
                            .setLocalizedEmailBody(Optional.of("first status email"))
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("second status configured text")
                            .setLocalizedStatusText(Optional.of("second status text"))
                            .setLocalizedEmailBody(Optional.of("second status email"))
                            .build()))
                .setScreens(ImmutableList.of())
                .build());
  }

  @Test
  public void bindFromRequest_includesScreenNameAndDescription() throws Exception {
    Request request =
        fakeRequestBuilder().bodyForm(REQUEST_DATA_WITH_TWO_BLOCKS_TRANSLATED).build();

    ImmutableList<Long> blockIds = ImmutableList.of(0l, 1l);

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 3,
            /* hasSummaryImageDescription= */ false,
            blockIds);
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setLocalizedShortDescription("short description")
                .setLocalizedConfirmationMessage("")
                .setStatuses(ImmutableList.of())
                .setScreens(
                    ImmutableList.of(
                        LocalizationUpdate.ScreenUpdate.builder()
                            .setBlockIdToUpdate(0l)
                            .setLocalizedName("first block name")
                            .setLocalizedDescription("first block description")
                            .setLocalizedEligibilityMessage("")
                            .build(),
                        LocalizationUpdate.ScreenUpdate.builder()
                            .setBlockIdToUpdate(1l)
                            .setLocalizedName("second block name")
                            .setLocalizedDescription("second block description")
                            .setLocalizedEligibilityMessage("")
                            .build()))
                .build());
  }

  @Test
  public void bindFromRequest_includesScreenNameAndDescriptionAndEligibilityMsg() throws Exception {
    Request request =
        fakeRequestBuilder()
            .bodyForm(REQUEST_DATA_WITH_TWO_BLOCKS_AND_ELIGIBILITY_MSG_TRANSLATED)
            .build();

    ImmutableList<Long> blockIds = ImmutableList.of(0l, 1l);

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 3,
            /* hasSummaryImageDescription= */ false,
            blockIds);
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setLocalizedShortDescription("short description")
                .setLocalizedConfirmationMessage("")
                .setStatuses(ImmutableList.of())
                .setScreens(
                    ImmutableList.of(
                        LocalizationUpdate.ScreenUpdate.builder()
                            .setBlockIdToUpdate(0l)
                            .setLocalizedName("first block name")
                            .setLocalizedDescription("first block description")
                            .setLocalizedEligibilityMessage("first eligibility message")
                            .build(),
                        LocalizationUpdate.ScreenUpdate.builder()
                            .setBlockIdToUpdate(1l)
                            .setLocalizedName("second block name")
                            .setLocalizedDescription("second block description")
                            .setLocalizedEligibilityMessage("second eligibility message")
                            .build()))
                .build());
  }

  @Test
  public void bindFromRequest_emptyStatusOrEmailIsConsideredNotProvided() throws Exception {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
                    .put(ProgramTranslationForm.CUSTOM_CONFIRMATION_MESSAGE_FORM_NAME, "")
                    .put(
                        ProgramTranslationForm.statusKeyToUpdateFieldName(0),
                        "first configured status text")
                    .put(ProgramTranslationForm.localizedStatusFieldName(0), "")
                    .put(ProgramTranslationForm.localizedEmailFieldName(0), "")
                    // Add a second status with a missing email field. This will happen
                    // if no email is configured on the original status.
                    .put(
                        ProgramTranslationForm.statusKeyToUpdateFieldName(1),
                        "second configured status text")
                    .put(ProgramTranslationForm.localizedStatusFieldName(1), "")
                    .build())
            .build();

    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 2,
            /* hasSummaryImageDescription= */ false,
            blockIds);
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setLocalizedShortDescription("short description")
                .setLocalizedConfirmationMessage("")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("first configured status text")
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("second configured status text")
                            .build()))
                .setScreens(ImmutableList.of())
                .build());
  }

  @Test
  public void bindFromRequest_hasSummaryImageDescriptionFalse_notIncluded() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
                    .build())
            .build();
    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 0,
            /* hasSummaryImageDescription= */ false,
            blockIds);

    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().isPresent())
        .isFalse();
  }

  @Test
  public void bindFromRequest_hasSummaryImageDescriptionFalse_formContentDropped() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
                    // When the form sets an image description...
                    .put(
                        ProgramTranslationForm.IMAGE_DESCRIPTION_FORM_NAME,
                        "fake image description")
                    .build())
            .build();

    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 0,
            // ... But the form is set up to not have a description...
            /* hasSummaryImageDescription= */ false,
            blockIds);

    // ... Then the localization update doesn't contain the image description from the form.
    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().isPresent())
        .isFalse();
  }

  @Test
  public void bindFromRequest_hasSummaryImageDescriptionTrue_included() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
                    .put(
                        ProgramTranslationForm.IMAGE_DESCRIPTION_FORM_NAME,
                        "fake image description")
                    .build())
            .build();
    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 0,
            /* hasSummaryImageDescription= */ true,
            blockIds);

    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().isPresent())
        .isTrue();
    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().get())
        .isEqualTo("fake image description");
  }

  @Test
  public void bindFromRequest_missingSummaryImageDescriptionIsOmitted() {
    // When the request doesn't have an image description set...
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
                    .build())
            .build();
    ImmutableList<Long> blockIds = ImmutableList.of();

    // ...even though the form says there is an image description...
    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 0,
            /* hasSummaryImageDescription= */ true,
            blockIds);

    // ... Then the form should still parse successfully.
    assertThat(form.getUpdateData(blockIds).localizedDisplayName()).isEqualTo("display name");
    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().isPresent())
        .isTrue();
    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().get()).isEqualTo("");
  }

  @Test
  public void bindFromRequest_emptyImageDescriptionUsed() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME, "short description")
                    .put(ProgramTranslationForm.IMAGE_DESCRIPTION_FORM_NAME, "")
                    .build())
            .build();
    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(
            request,
            instanceOf(FormFactory.class),
            /* maxStatusTranslations= */ 0,
            /* hasSummaryImageDescription= */ true,
            blockIds);

    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().isPresent())
        .isTrue();
    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().get()).isEqualTo("");
  }

  @Test
  public void fromProgram() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("english-name", "english-description")
            .withLocalizedName(Locale.FRENCH, "french-name")
            .withLocalizedDescription(Locale.FRENCH, "french-description")
            .withLocalizedShortDescription(Locale.FRENCH, "french-short-desc")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "us-image-desc", Locale.FRENCH, "french-image-desc"))
            .build();
    StatusDefinitions statusDefinitions =
        new StatusDefinitions(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("first-status-english")
                    .setLocalizedStatusText(
                        LocalizedStrings.withDefaultValue("first-status-english"))
                    .setLocalizedEmailBodyText(
                        Optional.of(
                            LocalizedStrings.withDefaultValue("first-status-email-english")
                                .updateTranslation(Locale.FRENCH, "first-status-email-french")))
                    .build(),
                StatusDefinitions.Status.builder()
                    .setStatusText("second-status-english")
                    .setLocalizedStatusText(
                        LocalizedStrings.withDefaultValue("second-status-english")
                            .updateTranslation(Locale.FRENCH, "second-status-french"))
                    .build()));
    ProgramDefinition programDef = program.getProgramDefinition();
    repo.createOrUpdateStatusDefinitions(programDef.adminName(), statusDefinitions);
    ImmutableList<Long> blockIds = ImmutableList.of();

    ProgramTranslationForm form =
        ProgramTranslationForm.fromProgram(
            program.getProgramDefinition(),
            Locale.FRENCH,
            instanceOf(FormFactory.class),
            repo.lookupActiveStatusDefinitions(programDef.adminName()));
    assertThat(form.getUpdateData(blockIds))
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("french-name")
                .setLocalizedDisplayDescription("french-description")
                .setLocalizedShortDescription("french-short-desc")
                .setLocalizedConfirmationMessage("")
                .setLocalizedSummaryImageDescription("french-image-desc")
                .setScreens(ImmutableList.of())
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("first-status-english")
                            .setLocalizedEmailBody(Optional.of("first-status-email-french"))
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setStatusKeyToUpdate("second-status-english")
                            .setLocalizedStatusText(Optional.of("second-status-french"))
                            .build()))
                .build());
  }

  @Test
  public void fromProgram_noSummaryImageDescription_fieldNotIncluded() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("english-name", "english-description").build();
    ImmutableList<Long> blockIds = ImmutableList.of();
    StatusDefinitions currentStatusDef =
        repo.lookupActiveStatusDefinitions(program.getProgramDefinition().adminName());
    ProgramTranslationForm form =
        ProgramTranslationForm.fromProgram(
            program.getProgramDefinition(),
            Locale.FRENCH,
            instanceOf(FormFactory.class),
            currentStatusDef);

    assertThat(form.getUpdateData(blockIds).localizedSummaryImageDescription().isPresent())
        .isFalse();
  }
}

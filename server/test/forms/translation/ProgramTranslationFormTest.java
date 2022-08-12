package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Program;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.LocalizationUpdate;
import services.program.StatusDefinitions;
import support.ProgramBuilder;

public class ProgramTranslationFormTest extends ResetPostgres {

  private static final ImmutableMap<String, String> REQUEST_DATA_WITH_TWO_TRANSLATIONS =
      ImmutableMap.<String, String>builder()
          .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
          .put(ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
          .put(ProgramTranslationForm.configuredStatusFieldName(0), "first status configured text")
          .put(ProgramTranslationForm.statusTextFieldName(0), "first status text")
          .put(ProgramTranslationForm.statusEmailFieldName(0), "first status email")
          .put(ProgramTranslationForm.configuredStatusFieldName(1), "second status configured text")
          .put(ProgramTranslationForm.statusTextFieldName(1), "second status text")
          .put(ProgramTranslationForm.statusEmailFieldName(1), "second status email")
          .build();

  @Test
  public void bindFromRequest() throws Exception {
    Request request = fakeRequest().bodyForm(REQUEST_DATA_WITH_TWO_TRANSLATIONS).build();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(request, instanceOf(FormFactory.class), 2);
    assertThat(form.getUpdateData())
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("first status configured text")
                            .setLocalizedStatusText(Optional.of("first status text"))
                            .setLocalizedEmailBody(Optional.of("first status email"))
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("second status configured text")
                            .setLocalizedStatusText(Optional.of("second status text"))
                            .setLocalizedEmailBody(Optional.of("second status email"))
                            .build()))
                .build());
  }

  @Test
  public void bindFromRequest_extraStatusesInFormBodyBeyondSpecifiedAreIgnored() throws Exception {
    Request request = fakeRequest().bodyForm(REQUEST_DATA_WITH_TWO_TRANSLATIONS).build();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(request, instanceOf(FormFactory.class), 1);
    assertThat(form.getUpdateData())
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("first status configured text")
                            .setLocalizedStatusText(Optional.of("first status text"))
                            .setLocalizedEmailBody(Optional.of("first status email"))
                            .build()))
                .build());
  }

  @Test
  public void bindFromRequest_notEnoughStatusesInFormBodyAreOmmitted() throws Exception {
    Request request = fakeRequest().bodyForm(REQUEST_DATA_WITH_TWO_TRANSLATIONS).build();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(request, instanceOf(FormFactory.class), 3);
    assertThat(form.getUpdateData())
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("first status configured text")
                            .setLocalizedStatusText(Optional.of("first status text"))
                            .setLocalizedEmailBody(Optional.of("first status email"))
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("second status configured text")
                            .setLocalizedStatusText(Optional.of("second status text"))
                            .setLocalizedEmailBody(Optional.of("second status email"))
                            .build()))
                .build());
  }

  @Test
  public void bindFromRequest_emptyStatusOrEmailIsConsideredNotProvided() throws Exception {
    Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME, "display name")
                    .put(
                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME, "display description")
                    .put(
                        ProgramTranslationForm.configuredStatusFieldName(0),
                        "first configured status text")
                    .put(ProgramTranslationForm.statusTextFieldName(0), "")
                    .put(ProgramTranslationForm.statusEmailFieldName(0), "")
                    // Add a second status with a missing email field. This will happen
                    // if no email is configured on the original status.
                    .put(
                        ProgramTranslationForm.configuredStatusFieldName(1),
                        "second configured status text")
                    .put(ProgramTranslationForm.statusTextFieldName(1), "")
                    .build())
            .build();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(request, instanceOf(FormFactory.class), 2);
    assertThat(form.getUpdateData())
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("first configured status text")
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("second configured status text")
                            .build()))
                .build());
  }

  @Test
  public void fromProgram() {
    Program program =
        ProgramBuilder.newDraftProgram("english-name", "english-description")
            .withLocalizedName(Locale.FRENCH, "french-name")
            .withLocalizedDescription(Locale.FRENCH, "french-description")
            .withStatusDefinitions(
                new StatusDefinitions(
                    ImmutableList.of(
                        StatusDefinitions.Status.builder()
                            .setStatusText("first-status-english")
                            .setLocalizedStatusText(
                                LocalizedStrings.withDefaultValue("first-status-english"))
                            .setLocalizedEmailBodyText(
                                Optional.of(
                                    LocalizedStrings.withDefaultValue("first-status-email-english")
                                        .updateTranslation(
                                            Locale.FRENCH, "first-status-email-french")))
                            .build(),
                        StatusDefinitions.Status.builder()
                            .setStatusText("second-status-english")
                            .setLocalizedStatusText(
                                LocalizedStrings.withDefaultValue("second-status-english")
                                    .updateTranslation(Locale.FRENCH, "second-status-french"))
                            .build())))
            .build();

    ProgramTranslationForm form =
        ProgramTranslationForm.fromProgram(
            program.getProgramDefinition(), Locale.FRENCH, instanceOf(FormFactory.class));
    assertThat(form.getUpdateData())
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("french-name")
                .setLocalizedDisplayDescription("french-description")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("first-status-english")
                            .setLocalizedEmailBody(Optional.of("first-status-email-french"))
                            .build(),
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("second-status-english")
                            .setLocalizedStatusText(Optional.of("second-status-french"))
                            .build()))
                .build());
  }
}

package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.program.LocalizationUpdate;

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
        ProgramTranslationForm.bindFromRequest(instanceOf(FormFactory.class), request, 2);
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
        ProgramTranslationForm.bindFromRequest(instanceOf(FormFactory.class), request, 1);
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
        ProgramTranslationForm.bindFromRequest(instanceOf(FormFactory.class), request, 3);
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
                        "configured status text")
                    .put(ProgramTranslationForm.statusTextFieldName(0), "")
                    .put(ProgramTranslationForm.statusEmailFieldName(0), "")
                    .build())
            .build();

    ProgramTranslationForm form =
        ProgramTranslationForm.bindFromRequest(instanceOf(FormFactory.class), request, 1);
    assertThat(form.getUpdateData())
        .isEqualTo(
            LocalizationUpdate.builder()
                .setLocalizedDisplayName("display name")
                .setLocalizedDisplayDescription("display description")
                .setStatuses(
                    ImmutableList.of(
                        LocalizationUpdate.StatusUpdate.builder()
                            .setConfiguredStatusText("configured status text")
                            .build()))
                .build());
  }

  @Test
  public void fromProgram() {
    assertThat(true).isFalse();
  }
}

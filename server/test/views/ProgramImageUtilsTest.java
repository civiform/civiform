package views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import j2html.tags.specialized.ImgTag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import support.cloud.FakePublicStorageClient;

public class ProgramImageUtilsTest extends ResetPostgres {
  private final PublicStorageClient publicStorageClient = new FakePublicStorageClient();
  private final SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
  private final ProgramImageUtils programImageUtils =
      new ProgramImageUtils(publicStorageClient, mockSettingsManifest);

  @Before
  public void setUp() {
    when(mockSettingsManifest.getProgramCardImages()).thenReturn(true);
  }

  @Test
  public void createProgramImage_featureNotEnabled_returnsEmpty() {
    when(mockSettingsManifest.getProgramCardImages()).thenReturn(false);

    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-10/myFile.jpg"))
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.getDefault(), /* isWithinProgramCard= */ true);

    assertThat(result).isEmpty();
  }

  @Test
  public void createProgramImage_noImageFileKey_returnsEmpty() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.empty())
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.getDefault(), /* isWithinProgramCard= */ true);

    assertThat(result).isEmpty();
  }

  @Test
  public void createProgramImage_fileKeyIncorrectlyFormatted_returnsEmpty() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.of("incorrectlyFormattedFileKey"))
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.getDefault(), /* isWithinProgramCard= */ true);

    assertThat(result).isEmpty();
  }

  @Test
  public void createProgramImage_hasCorrectFileKey_returnsImg() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-10/myFile.jpg"))
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.getDefault(), /* isWithinProgramCard= */ true);

    assertThat(result).isNotEmpty();
  }

  @Test
  public void createProgramImage_noDescSet_usesProgramNameAsAltText() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-10/myFile.jpg"))
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.getDefault(), /* isWithinProgramCard= */ true);

    assertThat(result).isNotEmpty();
    String renderedImage = result.get().render();
    assertThat(renderedImage).contains("alt=\"Test Program Name\"");
  }

  @Test
  public void createProgramImage_noDescForLocale_usesProgramNameAsAltText() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-10/myFile.jpg"))
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.ENGLISH, "English description"))
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.CHINESE, /* isWithinProgramCard= */ true);

    assertThat(result).isNotEmpty();
    String renderedImage = result.get().render();
    assertThat(renderedImage).contains("alt=\"Test Program Name\"");
  }

  @Test
  public void createProgramImage_hasDescForLocale_usesDescForAltText() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("Test Program Name")
            .setSummaryImageFileKey(Optional.of("program-summary-image/program-10/myFile.jpg"))
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.ENGLISH, "English description", Locale.CHINESE, "Chinese description"))
            .build()
            .getProgramDefinition();

    Optional<ImgTag> result =
        programImageUtils.createProgramImage(
            program, Locale.CHINESE, /* isWithinProgramCard= */ true);

    assertThat(result).isNotEmpty();
    String renderedImage = result.get().render();
    assertThat(renderedImage).contains("alt=\"Chinese description\"");
  }
}

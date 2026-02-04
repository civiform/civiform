package views;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.specialized.ImgTag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import support.ProgramBuilder;
import support.cloud.FakePublicStorageClient;

public class ProgramImageUtilsTest extends ResetPostgres {
  private final PublicStorageClient publicStorageClient = new FakePublicStorageClient();
  private final ProgramImageUtils programImageUtils = new ProgramImageUtils(publicStorageClient);
  private final String englishDescription = "Program description in English";
  private final String chineseDescription = "程序中文說明";
  private final String englishProgramName = "Program Name in English";
  private final String chineseProgramName = "程式的中文名稱";

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

  @Test
  public void getProgramImageAltText_picksTranslation() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram(englishProgramName)
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.ENGLISH, englishDescription, Locale.CHINESE, chineseDescription))
            .build()
            .getProgramDefinition();

    String actual = ProgramImageUtils.getProgramImageAltText(program, Locale.CHINESE);
    assertThat(actual).isEqualTo(chineseDescription);
  }

  @Test
  public void getProgramImageAltText_fallsBackToDefault() {
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram(englishProgramName)
            .withLocalizedName(Locale.CHINESE, chineseProgramName)
            .build()
            .getProgramDefinition();

    // Chinese description is not available, so expect the Chinese program name
    String actual = ProgramImageUtils.getProgramImageAltText(program, Locale.CHINESE);
    assertThat(actual).isEqualTo(chineseProgramName);
  }
}

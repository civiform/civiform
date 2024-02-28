package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ApplicantRequestedActionTest {

  @Test
  public void stripActionFromEndOfUrl_noActionInUrl_notStripped() {
    String input = "https://civiform.dev/programs/4/blocks/1/updateFile/true";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result).isEqualTo(input);
  }

  @Test
  public void stripActionFromEndOfUrl_endsInNextBlock_stripped() {
    String input = "https://civiform.dev/programs/4/blocks/1/updateFile/true/NEXT_BLOCK";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result).isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile/true");
  }

  @Test
  public void stripActionFromEndOfUrl_endsInPreviousBlock_stripped() {
    String input = "https://civiform.dev/programs/4/blocks/1/updateFile/PREVIOUS_BLOCK";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result).isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile");
  }

  @Test
  public void stripActionFromEndOfUrl_endsInReviewPage_stripped() {
    String input = "https://civiform.dev/programs/4/edit/REVIEW_PAGE";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result).isEqualTo("https://civiform.dev/programs/4/edit");
  }

  @Test
  public void stripActionFromEndOfUrl_actionInMiddleNotEnd_notStripped() {
    String input = "https://civiform.dev/programs/4/blocks/1/updateFile/NEXT_BLOCK/true";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result).isEqualTo(input);
  }

  @Test
  public void stripActionFromEndOfUrl_actionWithoutSlash_notStripped() {
    String input = "https://civiform.dev/programs/4/blocks/1/updateFile/trueREVIEW_PAGE";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result).isEqualTo(input);
  }

  @Test
  public void stripActionFromEndOfUrl_hasActionAndQueryParam_onlyActionStripped() {
    String input =
        "https://civiform.dev/programs/4/blocks/1/updateFile/true/REVIEW_PAGE?param=true";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result)
        .isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile/true?param=true");
  }

  @Test
  public void stripActionFromEndOfUrl_hasActionAndAnchorTag_onlyActionStripped() {
    String input =
        "https://civiform.dev/programs/4/blocks/1/updateFile/true/REVIEW_PAGE#section-name";

    String result = ApplicantRequestedAction.stripActionFromEndOfUrl(input);

    assertThat(result)
        .isEqualTo("https://civiform.dev/programs/4/blocks/1/updateFile/true#section-name");
  }
}

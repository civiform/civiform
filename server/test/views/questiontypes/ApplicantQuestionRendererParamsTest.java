package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;

public class ApplicantQuestionRendererParamsTest {

  @Test
  public void sampleParams_isSample() {

    Messages messages = stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
    ApplicantQuestionRendererParams params = ApplicantQuestionRendererParams.sample(messages);

    assertThat(params.isSample()).isTrue();
  }

  @Test
  public void normalParams_isNotSample() {

    Messages messages = stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
    ApplicantQuestionRendererParams params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .build();

    assertThat(params.isSample()).isFalse();
  }
}

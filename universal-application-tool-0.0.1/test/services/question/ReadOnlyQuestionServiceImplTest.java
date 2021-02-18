package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;

public class ReadOnlyQuestionServiceImplTest {
  @Test
  public void emptyServiceAlwaysReturnsInvaild() {
    ReadOnlyQuestionServiceImpl service = new ReadOnlyQuestionServiceImpl(ImmutableList.of());
    assertThat(service.isValid("invalidPath")).isFalse();
  }

  @Test
  public void emptyServiceAlwaysReturnsEmpty() {
    ReadOnlyQuestionServiceImpl service = new ReadOnlyQuestionServiceImpl(ImmutableList.of());
    assertThat(service.getAllQuestions()).isEmpty();
    assertThat(service.getAllScalars()).isEmpty();
  }
}

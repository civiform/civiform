package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Test;

public class YesNoQuestionOptionTest {

  @Test
  public void testBooleanConvention() {
    assertThat(YesNoQuestionOption.YES.getId()).isEqualTo(1L);
    assertThat(YesNoQuestionOption.NO.getId()).isEqualTo(0L);
    assertThat(YesNoQuestionOption.NOT_SURE.getId()).isEqualTo(2L);
    assertThat(YesNoQuestionOption.MAYBE.getId()).isEqualTo(3L);
  }

  @Test
  public void testAdminNames() {
    assertThat(YesNoQuestionOption.YES.getAdminName()).isEqualTo("yes");
    assertThat(YesNoQuestionOption.NO.getAdminName()).isEqualTo("no");
    assertThat(YesNoQuestionOption.NOT_SURE.getAdminName()).isEqualTo("not-sure");
    assertThat(YesNoQuestionOption.MAYBE.getAdminName()).isEqualTo("maybe");
  }

  @Test
  public void testGetAllAdminNames() {
    ImmutableSet<String> allNames = YesNoQuestionOption.getAllAdminNames();

    assertThat(allNames).hasSize(4);
    assertThat(allNames).containsExactlyInAnyOrder("yes", "no", "not-sure", "maybe");
  }

  @Test
  public void testGetRequiredAdminNames() {
    ImmutableSet<String> requiredNames = YesNoQuestionOption.getRequiredAdminNames();

    assertThat(requiredNames).hasSize(2);
    assertThat(requiredNames).containsExactlyInAnyOrder("yes", "no");
  }

  @Test
  public void testFromAdminName_validNames() {
    assertThat(YesNoQuestionOption.fromAdminName("yes")).isEqualTo(YesNoQuestionOption.YES);
    assertThat(YesNoQuestionOption.fromAdminName("no")).isEqualTo(YesNoQuestionOption.NO);
    assertThat(YesNoQuestionOption.fromAdminName("not-sure"))
        .isEqualTo(YesNoQuestionOption.NOT_SURE);
    assertThat(YesNoQuestionOption.fromAdminName("maybe")).isEqualTo(YesNoQuestionOption.MAYBE);
  }

  @Test
  public void testFromAdminName_invalidName_throwsException() {
    assertThatThrownBy(() -> YesNoQuestionOption.fromAdminName("invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown admin name: invalid");
  }

  @Test
  public void testFromId_validIds() {
    assertThat(YesNoQuestionOption.fromId(1)).isEqualTo(YesNoQuestionOption.YES);
    assertThat(YesNoQuestionOption.fromId(0)).isEqualTo(YesNoQuestionOption.NO);
    assertThat(YesNoQuestionOption.fromId(2)).isEqualTo(YesNoQuestionOption.NOT_SURE);
    assertThat(YesNoQuestionOption.fromId(3)).isEqualTo(YesNoQuestionOption.MAYBE);
  }

  @Test
  public void testFromId_invalidId_throwsException() {
    assertThatThrownBy(() -> YesNoQuestionOption.fromId(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown id: -1");
  }

  @Test
  public void testFromBoolean() {
    assertThat(YesNoQuestionOption.fromBoolean(true)).isEqualTo(YesNoQuestionOption.YES);
    assertThat(YesNoQuestionOption.fromBoolean(false)).isEqualTo(YesNoQuestionOption.NO);
  }

  @Test
  public void testToOptionalBoolean() {
    assertThat(YesNoQuestionOption.YES.toOptionalBoolean()).isEqualTo(Optional.of(true));
    assertThat(YesNoQuestionOption.NO.toOptionalBoolean()).isEqualTo(Optional.of(false));
    assertThat(YesNoQuestionOption.NOT_SURE.toOptionalBoolean()).isEqualTo(Optional.empty());
    assertThat(YesNoQuestionOption.MAYBE.toOptionalBoolean()).isEqualTo(Optional.empty());
  }
}

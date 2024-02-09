package controllers.applicant;

import static controllers.applicant.ApplicantRequestedAction.DEFAULT_ACTION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ApplicantRequestedActionWrapperTest {
  @Test
  public void getAction_constructedWithReviewPage_returnsReviewPage() {
    ApplicantRequestedActionWrapper wrapper =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE);

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.REVIEW_PAGE);
  }

  @Test
  public void getAction_constructedWithNextBlock_returnsNextBlock() {
    ApplicantRequestedActionWrapper wrapper =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.NEXT_BLOCK);

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.NEXT_BLOCK);
  }

  @Test
  public void getAction_defaultConstructor_returnsNextBlock() {
    ApplicantRequestedActionWrapper wrapper = new ApplicantRequestedActionWrapper();

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.NEXT_BLOCK);
  }

  @Test
  public void setAction_updatesAction() {
    ApplicantRequestedActionWrapper wrapper = new ApplicantRequestedActionWrapper();

    wrapper.setAction(ApplicantRequestedAction.REVIEW_PAGE);

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.REVIEW_PAGE);

    wrapper.setAction(ApplicantRequestedAction.NEXT_BLOCK);

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.NEXT_BLOCK);
  }

  @Test
  public void bind_nextBlock_actionIsNextBlock() {
    ApplicantRequestedActionWrapper wrapper = new ApplicantRequestedActionWrapper();

    wrapper.bind("key", ApplicantRequestedAction.NEXT_BLOCK.name());

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.NEXT_BLOCK);
  }

  @Test
  public void bind_reviewPage_actionIsReviewPage() {
    ApplicantRequestedActionWrapper wrapper = new ApplicantRequestedActionWrapper();

    wrapper.bind("key", ApplicantRequestedAction.REVIEW_PAGE.name());

    assertThat(wrapper.getAction()).isEqualTo(ApplicantRequestedAction.REVIEW_PAGE);
  }

  @Test
  public void bind_unknownValue_actionResetToDefault() {
    ApplicantRequestedActionWrapper wrapper = new ApplicantRequestedActionWrapper();
    wrapper.setAction(ApplicantRequestedAction.REVIEW_PAGE);

    wrapper.bind("key", "fake-enum-value");

    assertThat(wrapper.getAction()).isEqualTo(DEFAULT_ACTION);
  }

  @Test
  public void unbind_nextBlock_returnsNextBlockName() {
    ApplicantRequestedActionWrapper wrapper =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.NEXT_BLOCK);

    assertThat(wrapper.unbind("key")).isEqualTo(ApplicantRequestedAction.NEXT_BLOCK.name());
    assertThat(wrapper.javascriptUnbind()).isEqualTo(ApplicantRequestedAction.NEXT_BLOCK.name());
  }

  @Test
  public void unbind_reviewPage_returnsReviewPageName() {
    ApplicantRequestedActionWrapper wrapper =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE);

    assertThat(wrapper.unbind("key")).isEqualTo(ApplicantRequestedAction.REVIEW_PAGE.name());
    assertThat(wrapper.javascriptUnbind()).isEqualTo(ApplicantRequestedAction.REVIEW_PAGE.name());
  }

  @Test
  public void unbind_unknownValue_returnsNextBlockName() {
    ApplicantRequestedActionWrapper wrapper =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE);
    wrapper.bind("key", "fake-enum-value");

    assertThat(wrapper.unbind("key")).isEqualTo(DEFAULT_ACTION.name());
    assertThat(wrapper.javascriptUnbind()).isEqualTo(DEFAULT_ACTION.name());
  }
}

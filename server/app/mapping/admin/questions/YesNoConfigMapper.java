package mapping.admin.questions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import forms.questions.MultiOptionQuestionForm;
import services.question.YesNoQuestionOption;
import views.admin.questions.YesNoConfig;
import views.admin.questions.YesNoConfig.YesNoOptionRow;

/** Builds the {@link YesNoConfig} shared by the question new and edit pages. */
public final class YesNoConfigMapper {

  private YesNoConfigMapper() {}

  /**
   * Builds the YES_NO option rows: a new question (no saved options) shows the "Select answer
   * options" label and the default option set, all displayed; an existing question renders its
   * saved options with their displayed state. Required options ("yes"/"no") always render checked.
   */
  public static YesNoConfig buildYesNoConfig(MultiOptionQuestionForm form) {
    Preconditions.checkState(
        form.getOptionIds().size() == form.getOptions().size(),
        "Options and Option indexes need to be the same size.");

    boolean useDefaults = form.getOptions().isEmpty();
    ImmutableList.Builder<YesNoOptionRow> rows = ImmutableList.builder();
    if (useDefaults) {
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.YES.getId(),
              YesNoQuestionOption.YES.getAdminName(),
              "Yes",
              /* checked= */ true));
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.NO.getId(),
              YesNoQuestionOption.NO.getAdminName(),
              "No",
              /* checked= */ true));
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.NOT_SURE.getId(),
              YesNoQuestionOption.NOT_SURE.getAdminName(),
              "Not sure",
              /* checked= */ true));
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.MAYBE.getId(),
              YesNoQuestionOption.MAYBE.getAdminName(),
              "Maybe",
              /* checked= */ true));
    } else {
      for (int i = 0; i < form.getOptions().size(); i++) {
        long optionId = form.getOptionIds().get(i);
        rows.add(
            yesNoOptionRow(
                optionId,
                form.getOptionAdminNames().get(i),
                form.getOptions().get(i),
                form.getDisplayedOptionIds().contains(optionId)));
      }
    }
    return new YesNoConfig(/* showLabel= */ useDefaults, rows.build());
  }

  private static YesNoOptionRow yesNoOptionRow(
      long optionId, String adminName, String optionText, boolean checked) {
    boolean required = YesNoQuestionOption.getRequiredAdminNames().contains(adminName);
    return new YesNoOptionRow(
        String.valueOf(optionId),
        adminName,
        optionText,
        required,
        // Required options always render checked (and disabled).
        required || checked,
        String.format("Admin ID: %s. Option text: %s.", adminName, optionText));
  }
}

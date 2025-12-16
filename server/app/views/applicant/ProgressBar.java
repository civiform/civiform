package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.Block;

// Represents a USWDS progress bar. The last block is followed by the review page.
// So, there are (blockCount + 1) steps.
public final class ProgressBar {
  private final int stepIndex; // Index of the step the user is currently on, 0-indexed
  private final ImmutableList<Block> blocks;
  private final Messages messages;

  /**
   * @param blocks The blocks of the application
   * @param blockIndex The block visible to user on this page, 0-indexed
   * @param messages The localized {@link Messages} for the current applicant
   */
  public ProgressBar(ImmutableList<Block> blocks, int blockIndex, Messages messages) {
    this.blocks = checkNotNull(blocks);
    this.stepIndex = blockIndex;
    this.messages = checkNotNull(messages);
  }

  public int getStepCount() {
    return blocks.size() + 1; // Last block is the summary page
  }

  public String cssClassesForIndex(int index) {
    String classes = "";
    if (index == stepIndex) {
      classes += "usa-step-indicator__segment--current ";
    }

    if (index >= 0
        && index < blocks.size()
        && blocks.get(index).isCompletedInProgramWithoutErrors()) {
      classes += "usa-step-indicator__segment--complete ";
    }

    return classes;
  }

  // Returns a string such as "X of Y".
  // X is the current index.
  // Y is the total number of steps.
  public String getProgressString() {
    String format = messages.at(MessageKey.CONTENT_BLOCK_PROGRESS.getKeyName());
    // User-facing indexes start at 1 (not 0)
    String partiallyFormatted = format.replace("{0}", String.valueOf(stepIndex + 1));
    return partiallyFormatted.replace("{1}", String.valueOf(getStepCount()));
  }

  // Returns a localized step name
  // For all steps except the last, the name is the same as the block name.
  public String getStepName() {
    if (stepIndex >= blocks.size()) {
      return messages.at(MessageKey.HEADING_REVIEW_AND_SUBMIT.getKeyName());
    }

    Locale preferredLocale = messages.lang().toLocale();
    return blocks.get(stepIndex).getLocalizedName(preferredLocale);
  }
}

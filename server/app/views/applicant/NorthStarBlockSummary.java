package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import services.applicant.Block;

// Represents a block on the Application Summary page
public final class NorthStarBlockSummary {
  private final Block block;
  private final String editUrl;
  private ArrayList<NorthStarAnswerData> answerData;

  public NorthStarBlockSummary(Block block, String editUrl) {
    this.block = checkNotNull(block);
    this.editUrl = checkNotNull(editUrl);
    answerData = new ArrayList<NorthStarAnswerData>();
  }

  public Block block() {
    return block;
  }

  public String editUrl() {
    return editUrl;
  }

  public void addAnswerData(NorthStarAnswerData datum) {
    answerData.add(datum);
  }

  public ImmutableList<NorthStarAnswerData> answerData() {
    Collections.sort(answerData);
    return ImmutableList.copyOf(answerData);
  }
}

package services.program;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ProgramDefinition_BlockSlice extends ProgramDefinition.BlockSlice {

  private final int startIndex;

  private final int endIndex;

  AutoValue_ProgramDefinition_BlockSlice(
      int startIndex,
      int endIndex) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  @Override
  int startIndex() {
    return startIndex;
  }

  @Override
  int endIndex() {
    return endIndex;
  }

  @Override
  public String toString() {
    return "BlockSlice{"
        + "startIndex=" + startIndex + ", "
        + "endIndex=" + endIndex
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ProgramDefinition.BlockSlice) {
      ProgramDefinition.BlockSlice that = (ProgramDefinition.BlockSlice) o;
      return this.startIndex == that.startIndex()
          && this.endIndex == that.endIndex();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= startIndex;
    h$ *= 1000003;
    h$ ^= endIndex;
    return h$;
  }

}

package services.export;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.function.Function;
import services.Path;
import services.applicant.question.Question;
import services.export.enums.ColumnType;

/** Represents a data column in a CSV export file. */
@AutoValue
public abstract class Column {
  public abstract String header();

  public abstract Optional<Path> questionPath();

  public abstract Optional<Function<Question, String>> answerExtractor();

  public abstract ColumnType columnType();

  public static Column.Builder builder() {
    return new AutoValue_Column.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setHeader(String header);

    public abstract Builder setQuestionPath(Path questionPath);

    public abstract Builder setAnswerExtractor(Function<Question, String> answerExtractor);

    public abstract Builder setColumnType(ColumnType columnType);

    public abstract Column build();
  }
}

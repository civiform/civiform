package civiform.errorprone;

import com.google.errorprone.VisitorState;
import javax.tools.JavaFileObject;

/** Helpers for checks that should skip generated code. */
final class GeneratedSources {

  private GeneratedSources() {}

  /**
   * Returns true if the file being compiled lives under an sbt {@code target} directory, such as
   * the Java sources Play generates from {@code conf/routes}. Generated code without a
   * {@code @Generated} annotation isn't covered by {@code -XepDisableWarningsInGeneratedCode}.
   */
  static boolean isGenerated(VisitorState state) {
    JavaFileObject sourceFile = state.getPath().getCompilationUnit().getSourceFile();
    return sourceFile != null && sourceFile.toUri().getPath().contains("/target/");
  }
}

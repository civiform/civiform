package civiform.errorprone;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class FinalOrAbstractClassTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FinalOrAbstractClass.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(FinalOrAbstractClass.class, getClass());

  @Test
  public void nonFinalClasses_areFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "// BUG: Diagnostic contains: FinalOrAbstractClass",
            "public class Test {",
            "  // BUG: Diagnostic contains: FinalOrAbstractClass",
            "  public static class Nested {}",
            "  // BUG: Diagnostic contains: FinalOrAbstractClass",
            "  class Inner extends ArrayList<String> implements Runnable {",
            "    public void run() {}",
            "  }",
            "  void method() {",
            "    // BUG: Diagnostic contains: FinalOrAbstractClass",
            "    class Local {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericAndAnnotatedClasses_areFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "/** Javadoc. */",
            "@Deprecated",
            "// BUG: Diagnostic contains: FinalOrAbstractClass",
            "public class Test<T extends Comparable<T>> {}")
        .doTest();
  }

  @Test
  public void explicitSubclassingDecisions_areNotFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public final class Test {",
            "  abstract static class Abstract {}",
            "  static final class Final extends Abstract {}",
            "  abstract static sealed class Sealed permits NonSealed, FinalSealed {}",
            "  static non-sealed class NonSealed extends Sealed {}",
            "  static final class FinalSealed extends Sealed {}",
            "}")
        .doTest();
  }

  @Test
  public void nonClassTypes_areNotFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public final class Test {",
            "  interface Iface {}",
            "  enum Enum { A }",
            "  record Record(int a) {}",
            "  @interface Annotation {}",
            "  Runnable anonymous = new Runnable() {",
            "    @Override public void run() {}",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void persistenceClasses_areNotFlagged() {
    compilationHelper
        .addSourceLines(
            "Entity.java", "package jakarta.persistence;", "public @interface Entity {}")
        .addSourceLines(
            "MappedSuperclass.java",
            "package jakarta.persistence;",
            "public @interface MappedSuperclass {}")
        .addSourceLines(
            "Embeddable.java", "package jakarta.persistence;", "public @interface Embeddable {}")
        .addSourceLines(
            "Test.java",
            "import jakarta.persistence.Embeddable;",
            "import jakarta.persistence.Entity;",
            "import jakarta.persistence.MappedSuperclass;",
            "@MappedSuperclass",
            "public class Test {",
            "  @Entity static class EntityModel extends Test {}",
            "  @Embeddable static class Key {}",
            "}")
        .doTest();
  }

  @Test
  public void sourcesUnderTarget_areNotFlagged() {
    compilationHelper
        .addSourceLines("target/routes/main/controllers/routes.java", "public class routes {}")
        .doTest();
  }

  @Test
  public void suppressWarnings_allowsSubclassing() {
    compilationHelper
        .addSourceLines(
            "Test.java", "@SuppressWarnings(\"FinalOrAbstractClass\")", "public class Test {}")
        .doTest();
  }

  @Test
  public void fix_addsFinalModifier() {
    refactoringHelper
        .addInputLines("Test.java", "public class Test {", "  static class Nested {}", "}")
        .addOutputLines(
            "Test.java", "public final class Test {", "  static final class Nested {}", "}")
        .doTest();
  }
}

package architecture;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Enforces that {@link io.ebean.Database} usage outside the data layer is limited to a small
 * allowlist of safe methods.
 *
 * <p>Allowed scope (may call any method on {@code Database}):
 *
 * <ul>
 *   <li>{@code repository.*} — the data layer
 *   <li>{@code models.*} — entity classes
 *   <li>{@code durablejobs.*} — one-off batch operations (data migrations, backfills,
 *       recomputations) that don't fit a per-entity repository surface
 *   <li>An explicit {@link #ALLOWLIST} of dev/seeding classes
 * </ul>
 *
 * <p>Everywhere else, calls on {@code Database} are restricted to methods in {@link
 * #ALLOWED_DATABASE_METHODS} — currently transaction lifecycle and meta-info only. This is an
 * allowlist of methods (not a denylist of "bad" methods), so any future Ebean API additions are
 * disallowed by default until explicitly added here.
 *
 * <p>Test code is allowed to use {@code Database} freely. The importer reads the list of top-level
 * production packages by listing the directories under {@code server/app/} at test runtime, so new
 * packages are picked up automatically without needing to update this test. A custom {@link
 * ImportOption} also filters out sbt's {@code test-classes} output and third-party JARs as a
 * safeguard.
 */
public class RepositoryAccessTest {

  /**
   * Disable ArchUnit's default behavior of resolving referenced types from the classpath. Without
   * this, importing the project's production classes pulls every transitively-referenced JDK and
   * library class into memory (Guice, Play, Jackson, etc.), which OOMs in this large Play project.
   *
   * <p>With resolution disabled, ArchUnit creates lightweight "stub" {@link JavaClass} objects for
   * referenced types it doesn't directly import — sufficient for our predicate, which checks only
   * the target class's fully-qualified name.
   */
  @BeforeClass
  public static void configureArchUnit() {
    ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
  }

  /**
   * Import option that keeps only this project's production class output. sbt puts production
   * classes under {@code .../target/scala-X.Y/classes/} and tests under {@code
   * .../target/scala-X.Y/test-classes/}; everything else on the runtime classpath is third-party
   * JARs. This option excludes both, leaving just our production code.
   *
   * <p>By filtering on path rather than enumerating package names, this is robust to new top-level
   * packages being added under {@code server/app/}.
   */
  private static final ImportOption ONLY_PROJECT_PRODUCTION_CLASSES =
      (Location location) -> !location.contains("test-classes") && !location.contains(".jar");

  /**
   * Methods on {@link io.ebean.Database} that may be called from outside the allowed packages.
   * Anything not in this set is a violation when called from outside {@code repository.*}, {@code
   * models.*}, {@code durablejobs.*}, or the {@link #ALLOWLIST}.
   *
   * <p>Object-inherited methods ({@code equals}, {@code hashCode}, {@code toString}, etc.) are not
   * affected because their declared owner is {@code java.lang.Object}, not {@code Database}.
   */
  private static final Set<String> ALLOWED_DATABASE_METHODS =
      Set.of(
          // Transaction lifecycle — does not read or write data on its own.
          "beginTransaction",
          // Returns the MetaInfoManager for observability/metrics; not data access.
          "metaInfo");

  /**
   * Fully qualified class names allowed to call restricted Database methods despite living outside
   * {@code repository.*} / {@code models.*} / {@code durablejobs.*}.
   *
   * <p>Each entry MUST have a comment explaining why it is exempt.
   *
   * <p>Note: {@code models.Models} is in {@code models.*} and is therefore already allowed by the
   * package predicate; it is listed here only for documentation clarity.
   */
  private static final Set<String> ALLOWLIST =
      Set.of(
          // Dev-only admin tools, not enabled in production deployments.
          "controllers.dev.DevToolsController",
          // Dev-only seeding entrypoint, runs only in local/dev environments.
          "controllers.dev.seeding.DevDatabaseSeedTask",
          // Startup seeding task that initializes essential data; operates outside any
          // per-entity domain repository.
          "services.seeding.DatabaseSeedTask",
          // Needs raw JDBC access (via Database.dataSource()) to register a PostgreSQL
          // LISTEN/NOTIFY listener — a use case Ebean's high-level API doesn't express.
          "services.settings.SettingsCache");

  @Test
  public void databaseMethodCallsOutsideDataLayerLimitedToAllowedMethods() {
    JavaClasses classes =
        new ClassFileImporter()
            .withImportOption(ONLY_PROJECT_PRODUCTION_CLASSES)
            .importPackages(discoverProductionPackages());

    buildRule().check(classes);
  }

  /**
   * Negative test: import a class that intentionally violates the rule and verify the rule catches
   * it. Without this, a subtle bug in the rule (wrong package predicate, typo in method name, etc.)
   * would cause the main test to silently pass even when it shouldn't.
   */
  @Test
  public void rule_flagsHypotheticalViolator() {
    JavaClasses classes = new ClassFileImporter().importClasses(HypotheticalDatabaseViolator.class);

    assertThatThrownBy(() -> buildRule().check(classes))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("HypotheticalDatabaseViolator")
        .hasMessageContaining("find");
  }

  private static ArchRule buildRule() {
    return noClasses()
        .that(are(outsideAllowedPackages()))
        .and(are(notAllowlisted()))
        .should()
        .callMethodWhere(disallowedDatabaseMethodCall());
  }

  /**
   * Intentionally violates the rule. Used only by {@link #rule_flagsHypotheticalViolator()} to
   * verify the rule has teeth. Lives in package {@code architecture}, which is outside the allowed
   * scope and not in the {@link #ALLOWLIST}, so a call to a restricted {@link io.ebean.Database}
   * method here should be flagged.
   */
  @SuppressWarnings("unused")
  static class HypotheticalDatabaseViolator {
    void doSomethingBad(io.ebean.Database database) {
      database.find(Object.class);
    }
  }

  /**
   * Lists the top-level directories under {@code server/app/} as a dynamic package list, so new
   * top-level packages are picked up without editing this test.
   *
   * <p>Tries {@code server/app} first (cwd = repo root) then {@code app} (cwd = {@code server/},
   * which is sbt's typical forked-Test cwd).
   */
  private static String[] discoverProductionPackages() {
    Path appDir =
        Stream.of(Paths.get("server/app"), Paths.get("app"))
            .filter(Files::isDirectory)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Cannot find production source root; tried server/app and app"));
    try (Stream<Path> entries = Files.list(appDir)) {
      return entries
          .filter(Files::isDirectory)
          .map(p -> p.getFileName().toString())
          // Static assets directory, not a Java package.
          .filter(name -> !name.equals("assets"))
          // Hidden directories, just in case.
          .filter(name -> !name.startsWith("."))
          .toArray(String[]::new);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static DescribedPredicate<JavaClass> outsideAllowedPackages() {
    return new DescribedPredicate<JavaClass>("outside repository.*, models.*, and durablejobs.*") {
      @Override
      public boolean test(JavaClass javaClass) {
        String pkg = javaClass.getPackageName();
        return !(pkg.equals("repository")
            || pkg.startsWith("repository.")
            || pkg.equals("models")
            || pkg.startsWith("models.")
            || pkg.equals("durablejobs")
            || pkg.startsWith("durablejobs."));
      }
    };
  }

  private static DescribedPredicate<JavaClass> notAllowlisted() {
    return new DescribedPredicate<JavaClass>("not in the explicit allowlist") {
      @Override
      public boolean test(JavaClass javaClass) {
        return !ALLOWLIST.contains(javaClass.getFullName());
      }
    };
  }

  private static DescribedPredicate<JavaMethodCall> disallowedDatabaseMethodCall() {
    return new DescribedPredicate<JavaMethodCall>(
        "calls an io.ebean.Database method not in the allowlist " + ALLOWED_DATABASE_METHODS) {
      @Override
      public boolean test(JavaMethodCall call) {
        // We compare on fully-qualified name only (rather than using isAssignableTo) because
        // ArchUnit's classpath resolution is disabled (see configureArchUnit) — type-hierarchy
        // walks would not work against stub JavaClass objects. This codebase consistently
        // declares fields/parameters as the io.ebean.Database interface itself, so the bytecode
        // call site always references io.ebean.Database directly.
        return call.getTargetOwner().getFullName().equals("io.ebean.Database")
            && !ALLOWED_DATABASE_METHODS.contains(call.getName());
      }
    };
  }
}

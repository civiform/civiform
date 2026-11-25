# CiviForm errorprone checks

Custom [errorprone](https://errorprone.info) checks that run when sbt compiles
the server's `app/` code. A violation fails the build like any other compile
error.

## Checks

| Check                  | What it enforces                                                                             |
| ---------------------- | -------------------------------------------------------------------------------------------- |
| `FinalOrAbstractClass` | Classes are `final` or `abstract`. Opt out with `@SuppressWarnings("FinalOrAbstractClass")`. |

## Adding a check

1. Add a class to `src/main/java/civiform/errorprone/` that extends
   `BugChecker`, implements the matcher interface for the tree you care about
   (`ClassTreeMatcher`, `MethodInvocationTreeMatcher`, ...), and is annotated
   with `@BugPattern`. The class name is the check name. `FinalOrAbstractClass`
   is a small example.
2. Register it by adding its fully qualified name to
   `src/main/resources/META-INF/services/com.google.errorprone.bugpatterns.BugChecker`.
3. Add a test in `src/test/java/civiform/errorprone/` using
   `CompilationTestHelper`, and `BugCheckerRefactoringTestHelper` if the check
   suggests a fix.
4. Registered checks run at their `@BugPattern` severity in both `app/` and
   `test/` code. In `server/build.sbt`, list the check in both `customChecks`
   strings so its scope is explicit, e.g. `-Xep:<CheckName>:ERROR` for
   `Compile` and `-Xep:<CheckName>:OFF` for `Test`.

Skip generated code (Play routes, AutoValue) with
`GeneratedSources.isGenerated(state)`.

## Running the tests

From `server/`:

```sh
sbt errorproneChecks/test
```

The root project aggregates this one, so `sbt test` runs these tests too.

import WebAssetsBundler.autoImport.bundleWebAssets
import sbt.internal.io.Source
import play.sbt.PlayImport.PlayKeys.playRunHooks
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.Import.pipelineStages
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.gzip.Import.gzip
import com.typesafe.sbt.digest.Import.digest
import com.github.sbt.jacoco.JacocoPlugin.autoImport._

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean, SbtWeb)
  .settings(
    name := """civiform-server""",
    version := "0.0.1",
    scalaVersion := "2.13.14",
    maintainer := "uat-public-contact@google.com",
    libraryDependencies ++= Seq(
      // Provides in-memory caching via the Play cache interface.
      // More info: https://www.playframework.com/documentation/2.8.x/JavaCache
      caffeine,
      guice,
      javaJdbc,
      javaWs,
      // Collections
      "com.google.guava" % "guava" % "33.2.1-jre",
      "com.google.auto" % "auto-common" % "1.2.2",

      // JSON libraries
      "com.jayway.jsonpath" % "json-path" % "2.9.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.17.1",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.17.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.1",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.17.1",
      "com.google.inject.extensions" % "guice-assistedinject" % "6.0.0",

      // Templating
      "com.j2html" % "j2html" % "1.6.0",
      "org.thymeleaf" % "thymeleaf" % "3.1.2.RELEASE",
      "org.commonmark" % "commonmark" % "0.22.0",
      "org.commonmark" % "commonmark-ext-autolink" % "0.22.0",
      "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20240325.1",

      // Amazon AWS SDK
      "software.amazon.awssdk" % "s3" % "2.25.63",
      "software.amazon.awssdk" % "ses" % "2.25.63",

      // Microsoft Azure SDK
      "com.azure" % "azure-identity" % "1.12.1",
      "com.azure" % "azure-storage-blob" % "12.26.0",

      // Database and database testing libraries
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.h2database" % "h2" % "2.2.224" % Test,

      // Metrics collection and export for Prometheus
      "io.github.jyllands-posten" %% "play-prometheus-filters" % "0.6.1",

      // Parameterized testing
      "pl.pragmatists" % "JUnitParams" % "1.1.1" % Test,

      // Testing libraries
      "org.assertj" % "assertj-core" % "3.26.0" % Test,
      "org.mockito" % "mockito-inline" % "5.2.0",
      "org.assertj" % "assertj-core" % "3.26.0" % Test,
      // EqualsTester
      // https://javadoc.io/doc/com.google.guava/guava-testlib/latest/index.html
      "com.google.guava" % "guava-testlib" % "33.2.1-jre" % Test,

      // To provide an implementation of JAXB-API, which is required by Ebean.
      "jakarta.xml.bind" % "jakarta.xml.bind-api" % "4.0.2",
      "jakarta.activation" % "jakarta.activation-api" % "2.1.3",
      "org.glassfish.jaxb" % "jaxb-runtime" % "4.0.5",

      // Security libraries
      // pac4j core (https://github.com/pac4j/play-pac4j)
      "org.pac4j" %% "play-pac4j" % "11.1.0-PLAY2.8",
      "org.pac4j" % "pac4j-core" % "5.7.5",
      // basic http authentication (for the anonymous client)
      "org.pac4j" % "pac4j-http" % "5.7.5",
      // OIDC authentication
      "org.pac4j" % "pac4j-oidc" % "5.7.5",
      // SAML authentication
      "org.pac4j" % "pac4j-saml" % "5.7.5",

      // Encrypted cookies require encryption.
      "org.apache.shiro" % "shiro-crypto-cipher" % "1.13.0",

      // Autovalue
      "com.google.auto.value" % "auto-value-annotations" % "1.11.0",
      "com.google.auto.value" % "auto-value" % "1.11.0",

      // Errorprone
      "com.google.errorprone" % "error_prone_core" % "2.28.0",

      // Apache libraries for export
      "org.apache.commons" % "commons-csv" % "1.11.0",
      "commons-validator" % "commons-validator" % "1.9.0",

      // pdf library for export
      "com.itextpdf" % "itextpdf" % "5.5.13.3",
      // Phone number formatting and validation dependency
      "com.googlecode.libphonenumber" % "libphonenumber" % "8.13.38",

      // Slugs for deeplinking.
      "com.github.slugify" % "slugify" % "3.0.6",

      // Apache libraries for testing subnets
      "commons-net" % "commons-net" % "3.11.0",

      // Url detector for program descriptions.
      "com.linkedin.urls" % "url-detector" % "0.1.17",

      // Override defaul Play logback version. We need to use logback
      // compatible with sl4j 2.0 because the latter pulled in by pac4j.
      "ch.qos.logback" % "logback-classic" % "1.5.6"
    ),
    javacOptions ++= {
      val defaultCompilerOptions = Seq(
        "-encoding",
        "UTF-8",
        "-parameters",
        "-Xlint:unchecked",
        "-Xlint:deprecation",
        "-XDcompilePolicy=simple",
        "-implicit:class",
        // The compile option below is a hack that preserves generated files. Normally,
        // AutoValue generates .java files, compiles them into .class files, and then deletes
        // the .java files. This option keeps the .java files in the specified directory,
        // which allows an IDE to recognize the symbols.
        "-s",
        generateSourcePath(scalaVersion = scalaVersion.value)
      )

      // Disable errorprone checking if the DISABLE_ERRORPRONE environment variable
      // is set to true
      val errorProneCompilerOptions = Option(System.getenv("DISABLE_ERRORPRONE"))
        .filter(_ != "true")
        .map(_ =>
          Seq(
            // Turn off the AutoValueSubclassLeaked error since the generated
            // code contains it - we can't control that.
            "-Xplugin:ErrorProne -Xep:AutoValueSubclassLeaked:OFF -Xep:CanIgnoreReturnValueSuggester:OFF -XepDisableWarningsInGeneratedCode -Xep:WildcardImport:ERROR -Xep:CatchingUnchecked:ERROR -Xep:ThrowsUncheckedException:ERROR",
            "-Werror"
          )
        )
        .getOrElse(Seq.empty)

      defaultCompilerOptions ++ errorProneCompilerOptions
    },

    // Documented at https://github.com/sbt/zinc/blob/c18637c1b30f8ab7d1f702bb98301689ec75854b/internal/compiler-interface/src/main/contraband/incremental.contra
    // Recompile everything if >30% files have changed, to help avoid infinate
    // incremental compilation.
    // (but still allow some incremental building for speed.)
    incOptions := incOptions.value.withRecompileAllFraction(.3),
    // After 2 transitive steps, do more aggressive invalidation
    // https://github.com/sbt/zinc/issues/911
    incOptions := incOptions.value.withTransitiveStep(2),
    pipelineStages := Seq(bundleWebAssets, digest, gzip), // plugins to use for assets
    // Enable digest for local dev so that files can be served çached improving
    // page speed and also browser tests speed.
    Assets / pipelineStages := Seq(bundleWebAssets, digest, gzip),

    // Make verbose tests
    Test / testOptions := Seq(
      Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-q")
    ),
    // Allow tests to print to stdout when running in forking mode (default)
    Test / outputStrategy := Some(StdoutOutput),
    // Use test config for tests
    Test / javaOptions += "-Dconfig.file=conf/application.test.conf",
    // Play 2.9 started using a dynamically assigned port number. Setting it in the
    // application.test.conf file didn't have any effect so we set it here.
    Test / javaOptions += "-Dtestserver.port=9000",
    // Uncomment the following line to disable JVM forking, which allows attaching a remote
    // debugger (https://stackoverflow.com/a/57396198). This isn't disabled unilaterally
    // since running in non-forked mode causes javaOptions to not be propagated, which
    // causes the configuration override above not to have an effect.
    // Test / fork := false,
    // Turn off scaladoc link warnings
    Compile / doc / scalacOptions += "-no-link-warnings",
    // Turn off scaladoc
    Compile / packageDoc / publishArtifact := false,
    Compile / doc / sources := Seq.empty,

    // Save build artifacts to a cache that isn't shadowed by docker.
    // https://www.scala-sbt.org/1.x/docs/Remote-Caching.html
    // During the build step, we push build artifacts to the "build-cache" dir,
    // which is saved in the image file by the deploy process.
    // On later loads, we pull assets from that cache and incrementally compile,
    // any changes, plus the dynamically generated code (autovalue and routes).
    publish / skip := true,
    Global / pushRemoteCacheTo := Some(
      MavenCache("local-cache", file(baseDirectory.value + "/../build-cache"))
    ),
    Compile / pushRemoteCacheConfiguration := (Compile / pushRemoteCacheConfiguration).value
      .withOverwrite(true),
    Test / pushRemoteCacheConfiguration := (Test / pushRemoteCacheConfiguration).value
      .withOverwrite(true),

    // Load the "remote" cache on startup.
    Global / onLoad := {
      val previous = (Global / onLoad).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      startupTransition compose previous
    }
  )
  .settings(excludeTailwindGeneration: _*)
//jacoco report setting
jacocoReportSettings := JacocoReportSettings()
  .withFormats(JacocoReportFormats.HTML, JacocoReportFormats.XML)

jacocoExcludes := Seq("views*", "*Routes*")
jacocoDirectory := baseDirectory.value / "code-coverage"

// Define a transition to pull the "remote" (really local filesystem) cache on startup.
lazy val startupTransition: State => State = { s: State =>
  "pullRemoteCache" :: s
}

// Ignore the tailwind.sbt generated css file when watching for recompilation.
// Since this file is generated when build.sbt is loaded, it causes the server
// to reload when stopping/starting the server on watch mode.
lazy val excludeTailwindGeneration = Seq(watchSources := {
  val fileToExclude =
    "server/public/stylesheets/tailwind.css"
  val customSourcesFilter = new FileFilter {
    override def accept(f: File): Boolean =
      f.getPath.contains(fileToExclude)
    override def toString = s"CustomSourcesFilter($fileToExclude)"
  }

  watchSources.value.map { source =>
    new Source(
      source.base,
      source.includeFilter,
      source.excludeFilter || customSourcesFilter,
      source.recursive
    )
  }
})

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

resolvers += "Shibboleth" at "https://build.shibboleth.net/nexus/content/groups/public"
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.1",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.17.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.17.1"
)
playRunHooks += TailwindBuilder(baseDirectory.value)
// Reload when the build.sbt file changes.
Global / onChangedBuildSource := ReloadOnSourceChanges
// uncomment to show debug logging.
//logLevel := Level.Debug
//Compile / compile / logLevel := Level.Debug

// Register commands that run server in different modes from sbt shell.
addCommandAlias(
  "runDevServer",
  ";eval System.setProperty(\"config.file\", \"conf/application.dev.conf\");run"
)
addCommandAlias(
  "runBrowserTestsServer",
  ";eval System.setProperty(\"config.file\", \"conf/application.dev-browser-tests.conf\");run"
)

// scalaVersion is formatted as x.y.z, but we only want x.y in our path. This function
// removes the .z component and returns the path to the generated source file directory.
def generateSourcePath(scalaVersion: String): String = {
  val version = scalaVersion.split("\\.").take(2).mkString(".")
  s"target/scala-$version/src_managed/main"
}

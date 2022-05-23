import play.sbt.PlayImport.PlayKeys.playRunHooks
import sbt.internal.io.{Source, WatchState}

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)
  .settings(
    name := """civiform-server""",
    version := "0.0.1",
    scalaVersion := "2.13.8",
    maintainer := "uat-public-contact@google.com",
    libraryDependencies ++= Seq(
      // Provides in-memory caching via the Play cache interface.
      // More info: https://www.playframework.com/documentation/2.8.x/JavaCache
      caffeine,
      guice,
      javaJdbc,
      // JSON libraries
      "com.jayway.jsonpath" % "json-path" % "2.7.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.13.2",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.13.2",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",

      // Templating
      "com.j2html" % "j2html" % "1.4.0",

      // Amazon AWS SDK
      "software.amazon.awssdk" % "aws-sdk-java" % "2.17.185",

      // Microsoft Azure SDK
      "com.azure" % "azure-identity" % "1.5.1",
      "com.azure" % "azure-storage-blob" % "12.14.2",

      // Database and database testing libraries
      "org.postgresql" % "postgresql" % "42.3.5",
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.8.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-api" % "5.8.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-params" % "5.8.2" % Test,
      "com.h2database" % "h2" % "2.1.212" % Test,

      // Parameterized testing
      "pl.pragmatists" % "JUnitParams" % "1.1.1" % Test,

      // Testing libraries
      "org.assertj" % "assertj-core" % "3.14.0" % Test,
      "org.mockito" % "mockito-core" % "4.5.1",
      "org.assertj" % "assertj-core" % "3.22.0" % Test,
      // EqualsTester
      // https://javadoc.io/doc/com.google.guava/guava-testlib/latest/index.html
      "com.google.guava" % "guava-testlib" % "31.1-jre" % Test,

      // To provide an implementation of JAXB-API, which is required by Ebean.
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "javax.activation" % "activation" % "1.1.1",
      "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.6",

      // Security libraries
      // pac4j core (https://github.com/pac4j/play-pac4j)
      "org.pac4j" %% "play-pac4j" % "11.1.0-PLAY2.8",
      "org.pac4j" % "pac4j-core" % "5.4.3",
      // basic http authentication (for the anonymous client)
      "org.pac4j" % "pac4j-http" % "5.4.3",
      // OIDC authentication
      "org.pac4j" % "pac4j-oidc" % "5.4.3",
      // SAML authentication
      "org.pac4j" % "pac4j-saml" % "5.4.3",

      // Encrypted cookies require encryption.
      "org.apache.shiro" % "shiro-crypto-cipher" % "1.9.0",

      // Autovalue
      "com.google.auto.value" % "auto-value-annotations" % "1.9",
      "com.google.auto.value" % "auto-value" % "1.9",
      "com.google.auto.value" % "auto-value-parent" % "1.9" pomOnly(),

      // Errorprone
      "com.google.errorprone" % "error_prone_core" % "2.13.1",

      // Apache libraries for export
      "org.apache.commons" % "commons-csv" % "1.9.0",
      
      //pdf library for export
       "com.itextpdf" % "itextpdf" % "5.5.13.3",

      // Slugs for deeplinking.
      "com.github.slugify" % "slugify" % "2.5",

      // Apache libraries for testing subnets
      "commons-net" % "commons-net" % "3.8.0",

      // Url detector for program descriptions.
      "com.linkedin.urls" % "url-detector" % "0.1.17"
    ),
    javacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-parameters",
      "-Xlint:unchecked",
      "-Xlint:deprecation",
      "-XDcompilePolicy=simple",
      // Turn off the AutoValueSubclassLeaked error since the generated
      // code contains it - we can't control that.
      "-Xplugin:ErrorProne -Xep:AutoValueSubclassLeaked:OFF",
      "-implicit:class",
      "-Werror"
    ),
    // Documented at https://github.com/sbt/zinc/blob/c18637c1b30f8ab7d1f702bb98301689ec75854b/internal/compiler-interface/src/main/contraband/incremental.contra
    // Recompile everything if >10% files have changed
    incOptions := incOptions.value.withRecompileAllFraction(.1),
    // After 2 transitive steps, do more aggressive invalidation
    // https://github.com/sbt/zinc/issues/911
    incOptions := incOptions.value.withTransitiveStep(2),

    // Make verbose tests
    Test / testOptions := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-q")),
    // Use test config for tests
    Test / javaOptions += "-Dconfig.file=conf/application.test.conf",
    // Turn off scaladoc link warnings
    Compile / doc / scalacOptions += "-no-link-warnings",
    // Turn off scaladoc
    Compile / packageDoc / publishArtifact := false,
    Compile / doc / sources := Seq.empty
  )
  .settings(excludeTailwindGeneration: _*)

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
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.2.2",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.13.2",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.13.2"
)
resolveFromWebjarsNodeModulesDir := true
playRunHooks += TailwindBuilder(baseDirectory.value)
// Reload when the build.sbt file changes.
Global / onChangedBuildSource := ReloadOnSourceChanges

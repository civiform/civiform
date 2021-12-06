import play.sbt.PlayImport.PlayKeys.playRunHooks

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)
  .settings(
    name := """universal-application-tool""",
    version := "0.0.1",
    scalaVersion := "2.13.1",
    maintainer := "uat-public-contact@google.com",
    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      // JSON libraries
      "com.jayway.jsonpath" % "json-path" % "2.5.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.10.3",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.10.3",

      // Templating
      "com.j2html" % "j2html" % "1.4.0",

      // Amazon AWS SDK
      "software.amazon.awssdk" % "aws-sdk-java" % "2.15.81",

      // Microsoft Azure SDK
      "com.azure" % "azure-identity" % "1.4.2",
      "com.azure" % "azure-storage-blob" % "12.14.2",

      // Database and database testing libraries
      "org.postgresql" % "postgresql" % "42.2.18",
      "org.testcontainers" % "postgresql" % "1.15.1" % Test,
      "org.testcontainers" % "testcontainers" % "1.15.1" % Test,
      "org.testcontainers" % "junit-jupiter" % "1.15.1" % Test,
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.4.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-api" % "5.4.2" % Test,
      "org.junit.jupiter" % "junit-jupiter-params" % "5.4.2" % Test,
      "com.h2database" % "h2" % "1.4.199" % Test,

      // Parameterized testing
      "pl.pragmatists" % "JUnitParams" % "1.1.0" % Test,

      // Testing libraries
      "org.assertj" % "assertj-core" % "3.14.0" % Test,
      "org.mockito" % "mockito-core" % "3.1.0",

      // EqualsTester
      // https://javadoc.io/doc/com.google.guava/guava-testlib/latest/index.html
      "com.google.guava" % "guava-testlib" % "30.1-jre" % Test,

      // To provide an implementation of JAXB-API, which is required by Ebean.
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "javax.activation" % "activation" % "1.1.1",
      "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.2",

      // Security libraries
      // pac4j core (https://github.com/pac4j/play-pac4j)
      "org.pac4j" %% "play-pac4j" % "11.0.0-PLAY2.8-RC2",
      "org.pac4j" % "pac4j-core" % "5.0.0-RC2",
      // basic http authentication (for the anonymous client)
      "org.pac4j" % "pac4j-http" % "5.0.0-RC2",
      // OIDC authentication
      "org.pac4j" % "pac4j-oidc" % "5.0.0-RC2",
      // Encrypted cookies require encryption.
      "org.apache.shiro" % "shiro-crypto-cipher" % "1.7.1",

      // Autovalue
      "com.google.auto.value" % "auto-value-annotations" % "1.7.4",
      "com.google.auto.value" % "auto-value" % "1.7.4",
      "com.google.auto.value" % "auto-value-parent" % "1.7.4",

      // Errorprone
      "com.google.errorprone" % "error_prone_core" % "2.5.1",

      // Apache libraries for export
      "org.apache.pdfbox" % "pdfbox" % "2.0.22",
      "org.apache.commons" % "commons-csv" % "1.4",

      // Slugs for deeplinking.
      "com.github.slugify" % "slugify" % "2.5",

      // Url detector for program descriptions.
      "com.linkedin.urls" % "url-detector" % "0.1.17"
    ),
    javacOptions ++= Seq(
      "-encoding", "UTF-8",
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
    // Make verbose tests
    testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v")),
    // Use test config for tests
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",
    // Turn off scaladoc link warnings
    scalacOptions in (Compile, doc) += "-no-link-warnings"
  )
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
resolvers += Resolver.bintrayRepo("webjars","maven")
libraryDependencies ++= Seq(
    "org.webjars.npm" % "react" % "15.4.0",
    "org.webjars.npm" % "types__react" % "15.0.34"
)
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.10.5",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.10.5",
)
resolveFromWebjarsNodeModulesDir := true
playRunHooks += TailwindBuilder(baseDirectory.value)

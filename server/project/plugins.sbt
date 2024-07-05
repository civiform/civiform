// Play plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.4")
addSbtPlugin("com.typesafe.play" % "sbt-play-ebean" % "6.2.0")

// Dependency tree plugin. To use, open an sbt shell and run dependencyBrowseTree
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// IDE compatibility plugin
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

// Web Assets plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

// Code Coverage plugin
addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.5.0")

// Formatting plugin
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// This is needed per https://eed3si9n.com/sbt-1.8.0-beta
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

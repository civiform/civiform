// Play plugins
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.12")
addSbtPlugin("org.playframework" % "sbt-play-ebean" % "8.5.0")

// Dependency tree plugin. To use, open an sbt shell and run dependencyBrowseTree
addDependencyTreePlugin

// IDE compatibility plugin
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

// Web Assets plugins
addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.1.0")
addSbtPlugin("com.github.sbt" % "sbt-gzip" % "2.0.0")

// Code Coverage plugin
addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.7.0")

// Formatting plugin
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")

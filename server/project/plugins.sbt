// Play plugins
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.6")
addSbtPlugin("org.playframework" % "sbt-play-ebean" % "8.3.0")

// Dependency tree plugin. To use, open an sbt shell and run dependencyBrowseTree
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// IDE compatibility plugin
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

// Web Assets plugins
addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.0.0")
addSbtPlugin("com.github.sbt" % "sbt-gzip" % "2.0.0")

// Code Coverage plugin
addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.5.0")

// Formatting plugin
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")

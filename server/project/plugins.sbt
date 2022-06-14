// The Play plugin

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.16")
addSbtPlugin("com.typesafe.play" % "sbt-play-ebean" % "6.2.0-RC4")
addSbtPlugin("name.de-vries" % "sbt-typescript" % "2.6.2")

// Dependency tree plugin. To use, open an sbt shell and run dependencyBrowseTree
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// IDE compatibility plugin
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.2")
addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.4.0")

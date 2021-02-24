import scala.sys.process.Process

lazy val webpack = TaskKey[Unit]("run webpack when packaging the application")

def runWebpack(file: File) = {
  Process("./node_modules/.bin/webpack", file) !
}

webpack := {
  if(runWebpack(baseDirectory.value) != 0) throw new Exception("Something went wrong when running webpack.")
}

dist := (dist dependsOn webpack).value

stage := (stage dependsOn webpack).value

test := ((test in Test) dependsOn webpack).value

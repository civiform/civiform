import scala.sys.process.Process

lazy val tailwindCli = TaskKey[Unit]("run webpack when packaging the application")

def runTailwindCli(file: File) = {
  Process("npx tailwindcss-cli@latest build ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css", file) !
}

tailwindCli := {
  if(runTailwindCli(baseDirectory.value) != 0) throw new Exception("Something went wrong when running tailwind.")
}

dist := (dist dependsOn tailwindCli).value

stage := (stage dependsOn tailwindCli).value

test := ((test in Test) dependsOn tailwindCli).value

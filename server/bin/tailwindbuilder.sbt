import scala.sys.process.Process

lazy val tailwindCli =
  TaskKey[Unit]("run tailwindCLI when packaging the application")

def runTailwindCli(file: File) = {
  Process(
    "npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css",
    file
  ) !
}

tailwindCli := {
  if (runTailwindCli(baseDirectory.value) != 0)
    throw new Exception("Something went wrong when running tailwind.")
}

dist := (dist dependsOn tailwindCli).value
stage := (stage dependsOn tailwindCli).value
test := (Test / test dependsOn tailwindCli).value
testOnly := (Test / testOnly dependsOn tailwindCli).evaluated
testQuick := (Test / testQuick dependsOn tailwindCli).evaluated
jacoco := (Test / jacoco dependsOn tailwindCli).value

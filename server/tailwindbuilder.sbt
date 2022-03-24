import scala.sys.process.Process

lazy val tailwindCli = TaskKey[Unit]("run tailwindCLI when packaging the application")
lazy val tailwindCliProd = TaskKey[Unit]("run tailwindCLIProd when packaging the application")

def runTailwindCli(file: File) = {
  Process("npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css", file) !
}

tailwindCli := {
  if(runTailwindCli(baseDirectory.value) != 0) throw new Exception("Something went wrong when running tailwind.")
}

def runTailwindCliProd(file: File) = {
  Process("npx -e TRIM=1 tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css --minify", file, "NODE_ENV" -> "production") !
}

tailwindCliProd := {
  if(runTailwindCliProd(baseDirectory.value) != 0) throw new Exception("Something went wrong when running tailwind.")
}

dist := (dist dependsOn tailwindCliProd).value

stage := (stage dependsOn tailwindCliProd).value

test := (Test / test dependsOn tailwindCli).value

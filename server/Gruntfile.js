module.exports = function (grunt) {
  grunt.initConfig({
    watch: {
      files: ['app/views/**/*.java'],
      tasks: ['default'],
    },
  })

  grunt.loadNpmTasks('grunt-contrib-watch')
  grunt.registerTask(
    'default',
    'Compile tailwind.css from java files',
    function () {
      grunt.log.write('parsing .java files to update tailwind.css')
      grunt.util.spawn(
        {
          cmd: 'npx',
          args: [
            'tailwindcss',
            'build',
            '-i',
            './app/assets/stylesheets/styles.css',
            '-o',
            './public/stylesheets/tailwind.css',
          ],
        },
        function (error, result, code) {
          if (error) {
            console.log(result.stderr)
            throw (
              'failed to compile tailwind.css with exit code: ' +
              code.toString()
            )
          }

          console.log(result.stdout)
        }
      )
    }
  )
}

#! /bin/bash
echo "whoami"
whoami
echo "USER $USER"
npm install
ls -l public
touch public/stylesheets/tailwind.css
ls -l public/stylesheets/
npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css
# sbt "$@"

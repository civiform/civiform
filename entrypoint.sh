#! /bin/bash
echo "whoami"
whoami
echo "USER $USER"
npm install
npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css
# sbt "$@"

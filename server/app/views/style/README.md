# Intro

There are some minor constraints on what a developer can do when working with styles

## The backstory

We use Tailwind for CSS which has a builtin mechanism to trim unused styles out of the final CSS file.

Since we use Java with J2html, Tailwind's builtin mechanism for trimming out unused styles does not work on our setup.

Therefore we are doing that ourselves, trimming out a massive 1.7M CSS file and reducing it to 155K
by parsing our code for calls to styles in style definition files with regex.

The list of all possible tailwind style literals, denoted as reference styles, are in Styles.java and ReferenceClasses.java

## The constraints

- If reference styles are added to any other file they will not be registered unless that file is added
  in the tailwind.config.js

- If not obvious already, _fields in any styles definition files which are assigned with string literals can only have uppercase letters,
  numbers, and underscores._ In other words, they should match the /[0-9A-Z_]+/ regular expression, otherwise they will not
  show up in the final CSS style file without modifying the parse code

- Though it is legal in Java to have a a field declaraion span multiple lines, doing so in Styles.java or ReferencesClasses.java
  will break the ability to parse those specific lines

## Files and locations

The final CSS file is `/universal-application-tool-0.0.1/public/stylesheets/tailwind.css`.

Our regex parsing happens in `/universal-application-tool-0.0.1/tailwind.config.js`

You can refresh the styles by running `./bin/refresh-styles` or restarting the server. This probably needs to happen every time
you make a change to which styles are being called in the code

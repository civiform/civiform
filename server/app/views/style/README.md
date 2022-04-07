# Intro

There are some minor constraints on what a developer can do when working with styles

## The backstory

We use Tailwind for CSS which has a builtin mechanism to trim unused styles out of the final CSS file.

Since we use Java with J2html, Tailwind's builtin mechanism for trimming out unused styles does not work on our setup.

Therefore we are doing that ourselves, trimming out a massive 1.7M CSS file and reducing it to 33K
by parsing our code for calls to styles in style definition files with regex.

The list of all possible tailwind style literals, denoted as reference styles, are in Styles.java and ReferenceClasses.java

## The constraints

- You must pass direct style literal references to `StyleUtils.mediaQueryMethod(args..)` calls. If you pass a variable referencing
  those, it simply wont show up. 

  E.g. `StyleUtils.responsiveLarge(Styles.BG_BLUE_200, Styles.MT_1)` will work. However, with something like 
  `String S = Styles.BG_BLUE_200; StylesUtils.responsiveLarge(S);` the style will not register.

- All styles must be defined in Styles.java or ReferenceClasses.java. If added to any other file they will not be registered 
  without modifying tailwind.config.js

- If not obvious already, _fields denoting style literals in Styles.java and ReferenceClasses.java can only have uppercase letters,
  numbers, and underscores._ In other words, they should match the /[0-9A-Z_]+/ regular expression, otherwise they will not
  show up in the final CSS style file without modifying the parse code

- Though it is legal in Java to have a a field declaraion span multiple lines, doing so in Styles.java or ReferencesClasses.java
  will break the ability to parse those specific lines

## Non-constraints

- It is OK to have the arguments to `StyleUtils` method calls span multiple lines.

## Files and locations

The final CSS file is `/universal-application-tool-0.0.1/public/stylesheets/tailwind.css`.

Our regex parsing happens in `/universal-application-tool-0.0.1/tailwind.config.js`

You can refresh the styles by running `./bin/refresh-styles` or restarting the server. This probably needs to happen every time
you make a change to which styles are being called in the code

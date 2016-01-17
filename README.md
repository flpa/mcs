# MCS - Mixdown Comparison Software
MCS is a tool to help musicians compare different mixdowns of a recording.  

![MCS Sample Screenshot](/../screenshots/docs/screenshots/readme-sample-screen.png?raw=true "MCS Main Window")  

After adding an audio track to MCS, the track is automatically normalized:  
The loudness level is adjusted and the start point of the actual recording is determined.
By using the integrated playback functionality, these normalizations enable objective comparison of two or more audio tracks.  
In addition, tracks are visualized as waveforms to aid visual comparison and ease jumping to a specific section of an audio track.

## System requirements

- Windows, OSX or Linux operating system
- An installation of Java SE 8 Update 60 or newer

## Starting the application
MCS releases contain executable files for Microsoft Windows, Mac OSX and Linux.  
You'll find the latest relase at https://github.com/flpa/mcs/releases .  
After unpacking the release, the executable files are located in the `bin` directory.

### ![](/../screenshots/docs/icons/windows.png?raw=true) Windows
MCS can be started using the executable batch file `mcs.bat` by double-clicking the file in Windows Explorer. 
Alternatively, it can be launched from a command-prompt.

### ![](/../screenshots/docs/icons/mac.png?raw=true) Mac
MCS can be started from the commandline using the executable shell script `mcs`.  
For more convenience, our release page also contains standard OSX DMG files, e.g. `mcs-v0.5.0.dmg`. 
These can be integrated into the system easily.

### ![](/../screenshots/docs/icons/linux.png?raw=true) Linux
MCS can be started from the commandline using the executable shell script `mcs`.  

## Bugs and questions
Bugs and questions are collected as issues in our Github repository at https://github.com/flpa/mcs/issues/ .
Feel free to create new issues for any problems or questions you encounter while using MCS.  

You'll also find a list of known bugs at
https://github.com/flpa/mcs/issues?q=is%3Aopen+is%3Aissue+label%3Abug .

## Development
[![Build Status](https://travis-ci.org/flpa/mcs.svg?branch=develop)](https://travis-ci.org/flpa/mcs)  
Do you want to contribute? Or maybe you just want to browse the source code in the Eclipse Java IDE?  
Here's what you'll need:

- Git
- Gradle
- Eclipse
- Java 8

Thanks to Gradle, setting up the development environment is easy:

- Clone the git repository that you'll find on the project page https://github.com/flpa/mcs.
- Run the command `gradle eclipse` on the commandline. This generates an Eclipse project file.
- Import the generated project into Eclipse.
- That's it! To start the application, run the main class `at.fhtw.mcs.Main`.

## License

Copyright (c) 2015 Florian Patzl, Joshua Hercher, Ralf Rosskopf  
published under the MIT license

This product uses the following software from other open source projects:
- Apache Commons IO 2.4, Copyright (c) The Apache Software Foundation  
  license: The Apache Software License, Version 2.0
- JLayer 1.0.1, Copyright (c) JavaZOOM  
  license: GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1  
  sources: http://www.javazoom.net/javalayer/sources/jlayer1.0.1.zip

Copies of all licenses can be found in this file's directory.  
Icons used in the online version of this file are part of the free Logo's pack collection by Maxim Smirnov:
https://www.iconfinder.com/iconsets/logo-s-icon-pack-collection

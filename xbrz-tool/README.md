# xBRZ Tool

Command-line tool for applying xBRZ on image files.

Packages both of [xbrz-core](../xbrz-core) and [xbrz-awt](../xbrz-awt).  The
tool itself uses [`javax.imageio.ImageIO`](https://docs.oracle.com/javase/8/docs/api/javax/imageio/ImageIO.html)
for reading and writing the files.

## Command-line

    Usage: java -jar xbrz-tool.jar <source> [scaling_factor]

## Making Really Executable Jar

https://skife.org/java/unix/2011/06/20/really_executable_jars.html

### Linux

Create `xbrz` shell script like:

    #!/bin/sh
    exec java -jar $0 "$@"

Then:

    $ cat xbrz-tool.jar >> xbrz
    $ chmod +x xbrz
    $ ./xbrz <source> [scaling_factor]

### Windows

Create a `xbrz.cmd` batch file like:

    @echo off
    java -jar "%~f0" %*
    exit /b

Then:

    > copy /b xbrz.cmd + xbrz-tool.jar xbrz.cmd
    > xbrz <source> [scaling_factor]

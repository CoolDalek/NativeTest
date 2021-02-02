# NativeTest
Test project compiled into binary via ScalaNative or GraalVM Native Image

## Compiling

To compile this project you need:
 - installed GraalVM EE with jdk11 and Native Image
 - installed llvm and clang
 - installed sbt
 - installed ammonite shell (if you want to use compiling script)
 
If you want to compile project using script you need to run following command from project root:
  - `./compile.sc <arg>` where `<arg>` is:
    - `graal` to compile via GraalVM Native Image
    - `native` to compile via ScalaNative
    - `help` to print help

# NativeTest
Project to test compilation into binary via ScalaNative and GraalVM Native Image

## Compiling

To compile this project you need:
 - installed GraalVM EE with jdk11 and Native Image
 - installed llvm and clang
 - installed sbt
 - installed ammonite shell (if you want to use compiling script)
 
If you want to compile the project using a script, you need to run the following command from the project root:
  - `./compile.sc <graal|native|help>`
    - `graal --llvm=<true|false>` - compile via GraalVM Native Image
        - `--llvm` - enable llvm as compiler backend, false by default
    - `native` - compile via ScalaNative
    - `help` - prints help

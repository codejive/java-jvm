# jvm - Java Version Manager

A command line tool that helps you install and manage multiple Java versions on your machine.

## Installation

### JBang

For now the simplest way to install `jvm` is to use [JBang](jbang.dev):

```shell
jbang app install jvm@codejive
```

## Usage

See:

```shell
jvm --help
```

## Development

To build the project simply run:

```shell
./mvnw spotless:apply clean verify
```
 mavern have
### Building Native Executables

The project supports building native executables using GraalVM. This creates a standalone binary with faster startup time and lower memory footprint.

#### Prerequisites

1. Install GraalVM (recommended: GraalVM 22.3 or later)
2. Set `GRAALVM_HOME` or `JAVA_HOME` to point to your GraalVM installation
3. Install the native-image tool:
   ```shell
   gu install native-image
   ```

#### Build Native Executable

To build a native executable, run:

```shell
./mvnw clean package -Pnative
```

The native executable will be created in `target/jvm` (or `target/jvm.exe` on Windows).

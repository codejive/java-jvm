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

### Prerequisites

- **Java 11+** — required to compile and run (`JAVA_HOME` must be set)
- **Maven** — provided via the included wrapper (`mvnw` / `mvnw.cmd`), no separate install needed

### Building

Full build with formatting and tests:

```shell
./mvnw spotless:apply clean verify
```

Quick build, skipping tests:

```shell
./mvnw clean package -DskipTests
```

The build produces two JARs in `target/`:

- `jvm-<version>.jar` — thin JAR (no bundled dependencies)
- `jvm-<version>-cli.jar` — fat JAR, runnable directly

### Running the Built Artifact

```shell
java -jar target/jvm-<version>-cli.jar --help
java -jar target/jvm-<version>-cli.jar --version
java -jar target/jvm-<version>-cli.jar list
```

Or use the assembled launcher from `target/binary/bin/`:

```shell
# Linux/macOS
./target/binary/bin/jvm --help
# Windows
target\binary\bin\jvm.bat --help
```

### Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format (AOSP style).

```shell
# Apply formatting
./mvnw spotless:apply
# Check without modifying
./mvnw spotless:check
```

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

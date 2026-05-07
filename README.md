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

## JDK Resolution Strategy

`jvm` delegates JDK discovery and installation to the [devkitman](https://github.com/jbangdev/jbang-devkitman) library. When you request a Java version (e.g. `jvm install 17`), resolution follows a three-tier cascade:

### Version Syntax

- `17` — interpreted as `17+`, meaning version 17 **or newer** (open-ended)
- `17!` — **exact** major version 17 only
- `temurin-21.0.3` — a provider-specific JDK identifier (matched by ID, not version)

### Tier 1 — Local Discovery

Providers are queried in fixed registration order:

1. `current` — the JDK running the current process
2. `default` — the JDK set as the global default (`~/.jbang/currentjdk` symlink)
3. `javahome` — the JDK pointed to by `JAVA_HOME`
4. `path` — the JDK found on `PATH`
5. `linked` — manually linked JDK installations
6. `jbang` — JBang-managed JDKs (supports install/uninstall)
7. Platform-specific providers (`multihome`, `sdkman`, `scoop`, `brew`, `mise`, `linux`)

The **first provider** that returns a matching installed JDK wins. There is no cross-provider sorting or "pick the best version" logic — provider priority alone determines the result.

Run `jvm list-providers` to see which providers are active on your system.

### Tier 2 — Default Version Upgrade

When no installed JDK satisfies the request and the request is **open-ended below version 21** (the configured default), the system redirects to version 21 instead of downloading the minimum satisfying version. For example, `jvm install 11` (interpreted as `11+`) with no local JDKs available will install JDK 21, not 11.

Exact requests bypass this: `jvm install 11!` will always install exactly version 11.

### Tier 3 — Highest Available Fallback

If the default version itself cannot be resolved (e.g. the download API doesn't offer it), the system aggregates all downloadable JDKs from all installable providers, filters by the original minimum version, and selects the **highest available**.

### Key Behaviors

- **Installed JDKs always take priority** over downloads, regardless of version
- **Open-ended requests get upgraded** to the default version (21) when nothing is installed locally
- **Exact requests are honored literally** — no version upgrading occurs
- **Installation is lazy** — resolution may identify a downloadable JDK without downloading it until explicitly needed
- **The `.jvmrc` file** in the current directory (or home directory) can specify a default version via `java=<version>`, used when no version argument is provided

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

// spotless:off Dependencies for JBang
//SOURCES Jvm.java util/Version.java
// spotless:on

package org.codejive.jvm;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import dev.jbang.devkitman.*;
import dev.jbang.devkitman.jdkproviders.JBangJdkProvider;
import dev.jbang.devkitman.util.OsUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import org.codejive.jvm.util.Version;
import picocli.CommandLine;
import picocli.CommandLine.*;

/** Main class for the jvm command line tool. */
@Command(
        name = "jvm",
        versionProvider = Version.class,
        description = "Command line tool for installing and managing Java versions",
        subcommands = {
            Main.ListInstalled.class,
            Main.ListAvailable.class,
            Main.ListProviders.class,
            Main.Install.class,
            Main.Uninstall.class,
            Main.Default.class,
            Main.Link.class,
            Main.Unlink.class,
            Main.Env.class,
            Main.Run.class
        })
public class Main {

    @Option(
            names = {"-h", "--help"},
            description = "Show this help message and exit.",
            usageHelp = true)
    boolean showHelp;

    @Option(
            names = {"--quiet"},
            description = "We will be quiet, only print when error occurs.")
    boolean quiet;

    abstract static class CmdBase implements Callable<Integer> {
        @Option(
                names = {"-h", "--help"},
                description = "Show this help message and exit.",
                usageHelp = true,
                scope = CommandLine.ScopeType.INHERIT)
        boolean showHelp;

        @Option(
                names = {"--quiet"},
                description = "We will be quiet, only print when error occurs.")
        boolean quiet;
    }

    @Command(
            name = "list",
            aliases = {"l"},
            description = "List the installed Java versions")
    static class ListInstalled extends CmdBase {
        @Override
        public Integer call() {
            JdkManager manager = JdkManager.create();
            manager.getOrInstallJdk("11+");
            List<Jdk> jdks = manager.listInstalledJdks();
            jdks.sort(Comparator.<Jdk>naturalOrder().reversed());

            AsciiTable at = new AsciiTable();
            CWC_LongestLine cwc = new CWC_LongestLine();
            at.getRenderer().setCWC(cwc);
            at.addRule();
            at.addRow("V", "Version", "Id", "Provider", "Home");
            at.addRule();
            for (Jdk jdk : jdks) {
                at.addRow(
                        jdk.majorVersion(),
                        jdk.version(),
                        jdk.id(),
                        jdk.provider().name(),
                        jdk.home().toString());
                at.addRule();
            }
            System.out.println(at.render());

            return 0;
        }
    }

    @Command(
            name = "list-available",
            aliases = {"a"},
            description = "List the Java versions available for installation")
    static class ListAvailable extends CmdBase {
        @Override
        public Integer call() {
            JdkManager manager = JdkManager.create();
            List<Jdk> jdks = manager.listAvailableJdks();
            jdks.sort(Comparator.<Jdk>naturalOrder().reversed());

            AsciiTable at = new AsciiTable();
            CWC_LongestLine cwc = new CWC_LongestLine();
            at.getRenderer().setCWC(cwc);
            at.addRule();
            at.addRow("V", "Version", "Id", "Provider");
            at.addRule();
            for (Jdk jdk : jdks) {
                at.addRow(jdk.majorVersion(), jdk.version(), jdk.id(), jdk.provider().name());
                at.addRule();
            }
            System.out.println(at.render());

            return 0;
        }
    }

    @Command(
            name = "list-providers",
            aliases = {"P"},
            description =
                    "List the available JDK providers that can be used for '--providers' options")
    static class ListProviders extends CmdBase {
        @Override
        public Integer call() {
            Path installPath = JBangJdkProvider.getJBangJdkDir();
            JdkDiscovery.Config cfg = new JdkDiscovery.Config(installPath);
            List<JdkProvider> providers = JdkProviders.instance().all(cfg);

            AsciiTable at = new AsciiTable();
            CWC_LongestLine cwc = new CWC_LongestLine();
            at.getRenderer().setCWC(cwc);
            at.addRule();
            at.addRow("Provider", "Description");
            at.addRule();
            for (JdkProvider prov : providers) {
                at.addRow(prov.name(), prov.description());
                at.addRule();
            }
            System.out.println(at.render());

            return 0;
        }
    }

    @Command(
            name = "install",
            aliases = {"i"},
            description = "Install a Java JDK or JRE")
    static class Install extends CmdBase {

        @Option(
                names = {"-f", "--force"},
                description = "Force installation even when already installed")
        boolean force;

        @Mixin JavaVersionParamMixin javaVersionParamMixin;

        @Override
        public Integer call() {
            String versionOrId = javaVersionParamMixin.getVersionOrId(quiet);
            JdkManager jdkMan = JdkManager.create();
            Jdk jdk = jdkMan.getInstalledJdk(versionOrId, JdkProvider.Predicates.canUpdate);
            if (!force && jdk != null) {
                if (!quiet) {
                    System.err.println("Java version is already installed: " + jdk);
                    System.err.println("Use --force to install anyway");
                }
                return 1;
            }
            if (jdk == null) {
                jdk = jdkMan.getJdk(versionOrId, JdkProvider.Predicates.canUpdate);
                if (jdk == null) {
                    if (!quiet) {
                        System.err.println(
                                "Java version is not available for installation: " + versionOrId);
                    }
                    return 2;
                }
            }
            jdk.install();
            if (!quiet) {
                System.err.println("Successfully installed Java version " + versionOrId);
            }
            return 0;
        }
    }

    @Command(
            name = "uninstall",
            aliases = {"u"},
            description = "Install a Java JDK or JRE")
    static class Uninstall extends CmdBase {
        private String versionOrId;

        @Parameters(paramLabel = "version", description = "Java version to use", arity = "0..1")
        void setJavaVersion(String versionOrId) {
            this.versionOrId = versionOrId != null ? validateVersionOrId(versionOrId) : null;
        }

        @Override
        public Integer call() {
            JdkManager jdkMan = JdkManager.create();
            Jdk jdk = jdkMan.getInstalledJdk(versionOrId, JdkProvider.Predicates.canUpdate);
            if (jdk == null) {
                System.err.println("Java version not installed: " + versionOrId);
                return 1;
            }
            jdkMan.uninstallJdk(jdk);
            if (!quiet) {
                System.err.println("Successfully uninstalled Java version " + versionOrId);
            }
            return 0;
        }
    }

    @Command(
            name = "default",
            aliases = {"d"},
            description = "Manage the default Java version")
    static class Default extends CmdBase {
        @Override
        public Integer call() {
            return 0;
        }
    }

    @Command(
            name = "link",
            aliases = {"L"},
            description = "Link an existing Java installation")
    static class Link extends CmdBase {
        @Override
        public Integer call() {
            return 0;
        }
    }

    @Command(
            name = "unlink",
            aliases = {"U"},
            description = "Unlink a previously linked Java installation")
    static class Unlink extends CmdBase {
        @Override
        public Integer call() {
            return 0;
        }
    }

    @Command(
            name = "env",
            aliases = {"e"},
            description = "Print the required environment variables for a Java installation")
    static class Env extends CmdBase {
        @Mixin JavaVersionParamMixin javaVersionParamMixin;

        @Override
        public Integer call() {
            JdkManager jdkMan = JdkManager.create();
            Jdk jdk = jdkMan.getOrInstallJdk(javaVersionParamMixin.getVersionOrId(quiet));
            return 0;
        }
    }

    @Command(
            name = "run",
            aliases = {"r"},
            description = "Run a command making sure the correct Java version is used")
    static class Run extends CmdBase {

        @Mixin JavaVersionOptionMixin javaVersionOptionMixin;

        @Parameters(arity = "1..*", description = "Command to run")
        public List<String> cmd = new ArrayList<>();

        @Override
        public Integer call() throws IOException {
            if (cmd.isEmpty()) {
                return 0;
            }
            JdkManager jdkMan = JdkManager.create();
            Jdk jdk = jdkMan.getOrInstallJdk(javaVersionOptionMixin.getVersionOrId(quiet));
            if (Paths.get(cmd.get(0)).getNameCount() == 1) {
                Path cmdPath = OsUtils.searchPath(cmd.get(0), jdk.home().resolve("bin").toString());
                if (cmdPath != null) {
                    cmd.set(0, cmdPath.toString());
                }
            }
            ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
            String javaHomeKey = findKey(pb.environment(), "JAVA_HOME");
            pb.environment().put(javaHomeKey, jdk.home().toString());
            String pathKey = findKey(pb.environment(), "PATH");
            String newPath = jdk.home().resolve("bin") + File.pathSeparator + System.getenv("PATH");
            pb.environment().put(pathKey, newPath);
            try {
                return pb.start().waitFor();
            } catch (InterruptedException e) {
                return 254;
            }
        }

        private String findKey(Map<String, String> env, String key) {
            return env.keySet().stream()
                    .filter(k -> k.equalsIgnoreCase(key))
                    .findFirst()
                    .orElse(key);
        }
    }

    static class JavaVersionOptionMixin {
        private String versionOrId;

        @Option(
                names = {"-j", "--java"},
                description = "Java version to use")
        void setJavaVersion(String versionOrId) {
            if (!versionOrId.matches("[!]?\\d+")) {
                throw new IllegalArgumentException(
                        "Invalid version, should be a number optionally preceded by an exclamation mark");
            }
            this.versionOrId = versionOrId;
        }

        String getVersionOrId(boolean quiet) {
            if (versionOrId == null) {
                try {
                    return Main.getVersionOrId(null);
                } catch (IOException e) {
                    if (!quiet) {
                        System.err.println("Error reading .jvmrc file: " + e.getMessage());
                    }
                    return null;
                }
            }
            return versionOrId;
        }
    }

    static class JavaVersionParamMixin {
        private String versionOrId;

        @Parameters(paramLabel = "version", description = "Java version to use", arity = "0..1")
        void setJavaVersion(String versionOrId) {
            this.versionOrId = versionOrId != null ? validateVersionOrId(versionOrId) : null;
        }

        String getVersionOrId(boolean quiet) {
            if (versionOrId == null) {
                try {
                    return Main.getVersionOrId(null);
                } catch (IOException e) {
                    if (!quiet) {
                        System.err.println("Error reading .jvmrc file: " + e.getMessage());
                    }
                    return null;
                }
            }
            return versionOrId;
        }
    }

    private static String getVersionOrId(String versionOrId) throws IOException {
        if (versionOrId == null) {
            Path cfgFile = Paths.get(".jvmrc");
            if (!Files.exists(cfgFile)) {
                cfgFile = Paths.get(System.getProperty("user.home"), ".jvmrc");
            }
            if (Files.exists(cfgFile)) {
                Properties cfg = new Properties();
                try (InputStream is = Files.newInputStream(cfgFile)) {
                    cfg.load(is);
                    versionOrId = cfg.getProperty("java");
                }
            }
        }
        return versionOrId != null ? validateVersionOrId(versionOrId) : null;
    }

    private static String validateVersionOrId(String versionOrId) {
        String[] parts = versionOrId.split(",");
        String v = parts[0];
        if (v.endsWith("+") && isInteger(v.substring(0, v.length() - 1))) {
            // Nothing to do
        } else if (v.startsWith("!") && isInteger(v.substring(1))) {
            parts[0] = v.substring(1);
        } else if (isInteger(v)) {
            parts[0] = v + "+";
        } else if (parts.length == 1 && isId(v)) {
            // Nothing to do
        } else {
            throw new IllegalArgumentException(
                    "Invalid version, should be a number optionally preceded by an exclamation mark");
        }
        versionOrId = String.join(",", parts);
        return versionOrId;
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isId(String s) {
        return s.matches("[a-zA-Z0-9_\\-.]+");
    }

    /**
     * Main entry point for the jvm command line tool.
     *
     * @param args The command line arguments.
     */
    public static void main(String... args) {
        new CommandLine(new Main()).setStopAtPositional(true).execute(args);
    }
}

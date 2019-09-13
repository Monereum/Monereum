package exec;

import com.quorum.tessera.config.Config;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExecArgsBuilder {

    private Config config;

    private Path configFile;

    private Path pidFile;

    private Class mainClass;

    private Path executableJarFile;
    
    private final Map<String, String> argList = new HashMap<>();

    private final List<String> jvmArgList = new ArrayList<>();

    private final List<Path> classpathItems = new ArrayList<>();

    public ExecArgsBuilder withPidFile(Path pidFile) {
        this.pidFile = pidFile;
        return this;
    }

    public ExecArgsBuilder withJvmArg(String jvmArg) {
        this.jvmArgList.add(jvmArg);
        return this;
    }

    public ExecArgsBuilder withConfig(Config config) {
        this.config = config;
        return this;
    }

    public ExecArgsBuilder withConfigFile(Path configFile) {
        this.configFile = configFile;
        return this;
    }

    public ExecArgsBuilder withMainClass(Class mainClass) {
        this.mainClass = mainClass;
        return this;
    }
    
    public ExecArgsBuilder withExecutableJarFile(Path executableJarFile) {
        this.executableJarFile = executableJarFile;
        return this;
    }


    public ExecArgsBuilder withArg(String name) {
        argList.put(name, null);
        return this;
    }

    public ExecArgsBuilder withArg(String name, String value) {
        argList.put(name, value);
        return this;
    }

    public ExecArgsBuilder withClassPathItem(Path classpathItem) {
        this.classpathItems.add(classpathItem);
        return this;
    }

    public List<String> build() {

        List<String> tokens = new ArrayList<>();
        tokens.add("java");

        jvmArgList.forEach(tokens::add);

        if (!classpathItems.isEmpty()) {
            tokens.add("-cp");

            String classpathStr = classpathItems.stream()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator));
            tokens.add(classpathStr);
        }
        
        if(executableJarFile != null) {
            tokens.add("-jar");
            tokens.add(executableJarFile.toAbsolutePath().toString());
        } else {
            tokens.add(mainClass.getName());
        }

        tokens.add("-configfile");
        tokens.add(configFile.toAbsolutePath().toString());

        if (Objects.nonNull(pidFile)) {
            tokens.add("-pidfile");
            tokens.add(pidFile.toAbsolutePath().toString());
        }

        argList.entrySet().forEach(e -> {
            tokens.add(e.getKey());
            if (Objects.nonNull(e.getValue())) {
                tokens.add(e.getValue());
            }
        });

        return tokens;
    }

    public static void main(String[] args) throws Exception {
        List<String> argz = new ExecArgsBuilder()
                .withConfigFile(Paths.get("myconfig.json"))
                .withMainClass(Object.class)
                .withJvmArg("-Dsomething=something")
                .withClassPathItem(Paths.get("/some.jar"))
                .withClassPathItem(Paths.get("/someother.jar"))
                .withArg("-jdbc.autoCreateTables", "true")
                .build();

        System.out.println(String.join(" ", argz));

    }

}

package com.quorum.tessera.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * This is a command that is intended to not be a <main class> command, and gets attached as a subcommand to other CLI
 * adapters.
 */
@CommandLine.Command(name = "some-subcommand")
public class MockSubcommandCliAdapter implements CliAdapter, Callable<CliResult> {

    private static CliType t;

    private static CliResult r;

    private static Exception exceptionToBeThrown;

    @picocli.CommandLine.Unmatched private String[] allParameters = new String[0];

    public static void setType(CliType type) {
        t = type;
    }

    public static void setResult(CliResult result) {
        r = result;
    }

    public static void setExceptionToBeThrown(final Exception exceptionToBeThrown) {
        MockSubcommandCliAdapter.exceptionToBeThrown = exceptionToBeThrown;
    }

    @Override
    public CliType getType() {
        return t;
    }

    @Override
    public CliResult call() throws Exception {
        return this.execute(allParameters);
    }

    @Override
    public CliResult execute(String... args) throws Exception {
        if (exceptionToBeThrown != null) {
            throw exceptionToBeThrown;
        }
        return r;
    }
}

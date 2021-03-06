/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.initialization;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.gradle.CacheUsage;
import org.gradle.CommandLineArgumentException;
import org.gradle.StartParameter;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.ProjectDependenciesBuildInstruction;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.LogLevel;
import org.gradle.execution.BuiltInTasksBuildExecuter;
import org.gradle.util.GUtil;
import org.gradle.util.WrapUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Collection;

/**
 * @author Hans Dockter
 */
public class DefaultCommandLine2StartParameterConverter implements CommandLine2StartParameterConverter {
    public static final String GRADLE_HOME_PROPERTY_KEY = "gradle.home";

    private static final String NO_SEARCH_UPWARDS = "u";
    private static final String PROJECT_DIR = "p";
    private static final String PROJECT_DEPENDENCY_TASK_NAMES = "A";
    private static final String NO_PROJECT_DEPENDENCY_REBUILD = "a";
    private static final String PLUGIN_PROPERTIES_FILE = "l";
    private static final String DEFAULT_IMPORT_FILE = "K";
    private static final String BUILD_FILE = "b";
    public static final String INIT_SCRIPT = "I";
    private static final String SETTINGS_FILE = "c";
    public static final String TASKS = "t";
    private static final String PROPERTIES = "r";
    private static final String DEPENDENCIES = "n";
    public static final String DEBUG = "d";
    public static final String INFO = "i";
    public static final String QUIET = "q";
    public static final String FULL_STACKTRACE = "S";
    public static final String STACKTRACE = "s";
    private static final String SYSTEM_PROP = "D";
    private static final String PROJECT_PROP = "P";
    private static final String NO_DEFAULT_IMPORTS = "no-imports";
    private static final String GRADLE_USER_HOME = "g";
    private static final String EMBEDDED_SCRIPT = "e";
    private static final String VERSION = "v";
    private static final String CACHE = "C";
    private static final String DRY_RUN = "m";
    private static final String NO_OPT = "no-opt";
    private static final String EXCLUDE_TASK = "x";
    private static final String HELP = "h";
    private static final String GUI = "gui";

    OptionParser parser = new OptionParser() {
        {
            acceptsAll(WrapUtil.toList(NO_DEFAULT_IMPORTS), "Disable usage of default imports for build script files.");
            acceptsAll(WrapUtil.toList(NO_SEARCH_UPWARDS, "no-search-upward"), String.format(
                    "Don't search in parent folders for a %s file.", Settings.DEFAULT_SETTINGS_FILE));
            acceptsAll(WrapUtil.toList(CACHE, "cache"),
                    "Specifies how compiled build scripts should be cached. Possible values are: 'rebuild', 'off', 'on'. Default value is 'on'")
                    .withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(VERSION, "version"), "Print version info.");
            acceptsAll(WrapUtil.toList(DEBUG, "debug"), "Log in debug mode (includes normal stacktrace).");
            acceptsAll(WrapUtil.toList(QUIET, "quiet"), "Log errors only.");
            acceptsAll(WrapUtil.toList(DRY_RUN, "dry-run"), "Runs the builds with all task actions disabled.");
            acceptsAll(WrapUtil.toList(INFO, "info"), "Set log level to info.");
            acceptsAll(WrapUtil.toList(STACKTRACE, "stacktrace"),
                    "Print out the stacktrace also for user exceptions (e.g. compile error).");
            acceptsAll(WrapUtil.toList(FULL_STACKTRACE, "full-stacktrace"),
                    "Print out the full (very verbose) stacktrace for any exceptions.");
            acceptsAll(WrapUtil.toList(TASKS, "tasks"), "Show list of all available tasks and their dependencies.").
                    withOptionalArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PROPERTIES, "properties"), "Show list of all available project properties.").
                    withOptionalArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(DEPENDENCIES, "dependencies"), "Show list of all project dependencies.").
                    withOptionalArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(GUI), "Launches a GUI application");
            acceptsAll(WrapUtil.toList(PROJECT_DIR, "project-dir"),
                    "Specifies the start directory for Gradle. Defaults to current directory.").withRequiredArg()
                    .ofType(String.class);
            acceptsAll(WrapUtil.toList(GRADLE_USER_HOME, "gradle-user-home"),
                    "Specifies the gradle user home directory.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PLUGIN_PROPERTIES_FILE, "plugin-properties-file"),
                    "Specifies the plugin.properties file.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(DEFAULT_IMPORT_FILE, "default-import-file"),
                    "Specifies the default import file.").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(INIT_SCRIPT, "init-script"), "Specifies an initialization script.")
                    .withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(SETTINGS_FILE, "settings-file"), "Specifies the settings file.")
                    .withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(BUILD_FILE, "build-file"), "Specifies the build file.").withRequiredArg().ofType(
                    String.class);
            acceptsAll(WrapUtil.toList(SYSTEM_PROP, "system-prop"),
                    "Set system property of the JVM (e.g. -Dmyprop=myvalue).").withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PROJECT_PROP, "project-prop"),
                    "Set project property for the build script (e.g. -Pmyprop=myvalue).").withRequiredArg().ofType(
                    String.class);
            acceptsAll(WrapUtil.toList(EMBEDDED_SCRIPT, "embedded"), "Specify an embedded build script.")
                    .withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(PROJECT_DEPENDENCY_TASK_NAMES, "dep-tasks"),
                    "Specify additional tasks for building project dependencies.").withRequiredArg().ofType(
                    String.class);
            acceptsAll(WrapUtil.toList(NO_PROJECT_DEPENDENCY_REBUILD, "no-rebuild"),
                    "Do not rebuild project dependencies.");
            acceptsAll(WrapUtil.toList(NO_OPT), "Ignore any task optimization.");
            acceptsAll(WrapUtil.toList(EXCLUDE_TASK, "exclude-task"), "Specify a task to be excluded from execution.")
                    .withRequiredArg().ofType(String.class);
            acceptsAll(WrapUtil.toList(HELP, "?", "help"), "Shows this help message");
        }};

    private static BidiMap logLevelMap = new DualHashBidiMap();
    private static BidiMap showStacktraceMap = new DualHashBidiMap();

    //Initialize bi-directional maps so you can convert these back and forth from their command line options to their
    //object representation.

    static {
        logLevelMap.put(QUIET, LogLevel.QUIET);
        logLevelMap.put(INFO, LogLevel.INFO);
        logLevelMap.put(DEBUG, LogLevel.DEBUG);
        logLevelMap.put("", LogLevel.LIFECYCLE);
        //there are also other log levels that gradle doesn't support command-line-wise.

        showStacktraceMap.put(FULL_STACKTRACE, StartParameter.ShowStacktrace.ALWAYS_FULL);
        showStacktraceMap.put(STACKTRACE, StartParameter.ShowStacktrace.ALWAYS);
        //showStacktraceMap.put( , StartParameter.ShowStacktrace.INTERNAL_EXCEPTIONS ); there is no command argument for this. Rather, the lack of an argument means 'default to this'.
    }

    public StartParameter convert(String[] args) {
        StartParameter startParameter = new StartParameter();

        String gradleHome = System.getProperty(GRADLE_HOME_PROPERTY_KEY);
        if (!GUtil.isTrue(gradleHome)) {
            throw new CommandLineArgumentException(String.format(
                    "The %s property is not set. Please set it and try again.", GRADLE_HOME_PROPERTY_KEY));
        }
        startParameter.setGradleHomeDir(new File(gradleHome));

        convert(args, startParameter);
        return startParameter;
    }

    public void convert(String[] args, StartParameter startParameter) {
        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            throw new CommandLineArgumentException(e.getMessage());
        }

        if (options.has(HELP)) {
            startParameter.setShowHelp(true);
            return;
        }

        if (options.has(VERSION)) {
            startParameter.setShowVersion(true);
            return;
        }

        if (options.has(GUI)) {
            startParameter.setLaunchGUI(true);
        }

        if (options.has(NO_DEFAULT_IMPORTS)) {
            startParameter.setDefaultImportsFile(null);
        } else if (options.has(DEFAULT_IMPORT_FILE)) {
            startParameter.setDefaultImportsFile(new File(options.argumentOf(DEFAULT_IMPORT_FILE)));
        }

        if (options.has(SYSTEM_PROP)) {
            List<String> props = options.argumentsOf(SYSTEM_PROP);
            for (String keyValueExpression : props) {
                String[] elements = keyValueExpression.split("=");
                startParameter.getSystemPropertiesArgs().put(elements[0], elements.length == 1 ? "" : elements[1]);
            }
        }

        if (options.has(PROJECT_PROP)) {
            List<String> props = options.argumentsOf(PROJECT_PROP);
            for (String keyValueExpression : props) {
                String[] elements = keyValueExpression.split("=");
                startParameter.getProjectProperties().put(elements[0], elements.length == 1 ? "" : elements[1]);
            }
        }

        if (options.has(NO_SEARCH_UPWARDS)) {
            startParameter.setSearchUpwards(false);
        }

        if (options.has(PROJECT_DIR)) {
            startParameter.setProjectDir(new File(options.argumentOf(PROJECT_DIR)));
        }
        if (options.hasArgument(GRADLE_USER_HOME)) {
            startParameter.setGradleUserHomeDir(new File(options.argumentOf(GRADLE_USER_HOME)));
        }
        if (options.hasArgument(BUILD_FILE)) {
            startParameter.setBuildFile(new File(options.argumentOf(BUILD_FILE)));
        }
        if (options.hasArgument(SETTINGS_FILE)) {
            startParameter.setSettingsFile(new File(options.argumentOf(SETTINGS_FILE)));
        }

        for (String script : (List<String>) options.argumentsOf(INIT_SCRIPT)) {
            startParameter.addInitScript(new File(script));
        }

        if (options.hasArgument(PLUGIN_PROPERTIES_FILE)) {
            startParameter.setPluginPropertiesFile(new File(options.argumentOf(PLUGIN_PROPERTIES_FILE)));
        }

        if (options.has(CACHE)) {
            try {
                startParameter.setCacheUsage(CacheUsage.fromString(options.valueOf(CACHE).toString()));
            } catch (InvalidUserDataException e) {
                throw new CommandLineArgumentException(e.getMessage());
            }
        }

        if (options.has(EMBEDDED_SCRIPT)) {
            if (options.has(BUILD_FILE) || options.has(NO_SEARCH_UPWARDS) || options.has(SETTINGS_FILE)) {
                System.err.println(String.format(
                        "Error: The -%s option can't be used together with the -%s, -%s or -%s options.",
                        EMBEDDED_SCRIPT, BUILD_FILE, SETTINGS_FILE, NO_SEARCH_UPWARDS));
                throw new CommandLineArgumentException(String.format(
                        "Error: The -%s option can't be used together with the -%s, -%s or -%s options.",
                        EMBEDDED_SCRIPT, BUILD_FILE, SETTINGS_FILE, NO_SEARCH_UPWARDS));
            }
            startParameter.useEmbeddedBuildFile(options.argumentOf(EMBEDDED_SCRIPT));
        }

        if (options.has(FULL_STACKTRACE)) {
            if (options.has(STACKTRACE)) {
                throw new CommandLineArgumentException(String.format(
                        "Error: The -%s option can't be used together with the -%s option.", FULL_STACKTRACE,
                        STACKTRACE));
            }
            startParameter.setShowStacktrace(StartParameter.ShowStacktrace.ALWAYS_FULL);
        } else if (options.has(STACKTRACE)) {
            startParameter.setShowStacktrace(StartParameter.ShowStacktrace.ALWAYS);
        }

        if (options.has(TASKS) && options.has(PROPERTIES)) {
            throw new CommandLineArgumentException(String.format(
                    "Error: The -%s and -%s options cannot be used together.", TASKS, PROPERTIES));
        }

        if (options.has(PROJECT_DEPENDENCY_TASK_NAMES) && options.has(NO_PROJECT_DEPENDENCY_REBUILD)) {
            throw new CommandLineArgumentException(String.format(
                    "Error: The -%s and -%s options cannot be used together.", PROJECT_DEPENDENCY_TASK_NAMES,
                    NO_PROJECT_DEPENDENCY_REBUILD));
        } else if (options.has(NO_PROJECT_DEPENDENCY_REBUILD)) {
            startParameter.setProjectDependenciesBuildInstruction(new ProjectDependenciesBuildInstruction(null));
        } else if (options.has(PROJECT_DEPENDENCY_TASK_NAMES)) {
            List<String> normalizedTaskNames = new ArrayList<String>();
            for (Object o : options.valuesOf(PROJECT_DEPENDENCY_TASK_NAMES)) {
                String taskName = (String) o;
                normalizedTaskNames.add(taskName.trim());
            }
            startParameter.setProjectDependenciesBuildInstruction(new ProjectDependenciesBuildInstruction(
                    normalizedTaskNames));
        }

        if (options.has(TASKS)) {
            startParameter.setBuildExecuter(new BuiltInTasksBuildExecuter(
                    BuiltInTasksBuildExecuter.Options.TASKS, options.argumentOf(TASKS)));
        } else if (options.has(PROPERTIES)) {
            startParameter.setBuildExecuter(new BuiltInTasksBuildExecuter(
                    BuiltInTasksBuildExecuter.Options.PROPERTIES, options.argumentOf(PROPERTIES)));
        } else if (options.has(DEPENDENCIES)) {
            startParameter.setBuildExecuter(new BuiltInTasksBuildExecuter(
                    BuiltInTasksBuildExecuter.Options.DEPENDENCIES, options.argumentOf(DEPENDENCIES)));
        } else if (!options.nonOptionArguments().isEmpty()) {
            startParameter.setTaskNames(options.nonOptionArguments());
        }

        if (options.has(DRY_RUN)) {
            startParameter.setDryRun(true);
        }

        if (options.has(NO_OPT)) {
            startParameter.setNoOpt(true);
        }

        if (options.has(EXCLUDE_TASK)) {
            startParameter.setExcludedTaskNames(options.valuesOf(EXCLUDE_TASK));
        }

        startParameter.setLogLevel(getLogLevel(options));
    }

    private BuiltInTasksBuildExecuter createBuildInTasksExecuter(BuiltInTasksBuildExecuter.Options options, String path) {
        return new BuiltInTasksBuildExecuter(options, path);
    }

    public void showHelp(OutputStream out) {
        try {
            parser.printHelpOn(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private LogLevel getLogLevel(OptionSet options) {
        LogLevel logLevel = null;
        if (options.has(QUIET)) {
            logLevel = LogLevel.QUIET;
        }
        if (options.has(INFO)) {
            quitWithErrorIfLogLevelAlreadyDefined(logLevel, INFO);
            logLevel = LogLevel.INFO;
        }
        if (options.has(DEBUG)) {
            quitWithErrorIfLogLevelAlreadyDefined(logLevel, DEBUG);
            logLevel = LogLevel.DEBUG;
        }
        if (logLevel == null) {
            logLevel = LogLevel.LIFECYCLE;
        }
        return logLevel;
    }

    /*
       This returns the log level object represented by the command line argument
       @param  commandLineArgument a single command line argument (with no '-')
       @return the corresponding log level or null if it doesn't match any.
       @author mhunsicker
    */

    public LogLevel getLogLevel(String commandLineArgument) {
        LogLevel logLevel = (LogLevel) logLevelMap.get(commandLineArgument);
        if (logLevel == null) {
            return null;
        }

        return logLevel;
    }

    /*
       This returns the command line argument that represents the specified
       log level.
       @param  logLevel       the log level.
       @return the command line argument or null if this level cannot be
                represented on the command line.
       @author mhunsicker
    */

    public String getLogLevelCommandLine(LogLevel logLevel) {
        String commandLine = (String) logLevelMap.getKey(logLevel);
        if (commandLine == null) {
            return null;
        }

        return commandLine;
    }

    /*
       This returns the log levels that are supported on the command line.
       @return a collection of available log levels
       @author mhunsicker
    */

    public Collection<LogLevel> getLogLevels() {
        return Collections.unmodifiableCollection(logLevelMap.values());
    }

    /*
       This returns the stack trace level object represented by the command
       line argument
       @param  commandLineArgument a single command line argument (with no '-')
       @return the corresponding stack trace level or null if it doesn't match any.
       @author mhunsicker
    */

    public StartParameter.ShowStacktrace getShowStacktrace(String commandLineArgument) {
        StartParameter.ShowStacktrace showStacktrace = (StartParameter.ShowStacktrace) showStacktraceMap.get(
                commandLineArgument);
        if (showStacktrace == null) {
            return null;
        }

        return showStacktrace;
    }

    /*
       This returns the command line argument that represents the specified
       stack trace level.

       @param  showStacktrace the stack trace level.
       @return the command line argument or null if this level cannot be
                represented on the command line.
       @author mhunsicker
    */

    public String getShowStacktraceCommandLine(StartParameter.ShowStacktrace showStacktrace) {
        String commandLine = (String) showStacktraceMap.getKey(showStacktrace);
        if (commandLine == null) {
            return null;
        }

        return commandLine;
    }

    /*
       This returns the ShowStacktrace levels that are supported on the command
       line.
       @return a collection of available ShowStacktrace levels
       @author mhunsicker
    */

    public Collection<StartParameter.ShowStacktrace> getShowStacktrace() {
        return Collections.unmodifiableCollection(showStacktraceMap.values());
    }

    private void quitWithErrorIfLogLevelAlreadyDefined(LogLevel logLevel, String option) {
        if (logLevel != null) {
            System.err.println(String.format(
                    "Error: The log level is already defined by another option. Therefore the option %s is invalid.",
                    option));
            throw new InvalidUserDataException();
        }
    }
}

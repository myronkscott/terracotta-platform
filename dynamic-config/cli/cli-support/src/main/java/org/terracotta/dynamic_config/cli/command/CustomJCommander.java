/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.command;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;
import static java.util.function.Predicate.isEqual;

/**
 * Class containing overridden usage methods from JCommander.
 */
public class CustomJCommander extends JCommander {
  private final CommandRepository commandRepository;
  private final String toolName;
  private boolean deprecatedUsage;

  public CustomJCommander(String toolName, CommandRepository commandRepository, Command command) {
    super(command);
    this.toolName = toolName;
    this.commandRepository = commandRepository;
    setUsageFormatter(new UsageFormatter(this));
    commandRepository.getCommands()
        .stream()
        .filter(isEqual(command).negate())
        .forEach(cmd -> addCommand(Metadata.getName(cmd), cmd));
  }

  public Optional<Command> getAskedCommand() {
    return Optional.ofNullable(getParsedCommand()).map(commandRepository::getCommand);
  }

  public void printUsage() {
    try {
      String askedCommand = getParsedCommand();
      if (askedCommand != null) {
        if (askedCommand.contains("deprecated")) {
          this.deprecatedUsage = true;
        }
        getUsageFormatter().usage(askedCommand);
      } else {
        usage();
      }
    } finally {
      this.deprecatedUsage = false; // reset to false
    }
  }

  public void printAskedCommandUsage(String askedCommand) {
    try {
      if (askedCommand.contains("deprecated")) {
        deprecatedUsage = true;
      }
      getUsageFormatter().usage(askedCommand);
    } finally {
      deprecatedUsage = false;
    }
  }

  @Override
  public Map<String, JCommander> getCommands() {
    // force an ordering of commands by name
    return new TreeMap<>(super.getCommands());
  }

  protected void appendDefinitions(StringBuilder out, String indent) {

  }

  private class UsageFormatter implements IUsageFormatter {
    private final JCommander commander;

    public UsageFormatter(JCommander commander) {
      this.commander = commander;
    }

    @Override
    public void usage(String commandName) {
      StringBuilder sb = new StringBuilder();
      usage(commandName, sb);
      commander.getConsole().println(sb.toString());
    }

    @Override
    public void usage(String commandName, StringBuilder out) {
      usage(commandName, out, "");
    }

    @Override
    public void usage(StringBuilder out) {
      usage(out, "");
    }

    @Override
    public void usage(String commandName, StringBuilder out, String indent) {
      String description = getCommandDescription(commandName);
      JCommander jc = getCommands().get(commandName);
      if (description != null) {
        out.append(indent).append(description).append(lineSeparator());
      }
      appendUsage(commandRepository.getCommand(commandName), out, indent);
      appendOptions(jc, out, indent);
    }

    @Override
    public void usage(StringBuilder out, String indent) {
      Map<String, JCommander> commands = getCommands();
      boolean hasCommands = !commands.isEmpty();
      out.append(indent).append("Usage: ").append(toolName).append(" [options]");
      if (hasCommands) {
        out.append(indent).append(" [command] [command options]");
      }

      out.append(lineSeparator()).append(lineSeparator());
      appendOptions(commander, out, indent);
      appendDefinitions(out, indent);
      appendCommands(out, indent, commands, hasCommands);
    }

    @Override
    public String getCommandDescription(String commandName) {
      return new DefaultUsageFormatter(commander).getCommandDescription(commandName);
    }

    private void appendCommands(StringBuilder out, String indent, Map<String, JCommander> commands, boolean hasCommands) {
      if (hasCommands) {
        out.append(lineSeparator()).append("Commands:").append(lineSeparator()).append(lineSeparator());
        for (Map.Entry<String, JCommander> command : commands.entrySet()) {
          String name = command.getKey();
          if (name.endsWith("-deprecated")) {
            continue;
          }
          Object arg = command.getValue().getObjects().get(0);
          Parameters p = arg.getClass().getAnnotation(Parameters.class);
          if (p == null || !p.hidden()) {
            String description = getCommandDescription(name);
            out.append(lineSeparator());
            out.append(indent).append("    ").append(name).append("      ").append(description).append(lineSeparator()).append(lineSeparator());
            appendUsage(commandRepository.getCommand(name), out, indent + "        ");
            out.append(lineSeparator());

            // Options for this command
            JCommander jc = command.getValue();
            appendOptions(jc, out, "        ");
          }
        }
      }
    }

    private void appendUsage(Command command, StringBuilder out, String indent) {
      out.append(indent).append("Usage: ");
      String usage = deprecatedUsage ? Metadata.getDeprecatedUsage(command) : Metadata.getUsage(command);
      out.append(usage.replace(lineSeparator(), lineSeparator() + "    " + indent)).append(lineSeparator());
    }

    private void appendOptions(JCommander jCommander, StringBuilder out, String indent) {
      // Align the descriptions at the "longestName" column
      List<ParameterDescription> sorted = Lists.newArrayList();

      int colSize = "-security-root-directory".length(); // quick hack to get the largest possible option
      for (ParameterDescription pd : jCommander.getParameters()) {
        if (!pd.getParameter().hidden()) {
          sorted.add(pd);
          int length = pd.getParameterAnnotation().names()[0].length();
          if (length > colSize) {
            colSize = length;
          }
        }
      }

      // Display all the names and descriptions
      if (sorted.size() > 0) {

        // Sort the options by only considering the first displayed option
        // which will be the one with 1 dash (i.e. -help).
        sorted.sort(Comparator.comparing(pd -> pd.getParameterAnnotation().names()[0]));

        out.append(indent).append("Options:").append(lineSeparator());

        for (ParameterDescription pd : sorted) {
          WrappedParameter parameter = pd.getParameter();
          out.append(indent)
              .append("    ")
              .append(pad(pd.getParameterAnnotation().names()[0], colSize))
              .append(parameter.required() ? " (required)    " : " (optional)    ")
              .append(pd.getDescription())
              .append(lineSeparator());
        }
      }
    }
  }

  @SuppressFBWarnings("SBSC_USE_STRINGBUFFER_CONCATENATION")
  @SuppressWarnings("StringConcatenationInLoop")
  private String pad(String str, int colSize) {
    while (str.length() < colSize) {
      str += " ";
    }
    return str;
  }
}

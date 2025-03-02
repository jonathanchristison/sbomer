/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.cli;

import javax.inject.Inject;

import org.jboss.sbomer.cli.commands.generator.GenerateCommand;
import org.jboss.sbomer.cli.commands.processor.ProcessCommand;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@QuarkusMain
@CommandLine.Command(
        name = "sbomer",
        mixinStandardHelpOptions = true,
        subcommands = { GenerateCommand.class, ProcessCommand.class })
public class CLI implements QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    @Getter
    @Option(names = { "-v", "--verbose" }, scope = ScopeType.INHERIT)
    boolean verbose = false;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).setExecutionExceptionHandler(new ExceptionHandler()).execute(args);
    }

}

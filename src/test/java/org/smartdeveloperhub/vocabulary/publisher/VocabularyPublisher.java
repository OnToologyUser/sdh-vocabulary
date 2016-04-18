/**
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   This file is part of the Smart Developer Hub Project:
 *     http://www.smartdeveloperhub.org/
 *
 *   Center for Open Middleware
 *     http://www.centeropenmiddleware.com/
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Copyright (C) 2015-2016 Center for Open Middleware.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Artifact    : org.smartdeveloperhub.vocabulary:sdh-vocabulary:0.3.0-SNAPSHOT
 *   Bundle      : sdh-vocabulary-0.3.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.smartdeveloperhub.vocabulary.publisher;

import static io.undertow.Handlers.path;
import static org.smartdeveloperhub.vocabulary.publisher.handlers.MoreHandlers.allow;
import static org.smartdeveloperhub.vocabulary.publisher.handlers.MoreHandlers.catalogResolver;
import static org.smartdeveloperhub.vocabulary.publisher.handlers.MoreHandlers.contentNegotiation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import org.smartdeveloperhub.vocabulary.util.AppAssembler;
import org.smartdeveloperhub.vocabulary.util.Application;
import org.smartdeveloperhub.vocabulary.util.Catalog;
import org.smartdeveloperhub.vocabulary.util.Catalogs;
import org.smartdeveloperhub.vocabulary.util.Module;
import org.smartdeveloperhub.vocabulary.util.Result;

import com.google.common.collect.Lists;

import io.undertow.Undertow;
import io.undertow.util.Methods;

public class VocabularyPublisher {

	public static void main(final String... args) throws FileNotFoundException, IOException {
		if(args.length!=2) {
			System.err.printf("Invalid argument number: 2 arguments required (%d)%n", args.length);
			System.err.printf("USAGE: %s <path-to-ontologies> <base-namespace>%n",AppAssembler.applicationName(VocabularyPublisher.class));
			System.err.printf(" <path-to-ontologies> : Path where the ontologies to publish are available.%n");
			System.err.printf(" <base-namespace>     : Base namespace of the ontologies to publish.%n");
			Application.logContext(args);
			System.exit(-1);
		}
		try {
			final Path root = Paths.get(args[0]);
			final URI base = new URI(args[1]);
			final Result<Catalog> result = Catalogs.loadFrom(root, base);
			if(result.isAvailable()) {
				final Catalog catalog = result.get();
				showCatalog(catalog);
				try {
					publish(catalog);
				} finally {
					System.out.println("Publisher terminated.");
				}
			} else {
				System.err.println("Could not prepare catalog:\n"+result);
				System.exit(-5);
			}
		} catch (final InvalidPathException e) {
			System.err.printf("%s is not a valid root path (%s)%n", args[0],e.getMessage());
			System.exit(-2);
		} catch (final URISyntaxException e) {
			System.err.printf("%s is not a valid base URI (%s)%n", args[1],e.getMessage());
			System.exit(-3);
		} catch (final IOException e) {
			System.err.printf("Could not explore modules (%s)%n", e.getMessage());
			System.exit(-4);
		}
	}

	private static void showCatalog(final Catalog catalog) {
		System.out.printf("Found %d modules%n",catalog.size());
		final List<Module> externals=Lists.newArrayList();
		for(final String moduleId:catalog.modules()) {
			final Module module = catalog.resolve(URI.create(moduleId));
			if(module.isExternal()) {
				externals.add(module);
				continue;
			}
			showModule(module);
		}
		for(final Module module:externals) {
			showDependency(module);
		}
	}

	private static void showDependency(final Module module) {
		final StringBuilder builder=
			new StringBuilder("- [EXTERNAL] Module '").
				append(module.location()).
				append("' [").
				append(module.relativePath()).
				append("] (").
				append(module.format().getName()).
				append(")");
		if(module.isOntology()) {
			builder.append(" refers to ");
			if(module.isVersion()) {
				builder.append("version '").append(module.versionIRI()).append("' of ");
			}
			builder.append("IRI '").append(module.ontology()).append("'");
			System.out.println(builder);
		}
	}

	private static void showModule(final Module module) {
		System.out.printf("- Module '%s':%n",module.location());
		System.out.printf("  + Relative path: %s%n",module.relativePath());
		System.out.printf("  + Format: %s%n",module.format().getName());
		if(module.isOntology()) {
			System.out.printf("  + Ontology:%n");
			System.out.printf("    * IRI: %s%n",module.ontology());
			if(module.isVersion()) {
				System.out.printf("    * VersionIRI: %s%n",module.versionIRI());
			}
			if(module.hasImports()) {
				System.out.printf("    * Imports:%n");
				for(final String importedModule:module.imports()) {
					System.out.printf("      - %s%n",importedModule);
				}
			}
		}
	}

	private static void publish(final Catalog catalog) {
		final Undertow server =
			Undertow.
				builder().
					addHttpListener(8080, "localhost").
					setHandler(
							path().
								addPrefixPath(
									"/vocabulary/",
									catalogResolver(catalog,
										allow(Methods.GET,
											contentNegotiation(
												new RepresentionGenerator()
											)
										)
									)
								)
							).
					build();
		server.start();
		awaitTerminationRequest();
		server.stop();
	}


	private static void awaitTerminationRequest() {
		System.out.println("Hit <ENTER> to exit...");
		try(final Scanner scanner = new Scanner(System.in)) {
			String readString = scanner.nextLine();
			while(readString != null) {
				if (readString.isEmpty()) {
					break;
				}
				if (scanner.hasNextLine()) {
					readString = scanner.nextLine();
				} else {
					readString = null;
				}
			}
		}
		System.out.println("<ENTER> detected.");
	}

}
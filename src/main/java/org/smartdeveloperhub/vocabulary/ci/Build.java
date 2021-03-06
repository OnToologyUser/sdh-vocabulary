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
 *   Artifact    : org.smartdeveloperhub.vocabulary:sdh-vocabulary:0.3.0
 *   Bundle      : sdh-vocabulary-0.3.0.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.smartdeveloperhub.vocabulary.ci;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public final class Build extends Resource<Build> {

	private final DataSet dataSet;
	private final List<Execution> executions;
	private BuildType buildType;
	private String codebase;
	private final String buildName;

	Build(final DataSet dataSet, final String id) {
		super(id,Build.class);
		this.dataSet=dataSet;
		this.buildName = id;
		this.executions=Lists.newArrayList();
		withBuildType(BuildType.SUB);
	}

	@Override
	List<String> types() {
		return ImmutableList.copyOf(this.buildType.types());
	}

	public DataSet dataSet() {
		return this.dataSet;
	}

	public BuildType buildType() {
		return this.buildType;
	}

	@Override
	public String location() {
		String result=super.location();
		if(result==null) {
			result=composePath(this.dataSet.location(), this.buildName);
		}
		return result;
	}

	public String codebase() {
		String result = this.codebase;
		if(result==null) {
			result=composePath(location(),"repo.git");
		}
		return result;
	}

	public List<Execution> executions() {
		return this.executions;
	}

	public Execution addExecution(final String created) {
		final Execution execution =
			new Execution(this,this.executions.size()).
				withCreated(created);
		this.executions.add(execution);
		return execution;
	}

	public Build withBuildType(final BuildType buildType) {
		this.buildType=buildType;
		return this;
	}

	public Build withCodebase(final String codebase) {
		this.codebase=codebase;
		return this;
	}

	@Override
	void assembleItem(final Assembler assembler, final ValueFactory factory) {
		super.assembleItem(assembler, factory);
		assembler.addProperty("ci:codebase", factory.typedLiteral(codebase(),"http://www.w3.org/2001/XMLSchema#anyURI"));
		final List<Value> values=Lists.newArrayList();
		for(final Execution execution:this.executions) {
			values.add(factory.relativeUri(execution.id()));
		}
		assembler.addProperty("ci:hasExecution", values.toArray(new Value[values.size()]));
	}

}
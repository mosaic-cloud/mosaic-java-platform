/*
 * #%L
 * mosaic-interoperability-tools
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
 * %%
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
 * #L%
 */

package eu.mosaic_cloud.interoperability.tools;


import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.RoleSpecification;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;


public final class ErlangSpecificationDumper
			extends Object
{
	private ErlangSpecificationDumper () {
		throw (new IllegalAccessError ());
	}
	
	public static final void dump (final MessageSpecification message, final StringBuilder builder, final int indent) {
		builder.append ("#message_specification{\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("name = '").append (message.getQualifiedName ()).append ("',\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("identifier = <<\"").append (message.getIdentifier ()).append ("\">>,\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("type = '").append (message.getType ().name ().toLowerCase ()).append ("',\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("coder = undefined}");
	}
	
	public static final void dump (final RoleSpecification role, final StringBuilder builder, final int indent) {
		builder.append ("#role_specification{\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("name = '").append (role.getQualifiedName ()).append ("',\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("identifier = <<\"").append (role.getIdentifier ()).append ("\">>}");
	}
	
	public static final StringBuilder dump (final SessionSpecification ... sessions) {
		final StringBuilder builder = new StringBuilder ();
		builder.append ("[\n");
		for (int index = 0; index < sessions.length; index++) {
			ErlangSpecificationDumper.indent (builder, 1);
			ErlangSpecificationDumper.dump (sessions[index], builder, 1);
			builder.append ((index < (sessions.length - 1)) ? ",\n" : "].\n");
		}
		return (builder);
	}
	
	public static final void dump (final SessionSpecification session, final StringBuilder builder, final int indent) {
		builder.append ("#session_specification{\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("name = '").append (session.getQualifiedName ()).append ("',\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("self_role = ");
		ErlangSpecificationDumper.dump (session.getSelfRole (), builder, indent + 1);
		builder.append (",\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("peer_role = ");
		ErlangSpecificationDumper.dump (session.getPeerRole (), builder, indent + 1);
		builder.append (",\n");
		ErlangSpecificationDumper.indent (builder, indent + 1);
		builder.append ("messages = [\n");
		final MessageSpecification[] messages = Iterables.toArray (session.getMessages (), MessageSpecification.class);
		for (int index = 0; index < messages.length; index++) {
			ErlangSpecificationDumper.indent (builder, indent + 2);
			ErlangSpecificationDumper.dump (messages[index], builder, indent + 2);
			builder.append ((index < (messages.length - 1)) ? ",\n" : "]}");
		}
	}
	
	public static final void main (final String[] arguments, final SessionSpecification ... sessions) {
		Preconditions.checkNotNull (arguments);
		Preconditions.checkArgument (arguments.length == 0);
		Preconditions.checkNotNull (sessions);
		Preconditions.checkArgument (sessions.length > 0);
		final StringBuilder outcome = ErlangSpecificationDumper.dump (sessions);
		System.out.print (outcome.toString ());
	}
	
	private static final void indent (final StringBuilder builder, final int indent) {
		for (int index = 0; index < indent; index++)
			builder.append ('\t');
	}
}

/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.tools.v1.callbacks;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;


public class DefaultContext
			extends Object
{
	public DefaultContext (final CloudletController<?> cloudlet) {
		super ();
		Preconditions.checkNotNull (cloudlet);
		this.cloudlet = cloudlet;
		this.transcript = Transcript.create (this, true);
		this.logger = this.transcript.adaptAs (Logger.class);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
	}
	
	public DefaultContext (final DefaultContext parent) {
		this (parent.cloudlet);
	}
	
	public final CloudletController<?> cloudlet;
	public final TranscriptExceptionTracer exceptions;
	public final Logger logger;
	public final Transcript transcript;
}

/*
 * #%L
 * mosaic-cloudlet
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.cloudlet.runtime;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;

/**
 * Launches a driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class CloudletManagerLauncher {

	private CloudletManagerLauncher() {
		super();
		throw (new UnsupportedOperationException());
	}

	public static final void main(final String[] arguments) throws Throwable {
		Preconditions.checkArgument((arguments != null)
				&& (arguments.length == 2),
				"invalid arguments: expected <ip> <mos-url>");
		String clasz = ContainerComponentCallbacks.class.getCanonicalName();
		MosBasicComponentLauncher.main(new String[] { clasz, arguments[0],
				"29027", "29028", arguments[1] },
				CloudletManagerLauncher.class.getClassLoader());
	}
}

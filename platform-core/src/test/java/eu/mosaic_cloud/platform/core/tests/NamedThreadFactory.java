/*
 * #%L
 * mosaic-platform-core
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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
package eu.mosaic_cloud.platform.core.tests;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class NamedThreadFactory implements ThreadFactory {

	private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final ThreadGroup group;

	NamedThreadFactory(final String poolName) {
		this.group = new ThreadGroup(poolName + "-"
				+ NamedThreadFactory.POOL_NUMBER.getAndIncrement());
	}

	@Override
	public Thread newThread(final Runnable runner) {
		return new Thread(this.group, runner, this.group.getName() + "-thread-"
				+ this.threadNumber.getAndIncrement(), 0);
	}
}

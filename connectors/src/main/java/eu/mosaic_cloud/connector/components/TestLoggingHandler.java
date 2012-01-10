/*
 * #%L
 * mosaic-connector
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
package eu.mosaic_cloud.connector.components;

import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;

public class TestLoggingHandler<T extends Object> implements
		IOperationCompletionHandler<T> {
	private String testName = "";

	public TestLoggingHandler(String testName) {
		super();
		this.testName = testName;
	}

	@Override
	public void onSuccess(T result) {
		MosaicLogger.getLogger().trace(
				"Test " + this.testName + " finished with result: " + result);
	}

	@Override
	public <E extends Throwable> void onFailure(E error) {
		MosaicLogger.getLogger().error(
				"Test " + this.testName + " finished with error: "
						+ error.getMessage());
	}

}

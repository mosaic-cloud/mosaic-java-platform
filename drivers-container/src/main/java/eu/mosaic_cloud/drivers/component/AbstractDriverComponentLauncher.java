/*
 * #%L
 * mosaic-drivers-container
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

package eu.mosaic_cloud.drivers.component;

import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;

public final class AbstractDriverComponentLauncher {

    private AbstractDriverComponentLauncher() {
        super();
        throw (new UnsupportedOperationException());
    }

    public static final void main(final String callbacks, final String[] arguments) throws Throwable {
        MosBasicComponentLauncher.main(callbacks, arguments, AbstractDriverComponentCallbacks.class.getClassLoader());
    }
}

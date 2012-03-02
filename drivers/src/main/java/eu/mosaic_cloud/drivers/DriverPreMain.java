/*
 * #%L
 * mosaic-drivers
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

package eu.mosaic_cloud.drivers;

import java.lang.reflect.InvocationTargetException;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

import com.google.common.base.Preconditions;

public final class DriverPreMain {

    private DriverPreMain() {
    }

    /**
     * @param args
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws Exception
     */
    public static void main(String[] arguments) throws Throwable {
        Preconditions.checkArgument(arguments != null);
        Preconditions.checkArgument(arguments.length == 1,
                "invalid arguments; expected: <resource type: amqp | kv | memcached>");
        final String clasz = DriverCallbackType.valueOf(arguments[0].toUpperCase())
                .getCallbackClass();
        BasicComponentHarnessPreMain.main(new String[] {
            clasz });
    }
}

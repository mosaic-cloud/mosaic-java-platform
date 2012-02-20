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

package eu.mosaic_cloud.platform.core.utils;

import java.io.IOException;

import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;

public class JsonDataEncoder<T extends Object> implements DataEncoder<T> {

    private final Class<T> dataClass;;

    public JsonDataEncoder(final Class<T> dataClass) {
        this.dataClass = dataClass;
    }

    @Override
    public byte[] encode(final T data) throws EncodingException {
        try {
            return SerDesUtils.toJsonBytes(data);
        } catch (IOException e) {
            throw new EncodingException("JSON object cannot be serialized", e);
        }
    }

    @Override
    public T decode(byte[] dataBytes) {
        T object = null;
        try {
            object = this.dataClass.cast(SerDesUtils.jsonToObject(dataBytes, this.dataClass));
        } catch (IOException e) {
            ExceptionTracer.traceIgnored(e);
        } catch (ClassNotFoundException e) {
            ExceptionTracer.traceIgnored(e);
        }

        return object;
    }

}

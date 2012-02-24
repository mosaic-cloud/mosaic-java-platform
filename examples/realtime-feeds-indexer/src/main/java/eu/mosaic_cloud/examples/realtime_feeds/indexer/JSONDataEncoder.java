/*
 * #%L
 * mosaic-examples-realtime-feeds-indexer
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

package eu.mosaic_cloud.examples.realtime_feeds.indexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.EncodingException;

public class JSONDataEncoder implements DataEncoder<JSONObject> {

    @Override
    public JSONObject decode(byte[] dataBytes) throws EncodingException {
        if (dataBytes.length == 0) {
            return null;
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(dataBytes)));
        final JSONTokener tokener = new JSONTokener(reader);
        JSONObject object = null;
        try {
            object = new JSONObject(tokener);
        } catch (final JSONException e) {
            throw new EncodingException("JSON object cannot be deserialized", e);
        }
        return object;
    }

    @Override
    public byte[] encode(JSONObject data) throws EncodingException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Writer writer = new BufferedWriter(new OutputStreamWriter(baos));
        try {
            data.write(writer);
            writer.close();
        } catch (final JSONException e) {
            throw new EncodingException("JSON object cannot be serialized", e);
        } catch (final IOException e) {
            throw new EncodingException("JSON object cannot be serialized", e);
        }
        return baos.toByteArray();
    }
}

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
package eu.mosaic_cloud.examples.feeds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import eu.mosaic_cloud.core.utils.DataEncoder;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONDataEncoder implements DataEncoder<JSONObject> {

	@Override
	public byte[] encode(JSONObject data) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Writer writer = new BufferedWriter(new OutputStreamWriter(baos));
		data.write(writer);
		writer.close();
		return baos.toByteArray();
	}

	@Override
	public JSONObject decode(byte[] dataBytes) throws Exception {
		if (dataBytes.length == 0)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(dataBytes)));
		JSONTokener tokener = new JSONTokener(reader);
		return new JSONObject(tokener);
	}
}

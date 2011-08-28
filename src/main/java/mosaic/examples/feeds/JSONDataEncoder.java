package mosaic.examples.feeds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import mosaic.core.utils.DataEncoder;

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

package mosaic.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.Utf8;

public class SerDesUtils {
	private final static DecoderFactory DIRECT_DECODER = new DecoderFactory();

	private final static EncoderFactory DIRECT_ENCODER = new EncoderFactory();

	/**
	 * Serializes a single operation object.
	 * 
	 * @param op
	 *            operation to serialize
	 * @throws IOException
	 */
	public static <T extends SpecificRecord> byte[] serialize(T t)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BinaryEncoder enc = DIRECT_ENCODER.binaryEncoder(out, null);
		SpecificDatumWriter<T> writer = new SpecificDatumWriter<T>(
				t.getSchema());
		writer.write(t, enc);
		enc.flush();
		return out.toByteArray();
	}

	/**
	 * Serializes a single object along with its Schema. For performance
	 * critical areas, it is <b>much</b> more efficient to store the Schema
	 * independently.
	 * 
	 * @param o
	 *            object to serialize
	 */
	public static <T extends SpecificRecord> byte[] serializeWithSchema(T o)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BinaryEncoder enc = DIRECT_ENCODER.binaryEncoder(out, null);
		enc.writeString(new Utf8(o.getSchema().toString()));
		SpecificDatumWriter<T> writer = new SpecificDatumWriter<T>(
				o.getSchema());
		writer.write(o, enc);
		enc.flush();
		return out.toByteArray();
	}

	/**
	 * Deserializes a single object based on the given Schema.
	 * 
	 * @param writer
	 *            writer's schema
	 * @param bytes
	 *            array to deserialize from
	 * @param ob
	 *            an empty object to deserialize into (must not be null).
	 * @throws IOException
	 */
	public static <T extends SpecificRecord> T deserialize(Schema writer,
			byte[] bytes, T ob) throws IOException {
		BinaryDecoder dec = DIRECT_DECODER.binaryDecoder(bytes, null);
		SpecificDatumReader<T> reader = new SpecificDatumReader<T>(writer);
		reader.setExpected(ob.getSchema());
		return reader.read(ob, dec);
	}

	/**
	 * Deserializes a single object as stored along with its Schema by
	 * serialize(T).
	 * 
	 * @param ob
	 *            an empty object to deserialize into (must not be null).
	 * @param bytes
	 *            array to deserialize from
	 * @throws IOException
	 */
	public static <T extends SpecificRecord> T deserializeWithSchema(
			byte[] bytes, T ob) throws IOException {
		BinaryDecoder dec = DIRECT_DECODER.binaryDecoder(bytes, null);
		Schema writer = Schema.parse(dec.readString(new Utf8()).toString());
		SpecificDatumReader<T> reader = new SpecificDatumReader<T>(writer);
		reader.setExpected(ob.getSchema());
		return reader.read(ob, dec);
	}

	/**
	 * Converts an object to an array of bytes .
	 * 
	 * @param object
	 *            the object to convert.
	 * @return the associated byte array.
	 */
	public static byte[] toBytes(Object object) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		oos.close();
		byte[] bytes = baos.toByteArray();
		return bytes;
	}

	/**
	 * Converts an array of bytes back to its constituent object. The input
	 * array is assumed to have been created from the original object.
	 * 
	 * @param bytes
	 *            the byte array to convert.
	 * @return the associated object.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static Object toObject(byte[] bytes) throws IOException,
			ClassNotFoundException {
		Object object = null;
		ObjectInputStream stream = new ObjectInputStream(
				new ByteArrayInputStream(bytes));
		object = stream.readObject();
		stream.close();
		return object;
	}
}

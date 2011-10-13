package mosaic.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 * Defines utility methods for serializing and deserializing messages.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class SerDesUtils {

	private static ObjectMapper objectMapper = new ObjectMapper();

	static {
		SerDesUtils.objectMapper.configure(
				SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
	}

	private SerDesUtils() {
	}

	/**
	 * Converts an object to an array of bytes .
	 * 
	 * @param object
	 *            the object to convert.
	 * @return the associated byte array.
	 */
	public static byte[] pojoToBytes(final Object object) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		oos.close();
		return baos.toByteArray();
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
		if (bytes.length > 0) {
			ObjectInputStream stream = new ObjectInputStream(
					new ByteArrayInputStream(bytes));
			object = stream.readObject();
			stream.close();
		}
		return object;
	}

	/**
	 * Converts an array of bytes corresponding to a JSON object back to its
	 * constituent Java Bean object. The input array is assumed to have been
	 * created from the original object.
	 * 
	 * @param bytes
	 *            the byte array to convert.
	 * @param valueClass
	 *            the class of the bean object
	 * @return the associated object.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static <T extends Object> T jsonToObject(byte[] bytes,
			Class<T> valueClass) throws IOException, ClassNotFoundException {
		T object = null;
		if (bytes.length > 0) {
			object = SerDesUtils.objectMapper.readValue(bytes, 0, bytes.length,
					valueClass);
		}
		return object;
	}

	/**
	 * Converts an object (Java Bean) to an array of bytes corresponding to the
	 * JSON encoding of the bean..
	 * 
	 * @param object
	 *            the object to convert.
	 * @return the associated byte array.
	 */
	public static byte[] toJsonBytes(Object object) throws IOException {
		byte[] bytes = SerDesUtils.objectMapper.writeValueAsString(object)
				.getBytes();
		return bytes;
	}
}

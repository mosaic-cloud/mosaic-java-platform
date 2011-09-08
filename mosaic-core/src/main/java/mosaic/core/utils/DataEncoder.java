package mosaic.core.utils;

/**
 * Interface for defining data specific encoders (serializers) and decode
 * (deserializers).
 * 
 * @param <T>
 *            the type of data to encode and decode
 * 
 * @author Georgiana Macariu
 * 
 */
public interface DataEncoder<T> {
	/**
	 * Encodes (serializes) an object as a stream of bytes.
	 * 
	 * @param data
	 *            the data to serialize
	 * @return the bytes
	 */
	byte[] encode(T data) throws Exception;

	/**
	 * Decodes (deserializes) the data.
	 * 
	 * @param dataBytes
	 *            data bytes
	 * @return the decoded object
	 */
	T decode(byte[] dataBytes) throws Exception;

}

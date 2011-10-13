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
public  interface DataEncoder<T extends Object> {

	/**
	 * Encodes (serializes) an object as a stream of bytes.
	 * 
	 * @param data
	 *            the data to serialize
	 * @return the bytes
	 */
	byte[] encode(T data) throws Exception; // NOPMD by georgiana on 10/12/11 5:02 PM

	/**
	 * Decodes (deserializes) the data.
	 * 
	 * @param dataBytes
	 *            data bytes
	 * @return the decoded object
	 */
	T decode(byte[] dataBytes) throws Exception; // NOPMD by georgiana on 10/12/11 5:02 PM

}

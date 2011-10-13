package mosaic.core.utils;

import java.io.IOException;

import mosaic.core.exceptions.ExceptionTracer;

public class JsonDataEncoder<T extends Object> implements DataEncoder<T> {

	private final Class<T> dataClass;;

	public JsonDataEncoder(final Class<T> dataClass) {
		this.dataClass = dataClass;
	}

	@Override
	public byte[] encode(final T data) throws Exception { // NOPMD by georgiana on 10/12/11 5:02 PM
		return SerDesUtils.toJsonBytes(data);
	}

	@Override
	public T decode(byte[] dataBytes) {
		T object = null;
		try {
			object = this.dataClass.cast(SerDesUtils.jsonToObject(dataBytes,
					this.dataClass));
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceDeferred(e);
		}

		return object;
	}

}

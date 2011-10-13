package mosaic.core.utils;

import java.io.IOException;

import mosaic.core.exceptions.ExceptionTracer;

public class PojoDataEncoder<T extends Object> implements DataEncoder<T> {

	private final Class<T> dataClass;;

	public PojoDataEncoder(final Class<T> dataClass) {
		this.dataClass = dataClass;
	}

	@Override
	public byte[] encode(T data) throws Exception { // NOPMD by georgiana on 10/12/11 5:03 PM
		return SerDesUtils.pojoToBytes(data);
	}

	@Override
	public T decode(byte[] dataBytes) {
		T object = null; // NOPMD by georgiana on 10/12/11 5:03 PM
		try {
			object = this.dataClass.cast(SerDesUtils.toObject(dataBytes));
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceDeferred(e);
		}

		return object;
	}

}

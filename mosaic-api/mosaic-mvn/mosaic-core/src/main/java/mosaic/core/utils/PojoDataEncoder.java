package mosaic.core.utils;

import java.io.IOException;

import mosaic.core.exceptions.ExceptionTracer;

public class PojoDataEncoder<T> implements DataEncoder<T> {

	private Class<T> dataClass;;

	public PojoDataEncoder(Class<T> dataClass) {
		this.dataClass = dataClass;
	}

	@Override
	public byte[] encode(T data) throws Exception {
		byte[] bytes = SerDesUtils.pojoToBytes(data);
		return bytes;
	}

	@Override
	public T decode(byte[] dataBytes) {
		T ob = null;
		try {
			ob = dataClass.cast(SerDesUtils.toObject(dataBytes));
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceDeferred(e);
		}

		return ob;
	}

}

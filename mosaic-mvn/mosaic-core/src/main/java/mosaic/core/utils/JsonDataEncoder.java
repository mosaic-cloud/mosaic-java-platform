package mosaic.core.utils;

import java.io.IOException;

import mosaic.core.exceptions.ExceptionTracer;

public class JsonDataEncoder<T> implements DataEncoder<T> {

	private Class<T> dataClass;;

	public JsonDataEncoder(Class<T> dataClass) {
		this.dataClass = dataClass;
	}

	@Override
	public byte[] encode(T data) throws Exception {
		byte[] bytes = SerDesUtils.toJsonBytes(data);
		return bytes;
	}

	@Override
	public T decode(byte[] dataBytes) {
		T ob = null;
		try {
			ob = this.dataClass.cast(SerDesUtils.jsonToObject(dataBytes,
					this.dataClass));
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceDeferred(e);
		}

		return ob;
	}

}

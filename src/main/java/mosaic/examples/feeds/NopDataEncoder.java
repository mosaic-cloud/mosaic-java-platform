package mosaic.examples.feeds;

import mosaic.core.utils.DataEncoder;

public class NopDataEncoder  implements DataEncoder<byte[]> {
	
	public NopDataEncoder() {
	}

	@Override
	public byte[] encode(byte[] data) throws Exception {
		return (byte[]) data;
	}

	@Override
	public byte[] decode(byte[] dataBytes) throws Exception {
		return dataBytes;
	}

}

package mosaic.cloudlet.tests;

public class PongMessage {

	private String key;
	private PingPongData value;

	public PongMessage() {

	}

	public PongMessage(String key, PingPongData value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public PingPongData getValue() {
		return value;
	}

	public void setValue(PingPongData value) {
		this.value = value;
	}

}

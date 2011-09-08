package mosaic.cloudlet.tests;

public class PingPongData {

	private String ping;
	private String pong;

	public PingPongData() {
	}

	public String getPing() {
		return ping;
	}

	public void setPing(String ping) {
		this.ping = ping;
	}

	public String getPong() {
		return pong;
	}

	public void setPong(String pong) {
		this.pong = pong;
	}

	@Override
	public String toString() {
		return "(" + ping + ", " + pong + ")";
	}

}

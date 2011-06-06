package mosaic.cloudlet.tests;

public final class LoggingData {
	final String user;
	final String password;

	public LoggingData(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
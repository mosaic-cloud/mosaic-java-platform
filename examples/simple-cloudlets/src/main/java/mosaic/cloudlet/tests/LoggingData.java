package mosaic.cloudlet.tests;

import java.io.Serializable;

public final class LoggingData implements Serializable {

	private static final long serialVersionUID = 3715149789764562975L;
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

	@Override
	public String toString() {
		return user + "(" + password + ")";
	}
}
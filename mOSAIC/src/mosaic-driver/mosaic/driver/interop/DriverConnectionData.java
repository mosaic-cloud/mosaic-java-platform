package mosaic.driver.interop;

/**
 * Generic class holding connection information about a resource driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class DriverConnectionData {
	private final String host;
	private final int port;
	private String user = "";
	private String password = "";
	private final String driverName;

	/**
	 * Creates a new data class
	 * 
	 * @param host
	 *            the hostname or ip address of the machine running the resource
	 * @param port
	 *            the port on which the resource is listening
	 * @param driverName
	 *            driver name
	 */
	public DriverConnectionData(String host, int port, String driverName) {
		super();
		this.host = host;
		this.port = port;
		this.driverName = driverName;
	}

	/**
	 * Creates a new data class
	 * 
	 * @param host
	 *            the hostname or ip address of the machine running the resource
	 * @param port
	 *            the port on which the resource is listening
	 * @param driverName
	 *            driver name
	 * @param user
	 *            username for connecting to resource
	 * @param password
	 *            password for connecting to resource
	 */
	public DriverConnectionData(String host, int port, String driverName,
			String user, String password) {
		this(host, port, driverName);
		this.user = user;
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getDriverName() {
		return driverName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((driverName == null) ? 0 : driverName.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result + port;
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DriverConnectionData other = (DriverConnectionData) obj;
		if (driverName == null) {
			if (other.driverName != null)
				return false;
		} else if (!driverName.equals(other.driverName))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (port != other.port)
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}

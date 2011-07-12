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
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getUser() {
		return this.user;
	}

	public String getPassword() {
		return this.password;
	}

	public String getDriverName() {
		return this.driverName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.driverName == null) ? 0 : this.driverName.hashCode());
		result = prime * result
				+ ((this.host == null) ? 0 : this.host.hashCode());
		result = prime * result
				+ ((this.password == null) ? 0 : this.password.hashCode());
		result = prime * result + this.port;
		result = prime * result
				+ ((this.user == null) ? 0 : this.user.hashCode());
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
		if (this.driverName == null) {
			if (other.driverName != null)
				return false;
		} else if (!this.driverName.equals(other.driverName))
			return false;
		if (this.host == null) {
			if (other.host != null)
				return false;
		} else if (!this.host.equals(other.host))
			return false;
		if (this.password == null) {
			if (other.password != null)
				return false;
		} else if (!this.password.equals(other.password))
			return false;
		if (this.port != other.port)
			return false;
		if (this.user == null) {
			if (other.user != null)
				return false;
		} else if (!this.user.equals(other.user))
			return false;
		return true;
	}

}

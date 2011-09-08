package mosaic.cloudlet.tests;

import java.io.Serializable;

public final class AuthenticationToken implements Serializable {

	private static final long serialVersionUID = 8212390577294189529L;
	private final String token;

	public AuthenticationToken(String token) {
		super();
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String toString() {
		return token;
	}
}
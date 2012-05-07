
package eu.mosaic_cloud.connectors.httpg;


import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


public final class HttpgRequestMessage<TBody extends Object>
{
	private HttpgRequestMessage (final String version, final String method, final String path, final ImmutableMap<String, String> headers, final TBody body, final IHttpgMessageToken token)
	{
		super ();
		Preconditions.checkNotNull (version);
		Preconditions.checkNotNull (method);
		Preconditions.checkNotNull (path);
		Preconditions.checkNotNull (headers);
		Preconditions.checkNotNull (token);
		this.version = version;
		this.method = method;
		this.path = path;
		this.headers = headers;
		this.body = body;
		this.token = token;
	}
	
	public static final <TBody extends Object> HttpgRequestMessage<TBody> create (final String version, final String method, final String path, final ImmutableMap<String, String> headers, final TBody body, final IHttpgMessageToken token)
	{
		return (new HttpgRequestMessage<TBody> (version, method, path, headers, body, token));
	}
	
	public static final <TBody extends Object> HttpgRequestMessage<TBody> create (final String version, final String method, final String path, final Map<String, String> headers, final TBody body, final IHttpgMessageToken token)
	{
		return (new HttpgRequestMessage<TBody> (version, method, path, ImmutableMap.copyOf (headers), body, token));
	}
	
	public static final <TBody extends Object> HttpgRequestMessage<TBody> create (final String version, final String method, final String path, final TBody body, final IHttpgMessageToken token)
	{
		return (new HttpgRequestMessage<TBody> (version, method, path, ImmutableMap.<String, String>of (), body, token));
	}
	
	public final TBody body;
	public final ImmutableMap<String, String> headers;
	public final String method;
	public final String path;
	public final IHttpgMessageToken token;
	public final String version;
}

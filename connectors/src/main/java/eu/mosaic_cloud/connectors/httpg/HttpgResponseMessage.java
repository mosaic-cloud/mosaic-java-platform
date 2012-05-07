
package eu.mosaic_cloud.connectors.httpg;


import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


public final class HttpgResponseMessage<TBody extends Object>
{
	private HttpgResponseMessage (final String version, final int status, final ImmutableMap<String, String> headers, final TBody body, final IHttpgMessageToken token)
	{
		super ();
		Preconditions.checkNotNull (version);
		Preconditions.checkArgument ((status >= 100) && (status <= 599));
		Preconditions.checkNotNull (headers);
		Preconditions.checkNotNull (token);
		this.version = version;
		this.status = status;
		this.headers = headers;
		this.body = body;
		this.token = token;
	}
	
	public static final <TBody extends Object> HttpgResponseMessage<TBody> create (final String version, final int status, final ImmutableMap<String, String> headers, final TBody body, final IHttpgMessageToken token)
	{
		return (new HttpgResponseMessage<TBody> (version, status, headers, body, token));
	}
	
	public static final <TBody extends Object> HttpgResponseMessage<TBody> create (final String version, final int status, final Map<String, String> headers, final TBody body, final IHttpgMessageToken token)
	{
		return (new HttpgResponseMessage<TBody> (version, status, ImmutableMap.copyOf (headers), body, token));
	}
	
	public static final <TBody extends Object> HttpgResponseMessage<TBody> create (final String version, final int status, final TBody body, final IHttpgMessageToken token)
	{
		return (new HttpgResponseMessage<TBody> (version, status, ImmutableMap.<String, String>of (), body, token));
	}
	
	public static final <TBody extends Object> HttpgResponseMessage<TBody> create200 (final HttpgRequestMessage<?> request, final TBody body)
	{
		Preconditions.checkNotNull (request);
		return (new HttpgResponseMessage<TBody> (request.version, 200, ImmutableMap.<String, String>of (), body, request.token));
	}
	
	public final TBody body;
	public final ImmutableMap<String, String> headers;
	public final int status;
	public final IHttpgMessageToken token;
	public final String version;
}

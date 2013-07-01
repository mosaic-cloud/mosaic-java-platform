/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.connectors.v1.httpg;


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

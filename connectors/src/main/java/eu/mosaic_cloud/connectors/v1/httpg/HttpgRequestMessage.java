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

/*
 * #%L
 * mosaic-tools-miscellaneous
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.tools.miscellaneous;


import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Formatter;
import java.util.IllegalFormatFlagsException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;


public final class ExtendedFormatter
		extends Object
{
	private ExtendedFormatter (final Locale locale)
	{
		super ();
		Preconditions.checkNotNull (locale);
		this.locale = locale;
	}
	
	public final String format (final String format, final Object ... tokens)
	{
		Preconditions.checkNotNull (format);
		Preconditions.checkNotNull (tokens);
		final String finalFormat;
		final Object[] finalTokens;
		if (format.contains ("%{")) {
			final StringBuilder finalFormatBuilder = new StringBuilder ();
			final LinkedList<Object> finalTokensBuilder = new LinkedList<Object> (Arrays.asList (tokens));
			final Matcher extendedFormatMatcher = ExtendedFormatter.extendedFormatPattern.matcher (format);
			int lastFormatMatchEnd = 0;
			int currentTokenIndex = 0;
			while (extendedFormatMatcher.find ()) {
				final String formatSpecification = extendedFormatMatcher.group ();
				if (formatSpecification.startsWith ("%{")) {
					finalFormatBuilder.append (format, lastFormatMatchEnd, extendedFormatMatcher.start ());
					lastFormatMatchEnd = extendedFormatMatcher.end ();
					if (formatSpecification.equals ("%{class}")) {
						final Object token = finalTokensBuilder.get (currentTokenIndex);
						final String tokenReplacement;
						if (token == null)
							tokenReplacement = "null";
						else if (token instanceof Class)
							tokenReplacement = ((Class<?>) token).getName ();
						else
							throw (new IllegalArgumentException ());
						finalFormatBuilder.append ("%s");
						finalTokensBuilder.set (currentTokenIndex, tokenReplacement);
					} else if (formatSpecification.equals ("%{method}")) {
						final Object token = finalTokensBuilder.get (currentTokenIndex);
						final String tokenReplacement;
						if (token == null)
							tokenReplacement = "null";
						else if (token instanceof Method)
							tokenReplacement = ((Method) token).toString ();
						else
							throw (new IllegalArgumentException ());
						finalFormatBuilder.append ("%s");
						finalTokensBuilder.set (currentTokenIndex, tokenReplacement);
					} else if (formatSpecification.equals ("%{object:class}")) {
						final Object token = finalTokensBuilder.get (currentTokenIndex);
						final String tokenReplacement;
						if (token == null)
							tokenReplacement = "null";
						else
							tokenReplacement = token.getClass ().getName ();
						finalFormatBuilder.append ("%s");
						finalTokensBuilder.set (currentTokenIndex, tokenReplacement);
					} else if (formatSpecification.equals ("%{object}")) {
						final Object token = finalTokensBuilder.get (currentTokenIndex);
						final String tokenReplacement;
						if (token == null)
							tokenReplacement = "null";
						else
							tokenReplacement = String.format ("%s#%08x", token.getClass ().getName (), Integer.valueOf (System.identityHashCode (token)));
						finalFormatBuilder.append ("%s");
						finalTokensBuilder.set (currentTokenIndex, tokenReplacement);
					} else if (formatSpecification.equals ("%{array}")) {
						final Object token = finalTokensBuilder.get (currentTokenIndex);
						final String tokenReplacement;
						if (token == null)
							tokenReplacement = "null";
						else if (token.getClass ().isArray ())
							tokenReplacement = Arrays.toString ((Object[]) token);
						else
							throw (new IllegalArgumentException ());
						finalFormatBuilder.append ("%s");
						finalTokensBuilder.set (currentTokenIndex, tokenReplacement);
					} else if (formatSpecification.equals ("%{object:identity}")) {
						final Object token = finalTokensBuilder.get (currentTokenIndex);
						final String tokenReplacement;
						if (token == null)
							tokenReplacement = "null";
						else
							tokenReplacement = String.format ("%08x", Integer.valueOf (System.identityHashCode (token)));
						finalFormatBuilder.append ("%s");
						finalTokensBuilder.set (currentTokenIndex, tokenReplacement);
					} else
						throw (new IllegalFormatFlagsException (formatSpecification));
					currentTokenIndex++;
				} else {
					if (!formatSpecification.contains ("$") && !formatSpecification.contains ("$"))
						currentTokenIndex++;
				}
			}
			finalFormatBuilder.append (format, lastFormatMatchEnd, format.length ());
			finalFormat = finalFormatBuilder.toString ();
			finalTokens = finalTokensBuilder.toArray ();
		} else {
			finalFormat = format;
			finalTokens = tokens;
		}
		final StringBuilder builder = new StringBuilder ();
		final Formatter formatter = new Formatter (builder, this.locale);
		formatter.format (this.locale, finalFormat, finalTokens);
		return (builder.toString ());
	}
	
	public static final ExtendedFormatter create ()
	{
		return (new ExtendedFormatter (Locale.getDefault ()));
	}
	
	public static final ExtendedFormatter create (final Locale locale)
	{
		return (new ExtendedFormatter (locale));
	}
	
	private final Locale locale;
	public static final ExtendedFormatter defaultInstance = ExtendedFormatter.create ();
	private static final Pattern extendedFormatPattern = Pattern.compile (ExtendedFormatter.extendedFormatPatternSpecification);
	private static final String extendedFormatPatternSpecification = "(%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%]))|(%\\{[a-z:-]+\\})";
}

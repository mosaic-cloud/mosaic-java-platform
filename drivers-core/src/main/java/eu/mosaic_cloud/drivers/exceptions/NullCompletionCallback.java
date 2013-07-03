/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.drivers.exceptions;


/**
 * Exception thrown when no operation completion callback is set.
 * 
 * @author Georgiana Macariu
 */
public class NullCompletionCallback
			extends Exception
{
	/**
	 * Constructs a new exception with null as its detail message. The cause is not initialized, and may subsequently be
	 * initialized by a call to {@link Throwable#initCause(Throwable)}.
	 */
	public NullCompletionCallback () {
		super ();
	}
	
	/**
	 * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently be
	 * initialized by a call to {@link Throwable#initCause(Throwable)}.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()}
	 *            method
	 */
	public NullCompletionCallback (final String message) {
		super (message);
	}
	
	/**
	 * Constructs a new exception with the specified detail message and cause. Note that the detail message associated with
	 * cause is not automatically incorporated in this exception's detail message.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()}
	 *            method
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is
	 *            permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public NullCompletionCallback (final String message, final Throwable cause) {
		super (message, cause);
	}
	
	/**
	 * Constructs a new exception with the specified cause and a detail message of (cause==null ? null : cause.toString())
	 * (which typically contains the class and detail message of cause). This constructor is useful for exceptions that are
	 * little more than wrappers for other throwables.
	 * 
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is
	 *            permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public NullCompletionCallback (final Throwable cause) {
		super (cause);
	}
	
	private static final long serialVersionUID = -3388438945086356985L;
}

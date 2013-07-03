/*
 * #%L
 * mosaic-tools-threading
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

package eu.mosaic_cloud.tools.threading.implementations.basic;


import java.security.Permission;

import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import sun.security.util.SecurityConstants;


@SuppressWarnings ("restriction")
public final class BasicThreadingSecurityManager
			extends SecurityManager
			implements
				ThreadingSecurityManager
{
	private BasicThreadingSecurityManager () {
		super ();
		this.logger = LoggerFactory.getLogger (this.getClass ());
	}
	
	@Override
	public final void checkAccess (final Thread thread) {
		Preconditions.checkNotNull (thread);
		final ThreadingContext context = Threading.getCurrentContext ();
		// FIXME: ??? (I don't remember what the problem was...)
		if (context != null) {
			// FIXME: do an enforcement, not just logging...
			if (!context.isManaged (thread)) {
				this.logger.warn ("thread access check failed for thread: {}; ignoring!", thread);
				//# throw (new SecurityException ());
			}
		}
		super.checkAccess (thread);
	}
	
	@Override
	public final void checkAccess (final ThreadGroup group) {
		Preconditions.checkNotNull (group);
		final ThreadingContext context = Threading.getCurrentContext ();
		// FIXME: ??? (I don't remember what the problem was...)
		if (context != null) {
			// FIXME: do an enforcement, not just logging...
			if (!context.isManaged (group)) {
				this.logger.warn ("thread access check failed for thread group: {}; ignoring!", group);
				//# throw (new SecurityException ());
			}
		}
		super.checkAccess (group);
	}
	
	@Override
	public final void checkPermission (final Permission permission) {
		this.checkPermission_ (permission);
		// FIXME: Shouldn't we delegate them? (We should investigate deeper the implications...)
		//-- It seems that the default implementation is to "deny" everything, thus we "allow" everything.
		//# super.checkPermission (permission);
	}
	
	@Override
	public final void checkPermission (final Permission permission, final Object context) {
		this.checkPermission_ (permission);
		// FIXME: Should we delegate them? (We should investigate deeper the implications...)
		//-- It seems that the default implementation is to "allow" everything with a context.
		super.checkPermission (permission, context);
	}
	
	@Override
	public final ThreadGroup getThreadGroup () {
		final ThreadingContext context = Threading.getCurrentContext ();
		if (context != null) {
			final ThreadGroup group = context.getDefaultThreadGroup ();
			if (group == null)
				throw (new IllegalThreadStateException ());
			return (group);
		}
		return (super.getThreadGroup ());
	}
	
	private final void checkPermission_ (final Permission permission) {
		if (BasicThreadingSecurityManager.permissionSetSecurityManager.equals (permission))
			throw (new SecurityException ());
		boolean checkRead = false;
		boolean checkWrite = false;
		if (BasicThreadingSecurityManager.permissionGetContextClassLoader.equals (permission))
			checkRead |= true;
		if (BasicThreadingSecurityManager.permissionSetContextClassLoader.equals (permission))
			checkWrite |= true;
		if (BasicThreadingSecurityManager.permissionOverrideContextClassLoader.equals (permission))
			checkWrite |= true;
		if (BasicThreadingSecurityManager.permissionGetStackTrace.equals (permission))
			checkRead |= true;
		if (BasicThreadingSecurityManager.permissionModifyThreadGroup.equals (permission))
			checkWrite |= true;
		if (BasicThreadingSecurityManager.permissionSetDefaultUncaughtExceptionHandler.equals (permission))
			checkWrite |= true;
		if (BasicThreadingSecurityManager.permissionStop.equals (permission))
			checkWrite |= true;
		checkRead |= checkWrite;
		if (checkRead || checkWrite) {
			// FIXME: We should do something useful with these permissions which impact the behaviour of created threads.
			//-- Currently we accept any operation.
		}
	}
	
	private final Logger logger;
	
	public static final void initialize () {
		/*
		 * We apply the well known "singleton initialization pattern".
		 * 
		 * We synchronize on `System.class` because in the Sun / Oracle JRE 1.7.0 implementation the static method
		 * `setSecurityManager` delegates to a private method which in turn synchronizes on `System.class`.
		 * Thus we eliminate a possible race condition.
		 * 
		 * We recurse after setting (in practice at most once) to be sure that we indeed initialized the correct
		 * security manager.
		 */
		synchronized (System.class) {
			final SecurityManager manager = System.getSecurityManager ();
			if (manager instanceof BasicThreadingSecurityManager)
				return;
			if (manager != null)
				throw (new IllegalStateException ());
			System.setSecurityManager (new BasicThreadingSecurityManager ());
			BasicThreadingSecurityManager.initialize ();
		}
	}
	
	private static final RuntimePermission permissionGetContextClassLoader = SecurityConstants.GET_CLASSLOADER_PERMISSION;
	private static final RuntimePermission permissionGetStackTrace = SecurityConstants.GET_STACK_TRACE_PERMISSION;
	private static final RuntimePermission permissionModifyThreadGroup = SecurityConstants.MODIFY_THREADGROUP_PERMISSION;
	private static final RuntimePermission permissionOverrideContextClassLoader = new RuntimePermission ("enableContextClassLoaderOverride");
	private static final RuntimePermission permissionSetContextClassLoader = new RuntimePermission ("setContextClassLoader");
	private static final RuntimePermission permissionSetDefaultUncaughtExceptionHandler = new RuntimePermission ("setDefaultUncaughtExceptionHandler");
	private static final RuntimePermission permissionSetSecurityManager = new RuntimePermission ("setSecurityManager");
	private static final RuntimePermission permissionStop = SecurityConstants.STOP_THREAD_PERMISSION;
}

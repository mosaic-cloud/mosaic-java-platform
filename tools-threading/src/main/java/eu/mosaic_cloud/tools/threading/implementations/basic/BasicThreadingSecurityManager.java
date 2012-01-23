
package eu.mosaic_cloud.tools.threading.implementations.basic;


import java.security.Permission;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import sun.security.util.SecurityConstants;


@SuppressWarnings ("restriction")
public final class BasicThreadingSecurityManager
		extends SecurityManager
		implements
			ThreadingSecurityManager
{
	private BasicThreadingSecurityManager ()
	{
		super ();
	}
	
	@Override
	public final void checkAccess (final Thread thread)
	{
		Preconditions.checkNotNull (thread);
		final ThreadingContext context = Threading.getCurrentContext ();
		if (context != null) {
			if (!context.isManaged (thread))
				throw (new SecurityException ());
		}
		super.checkAccess (thread);
	}
	
	@Override
	public final void checkAccess (final ThreadGroup group)
	{
		Preconditions.checkNotNull (group);
		final ThreadingContext context = Threading.getCurrentContext ();
		if (context != null) {
			if (!context.isManaged (group))
				throw (new SecurityException ());
		}
		super.checkAccess (group);
	}
	
	@Override
	public final void checkPermission (final Permission permission)
	{
		this.checkPermission_ (permission);
		// super.checkPermission (permission);
	}
	
	@Override
	public final void checkPermission (final Permission permission, final Object context)
	{
		this.checkPermission_ (permission);
		// super.checkPermission (permission, context);
	}
	
	@Override
	public final ThreadGroup getThreadGroup ()
	{
		final ThreadingContext context = Threading.getCurrentContext ();
		if (context != null) {
			final ThreadGroup group = context.getDefaultThreadGroup ();
			if (group == null)
				throw (new IllegalThreadStateException ());
			return (group);
		}
		return (super.getThreadGroup ());
	}
	
	private final void checkPermission_ (final Permission permission)
	{
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
			// ...
		}
	}
	
	public static final void initialize ()
	{
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

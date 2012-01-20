
package eu.mosaic_cloud.tools.threading.implementations.basic;


import java.security.Permission;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;


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
			final ThreadGroup group = thread.getThreadGroup ();
			if (group != null)
				if (!context.isManaged (group))
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
		if (BasicThreadingSecurityManager.permissionSetSecurityManager.equals (permission))
			throw (new SecurityException ());
		// super.checkPermission (permission);
	}
	
	@Override
	public final void checkPermission (final Permission permission, final Object context)
	{
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
	
	private static final RuntimePermission permissionSetSecurityManager = new RuntimePermission ("setSecurityManager");
}

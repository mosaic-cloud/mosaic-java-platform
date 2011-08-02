package mosaic.cloudlet.runtime;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.ConfigProperties;
import mosaic.cloudlet.core.Cloudlet;
import mosaic.cloudlet.core.CloudletException;
import mosaic.cloudlet.core.ICloudlet;
import mosaic.cloudlet.core.ICloudletCallback;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;

/**
 * Implements a container holding a list of cloudlet instances. All instances
 * have the same cloudlet type.
 * 
 * @author Georgiana Macariu
 * 
 */
public class CloudletManager {

	/**
	 * Pool containing all cloudlet instances of the cloudlet. Accessed only
	 * when holding mainLock.
	 */
	private List<ICloudlet> cloudletPool;

	private ClassLoader classLoader;

	private IConfiguration configuration;

	/**
	 * Creates a new container.
	 * 
	 * @param classLoader
	 *            class loader to be used for loading the classes of the
	 *            cloudlet
	 * @param configuration
	 *            configuration object of the cloudlet
	 */
	public CloudletManager(ClassLoader classLoader,
			IConfiguration configuration) {
		super();
		this.classLoader = classLoader;
		this.configuration = configuration;
		this.cloudletPool = new ArrayList<ICloudlet>();
		// TODO
	}

	/**
	 * Starts the container.
	 * 
	 * @throws CloudletException
	 */
	public void start() throws CloudletException {
		synchronized (this) {
			createCloudletInstance();
		}
	}

	/**
	 * Stops the container and destroys all hosted cloudlets.
	 */
	public final void stop() {
		synchronized (this) {
			for (ICloudlet cloudlet : this.cloudletPool) {
				if (cloudlet.isActive()) {
					cloudlet.destroy();
				}
			}
		}
	}

	/**
	 * Creates a new cloudlet instance.
	 * 
	 * @throws CloudletException
	 *             if the cloudlet instance cannot be created
	 */
	private void createCloudletInstance() throws CloudletException {
		Class<?> handlerClasz;
		Class<?> stateClasz;
		IConfiguration resourceConfig;

		try {

			String cloudletClass = ConfigUtils
					.resolveParameter(
							this.configuration,
							ConfigProperties
									.getString("CloudletDummyContainer.0"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (cloudletClass.equals(""))
				throw new CloudletException("The configuration file " //$NON-NLS-1$
						+ this.configuration.toString()
						+ " does not specify a handler class for cloudlet " //$NON-NLS-1$
						+ cloudletClass + "."); //$NON-NLS-1$

			String cloudletStateClass = ConfigUtils
					.resolveParameter(
							this.configuration,
							ConfigProperties
									.getString("CloudletDummyContainer.1"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (cloudletStateClass.equals(""))
				throw new CloudletException("The configuration file " //$NON-NLS-1$
						+ this.configuration.toString()
						+ " does not specify a state class for cloudlet " //$NON-NLS-1$
						+ cloudletClass + "."); //$NON-NLS-1$

			String resourceFile = ConfigUtils
					.resolveParameter(
							this.configuration,
							ConfigProperties
									.getString("CloudletDummyContainer.2"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$

			handlerClasz = this.classLoader.loadClass(cloudletClass);
			stateClasz = this.classLoader.loadClass(cloudletStateClass);
			resourceConfig = PropertyTypeConfiguration.create(this.classLoader,
					resourceFile);

			ICloudletCallback<?> cloudlerHandler = (ICloudletCallback<?>) createHandler(handlerClasz);
			Object cloudletState = invokeConstructor(stateClasz);
			Cloudlet<?> cloudlet = new Cloudlet(cloudletState, cloudlerHandler);
			cloudlet.initialize(resourceConfig);
			this.cloudletPool.add(cloudlet);
		} catch (ClassNotFoundException e) {
			MosaicLogger.getLogger().error(
					"Could not resolve class: " + e.getMessage()); //$NON-NLS-1$
			throw new IllegalArgumentException();
		}
	}

	private final Object invokeConstructor(final Class<?> clasz) {
		Object instance;
		try {
			instance = clasz.newInstance();
		} catch (final Throwable exception) {
			MosaicLogger.getLogger().error(
					"Could not instantiate class: `" + clasz + "`"); //$NON-NLS-1$ //$NON-NLS-2$
			ExceptionTracer.traceIgnored(exception);
			throw new IllegalArgumentException();
		}
		return (instance);
	}

	private <T> T createHandler(final Class<T> clasz) {
		T instance = null;
		boolean isCallback = implementsType(clasz, ICloudletCallback.class);

		if (!isCallback) {
			MosaicLogger.getLogger().error(
					"Missmatched object class: `" + clasz.getName() + "`"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new IllegalArgumentException();
		}
		try {
			instance = clasz.newInstance();
		} catch (final Throwable exception) {
			MosaicLogger.getLogger().error(
					"Could not instantiate class: `" + clasz + "`"); //$NON-NLS-1$ //$NON-NLS-2$
			ExceptionTracer.traceIgnored(exception);
			throw new IllegalArgumentException();
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private <T> boolean implementsType(Class<T> clasz, Class<?> searchedType) {
		Type genericTypes[] = clasz.getInterfaces();
		for (int i = 0; i < genericTypes.length; i++) {
			if (genericTypes[i] == searchedType)
				return true;
		}
		boolean found = false;
		for (int i = 0; i < genericTypes.length; i++) {
			found = implementsType((Class<T>) genericTypes[i], searchedType);
			if (found)
				return true;
		}
		Class<? super T> superClass = clasz.getSuperclass();
		found = implementsType(superClass, searchedType);

		return found;
	}

}

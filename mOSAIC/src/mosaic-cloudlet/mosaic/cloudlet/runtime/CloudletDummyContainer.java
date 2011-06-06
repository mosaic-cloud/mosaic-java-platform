package mosaic.cloudlet.runtime;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

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

public class CloudletDummyContainer {

	/**
	 * Pool containing all cloudlet instances of the cloudlet. Accessed only
	 * when holding mainLock.
	 */
	private Map<String, ICloudlet> cloudletPool;

	private ClassLoader classLoader;

	private IConfiguration configuration;

	public CloudletDummyContainer(ClassLoader classLoader,
			IConfiguration configuration) {
		super();
		this.classLoader = classLoader;
		this.configuration = configuration;
		this.cloudletPool = new HashMap<String, ICloudlet>();
		// TODO
	}

	public void start() throws CloudletException {
		synchronized (this) {
			createCloudlets();
		}
	}

	public final void stop() {
		synchronized (this) {
			for (ICloudlet cloudlet : this.cloudletPool.values()) {
				if (cloudlet.isActive())
					cloudlet.destroy();
			}
		}
	}

	private void createCloudlets() throws CloudletException {
		int noCloudlets = 1;
		Class<?> handlerClasz;
		Class<?> stateClasz;
		IConfiguration resourceConfig;

		try {
			while (true) {
				String cloudletClass = ConfigUtils
						.resolveParameter(
								configuration,
								ConfigProperties
										.getString("CloudletDummyContainer.0") + noCloudlets //$NON-NLS-1$
										+ ConfigProperties
												.getString("CloudletDummyContainer.1"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (cloudletClass.equals("")) //$NON-NLS-1$
					break;
				String cloudletStateClass = ConfigUtils
						.resolveParameter(
								configuration,
								ConfigProperties
										.getString("CloudletDummyContainer.2") + noCloudlets //$NON-NLS-1$
										+ ConfigProperties
												.getString("CloudletDummyContainer.3"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (cloudletStateClass.equals("")) { //$NON-NLS-1$
					throw new CloudletException("The configuration file " //$NON-NLS-1$
							+ configuration.toString()
							+ " does not specify a state class for cloudlet " //$NON-NLS-1$
							+ cloudletClass + "."); //$NON-NLS-1$
				}
				String resourceFile = ConfigUtils
						.resolveParameter(
								configuration,
								ConfigProperties
										.getString("CloudletDummyContainer.4") + noCloudlets //$NON-NLS-1$
										+ ConfigProperties
												.getString("CloudletDummyContainer.5"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$

				handlerClasz = this.classLoader.loadClass(cloudletClass);
				stateClasz = this.classLoader.loadClass(cloudletStateClass);
				resourceConfig = PropertyTypeConfiguration.create(
						this.classLoader, resourceFile);

				ICloudletCallback<?> cloudlerHandler = (ICloudletCallback<?>) createHandler(handlerClasz);
				Object cloudletState = invokeConstructor(stateClasz);
				Cloudlet<?> cloudlet = new Cloudlet(cloudletState,
						cloudlerHandler);
				cloudlet.initialize(resourceConfig);
				this.cloudletPool.put(cloudletClass, cloudlet);
			}
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
	private <T> boolean implementsType(Class<T> clasz,
			Class<?> searchedType) {
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

package mosaic.cloudlet.resources.amqp;

import mosaic.cloudlet.resources.IResourceAccessor;

/**
 * Interface for registering and using an AMQP resources as a consumer or a
 * publisher.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IAmqpQueueAccessor<S> extends IResourceAccessor<S> {

	/**
	 * Register the accessor with the queuing system.
	 */
	public void register();

	/**
	 * Unregister the accessor with the queuing system.
	 */
	public void unregister();

}

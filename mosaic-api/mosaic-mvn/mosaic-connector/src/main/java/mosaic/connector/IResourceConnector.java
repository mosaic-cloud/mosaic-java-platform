package mosaic.connector;

/**
 * Generic interface that should be implemented by all resource connectors.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IResourceConnector {
	/**
	 * Destroy the connection with the resource.
	 * 
	 * @throws Throwable
	 *             if connector cannot be destroyed
	 */
	void destroy() throws Throwable;
}

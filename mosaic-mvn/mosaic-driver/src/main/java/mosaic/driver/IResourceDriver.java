package mosaic.driver;

/**
 * Generic interface that should be implemented by all resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IResourceDriver {
	/**
	 * Destroy the connection with the resource.
	 */
	void destroy();
}

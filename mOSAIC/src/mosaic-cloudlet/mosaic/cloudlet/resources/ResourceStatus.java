package mosaic.cloudlet.resources;

/**
 * Enum defining the life cycle of a resource accessor.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum ResourceStatus {
	CREATED, INITIALIZING, INITIALIZED, READY, DESTROYING, DESTROYED
}

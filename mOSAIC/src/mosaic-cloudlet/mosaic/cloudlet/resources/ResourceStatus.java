package mosaic.cloudlet.resources;

/**
 * Enum defining the life cycle of a resource accessor.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum ResourceStatus {
	INITIALIZING, CREATED, READY, DESTROYING, DESTROYED
}

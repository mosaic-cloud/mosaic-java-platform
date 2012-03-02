
package eu.mosaic_cloud.interoperability.core;


public interface Resolver
{
	public abstract void resolve (final String target, final ResolverCallbacks callbacks);
}

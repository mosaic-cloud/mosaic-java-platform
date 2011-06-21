
package eu.mosaic_cloud.json.core;


public interface JsonMapper
{
	public abstract <_Object_ extends Object> _Object_ decode (final Object structure, final Class<_Object_> mappingClass)
			throws Throwable;
	
	public abstract <_Object_ extends Object> Object encode (final _Object_ object, final Class<_Object_> mappingClass)
			throws Throwable;
}

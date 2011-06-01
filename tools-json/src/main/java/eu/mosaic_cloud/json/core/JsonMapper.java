
package eu.mosaic_cloud.json.core;


import java.util.Map;


public interface JsonMapper
{
	public abstract <_Object_ extends Object> _Object_ decode (final Map<String, ? extends Object> structure, final Class<_Object_> mappingClass)
			throws Throwable;
	
	public abstract <_Object_ extends Object> Map<String, ? extends Object> encode (final _Object_ object, final Class<_Object_> mappingClass)
			throws Throwable;
}

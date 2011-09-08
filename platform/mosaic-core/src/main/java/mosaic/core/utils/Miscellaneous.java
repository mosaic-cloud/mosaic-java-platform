package mosaic.core.utils;

/**
 * Various utility methods.
 * 
 * @author Georgiana Macariu
 * 
 */
public class Miscellaneous {
	/**
	 * Casts an object to a specified type.
	 * 
	 * @param <T>
	 *            the type to cast to
	 * @param classToCast
	 *            the class object for the type
	 * @param valueToCast
	 *            the object to cast
	 * @return the casted object
	 */
	public static <T> T cast(Class<T> classToCast, Object valueToCast) {
		return classToCast.cast(valueToCast);
	}
}

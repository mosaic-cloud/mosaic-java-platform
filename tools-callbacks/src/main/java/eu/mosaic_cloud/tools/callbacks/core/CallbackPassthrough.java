
package eu.mosaic_cloud.tools.callbacks.core;


import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target ({METHOD})
@Retention (RetentionPolicy.RUNTIME)
public @interface CallbackPassthrough
{}

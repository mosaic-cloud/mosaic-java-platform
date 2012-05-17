
package eu.mosaic_cloud.cloudlets.tools;


import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.slf4j.Logger;


public class DefaultContext
{
	public DefaultContext ()
	{
		this.transcript = Transcript.create (this, true);
		this.logger = this.transcript.adaptAs (Logger.class);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
	}
	
	public final TranscriptExceptionTracer exceptions;
	public final Logger logger;
	public final Transcript transcript;
}

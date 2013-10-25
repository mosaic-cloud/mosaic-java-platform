
package eu.mosaic_cloud.platform.implementation.v2.connectors.direct.riak;


import java.io.IOException;

import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.tools.ComponentIdentifierConfigurationParameter;
import eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore.BaseKvStoreConnectorProxy;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder.EncodeOutcome;
import eu.mosaic_cloud.platform.v2.serialization.EncodingException;
import eu.mosaic_cloud.platform.v2.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionTrigger;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.tools.StringConfigurationParameter;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.basho.riak.pbc.RiakClient;
import com.basho.riak.pbc.RiakObject;
import com.google.common.base.Preconditions;


public final class RiakKvStoreConnectorProxy<TValue extends Object>
			extends Object
			implements
				BaseKvStoreConnectorProxy<TValue>
{
	protected RiakKvStoreConnectorProxy (final ConnectorConfiguration configuration, final DataEncoder<TValue> valueEncoder) {
		super ();
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (valueEncoder);
		this.configuration = configuration;
		this.valueEncoder = valueEncoder;
		this.transcript = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
		this.component = RiakKvStoreConnectorProxy.componentConfigurationParameter.resolve (this.configuration.getConfiguration ());
		this.bucket = RiakKvStoreConnectorProxy.bucketConfigurationParameter.resolve (this.configuration.getConfiguration ());
	}
	
	@Override
	public CallbackCompletion<Void> delete (final String key, final CallbackCompletionTrigger<Void> trigger) {
		try {
			this.client.delete (this.bucket, key);
			trigger.triggerSucceeded (null);
			return (trigger.completion);
		} catch (final IOException exception) {
			trigger.triggerFailed (exception);
			return (trigger.completion);
		}
	}
	
	@Override
	public CallbackCompletion<Void> destroy (final CallbackCompletionTrigger<Void> trigger) {
		this.client.shutdown ();
		this.client = null;
		trigger.triggerSucceeded (null);
		return (trigger.completion);
	}
	
	@Override
	public CallbackCompletion<TValue> get (final String key, final CallbackCompletionTrigger<TValue> trigger) {
		Preconditions.checkState (this.client != null);
		try {
			final RiakObject[] objects = this.client.fetch (this.bucket, key);
			if (objects.length != 1) {
				trigger.triggerFailed (new IllegalStateException ("divergent copies found"));
				return (trigger.completion);
			}
			final TValue value = this.decodeValue (objects[0]);
			trigger.triggerSucceeded (value);
			return (trigger.completion);
		} catch (final IOException exception) {
			trigger.triggerFailed (exception);
			return (trigger.completion);
		} catch (final EncodingException exception) {
			trigger.triggerFailed (exception);
			return (trigger.completion);
		}
	}
	
	@Override
	public CallbackCompletion<Void> initialize (final CallbackCompletionTrigger<Void> trigger) {
		final CallbackCompletion<GetStorePbEndpointOutputs> completion = this.configuration.getEnvironment ().getComponentConnector ().call (this.component, RiakKvStoreConnectorProxy.getStorePbEndpointOperation, null, GetStorePbEndpointOutputs.class);
		completion.await ();
		if (completion.getException () != null) {
			trigger.triggerFailed (completion.getException ());
			return (trigger.completion);
		}
		final GetStorePbEndpointOutputs outcome = completion.getOutcome ();
		final String endpointHost = outcome.ip;
		final int endpointPort = outcome.port;
		try {
			this.client = new RiakClient (endpointHost, endpointPort);
			trigger.triggerSucceeded (null);
		} catch (final IOException exception) {
			trigger.triggerFailed (exception);
		}
		return (trigger.completion);
	}
	
	@Override
	public CallbackCompletion<Void> set (final String key, final TValue value, final CallbackCompletionTrigger<Void> trigger) {
		try {
			final RiakObject object = this.encodeValue (key, value);
			this.client.store (object);
			trigger.triggerSucceeded (null);
			return (trigger.completion);
		} catch (final IOException exception) {
			trigger.triggerFailed (exception);
			return (trigger.completion);
		} catch (final EncodingException exception) {
			trigger.triggerFailed (exception);
			return (trigger.completion);
		}
	}
	
	protected final TValue decodeValue (final RiakObject envelope)
				throws EncodingException {
		if (envelope.getValue ().isEmpty ())
			return (null);
		return (this.valueEncoder.decode (envelope.getValue ().toByteArray (), new EncodingMetadata (envelope.getContentType ())));
	}
	
	protected final RiakObject encodeValue (final String key, final TValue value)
				throws EncodingException {
		final EncodeOutcome encoding = this.valueEncoder.encode (value, null);
		final RiakObject envelope = new RiakObject (this.bucket, key, encoding.data);
		envelope.setContentType (encoding.metadata.getContentType ());
		return (envelope);
	}
	
	protected final String bucket;
	protected RiakClient client;
	protected final ComponentIdentifier component;
	protected final ConnectorConfiguration configuration;
	protected final TranscriptExceptionTracer exceptions;
	protected final Transcript transcript;
	protected final DataEncoder<TValue> valueEncoder;
	
	public static final <TValue extends Object> RiakKvStoreConnectorProxy<TValue> create (final ConnectorConfiguration configuration, final DataEncoder<TValue> encoder) {
		return (new RiakKvStoreConnectorProxy<TValue> (configuration, encoder));
	}
	
	public static final ConfigurationIdentifier bucketConfigurationIdentifier = ConfigurationIdentifier.resolveRelative ("riak-kv/bucket");
	public static final ConfigurationIdentifier componentConfigurationIdentifier = ConfigurationIdentifier.resolveRelative ("riak-kv/component");
	public static final ComponentIdentifier defaultComponentConfigurationValue = ComponentIdentifier.resolve ("9cdce23e78027ef6a52636da7db820c47e695d11");
	public static final String getStorePbEndpointOperation = "mosaic-riak-kv:get-store-pb-endpoint";
	protected static final StringConfigurationParameter bucketConfigurationParameter = StringConfigurationParameter.create (RiakKvStoreConnectorProxy.bucketConfigurationIdentifier);
	protected static final ComponentIdentifierConfigurationParameter componentConfigurationParameter = ComponentIdentifierConfigurationParameter.create (RiakKvStoreConnectorProxy.componentConfigurationIdentifier, RiakKvStoreConnectorProxy.defaultComponentConfigurationValue, true);
	
	public static final class GetStorePbEndpointOutputs
	{
		public final String fqdn = null;
		public final String ip = null;
		public final int port = 0;
		public final String url = null;
	}
}

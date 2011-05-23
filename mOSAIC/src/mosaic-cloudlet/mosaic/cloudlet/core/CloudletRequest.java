package mosaic.cloudlet.core;

import java.util.List;

import mosaic.core.ops.EventDrivenOperation;
import mosaic.core.ops.IOperationCompletionHandler;

public class CloudletRequest<T> extends EventDrivenOperation<T> {
	private CloudletRequestTag tag;

	public CloudletRequest(List<IOperationCompletionHandler<T>> complHandlers) {
		super(complHandlers);
		// TODO Auto-generated constructor stub
	}

}

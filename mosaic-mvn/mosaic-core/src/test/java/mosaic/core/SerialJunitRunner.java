package mosaic.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;

public class SerialJunitRunner extends BlockJUnit4ClassRunner {
	public SerialJunitRunner(final Class<?> klass) throws InitializationError {
		super(klass);
		setScheduler(new RunnerScheduler() {
			ExecutorService executorService = Executors.newFixedThreadPool(1,
					new NamedThreadFactory(klass.getSimpleName()));
			CompletionService<Void> completionService = new ExecutorCompletionService<Void>(
					this.executorService);
			Queue<Future<Void>> tasks = new LinkedList<Future<Void>>();

			@Override
			public void schedule(Runnable childStatement) {
				this.tasks.offer(this.completionService.submit(childStatement,
						null));
			}

			@Override
			public void finished() {
				try {
					while (!this.tasks.isEmpty()) {
						this.tasks.remove(this.completionService.take());
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					while (!this.tasks.isEmpty()) {
						this.tasks.poll().cancel(true);
					}
					this.executorService.shutdownNow();
				}
			}
		});
	}

}

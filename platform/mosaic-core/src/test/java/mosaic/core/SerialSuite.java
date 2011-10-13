package mosaic.core;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;

public final class SerialSuite extends Suite {

	public SerialSuite(final Class<?> klass) throws InitializationError {
		super(klass, new AllDefaultPossibilitiesBuilder(true) {

			@Override
			public Runner runnerForClass(Class<?> testClass) throws Throwable {
				List<RunnerBuilder> builders = Arrays.asList(
						new RunnerBuilder() {

							@Override
							public Runner runnerForClass(Class<?> testClass)
									throws Throwable {
								Serial annotation = testClass
										.getAnnotation(Serial.class);
								if (annotation != null) {
									return new SerialJunitRunner(testClass);
								}
								return null;
							}
						}, ignoredBuilder(), annotatedBuilder(),
						suiteMethodBuilder(), junit3Builder(), junit4Builder());
				for (RunnerBuilder each : builders) {
					Runner runner = each.safeRunnerForClass(testClass);
					if (runner != null) {
						return runner;
					}
				}
				return null;
			}
		});
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

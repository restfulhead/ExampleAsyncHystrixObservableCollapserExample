package name.ruhkopf.rx.hystrix.example;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

/**
 * This tests demonstrates the use of the Hystrix request log.
 *
 * @author Patrick Ruhkopf
 */
public class IssueWithBackgroundTasks
{
	private static final Logger LOG = LoggerFactory.getLogger(IssueWithBackgroundTasks.class);

	@Before
	public void before()
	{
		ExampleHystrixObservableCollapser.resetCmdCounter();
	}

	/**
	 * Assuming our service layer is reactive and returning a single observable, then in our synchronous http layer we can simply
	 * block before we close the Hystrix context. This works fine.
	 */
	@Test
	public void demonstrateSyncRequest()
	{
		final Observable<?> response = Observable.defer(() -> new ExampleHystrixObservableCollapser(1L).toObservable());

		simulateHttpRequest(response);

		assertThat(ExampleHystrixObservableCollapser.getCmdCount(), is(1));
	}

	/**
	 * However, this pattern fails if we start an asynchronous task within the request. The background thread inherits the Hystrix
	 * Context, however, there's a race condition where the task fails if the request completed before. That's because the request
	 * removes the Hystrix Context.
	 */
	@Test
	public void demonstrateSyncRequestThatSchedulesAnAsyncTask()
	{
		// assuming this is a task that we want to run in the background and can't afford to wait for its result
		final TestSubscriber<String> bgSubscriber = new TestSubscriber<>();
		final Observable<String> bgObs = Observable.defer(() -> {
			sleep(5000); // add some sleep to simulate long running transaction to show race condition
			return new ExampleHystrixObservableCollapser(200L).toObservable();
		});

		final Observable<?> response = Observable.defer(() -> {

			// subscribe to background task, then walk away...
			bgObs.subscribeOn(Schedulers.computation()).subscribe(bgSubscriber);

			// and do something else
			return new ExampleHystrixObservableCollapser(1L).toObservable();
		});

		// this initializes the context, then waits (blocks) for response to complete, then closes the context
		simulateHttpRequest(response);

		// the "foreground" task was executed fine
		assertThat(ExampleHystrixObservableCollapser.getCmdCount(), is(1));

		// however, the "background" task is still running. let's wait for it
		bgSubscriber.awaitTerminalEvent(10, TimeUnit.SECONDS);

		// and print out the error of the background task :(
		bgSubscriber.getOnErrorEvents().stream().forEach(e -> e.printStackTrace());

		// this fails, because we received java.lang.IllegalStateException: HystrixRequestContext.initializeContext() ...
		assertThat(bgSubscriber.getOnErrorEvents().toString(), bgSubscriber.getOnErrorEvents().size(), is(0));
		assertThat(bgSubscriber.getOnNextEvents().size(), is(1));
		assertThat(bgSubscriber.getOnNextEvents().get(0), is("two hundred"));
	}


	/**
	 * In a real project it's common to use the <a
	 * href="https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-request-servlet">Hystrix request servlet
	 * filter</a> to initialize the Hystrix context before the request is processed and shut it down after.
	 * <p>
	 * It's also often the case that you need to deal with synchronous http requests, so a common pattern is to run your bussiness
	 * logic reactively, and the block before returning the request. (see <a
	 * href="https://speakerdeck.com/benjchristensen/applying-reactive-programming-with-rxjava-at-goto-chicago-2015"
	 * >benjchristensen's presentation</a> on this.
	 * <p>
	 * This method simulates this (in a very simplified version ;-))
	 *
	 * @param response Observable coming from the service layer
	 */
	protected void simulateHttpRequest(final Observable<?> response)
	{
		final HystrixRequestContext ctx = HystrixRequestContext.initializeContext();
		try
		{
			response.toBlocking().single();
		}
		finally
		{
			LOG.info(HystrixRequestLog.getCurrentRequest().getExecutedCommandsAsString());
			ctx.shutdown();
		}
	}

	private void sleep(final long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (final Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}

package name.ruhkopf.rx.hystrix.example;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaPlugins;
import rx.schedulers.Schedulers;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.netflix.hystrix.HystrixObservableCollapser;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

/**
 * Test demonstrating how the {@link HystrixObservableCollapser} works. It also shows how an RxJava plugin can be used to make the
 * Collapser work across different threads (asynchronous scheduling).
 *
 * @author Patrick Ruhkopf
 */
public class ExampleHystrixObservableCollapserTest
{
	private static final Logger LOG = LoggerFactory.getLogger(ExampleHystrixObservableCollapserTest.class);

	private HystrixRequestContext ctx;

	@Before
	public void before()
	{
		ctx = HystrixRequestContext.initializeContext();
		ExampleHystrixObservableCollapser.resetCmdCounter();
	}

	@After
	public void after()
	{
		LOG.info(HystrixRequestLog.getCurrentRequest().getExecutedCommandsAsString());
		ctx.shutdown();
	}

	@Test
	public void shouldCollapseRequestsSync() throws InterruptedException
	{
		final int noOfRequests = 1000;
		final Map<Long, TestSubscriber<String>> subscribersByNumber = new HashMap<>(noOfRequests);

		for (long number = 0; number < noOfRequests; number++)
		{
			final TestSubscriber<String> subscriber = new TestSubscriber<>();
			new ExampleHystrixObservableCollapser(number).toObservable().subscribe(subscriber);
			subscribersByNumber.put(number, subscriber);

			// wait a little bit after running half of the requests so that we don't collapse all of them into one batch
			// TODO this can probably be improved by using a test scheduler
			if (number == noOfRequests / 2)
				Thread.sleep(1000);

		}

		assertThat(subscribersByNumber.size(), is(noOfRequests));
		for (final Entry<Long, TestSubscriber<String>> subscriberByNumber : subscribersByNumber.entrySet())
		{
			final TestSubscriber<String> subscriber = subscriberByNumber.getValue();
			subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);

			assertThat(subscriber.getOnErrorEvents().toString(), subscriber.getOnErrorEvents().size(), is(0));
			assertThat(subscriber.getOnNextEvents().size(), is(1));

			final String word = subscriber.getOnNextEvents().get(0);
			LOG.info("Translated {} to {}", subscriberByNumber.getKey(), word);
			assertThat(word, equalTo(numberToWord(subscriberByNumber.getKey())));
		}

		assertThat(ExampleHystrixObservableCollapser.getCmdCount(), greaterThan(1));
		assertThat(ExampleHystrixObservableCollapser.getCmdCount(), lessThan(noOfRequests));
	}

	@Test
	public void shouldCollapseRequestsAsync() throws InterruptedException
	{
		// NOTE: This test requires the RX Plugin, that transfers the Hystrix context.
		// Without it we get IllegalStateException: HystrixRequestContext.initializeContext() must be called ...
		RxJavaPlugins.getInstance().registerSchedulersHook(new HystrixRxJavaSchedulersHook());

		final int noOfRequests = 1000;
		final Map<Long, TestSubscriber<String>> subscribersByNumber = new HashMap<>(noOfRequests);

		for (long number = 0; number < noOfRequests; number++)
		{
			final TestSubscriber<String> subscriber = new TestSubscriber<>();
			final long finalNumber = number;
			Observable.defer(() -> new ExampleHystrixObservableCollapser(finalNumber).toObservable())
					.subscribeOn(Schedulers.computation()).subscribe(subscriber);
			subscribersByNumber.put(number, subscriber);

			// wait a little bit after running half of the requests so that we don't collapse all of them into one batch
			// TODO this can probably be improved by using a test scheduler
			if (number == noOfRequests / 2)
				Thread.sleep(1000);
		}

		assertThat(subscribersByNumber.size(), is(noOfRequests));
		for (final Entry<Long, TestSubscriber<String>> subscriberByNumber : subscribersByNumber.entrySet())
		{
			final TestSubscriber<String> subscriber = subscriberByNumber.getValue();
			subscriber.awaitTerminalEvent();
			// subscriber.awaitTerminalEvent(100, TimeUnit.SECONDS);

			assertThat(subscriber.getOnErrorEvents().toString(), subscriber.getOnErrorEvents().size(), is(0));
			assertThat(subscriber.getOnNextEvents().size(), is(1));

			final String word = subscriber.getOnNextEvents().get(0);
			LOG.info("Translated {} to {}", subscriberByNumber.getKey(), word);
			assertThat(word, equalTo(numberToWord(subscriberByNumber.getKey())));
		}

		assertThat(ExampleHystrixObservableCollapser.getCmdCount(), greaterThan(1));
		assertThat(ExampleHystrixObservableCollapser.getCmdCount(), lessThan(noOfRequests));
	}

	private String numberToWord(final long number)
	{
		return new RuleBasedNumberFormat(new Locale("EN", "US"), RuleBasedNumberFormat.SPELLOUT).format(number);
	}

}

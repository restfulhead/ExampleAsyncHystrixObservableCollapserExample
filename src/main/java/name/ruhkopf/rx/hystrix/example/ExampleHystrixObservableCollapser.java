package name.ruhkopf.rx.hystrix.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Func1;

import com.netflix.hystrix.HystrixCollapser.CollapsedRequest;
import com.netflix.hystrix.HystrixObservableCollapser;
import com.netflix.hystrix.HystrixObservableCommand;

/**
 * HystrixObservableCollapser that batches multiple requests for {@link NumbersToWordsHystrixObservableCommand}.
 *
 * @author Patrick Ruhkopf
 */
public class ExampleHystrixObservableCollapser extends HystrixObservableCollapser<Long, NumberWord, String, Long>
{
	private static final Logger LOG = LoggerFactory.getLogger(ExampleHystrixObservableCollapser.class);

	private final Long number;

	private final static AtomicInteger counter = new AtomicInteger();

	public static void resetCmdCounter()
	{
		counter.set(0);
	}

	public static int getCmdCount()
	{
		return counter.get();
	}

	public ExampleHystrixObservableCollapser(final Long number)
	{
		this.number = number;
	}

	@Override
	public Long getRequestArgument()
	{
		return number;
	}

	@SuppressWarnings("boxing")
	@Override
	protected HystrixObservableCommand<NumberWord> createCommand(final Collection<CollapsedRequest<String, Long>> requests)
	{
		final int count = counter.incrementAndGet();
		LOG.debug("Creating batch for {} requests. Total invocations so far: {}", requests.size(), count);

		final List<Long> numbers = new ArrayList<>();
		for (final CollapsedRequest<String, Long> request : requests)
		{
			numbers.add(request.getArgument());
		}

		return new NumbersToWordsHystrixObservableCommand(numbers);
	}

	@Override
	protected Func1<NumberWord, Long> getBatchReturnTypeKeySelector()
	{
		return (final NumberWord nw) -> nw.getNumber();
	}

	@Override
	protected Func1<Long, Long> getRequestArgumentKeySelector()
	{
		return (final Long no) -> no;
	}

	@Override
	protected Func1<NumberWord, String> getBatchReturnTypeToResponseTypeMapper()
	{
		return (final NumberWord nw) -> nw.getWord();
	}

	@Override
	protected void onMissingResponse(final CollapsedRequest<String, Long> request)
	{
		request.setException(new Exception("No word"));
	}
}

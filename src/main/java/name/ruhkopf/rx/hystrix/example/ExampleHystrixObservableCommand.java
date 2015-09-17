package name.ruhkopf.rx.hystrix.example;

import java.util.List;
import java.util.Locale;

import rx.Observable;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

/**
 * A simple Hystrix Observable command that translates a number (<code>Long</code>) into an English text.
 *
 * @author Patrick Ruhkopf
 */
public class ExampleHystrixObservableCommand extends HystrixObservableCommand<NumberWord>
{
	private final List<Long> numbers;

	public ExampleHystrixObservableCommand(final List<Long> numbers)
	{
		super(HystrixCommandGroupKey.Factory.asKey(ExampleHystrixObservableCommand.class.getName()));
		this.numbers = numbers;
	}

	@Override
	protected Observable<NumberWord> construct()
	{
		final RuleBasedNumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(new Locale("EN", "US"),
				RuleBasedNumberFormat.SPELLOUT);

		return Observable.from(numbers).map(number -> new NumberWord(number, ruleBasedNumberFormat.format(number)));
	}

}

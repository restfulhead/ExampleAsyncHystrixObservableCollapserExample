package name.ruhkopf.rx.hystrix.example;

import rx.functions.Action0;
import rx.plugins.RxJavaSchedulersHook;

import com.netflix.hystrix.strategy.concurrency.HystrixContextScheduler;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

/**
 * RxJava plugin that propagates the {@link HystrixRequestContext} of the calling thread to the thread that executes the action.
 * This is necessary for example to make request collapsing work when scheduling Observables on another thread. As an alternative
 * to this plugin, you can make use of the {@link HystrixContextScheduler}.
 *
 * @author Patrick Ruhkopf
 */
public class HystrixRxJavaSchedulersHook extends RxJavaSchedulersHook
{
	@Override
	public Action0 onSchedule(final Action0 action)
	{
		return super.onSchedule(new WrappedAction0(action, HystrixRequestContext.getContextForCurrentThread()));
	}

	private class WrappedAction0 implements Action0
	{
		private final Action0 actual;
		private final HystrixRequestContext hystrixCtx;

		public WrappedAction0(final Action0 actual, final HystrixRequestContext hystrixCtx)
		{
			this.actual = actual;
			this.hystrixCtx = hystrixCtx;
		}

		@Override
		public void call()
		{
			HystrixRequestContext.setContextOnCurrentThread(hystrixCtx);

			try
			{
				actual.call();
			}
			finally
			{
				HystrixRequestContext.setContextOnCurrentThread(null);
			}

		}
	}
}

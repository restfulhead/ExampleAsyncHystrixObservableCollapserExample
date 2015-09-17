The idea for this basic example project came out of a discussion in https://github.com/Netflix/Hystrix/issues/895. The `ExampleHystrixObservableCollapserTest` demonstrates how to use Hystrix request collapsing with Observables (`HystrixObservableCollapser`).

The key take-aways are:
* You need to call 'HystrixRequestContext.initializeContext()' before and `HystrixRequestContext.shutdown()` after the request.
* When subscribing to the observable on the same thread, everything works out of the box
* When subscribing on another thread (asynchronously), you need to make that thread aware of the Hystrix Context
* This can be done in a generic way by implementing a `RxJavaSchedulersHook`

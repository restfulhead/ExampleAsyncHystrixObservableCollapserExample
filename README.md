The idea for this basic example project came out of a [discussion in the Hystrix repo](https://github.com/Netflix/Hystrix/issues/895). The `ExampleHystrixObservableCollapserTest` demonstrates how to use [Hystrix request collapsing](ReactiveLab/reactive-lab-edge/src/main/java/io/reactivex/lab/edge/clients/BookmarkCommand.java) with [Rx Observables](http://reactivex.io/intro.html) (`HystrixObservableCollapser`).

The key take-aways are:
* You need to call 'HystrixRequestContext.initializeContext()' before and `HystrixRequestContext.shutdown()` after the request.
* When subscribing to the observable on the same thread, everything works out of the box
* When subscribing on another thread (asynchronously), you need to make that thread aware of the Hystrix Context
* This can be done in a generic way by implementing a `RxJavaSchedulersHook`

As for the "business logic": The `ExampleHystrixObservableCommand` simply takes a number (e.g. `100`) and translates into English words (e.g. `one hundred`). This is done by the awsome [ICU](http://site.icu-project.org/) library. The `ExampleHystrixObservableCollapser` then batches multiple requests to translate multiple numbers at once.

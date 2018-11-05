# KotlinPromise

[![GitHub release](https://img.shields.io/github/release/moesama/KotlinPromise.svg)](https://github.com/moesama/KotlinPromise) [![GitHub license](https://img.shields.io/github/license/moesama/KotlinPromise.svg)](https://github.com/moesama/KotlinPromise/blob/master/LICENSE)

A kotlin library which provides promise apis just like javascript.

## Implementation

```groovy
implementation 'io.github.moesama:kotlin-promise:1.0.0'
```



## Keywords

### origin:

​	Data that the promise receives from outter side(Maybe from the execute method or a prev promise);

### resolve:

​	Make a success return;

### reject:

​	Make a failed return (Throwable);



## Usage

### Create and use a normal promise:

```kotlin
val promise = promise<Int> { resolve, reject ->
	// do something sync or async
    resolve(100)
    // catch a Exception
    reject(exception)
}.then { value ->
    // do something after value returns (here value is 100)
    doSomething(value)
}.catch { throwable ->
    // do something when catches an exception
    doSomething(throwable)
}.finally {
    // do something in the end whatever the status is except canceled
    doSomething()
}
// execute a promise
promise.execute()
```



### Next:

```kotlin
// promisePrev is a promise that has a return type
// origin's type is the same as promisePrev's return type
val promise = promisePrev.next<Int> { origin, resolve, reject ->
    // origin's type is promiseParent's return type
    resolve(origin)
}

// promiseNext is a promise which has the same return type as promisePrev
val promise2 = promisePrev.next(promiseNext)
```



### Trans:

```kotlin
// promisePrev is a promise that has a return type
val promise = promiseParent.trans<Int> { resolve, reject ->
    // origin's type is promiseParent's return type
    resolve(1)
}

// promiseNext is a promise which has the a "Unit" accept type
val promise2 = promisePrev.trans(promiseNext)
```



### Execute:

```kotlin
// promisePrev is a promise that has a return type
val promise = promiseParent.trans<Int> { resolve, reject ->
    // origin's type is promiseParent's return type
    resolve(1)
}

// promiseNext is a promise which has the a "Unit" accept type
val promise2 = promisePrev.trans(promiseNext)
```



### Cancel:

```kotlin
// when a promise has been canceled, it's finally callback will not proceed any more
promise.cancel()
```



### All:

The **Promise.all(vararg promises: Promise)** method returns a single Promise that resolves when all of the promises in the vararg argument have resolved or when the iterable argument contains no promises. It rejects with the reason of the first promise that rejects.

```kotlin
val promise = promiseAll(promise1, promise2, promise3)
```



### Race:

The **Promise.race(vararg promises: Promise)** method returns a promise that resolves or rejects as soon as one of the promises in the iterable resolves or rejects, with the value or reason from that promise.

```kotlin
val promise = promiseRace(promise1, promise2, promise3)
```



### From:

```kotlin
val promisInt = Promise.from(1)
val promiseString = Promise.from("1")
```






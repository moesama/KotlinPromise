package io.github.moesama.promise

import java.lang.ref.WeakReference

class Promise<V, T> private constructor(
    private val mRunner: ((origin: V?, resolve: (T?) -> Unit, reject: (Throwable) -> Unit) -> Unit)
) {

    private val mThen = ArrayList<WeakReference<(T?) -> Unit>>()
    private val mCatch = ArrayList<WeakReference<(Throwable) -> Unit>>()
    private val mFinally = ArrayList<WeakReference<() -> Unit>>()

    private var mFinal: Boolean = true

    var status = Status.IDLE
        private set

    private val doResolve: (value: T?) -> Unit = { value ->
        if (status == Status.PROCESSING) {
            status = Status.COMPLETE
            mThen.forEach { reference -> reference.get()?.let { it(value) } }
            if (mFinal) {
                doFinal()
            }
        }
    }

    private val doReject: (throwable: Throwable) -> Unit = { throwable ->
        if (status == Status.PROCESSING) {
            status = Status.ERROR
            mCatch.forEach { reference -> reference.get()?.let { it(throwable) } }
            if (mFinal) {
                doFinal()
            }
        }
    }

    private val doFinal: () -> Unit = {
        mFinally.forEach { reference -> reference.get()?.let { it() } }
        status = Status.IDLE
    }

    fun then(callback: (T?) -> Unit): Promise<V, T> {
        mThen.add(WeakReference(callback))
        return this
    }

    fun catch(callback: (Throwable) -> Unit): Promise<V, T> {
        mCatch.add(WeakReference(callback))
        return this
    }

    fun finally(callback: () -> Unit): Promise<V, T> {
        mFinally.add(WeakReference(callback))
        return this
    }

    private fun prependFinally(callback: () -> Unit): Promise<V, T> {
        mFinally.add(0, WeakReference(callback))
        return this
    }

    fun <P> next(runner: (origin: T?, resolve: (P?) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T, P> {
        mFinal = false
        return Promise.create<T, P>(runner).also { _next ->
            then { _next.execute(it) }.catch {
                _next.doReject(it)
            }
        }.prependFinally(doFinal)
    }

    fun <P> next(promise: Promise<T, P>): Promise<T, P> {
        mFinal = false
        return promise.also { _next ->
            then {
                _next.execute(it)
            }.catch {
                _next.doReject(it)
            }
        }.prependFinally(doFinal)
    }

    fun <P> trans(runner: (resolve: (P?) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<Unit, P> {
        mFinal = false
        return Promise.create(runner).also { _next ->
            then {
                _next.execute()
            }.catch {
                _next.doReject(it)
            }
        }.prependFinally(doFinal)
    }

    fun <P> trans(promise: Promise<Unit, P>): Promise<Unit, P> {
        mFinal = false
        return promise.also { _next ->
            then {
                _next.execute()
            }.catch {
                _next.doReject(it)
            }
        }.prependFinally(doFinal)
    }

    fun execute(value: V? = null) {
        status = Status.PROCESSING
        mRunner(value, doResolve, doReject)
    }

    fun cancel() {
        status = Status.CANCELED
    }

    companion object {
        @JvmStatic
        fun <T> all(vararg promises: Promise<T, *>): Promise<T, Array<Any?>> {
            return Promise<T, Array<Any?>> { origin, resolve, reject ->
                val result = arrayOfNulls<Any>(promises.size)
                var count = 0
                promises.forEachIndexed { index, promise ->
                    promise.mFinal = false
                    promise.then {
                        result[index] = it
                        count++
                        if (count == result.size) {
                            resolve(result)
                        }
                    }.catch { throwable ->
                        reject(throwable)
                        promises.forEach { it.cancel() }
                    }.execute(origin)
                }
            }.also { promise ->
                promises.forEach { promise.prependFinally(it.doFinal) }
            }
        }

        @JvmStatic
        fun <T> race(vararg promises: Promise<T, *>): Promise<T, Any> {
            return Promise<T, Any> { origin, resolve, reject ->
                promises.forEach { promise ->
                    promise.mFinal = false
                    promise.then { any ->
                        resolve(any)
                        promises.forEach { it.cancel() }
                    }.catch { throwable ->
                        reject(throwable)
                        promises.forEach { it.cancel() }
                    }.execute(origin)
                }
            }.also { promise ->
                promises.forEach { promise.prependFinally(it.doFinal) }
            }
        }

        @JvmStatic
        fun <T> from(value: T): Promise<Unit, T> = promise { resolve, _ -> resolve(value) }

        @JvmStatic
        fun <T> create(block: (resolve: (T?) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<Unit, T> {
            return Promise { _, resolve, reject ->
                block(resolve, reject)
            }
        }

        @JvmStatic
        fun <V, T> create(block: (origin: V?, resolve: (T?) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<V, T> {
            return Promise(block)
        }
    }

    enum class Status {
        IDLE, PROCESSING, COMPLETE, ERROR, CANCELED
    }
}

fun <T> promise(block: (resolve: (T?) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<Unit, T> =
    Promise.create(block)

fun <V, T> promise(block: (origin: V?, resolve: (T?) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<V, T> =
    Promise.create(block)

fun <T> promiseAll(vararg promises: Promise<T, Any>): Promise<T, Array<Any?>> = Promise.all(*promises)
fun <T> promiseRace(vararg promises: Promise<T, Any>): Promise<T, Any> = Promise.race(*promises)
fun <T> promise(value: T): Promise<Unit, T> = Promise.from(value)
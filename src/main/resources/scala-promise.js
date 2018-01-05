'use strict';

var Duration = Packages.scala.concurrent.duration.Duration;
var Function = Packages.akka.japi.Function;
var Callable = Packages.java.util.concurrent.Callable;
var future = Packages.akka.dispatch.Futures.future;
var SECONDS = Packages.java.util.concurrent.TimeUnit.SECONDS;
var dispatcher = Packages.scala.concurrent.ExecutionContext$.MODULE$.global();

var setTimeout = function (fn, millis /* [, args...] */) {
    print ("setTimeout");
    future(fn, dispatcher);
};

var PENDING = 0;
var FULFILLED = 1;
var REJECTED = 2;

var Fallback = {
    onFulfilled: function (v) {
        return v;
    },
    onRejected: function (r) {
        throw r;
    }
};

function Promise(resolver) {
    var state = PENDING;
    var value = null;

    var callbacks = [];

    var self = this;

    function resolve(x) {
        if (self === x) {
            reject(new TypeError('Promise and x refer to the same object.'));
        } else if (typeof x === 'object' || typeof x === 'function') {
            var called = false;
            var then = null;
            try {
                if (x && typeof (then = x.then) === 'function') {
                    then.bind(x)(function resolvePromise(y) {
                        if (called) return;
                        called = true;
                        resolve(y);
                    }, function rejectPromise(r) {
                        if (called) return;
                        called = true;
                        reject(r);
                    });
                } else {
                    fulfill(x);
                }
            } catch (e) {
                if (!called) reject(e);
            }
        } else {
            fulfill(x);
        }
    }

    function fulfill(v) {
        if (state === PENDING) {
            state = FULFILLED;
            value = v;
            done();
        }
    }

    function reject(r) {
        if (state === PENDING) {
            state = REJECTED;
            value = r;
            done();
        }
    }

    function done() {
        if (state === PENDING) return;
        setTimeout(function () {
            while (callbacks.length) {
                var callback = callbacks.shift();
                var resolveThat = callback.resolveThat;
                var rejectThat = callback.rejectThat;

                var handler = state === FULFILLED ? callback.onFulfilled : callback.onRejected;
                var fallback = state === FULFILLED ? Fallback.onFulfilled : Fallback.onRejected;
                handler = handler || fallback;
                try {
                    resolveThat(handler(value));
                } catch (e) {
                    rejectThat(e);
                }
            }
        }, 0);
    }

    function promise(onFulfilled, onRejected, resolveThat, rejectThat) {
        callbacks.push({
            onFulfilled: onFulfilled,
            onRejected: onRejected,
            resolveThat: resolveThat,
            rejectThat: rejectThat
        });
        done();
    };

    Object.defineProperty(this, 'promise', {
        enumerable: false,
        configurable: false,
        writable: false,
        value: promise
    });
    //execute the resolver
    try {
        resolver(resolve, reject);
    } catch (e) {
        reject(e);
    }
}

Promise.prototype.then = function (onFulfilled, onRejected) {
    var that = this;
    onFulfilled = typeof onFulfilled === 'function' ? onFulfilled : null;
    onRejected = typeof onRejected === 'function' ? onRejected : null;
    return new Promise(function (resolve, reject) {
        //register `new` promise's `resolve` and `reject`
        //in order to observe `this` promise's state change.
        that.promise(onFulfilled, onRejected, resolve, reject);
    });
};

Promise.prototype.catch = function (onRejected) {
    this.then(void 0, onRejected);
};

Promise.all = function (iterable) {
};

Promise.race = function (iterable) {
};

Promise.reject = function (reason) {
    return new Promise(function (resolve, reject) {
        reject(reason);
    });
};

Promise.resolve = function (value) {
    return new Promise(function (resolve, reject) {
        resolve(value);
    });
};

//module.exports = Promise;
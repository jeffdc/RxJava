package rx.operators;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class OperationBuffer {

    public static <T> Func1<Observer<Iterable<T>>, Subscription> buffer(Observable<T> that, Integer count) {
        return new Buffer<T>(that, count);
    }

    private static class Buffer<T> implements Func1<Observer<Iterable<T>>,Subscription> {

        private final Observable<T> that;
        private final Integer count;
        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        private final List<T> buffer;
        private boolean done;

        Buffer(Observable<T> that, Integer count) {
            this.that = that;
            this.count = count;
            this.buffer = new ArrayList<T>(count);
        }

        @Override
        public Subscription call(final Observer<Iterable<T>> observer) {
            return subscription.wrap(that.subscribe(new Observer<T>() {

                public synchronized void onNext(T t) {
                    buffer.add(t);
                    if (buffer.size() >= count) {
                        List<T> tmp = new ArrayList<T>(buffer);
                        buffer.clear();
                        observer.onNext(tmp);
                    }
                }

                public synchronized void onCompleted() {
                    if (!buffer.isEmpty()) {
                        List<T> tmp = new ArrayList<T>(buffer);
                        buffer.clear();
                        observer.onNext(tmp);
                    }
                    observer.onCompleted();
                }

                public synchronized void onError(Exception e) {
                    buffer.clear();
                    observer.onError(e);
                }
            }));
        }
    }

    public static class UnitTest {

        @Test
        public void testBuffer() {
            Observable<String> w = Observable.toObservable("one", "two", "three", "four");
            Observable<Iterable<String>> observable = Observable.create(buffer(w, 2));

            Observer<Iterable<String>> observer = mock(Observer.class);
            observable.subscribe(observer);
            verify(observer, Mockito.never()).onNext(Arrays.asList("one"));
            verify(observer, times(1)).onNext(Arrays.asList("one", "two"));
            verify(observer, Mockito.never()).onNext(Arrays.asList("one", "two", "three"));
            verify(observer, times(1)).onNext(Arrays.asList("three", "four"));
            verify(observer, Mockito.never()).onError(any(Exception.class));
            verify(observer, times(1)).onCompleted();
        }
    }
}

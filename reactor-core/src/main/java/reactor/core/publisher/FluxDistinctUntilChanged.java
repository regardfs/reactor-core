/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.publisher;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.annotation.Nullable;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Fuseable.ConditionalSubscriber;
import reactor.util.context.Context;

/**
 * Filters out subsequent and repeated elements.
 *
 * @param <T> the value type
 * @param <K> the key type used for comparing subsequent elements
 * @see <a href="https://github.com/reactor/reactive-streams-commons">Reactive-Streams-Commons</a>
 */
final class FluxDistinctUntilChanged<T, K> extends FluxOperator<T, T> {

	final Function<? super T, K>            keyExtractor;
	final BiPredicate<? super K, ? super K> keyComparator;

	FluxDistinctUntilChanged(Flux<? extends T> source,
			Function<? super T, K> keyExtractor,
			BiPredicate<? super K, ? super K> keyComparator) {
		super(source);
		this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
		this.keyComparator = Objects.requireNonNull(keyComparator, "keyComparator");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void subscribe(Subscriber<? super T> s, Context ctx) {
		if (s instanceof ConditionalSubscriber) {
			source.subscribe(new DistinctUntilChangedConditionalSubscriber<>((ConditionalSubscriber<? super T>) s,
					keyExtractor, keyComparator), ctx);
		}
		else {
			source.subscribe(new DistinctUntilChangedSubscriber<>(s, keyExtractor, keyComparator), ctx);
		}
	}

	static final class DistinctUntilChangedSubscriber<T, K>
			implements ConditionalSubscriber<T>, InnerOperator<T, T> {
		final Subscriber<? super T> actual;

		final Function<? super T, K> keyExtractor;
		final BiPredicate<? super K, ? super K> keyComparator;

		Subscription s;

		boolean done;

		K lastKey;

		DistinctUntilChangedSubscriber(Subscriber<? super T> actual,
				Function<? super T, K> keyExtractor,
				BiPredicate<? super K, ? super K> keyComparator) {
			this.actual = actual;
			this.keyExtractor = keyExtractor;
			this.keyComparator = keyComparator;
		}

		@Override
		public void onSubscribe(Subscription s) {
			if (Operators.validate(this.s, s)) {
				this.s = s;

				actual.onSubscribe(this);
			}
		}

		@Override
		public void onNext(T t) {
			if (!tryOnNext(t)) {
				s.request(1);
			}
		}

		@Override
		public boolean tryOnNext(T t) {
			if (done) {
				Operators.onNextDropped(t);
				return true;
			}

			K k;

			try {
				k = Objects.requireNonNull(keyExtractor.apply(t),
				"The distinct extractor returned a null value.");
			}
			catch (Throwable e) {
				onError(Operators.onOperatorError(s, e, t));
				return true;
			}

			if (null == lastKey) {
				lastKey = k;
				actual.onNext(t);
				return true;
			}

			boolean equiv;

			try {
				equiv = keyComparator.test(lastKey, k);
			}
			catch (Throwable e) {
				onError(Operators.onOperatorError(s, e, t));
				return true;
			}

			if (equiv) {
				return false;
			}

			lastKey = k;
			actual.onNext(t);
			return true;
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t);
				return;
			}
			done = true;

			actual.onError(t);
		}

		@Override
		public void onComplete() {
			if (done) {
				return;
			}
			done = true;

			actual.onComplete();
		}

		@Override
		@Nullable
		public Object scanUnsafe(Attr key) {
			if (key == ScannableAttr.PARENT) return s;
			if (key == BooleanAttr.TERMINATED) return done;

			return InnerOperator.super.scanUnsafe(key);
		}

		@Override
		public Subscriber<? super T> actual() {
			return actual;
		}

		@Override
		public void request(long n) {
			s.request(n);
		}

		@Override
		public void cancel() {
			s.cancel();
		}
	}

	static final class DistinctUntilChangedConditionalSubscriber<T, K>
			implements ConditionalSubscriber<T>, InnerOperator<T, T> {
		final ConditionalSubscriber<? super T> actual;

		final Function<? super T, K> keyExtractor;
		final BiPredicate<? super K, ? super K> keyComparator;

		Subscription s;

		boolean done;

		K lastKey;

		DistinctUntilChangedConditionalSubscriber(ConditionalSubscriber<? super T> actual,
				Function<? super T, K> keyExtractor,
				BiPredicate<? super K, ? super K> keyComparator) {
			this.actual = actual;
			this.keyExtractor = keyExtractor;
			this.keyComparator = keyComparator;
		}

		@Override
		public void onSubscribe(Subscription s) {
			if (Operators.validate(this.s, s)) {
				this.s = s;

				actual.onSubscribe(this);
			}
		}

		@Override
		public void onNext(T t) {
			if (!tryOnNext(t)) {
				s.request(1);
			}
		}

		@Override
		public boolean tryOnNext(T t) {
			if (done) {
				Operators.onNextDropped(t);
				return true;
			}

			K k;

			try {
				k = Objects.requireNonNull(keyExtractor.apply(t),
				"The distinct extractor returned a null value.");
			}
			catch (Throwable e) {
				onError(Operators.onOperatorError(s, e, t));
				return true;
			}

			if (null == lastKey) {
				lastKey = k;
				return actual.tryOnNext(t);
			}

			boolean equiv;

			try {
				equiv = keyComparator.test(lastKey, k);
			}
			catch (Throwable e) {
				onError(Operators.onOperatorError(s, e, t));
				return true;
			}

			if (equiv) {
				return false;
			}

			lastKey = k;
			return actual.tryOnNext(t);
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t);
				return;
			}
			done = true;

			actual.onError(t);
		}

		@Override
		public void onComplete() {
			if (done) {
				return;
			}
			done = true;

			actual.onComplete();
		}

		@Override
		@Nullable
		public Object scanUnsafe(Attr key) {
			if (key == ScannableAttr.PARENT) return s;
			if (key == BooleanAttr.TERMINATED) return done;

			return InnerOperator.super.scanUnsafe(key);
		}

		@Override
		public Subscriber<? super T> actual() {
			return actual;
		}

		@Override
		public void request(long n) {
			s.request(n);
		}

		@Override
		public void cancel() {
			s.cancel();
		}
	}

}

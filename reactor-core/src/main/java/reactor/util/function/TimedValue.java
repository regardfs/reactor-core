/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.util.function;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * A {@link Tuple2} that contains a {@link Long} t1 (a timing measurement of some sort)
 * and a T t2 (the associated value). This variant enforces both values to be non-null
 * (the timing is provided as a {@code long} and the value is checked for nulls).
 *
 * It exposes the timing and value through meaningful getters: {@link #getTiming()} and
 * {@link #getValue()}.
 *
 * @author Simon Baslé
 */
public class TimedValue<T> extends Tuple2<Long, T> {

	/**
	 * Construct a new {@link TimedValue}.
	 *
	 * @param timing the timing to associate with the value
	 * @param value the value that was somehow timed, not null.
	 */
	public TimedValue(long timing, T value) {
		super(timing, Objects.requireNonNull(value, "value must not be null")
		);
	}

	/**
	 * @return the timing associated with the value
	 * @see #getT1()
	 */
	public Long getTiming() {
		if (t1 == null) {
			throw new IllegalStateException("TimedValue has a null timing");
		}
		return t1;
	}

	/**
	 * @return the value that was timed
	 * @see #getT2()
	 */
	public T getValue() {
		if (t2 == null) {
			throw new IllegalStateException("TimedValue has a null value");
		}
		return t2;
	}

	/**
	 * @deprecated prefer using more meaningful {@link #getTiming()}
	 */
	@Override
	@Deprecated
	@Nonnull
	public Long getT1() {
		return getTiming();
	}

	/**
	 * @deprecated prefer using more meaningful {@link #getValue()}
	 */
	@Override
	@Deprecated
	@Nonnull
	public T getT2() {
		return getValue();
	}
}

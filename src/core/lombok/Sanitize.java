/*
 * Copyright © 2011 Philipp Eichhorn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.Normalizer;

/**
 * Explicitly turns on sanitation for all method
 * parameter annotated with {@code @Sanitize.With("methodname")} and
 * {@code @Sanitize.Normalize}.
 * <p>
 * <b>Note:</b> All lombok-pg method-level annotations automatically
 * trigger a parameter sanitation.
 */
public @interface Sanitize {

	/**
	 * Method that should be used to sanitize the parameter.
	 * <p>
	 * <b>Note:</b> This works with all types, but the parameter type
	 * has to match the method signature.
	 */
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.SOURCE)
	public static @interface With {
		String value();
	}

	/**
	 * {@link String} parameter gets normalized using
	 * {@link Normalizer#normalize(CharSequence, Normalizer.Form)}
	 * with default form being {@link Normalizer.Form#NFKC NFKC}
	 * <p>
	 * <b>Note:</b> This works with only on {@link String Strings}.
	 */
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.SOURCE)
	public static @interface Normalize {
		Normalizer.Form value() default Normalizer.Form.NFKC;
	}
}

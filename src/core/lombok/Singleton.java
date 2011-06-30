/*
 * Copyright © 2010-2011 Philipp Eichhorn
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

/**
 * The nicest singleton templates of them all.
 * <p>
 * Although not much code is generated the annotation also acts as documentation.
 * <p>
 * Before:
 * <pre>
 * &#64;Singleton
 * class MySingleton {
 *   public MySingleton() {
 *   }
 * }
 * </pre>
 * After:
 * <pre>
 * enum MySingleton {
 *   INSTANCE;
 *   MySingleton() {
 *   }
 *
 *   public static MySingleton getInstance() {
 *     return INSTANCE;
 *   }
 * }
 * </pre>
 * <p>
 * <b>Note:</b> If you don't like the enum-approach, try the classic singleton using<br>
 * {@code @Singleton(style = Singleton.Style.HOLDER)}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Singleton {
	Style style() default Style.ENUM;

	public static enum Style {
		ENUM, HOLDER;
	}
}

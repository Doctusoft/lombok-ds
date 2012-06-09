/*
 * Copyright © 2010-2012 Philipp Eichhorn
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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.*;

/**
 * Two popular singleton templates.
 * <p>
 * Since not much code is generated, the annotation also acts as documentation.
 * <p>
 * With lombok:
 * 
 * <pre>
 * &#064;Singleton
 * class MySingleton {
 * 	public MySingleton() {
 * 	}
 * }
 * </pre>
 * 
 * Vanilla Java:
 * 
 * <pre>
 * enum MySingleton {
 * 	INSTANCE;
 * 	MySingleton() {
 * 	}
 * 
 * 	public static MySingleton getInstance() {
 * 		return INSTANCE;
 * 	}
 * }
 * </pre>
 * <p>
 * <b>Note:</b> If you don't like the enum approach, try the holder approach using<br>
 * <code>@Singleton(style = {@link Singleton.Style#HOLDER})</code>.
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface Singleton {
	/** Specifies the singleton-style to use, default is ENUM. */
	Style style() default Style.ENUM;

	public static enum Style {
		/**
		 * <pre>
		 * enum SingletonEnumExample {
		 * 	INSTANCE;
		 * 
		 * 	public static SingletonEnumExample getInstance() {
		 * 		return INSTANCE;
		 * 	}
		 * }
		 * </pre>
		 */
		ENUM,
		/**
		 * <pre>
		 * class SingletonHolderExample {
		 * 
		 * 	private static class SingletonHolderExampleHolder {
		 * 		private static final SingletonHolderExample INSTANCE = new SingletonHolderExample();
		 * 	}
		 * 
		 * 	public static SingletonHolderExample getInstance() {
		 * 		return SingletonHolderExampleHolder.INSTANCE;
		 * 	}
		 * }
		 * </pre>
		 */
		HOLDER;
	}
}

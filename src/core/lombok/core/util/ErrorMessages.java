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
package lombok.core.util;

import java.lang.annotation.Annotation;

/**
 * Error messages
 */
public final class ErrorMessages {
	private ErrorMessages() {
	}
	
	public static String canBeUsedOnConcreteClassOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on concrete classes only", annotationType);
	}
	
	public static String canBeUsedOnClassOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on classes only", annotationType);
	}
	
	public static String canBeUsedOnClassAndEnumOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on classes and enums only", annotationType);
	}	
	
	public static String canBeUsedOnClassAndFieldOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on classes and fields only", annotationType);
	}	

	public static String canBeUsedOnFieldOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on fields only", annotationType);
	}
	
	public static String canBeUsedOnMethodOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on methods only", annotationType);
	}
	
	public static String canBeUsedOnConcreteMethodOnly(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s can be used on concrete methods only", annotationType);
	}
	
	public static String requiresDefaultOrNoArgumentConstructor(Class<? extends Annotation> annotationType) {
		return errorMessage("@%s requires a default or no-argument constructor", annotationType);
	}
	
	public static String unsupportedExpressionIn(String where, Object expr) {
		return String.format("Unsupported Expression in '%s': %s", where, expr);
	}
	
	public static String isNotAllowedHere(String what) {
		return String.format("'%s' is not allowed here.", what);
	}
	
	public static String firstArgumentCanBeVariableNameOrNewClassStatementOnly(String what) {
		return String.format("The first argument of '%s' can be variable name or new-class statement  only", what);
	}

	private static String errorMessage(String format, Class<? extends Annotation> annotationType) {
		return String.format(format, annotationType.getName());
	}
}
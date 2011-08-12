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

import static java.lang.Character.*;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Names {
	
	public static String string(final char[] s) {
		return new String(s);
	}
	
	public static String string(final Object s) {
		return s.toString();
	}
	/**
	 * <pre>
	 * null        -> null
	 *             ->
	 * IOInterface -> IOInterface
	 * Irony       -> Irony
	 * IObject     -> Object
	 *
	 * {@code [I]([A-Z][a-z].*)}
	 * <pre>
	 */
	public static String interfaceName(final String s) {
		if (isEmpty(s) || (s.length() <= 2)) return s;
		return ((s.charAt(0) == 'I') && isUpperCase(s.charAt(1)) && isLowerCase(s.charAt(2))) ? s.substring(1) : s;
	}

	public static String decapitalize(final String s) {
		if (s == null) return "";
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	public static String capitalize(final String s) {
		if (s == null) return "";
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static boolean isEmpty(final String s) {
		if (s == null) return true;
		return s.isEmpty();
	}

	public static boolean isNotEmpty(final String s) {
		return !isEmpty(s);
	}

	public static boolean isOneOf(final String s, final String... candidates) {
		if (candidates != null) for (String candidate : candidates) {
			if (candidate.equals(s)) return true;
		}
		return false;
	}

	/**
	 * <pre>
	 * null  ->
	 *       ->
	 *   s   -> s
	 * </pre>
	 */
	public static String trim(final String s) {
		if (s == null) return "";
		else return s.trim();
	}

	public static String singular(final String s) {
		return s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
	}

	/**
	 * Creates the name of the constant that holds the name of a property. For
	 * example, if the name of a property is "firstName," this method will
	 * return "PROP_FIRST_NAME."
	 */
	public static String nameOfConstantBasedOnProperty(final String propertyName) {
		char[] chars = propertyName.toCharArray();
		StringBuilder b = new StringBuilder();
		b.append("PROP_");
		int charCount = chars.length;
		for (int i = 0; i < charCount; i++) {
			char c = chars[i];
			if (isUpperCase(c) && i > 0)
				b.append('_');
			if (isLowerCase(c))
				c = toUpperCase(c);
			b.append(c);
		}
		return b.toString();
	}

	public static String camelCase(final String first, final String... rest) {
		List<String> nonEmptyStrings = new ArrayList<String>();
		if (isNotEmpty(first)) nonEmptyStrings.add(first);
		if (Arrays.isNotEmpty(rest)) for (String s : rest) {
			if (isNotEmpty(s)) nonEmptyStrings.add(s);
		}
		return camelCase0(nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]));
	}

	private static String camelCase0(final String[] s) {
		if (Arrays.isEmpty(s)) return "";
		StringBuilder builder = new StringBuilder();
		builder.append(s[0]);
		for (int i = 1, iend = s.length; i < iend; i++) {
			builder.append(capitalize(s[i]));
		}
		return builder.toString();
	}
}

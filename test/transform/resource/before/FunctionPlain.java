import lombok.Functions.Function1;
import lombok.Function;

class FunctionPlain {
	
	public void test() {
		Float foo = 1.618F;
		notNull(foo, sqrt());
	}

	@Function
	public static boolean startsWith(String string, String _prefix) {
		return string.startsWith(_prefix);
	}

	@Function
	public static float sqrt(float f) {
		return (float) Math.sqrt(f);
	}

	@Function
	public static <T> void notNull(T object, Function1<T, Void> notNullFunction) {
		if (object != null) notNullFunction.apply(object);
	}
}
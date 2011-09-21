import lombok.Functions.Function1;

class FunctionPlain {
	
	public void test() {
		Float foo = 1.618F;
		notNull(foo, sqrt());
	}
	
	@java.lang.SuppressWarnings("all")
	public static lombok.Functions.Function1<String, java.lang.Boolean> startsWith(final String _prefix) {
		return new lombok.Functions.Function1<String, java.lang.Boolean>(){
			public java.lang.Boolean apply(final String string) {
				return string.startsWith(_prefix);
			}
		};
	}
	
	@java.lang.SuppressWarnings("all")
	public static lombok.Functions.Function1<java.lang.Float, java.lang.Float> sqrt() {
		return new lombok.Functions.Function1<java.lang.Float, java.lang.Float>(){
			public java.lang.Float apply(final java.lang.Float f) {
				return (float)Math.sqrt(f);
			}
		};
	}
	
	@java.lang.SuppressWarnings("all")
	public static <T>lombok.Functions.Function2<T, Function1<T, Void>, java.lang.Void> notNull() {
		return new lombok.Functions.Function2<T, Function1<T, Void>, java.lang.Void>(){
			public java.lang.Void apply(final T object, final Function1<T, Void> notNullFunction) {
				if (object != null) notNullFunction.apply(object);
				return null;
			}
		};
	}
}
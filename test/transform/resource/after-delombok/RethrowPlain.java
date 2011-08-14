class RethrowPlain {

	@java.lang.SuppressWarnings("all")
	void testRethrowAs() {
		try {
			System.out.println("code that throws FileNotFoundException");
		} catch (final java.io.FileNotFoundException $e1) {
			throw new java.lang.IllegalArgumentException($e1);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testRethrowAsRuntimeException() {
		try {
			System.out.println("code that might throw InterruptedException due to cancelation");
		} catch (final java.lang.InterruptedException $e1) {
			throw new java.lang.RuntimeException($e1);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testRethrowEveryExceptionAsSpecifiedException(final String arg) {
		try {
			if (arg == null || arg.isEmpty()) {
				throw new java.lang.IllegalArgumentException("The validated object is empty");
			}
			System.out.println("code throws all kinds of Exceptions");
		} catch (final java.lang.RuntimeException $e1) {
			throw $e1;
		} catch (final java.lang.Exception $e2) {
			throw new java.lang.IllegalArgumentException(java.lang.String.format("meh."), $e2);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testFullyCustomizedRethrow() {
		try {
			System.out.println("code that throws FileNotFoundException and IOException");
		} catch (final java.io.FileNotFoundException $e1) {
			throw new java.lang.IllegalArgumentException($e1);
		} catch (final java.io.IOException $e2) {
			throw new java.lang.RuntimeException($e2);
		}
	}

	@java.lang.SuppressWarnings("all")
	void testExceptionsInSanitizeAlsoGetRethrown(final String arg) {
		try {
			final String sanitizedArg = filterArg(arg);
			System.out.println("code throws all kinds of Exceptions");
		} catch (final java.lang.RuntimeException $e1) {
			throw $e1;
		} catch (final java.lang.Exception $e2) {
			throw new java.lang.IllegalArgumentException(java.lang.String.format("%s meh.", arg), $e2);
		}
	}
	
	String filterArg(final String arg) throws Exception {
		throw new Exception();
	}
}
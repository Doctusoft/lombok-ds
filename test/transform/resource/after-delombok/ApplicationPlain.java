import java.lang.instrument.Instrumentation;

class ApplicationPlain1 {
	public void runApp(String[] args) throws Throwable {
	}
	@java.lang.SuppressWarnings("all")
	public static void main(final java.lang.String[] args) throws java.lang.Throwable {
		new ApplicationPlain1().runApp(args);
	}
}
class ApplicationPlain2 {
	@java.lang.SuppressWarnings("all")
	public static void main(final java.lang.String[] args) throws java.lang.Throwable {
		new ApplicationPlain2().runApp(args);
	}
}
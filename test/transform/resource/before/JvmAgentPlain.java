import lombok.JvmAgent;
import java.lang.instrument.Instrumentation;

class JvmAgentPlain implements JvmAgent {
	JvmAgentPlain() {
		super();
	}
	public void runAgent(boolean injected, String params, Instrumentation instrumentation) throws Throwable {

	}
}

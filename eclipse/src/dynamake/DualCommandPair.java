package dynamake;

import java.util.Date;

public class DualCommandPair<T> implements DualCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Command<T> forward;
	private Command<T> backward;
	
	public DualCommandPair(Command<T> forward, Command<T> backward) {
		this.forward = forward;
		if(backward == null) {
//			new String();
			System.out.println(forward);
		}
		this.backward = backward;
	}

	@Override
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch) {
		forward.executeOn(propCtx, prevalentSystem, executionTime, branch);
	}

	@Override
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch) {
		System.out.println("Backwarding " + backward);
		backward.executeOn(propCtx, prevalentSystem, executionTime, branch);
	}
}

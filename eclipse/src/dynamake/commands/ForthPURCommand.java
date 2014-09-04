package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ForthPURCommand<T> implements PURCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ReversibleCommand<T> reversibleCommand;
	
	public ForthPURCommand(ReversibleCommand<T> reversibleCommand) {
		this.reversibleCommand = reversibleCommand;
	}

	@Override
	public void executeForward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		reversibleCommand.executeForward(propCtx, prevalentSystem, collector, location, scope);
	}

	@Override
	public void executeBackward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		reversibleCommand.executeBackward(propCtx, prevalentSystem, collector, location, scope);
	}

	@Override
	public PURCommand<T> inReplayState() {
		return this;
	}

	@Override
	public PURCommand<T> inUndoState() {
		return new BackPURCommand<T>(reversibleCommand);
	}

	@Override
	public PURCommand<T> inRedoState() {
		return this;
	}
	
	@Override
	public PURCommand<T> inNextState() {
		return inUndoState();
	}

	@Override
	public ReversibleCommand<T> forForwarding() {
		return new ForthPURCommand<T>((ReversibleCommand<T>) reversibleCommand.forForwarding());
	}

	@Override
	public ReversibleCommand<T> forUpwarding() {
		return new ForthPURCommand<T>((ReversibleCommand<T>) reversibleCommand.forUpwarding());
	}

	@Override
	public ReversibleCommand<T> mapToReferenceLocation(T source, T target) {
		return new ForthPURCommand<T>((ReversibleCommand<T>) reversibleCommand.mapToReferenceLocation(source, target));
	}
}

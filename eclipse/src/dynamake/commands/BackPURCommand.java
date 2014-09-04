package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class BackPURCommand<T> implements PURCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ReversibleCommand<T> reversibleCommand;
	
	public BackPURCommand(ReversibleCommand<T> reversibleCommand) {
		this.reversibleCommand = reversibleCommand;
	}

	@Override
	public void executeForward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		reversibleCommand.executeBackward(propCtx, prevalentSystem, collector, location, scope);
	}

	@Override
	public void executeBackward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		reversibleCommand.executeForward(propCtx, prevalentSystem, collector, location, scope);
	}

	@Override
	public PURCommand<T> inReplayState() {
		return inRedoState();
	}

	@Override
	public PURCommand<T> inUndoState() {
		return this;
	}

	@Override
	public PURCommand<T> inRedoState() {
		return new ForthPURCommand<T>(reversibleCommand);
	}
	
	@Override
	public PURCommand<T> inNextState() {
		return inRedoState();
	}

	@Override
	public BaseValue<T> forForwarding() {
		return new BackPURCommand<T>((ReversibleCommand<T>)reversibleCommand.forForwarding());
	}

	@Override
	public BaseValue<T> forUpwarding() {
		return new BackPURCommand<T>((ReversibleCommand<T>)reversibleCommand.forUpwarding());
	}

	@Override
	public BaseValue<T> mapToReferenceLocation(T source, T target) {
		return new BackPURCommand<T>((ReversibleCommand<T>)reversibleCommand.mapToReferenceLocation(source, target));
	}
}

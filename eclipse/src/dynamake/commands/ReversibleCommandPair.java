package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ReversibleCommandPair<T> implements ReversibleCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final Command<T> forth;
	public final Command<T> back;
	
	public ReversibleCommandPair(Command<T> forth, Command<T> back) {
		this.forth = forth;
		this.back = back;
	}

	@Override
	public void executeForward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		forth.executeOn(propCtx, prevalentSystem, collector, location, scope);
	}

	@Override
	public void executeBackward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		back.executeOn(propCtx, prevalentSystem, collector, location, scope);
	}

	@Override
	public BaseValue<T> forForwarding() {
		return this;
	}

	@Override
	public BaseValue<T> mapToReferenceLocation(T source, T target) {
		return this;
	}
}

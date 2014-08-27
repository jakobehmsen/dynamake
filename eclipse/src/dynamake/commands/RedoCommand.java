package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.transcription.RedoTransactionHandler;
import dynamake.models.transcription.RedoTransactionHandlerFactory;
import dynamake.transcription.Collector;
import dynamake.transcription.IsolatingCollector;

public class RedoCommand implements Command<Model> {
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final CommandState<Model> command;
		
		public Output(CommandState<Model> command) {
			this.command = command;
		}
	}
	
	/*
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean isolate;

	public RedoCommand(boolean isolate) {
		this.isolate = isolate;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		if(isolate)
			collector = new IsolatingCollector<Model>(collector);

		collector.startTransaction(model, new RedoTransactionHandlerFactory());
//		CommandState<Model> command = model.redo(propCtx, 0, collector);
		model.redo(propCtx, 0, collector);
		collector.commitTransaction();
		
//		return new Output(command);
		return null;
	}
}
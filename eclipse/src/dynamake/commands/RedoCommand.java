package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.Model.DualCommand;
import dynamake.models.PropogationContext;
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
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		if(isolate)
			collector = new IsolatingCollector<Model>(collector);
		
		CommandState<Model> command = model.redo2(propCtx, 0, collector);
		
		return new Output(command);
	}
}
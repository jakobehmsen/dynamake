package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class SetPropertyCommand implements Command<Model> {
	public static class AfterSetProperty implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			SetPropertyCommand.Output setPropertyOutput = (SetPropertyCommand.Output)output;
			return new SetPropertyCommand(setPropertyOutput.name, setPropertyOutput.previousValue);
		}
	}
	
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final String name;
		public final Object previousValue;
		
		public Output(String name, Object previousValue) {
			this.name = name;
			this.previousValue = previousValue;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String name;
	private Object value;
	
	public SetPropertyCommand(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		Object previousValue = model.getProperty(name);
		model.setProperty(name, value, propCtx, 0, collector);
		
		return new Output(name, previousValue);
	}
}
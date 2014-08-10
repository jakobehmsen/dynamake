package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
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
		public final Object newValue;
		
		public Output(String name, Object previousValue, Object newValue) {
			this.name = name;
			this.previousValue = previousValue;
			this.newValue = newValue;
		}
		
		@Override
		public String toString() {
			return "Changed property " + name + " from " + previousValue + " to " + newValue;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String name;
	private Object value;
	
	public SetPropertyCommand(String name, Object value) {
		if(name.equals(RestorableModel.PROPERTY_CREATION) && value != null && ((List<?>)value).isEmpty())
			new String();
		
		this.name = name;
		this.value = value;
	}
	
	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		Object previousValue = model.getProperty(name);
		model.setProperty(name, value, propCtx, 0, collector);
//		System.out.println("Set property " + name + " to " + value);
		
		return new Output(name, previousValue, value);
	}
	
	@Override
	public String toString() {
		return "Set property " + name + " to " + value;
	}
}
package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.commands.AppendToListCommand2.Output;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RemovedFromListCommand2<T> implements Command<Model> {
	public static class AfterAppendToList<T> implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			AppendToListCommand2.Output<T> appendToListOutput = (AppendToListCommand2.Output<T>)output;
			return new RemovedFromListCommand2<T>(appendToListOutput.propertyName, appendToListOutput.start, appendToListOutput.length);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String propertyName;
	private int start;
	private int length;

	public RemovedFromListCommand2(String propertyName, int start, int length) {
		this.propertyName = propertyName;
		this.start = start;
		this.length = length;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		List<T> list = (List<T>)model.getProperty(propertyName);
		list.subList(start, start + length).clear();

		return null;
	}
}

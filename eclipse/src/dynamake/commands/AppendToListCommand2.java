package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class AppendToListCommand2<T> implements Command<Model> {
	public static class Output<T> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final String propertyName;
		public final int length;
		public final int start;
		
		public Output(String propertyName, int length, int start) {
			this.propertyName = propertyName;
			this.length = length;
			this.start = start;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String propertyName;
	private List<T> toAppend;

	public AppendToListCommand2(String propertyName, List<T> toAppend) {
		this.propertyName = propertyName;
		this.toAppend = toAppend;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		List<T> list = (List<T>)model.getProperty(propertyName);
		int start = list.size();
		int length = toAppend.size();
		list.addAll(toAppend);

		return new Output<T>(propertyName, start, length);
	}
}

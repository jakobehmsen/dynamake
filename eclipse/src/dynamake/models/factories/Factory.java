package dynamake.models.factories;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;

import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberBranch;

public interface Factory extends Serializable {
	// Should provide parametric information?
	// - In general, constraints?
	
	// With such parameters (and constraints, in general), it would be possible to implicitly support creation of an intermediate CreationModel
	
	String getName();
	Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch);
	
	public static class Constant implements Factory {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String name;
		private Object value;

		public Constant(String name, Object value) {
			super();
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
			return value;
		}
	}
}

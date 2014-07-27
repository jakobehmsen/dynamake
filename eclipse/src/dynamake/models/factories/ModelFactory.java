package dynamake.models.factories;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

/**
 * Instances of implementors are supposed to be able to create models and describes the kind of model.
 */
public interface ModelFactory extends Serializable {
	// Should provide parametric information?
	// - In general, constraints?
	
	// With such parameters (and constraints, in general), it would be possible to implicitly support creation of an intermediate CreationModel
	
	Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location);
	void setup(Model rootModel, Model modelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location);
	
	public static class Constant implements ModelFactory {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Model value;

		public Constant(Model value) {
			this.value = value;
		}

		@Override
		public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
			return value;
		}
		
		@Override
		public void setup(Model rootModel, Model modelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
	}
}

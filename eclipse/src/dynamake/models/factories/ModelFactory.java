package dynamake.models.factories;

import java.io.Serializable;

import dynamake.commands.ExecutionScope;
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
	
	ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location);
	
	public static class Constant implements ModelFactory, ModelCreation {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Model value;

		public Constant(Model value) {
			this.value = value;
		}
		
		@Override
		public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) {
			return this;
		}
		
		@Override
		public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
			return value;
		}
		
		@Override
		public void setup(Model rootModel, Model createdModel, Location<Model> locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) { }

		@Override
		public ModelFactory forForwarding() {
			return this;
		}
	}

	ModelFactory forForwarding();
}

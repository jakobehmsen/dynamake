package dynamake.models.factories;

import dynamake.commands.ExecutionScope;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface ModelCreation {
	Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope);
	void setup(Model rootModel, Model createdModel, Location<Model> locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location);
	
	public static class Const implements ModelCreation {
		private Model value;
		
		public Const(Model value) {
			this.value = value;
		}

		@Override
		public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
			return value;
		}
		
		@Override
		public void setup(Model rootModel, Model createdModel, Location<Model> locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) { }
	}
}

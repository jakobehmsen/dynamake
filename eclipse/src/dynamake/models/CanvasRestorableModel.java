package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.MappableForwardable;
import dynamake.transcription.Collector;

public class CanvasRestorableModel extends RestorableModel {
	public static class Entry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public final Location id;
		public final RestorableModel restorable;
		
		public Entry(Location id, RestorableModel restorable) {
			this.id = id;
			this.restorable = restorable;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<Entry> innerRestorables;
	
	protected CanvasRestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins, List<CommandState<Model>> modelCreation, MappableForwardable modelHistory, List<CommandState<Model>> modelCleanup, List<Entry> innerRestorables) {
		super(modelBaseSerialization, modelOrigins, modelCreation, modelHistory, modelCleanup);
		this.innerRestorables = innerRestorables;
	}

	protected CanvasRestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins) {
		super(modelBaseSerialization, modelOrigins);
		innerRestorables = new ArrayList<Entry>();
	}

	protected CanvasRestorableModel() { 
		innerRestorables = new ArrayList<Entry>();
	}
	
	@Override
	protected RestorableModel createRestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins) {
		return new CanvasRestorableModel(modelBaseSerialization, modelOrigins);
	}
	
	@Override
	protected void afterMapToReferenceLocation(RestorableModel mapped, Model sourceReference, Model targetReference) {
		for(Entry innerRestorable: this.innerRestorables) {
			Entry newInnerRestorable = new Entry(
				innerRestorable.id,
				innerRestorable.restorable.mapToReferenceLocation(sourceReference, targetReference)
			);
			((CanvasRestorableModel)mapped).innerRestorables.add(newInnerRestorable);
		}
	}
	
	@Override
	protected void afterForForwarding(RestorableModel forForwarded) {
		for(Entry innerRestorable: this.innerRestorables) {
			Entry newInnerRestorable = new Entry(
				innerRestorable.id.forForwarding(),
				innerRestorable.restorable.forForwarding()
			);
			((CanvasRestorableModel)forForwarded).innerRestorables.add(newInnerRestorable);
		}
	}
	
	@Override
	protected void afterRestoreChangesOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(Entry innerRestorable: this.innerRestorables) {
			Model innerModelToRestore = ((CanvasModel)modelBase).getModelByLocation(innerRestorable.id);
			// Necessary to restore origins?
//			innerRestorable.restorable.restoreOriginsOnBase(innerModelToRestore, propCtx, propDistance, collector);
			innerRestorable.restorable.restoreChangesOnBase(innerModelToRestore, propCtx, propDistance, collector);
		}
	}
	
	@Override
	protected void afterRestoreCleanupOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(Entry innerRestorable: this.innerRestorables) {
			Model innerModelToRestore = ((CanvasModel)modelBase).getModelByLocation(innerRestorable.id);
		}
	}
	
	public static CanvasRestorableModel wrap(CanvasModel model, boolean includeLocalHistory) {
		CanvasRestorableModel wrapper = new CanvasRestorableModel();
		wrap(wrapper, model, includeLocalHistory);
		for(Location modelLocation: model.getLocations()) {
			Model innerModel = model.getModelByLocation(modelLocation);
			wrapper.innerRestorables.add(new Entry(modelLocation, innerModel.toRestorable(includeLocalHistory)));
		}
		return wrapper;
	}
}

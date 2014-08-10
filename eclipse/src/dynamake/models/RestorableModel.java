package dynamake.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.MappableForwardable;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.transcription.Collector;
import dynamake.transcription.SimpleExPendingCommandFactory;
import dynamake.transcription.Trigger;

public class RestorableModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String PROPERTY_ORIGINS = "Origins";
	public static String PROPERTY_CREATION = "Creation";
	public static String PROPERTY_CLEANUP = "Cleanup";
	
	protected byte[] modelBaseSerialization;
	// Origins must guarantee to not require mapping to new references
	protected List<CommandState<Model>> modelOrigins;
	protected List<CommandState<Model>> modelCreation;
	protected MappableForwardable modelHistory;
	protected List<CommandState<Model>> modelCleanup;
	
	public static RestorableModel wrap(Model model, boolean includeLocalHistory) {
		RestorableModel wrapper = new RestorableModel();
		wrap(wrapper, model, includeLocalHistory);
		return wrapper;
	}
	
	protected static void wrap(RestorableModel wrapper, Model model, boolean includeLocalHistory) {
		MappableForwardable modelHistory = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutputStream out = new ObjectOutputStream(bos);
			Model modelBase = model.cloneBase();
			
			if(includeLocalHistory)
				modelHistory = model.cloneHistory(includeLocalHistory);
			
			out.writeObject(modelBase);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] modelBaseSerialization = bos.toByteArray();
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelOrigins = new ArrayList<CommandState<Model>>((List<CommandState<Model>>)model.getProperty(RestorableModel.PROPERTY_ORIGINS));
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCreation1 = (List<CommandState<Model>>)model.getProperty(RestorableModel.PROPERTY_CREATION);
		List<CommandState<Model>> modelCreation = modelCreation1 != null ? new ArrayList<CommandState<Model>>(modelCreation1) : null;
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCleanup1 = (List<CommandState<Model>>)model.getProperty(RestorableModel.PROPERTY_CLEANUP);
		List<CommandState<Model>> modelCleanup = modelCleanup1 != null ? new ArrayList<CommandState<Model>>(modelCleanup1) : null;
		
		wrapper.modelBaseSerialization = modelBaseSerialization;
		wrapper.modelOrigins = modelOrigins;
		wrapper.modelCreation = modelCreation;
		wrapper.modelHistory = modelHistory;
		wrapper.modelCleanup = modelCleanup;
	}
	
	protected RestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins, List<CommandState<Model>> modelCreation, MappableForwardable modelHistory, List<CommandState<Model>> modelCleanup) {
		this.modelBaseSerialization = modelBaseSerialization;
		this.modelOrigins = modelOrigins;
		this.modelCreation = modelCreation;
		this.modelHistory = modelHistory;
		this.modelCleanup = modelCleanup;
	}
	
	protected RestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins) {
		this.modelBaseSerialization = modelBaseSerialization;
		this.modelOrigins = modelOrigins;
	}
	
	protected RestorableModel() { }
	
	public RestorableModel mapToReferenceLocation(Model sourceReference, Model targetReference) {
		RestorableModel mapped = createRestorableModel(modelBaseSerialization, modelOrigins);
		mapToReferenceLocation(mapped, sourceReference, targetReference);
		return mapped;
	}
	
	protected void mapToReferenceLocation(RestorableModel mapped, Model sourceReference, Model targetReference) {
		if(modelCreation != null) {
			mapped.modelCreation = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> modelCreationPart: modelCreation) {
				CommandState<Model> newModelCreationPart = modelCreationPart.mapToReferenceLocation(sourceReference, targetReference);
				mapped.modelCreation.add(newModelCreationPart);
			}
		}
		
		if(modelHistory != null)
			mapped.modelHistory = modelHistory.mapToReferenceLocation(sourceReference, targetReference);
		
		if(modelCleanup != null) {
			mapped.modelCleanup = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> mc: modelCleanup) {
				CommandState<Model> newModelCleanup = mc.mapToReferenceLocation(sourceReference, targetReference);
				mapped.modelCleanup.add(newModelCleanup);
			}
		}
		
		afterMapToReferenceLocation(mapped, sourceReference, targetReference);
	}
	
	public RestorableModel forForwarding() {
		RestorableModel mapped = createRestorableModel(modelBaseSerialization, modelOrigins);
		forForwarding(mapped);
		return mapped;
	}
	
	protected void forForwarding(RestorableModel mapped) {
		if(modelCreation != null) {
			mapped.modelCreation = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> modelCreationPart: modelCreation) {
				CommandState<Model> newModelCreationPart = modelCreationPart.forForwarding();
				mapped.modelCreation.add(newModelCreationPart);
			}
		}
		
		if(modelHistory != null)
			mapped.modelHistory = modelHistory.forForwarding();
		
		if(modelCleanup != null) {
			mapped.modelCleanup = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> mc: modelCleanup) {
				CommandState<Model> newModelCleanup = mc.forForwarding();
				mapped.modelCleanup.add(newModelCleanup);
			}
		}
		
		afterForForwarding(mapped);
	}
	
	protected RestorableModel createRestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins) {
		return new RestorableModel(modelBaseSerialization, modelOrigins);
	}
	
	public Model unwrapBase(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model modelBase = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(modelBaseSerialization);
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(bis);
			modelBase = (Model) in.readObject();
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return modelBase;
	}
	
	public void restoreOriginsOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		modelBase.playThenReverse(modelOrigins, propCtx, propDistance, collector);
		modelBase.setProperty(RestorableModel.PROPERTY_ORIGINS, modelOrigins, propCtx, propDistance, collector);
	}
	
	public void restoreChangesOnBase(final Model modelBase, final PropogationContext propCtx, final int propDistance, Collector<Model> collector) {
		ArrayList<CommandState<Model>> modelCreationAsPendingCommands = new ArrayList<CommandState<Model>>();
		
		if(modelCreation != null) {
			for(CommandState<Model> modelCreationPart: modelCreation) {
				modelCreationAsPendingCommands.add(((Model.PendingUndoablePair)modelCreationPart).pending);
			}
		}
		
		modelCreationAsPendingCommands.addAll(appendedCreation);
		
//		if(modelCreationAsPendingCommands.size() > 0) {
//			collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, modelCreationAsPendingCommands) {
//				@Override
//				public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//					collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, new PendingCommandState<Model>(
//						new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, pendingUndoablePairs), 
//						new SetPropertyCommand.AfterSetProperty()
//					)));
//				}
//			});
//		}
		
		restoreChanges(modelBase, collector, modelCreationAsPendingCommands, 0, new ArrayList<PendingUndoablePair>());

		if(modelHistory != null)
			modelBase.restoreHistory(modelHistory, propCtx, propDistance, collector);
		
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				afterRestoreChangesOnBase(modelBase, propCtx, propDistance, collector);
			}
		});
	}
	
	private void restoreChanges(final Model modelBase, Collector<Model> collector, final List<CommandState<Model>> modelCreation, final int i, final List<PendingUndoablePair> allPendingUndoablePairs) {
		// Execute one command at a time to leave space for side effect in between
		if(i < modelCreation.size()) {
			collector.execute(new SimpleExPendingCommandFactory<Model>(modelBase, modelCreation.subList(i, i + 1)) {
				@Override				
				public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
					allPendingUndoablePairs.addAll(pendingUndoablePairs);
					restoreChanges(modelBase, collector, modelCreation, i + 1, allPendingUndoablePairs);
				}
			});
		} else {
			collector.execute(new SimpleExPendingCommandFactory<Model>(modelBase, new PendingCommandState<Model>(
				new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, allPendingUndoablePairs), 
				new SetPropertyCommand.AfterSetProperty()
			)));
		}
	}
	
	public void restoreCleanupOnBase(final Model modelBase, final PropogationContext propCtx, final int propDistance, Collector<Model> collector) {
		collector.execute(new SimpleExPendingCommandFactory<Model>(modelBase, new PendingCommandState<Model>(
			new SetPropertyCommand(RestorableModel.PROPERTY_CLEANUP, modelCleanup), 
			new SetPropertyCommand.AfterSetProperty()
		)));
		
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				afterRestoreCleanupOnBase(modelBase, propCtx, propDistance, collector);
			}
		});
	}
	
	protected void afterMapToReferenceLocation(RestorableModel mapped, Model sourceReference, Model targetReference) { }
	protected void afterForForwarding(RestorableModel forForwarded) { }
	protected void afterRestoreChangesOnBase(final Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) { }
	protected void afterRestoreCleanupOnBase(final Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) { }
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model modelBase = unwrapBase(propCtx, propDistance, collector);
		restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
		restoreChangesOnBase(modelBase, propCtx, propDistance, collector);
		return modelBase;
	}
	
	private ArrayList<CommandState<Model>> appendedCreation = new ArrayList<CommandState<Model>>();

	public void appendCreation(CommandState<Model> creationPartToAppend) {
		appendedCreation.add(creationPartToAppend);
	}

	public void clearCreation() {
		if(modelCreation != null)
			modelCreation.clear();
	}
}

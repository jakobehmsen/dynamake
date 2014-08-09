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
import dynamake.commands.Mappable;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.transcription.Collector;
import dynamake.transcription.SimpleExPendingCommandFactory2;

public class RestorableModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String PROPERTY_ORIGINS = "Origins";
	public static String PROPERTY_CREATION = "Creation";
	public static String PROPERTY_CLEANUP = "Cleanup";
	
	private byte[] modelBaseSerialization;
	// Origins must guarantee to not require mapping to new references
	private List<CommandState<Model>> modelOrigins;
	private List<CommandState<Model>> modelCreation;
	private Mappable modelHistory;
	private List<CommandState<Model>> modelCleanup;
	
	public static RestorableModel wrap(Model model, boolean includeLocalHistory) {
		Mappable modelHistory = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutputStream out = new ObjectOutputStream(bos);
			Model modelBase = model.cloneBase();
			
			if(includeLocalHistory)
				modelHistory = model.cloneHistory();
			
			out.writeObject(modelBase);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] modelBaseSerialization = bos.toByteArray();
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelOrigins = (List<CommandState<Model>>)model.getProperty(RestorableModel.PROPERTY_ORIGINS);
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCreation = (List<CommandState<Model>>)model.getProperty(RestorableModel.PROPERTY_CREATION);
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCleanup = (List<CommandState<Model>>)model.getProperty(RestorableModel.PROPERTY_CLEANUP);
		
		return new RestorableModel(modelBaseSerialization, modelOrigins, modelCreation, modelHistory, modelCleanup);
	}
	
	private RestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins, List<CommandState<Model>> modelCreation, Mappable modelHistory, List<CommandState<Model>> modelCleanup) {
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
	
	public RestorableModel mapToReferenceLocation(Model sourceReference, Model targetReference) {
		RestorableModel mapped = new RestorableModel(modelBaseSerialization, modelOrigins);
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
		
		mapped.modelHistory = modelHistory.mapToReferenceLocation(sourceReference, targetReference);
		
		if(modelCleanup != null) {
			mapped.modelCleanup = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> mc: modelCleanup) {
				CommandState<Model> newModelCleanup = mc.mapToReferenceLocation(sourceReference, targetReference);
				mapped.modelCleanup.add(newModelCleanup);
			}
		}
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
	
	public void restoreChangesOnBase(final Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(modelCreation != null) {
			ArrayList<CommandState<Model>> modelCreationAsPendingCommands = new ArrayList<CommandState<Model>>();
			
			for(CommandState<Model> modelCreationPart: modelCreation) {
				modelCreationAsPendingCommands.add(((Model.PendingUndoablePair)modelCreationPart).pending);
			}
			
			collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, modelCreationAsPendingCommands) {
				@Override
				public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
					collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, new PendingCommandState<Model>(
						new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, pendingUndoablePairs), 
						new SetPropertyCommand.AfterSetProperty()
					)));
				}
			});
		}

		modelBase.restoreHistory(modelHistory, propCtx, propDistance, collector);
		
		afterRestoreChangesOnBase(modelBase, propCtx, propDistance, collector);
	}
	
	protected void afterRestoreChangesOnBase(final Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) { }
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model modelBase = unwrapBase(propCtx, propDistance, collector);
		restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
		restoreChangesOnBase(modelBase, propCtx, propDistance, collector);
		return modelBase;
	}
}

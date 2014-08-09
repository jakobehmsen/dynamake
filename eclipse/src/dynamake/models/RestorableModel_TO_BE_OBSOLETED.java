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
import dynamake.transcription.SimpleExPendingCommandFactory2;

public class RestorableModel_TO_BE_OBSOLETED implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String PROPERTY_ORIGINS = "Origins";
	public static String PROPERTY_CREATION = "Creation";
	public static String PROPERTY_POST_CREATION = "PostCreation";
	public static String PROPERTY_CLEANUP = "Cleanup";
	
	private byte[] modelBaseSerialization;
	// Origins must guarantee to not require mapping to new references
	private List<CommandState<Model>> modelOrigins;
	private List<CommandState<Model>> modelCreation;
	private List<CommandState<Model>> modelPostCreation;
	private MappableForwardable modelHistory;
	private List<CommandState<Model>> modelCleanup;
	
	public static RestorableModel_TO_BE_OBSOLETED wrap(Model model, boolean includeLocalHistory) {
		MappableForwardable modelHistory = null;
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
		List<CommandState<Model>> modelOrigins = (List<CommandState<Model>>)model.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_ORIGINS);
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCreation = (List<CommandState<Model>>)model.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION);
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelPostCreation = (List<CommandState<Model>>)model.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_POST_CREATION);
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCleanup = (List<CommandState<Model>>)model.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CLEANUP);
		
		return new RestorableModel_TO_BE_OBSOLETED(modelBaseSerialization, modelOrigins, modelCreation, modelPostCreation, modelHistory, modelCleanup);
	}
	
	private RestorableModel_TO_BE_OBSOLETED(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins, List<CommandState<Model>> modelCreation, List<CommandState<Model>> modelPostCreation, MappableForwardable modelHistory, List<CommandState<Model>> modelCleanup) {
		this.modelBaseSerialization = modelBaseSerialization;
		this.modelOrigins = modelOrigins;
		this.modelCreation = modelCreation;
		this.modelPostCreation = modelPostCreation;
		this.modelHistory = modelHistory;
		this.modelCleanup = modelCleanup;
	}
	
	public RestorableModel_TO_BE_OBSOLETED mapToReferenceLocation(Model sourceReference, Model targetReference) {
		ArrayList<CommandState<Model>> mappedModelCreation = new ArrayList<CommandState<Model>>();
		
		if(modelCreation != null) {
			for(CommandState<Model> modelCreationPart: modelCreation) {
				CommandState<Model> newModelCreationPart = modelCreationPart.mapToReferenceLocation(sourceReference, targetReference);
				mappedModelCreation.add(newModelCreationPart);
			}
		}
		
		ArrayList<CommandState<Model>> mappedModelPostCreation = new ArrayList<CommandState<Model>>();
		
		if(modelPostCreation != null) {
			for(CommandState<Model> modelPostCreationPart: modelPostCreation) {
				CommandState<Model> newModelPostCreationPart = modelPostCreationPart.mapToReferenceLocation(sourceReference, targetReference);
				mappedModelPostCreation.add(newModelPostCreationPart);
			}
		}
		
		MappableForwardable mappedModelHistory = modelHistory.mapToReferenceLocation(sourceReference, targetReference);
		
		ArrayList<CommandState<Model>> mappedModelCleanup = new ArrayList<CommandState<Model>>();
		
		if(modelCleanup != null) {
			for(CommandState<Model> mc: modelCleanup) {
				CommandState<Model> newModelCleanup = mc.mapToReferenceLocation(sourceReference, targetReference);
				mappedModelCleanup.add(newModelCleanup);
			}
		}
		
		return new RestorableModel_TO_BE_OBSOLETED(modelBaseSerialization, modelOrigins, mappedModelCreation, mappedModelPostCreation, mappedModelHistory, mappedModelCleanup);
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
		modelBase.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_ORIGINS, modelOrigins, propCtx, propDistance, collector);
	}
	
	public void restoreChangesOnBase(final Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(modelCreation != null) {
			// TODO: modelCreation should be scheduled for execution on collector
//			modelBase.playThenReverse(modelCreation, propCtx, propDistance, collector);
			
			ArrayList<CommandState<Model>> modelCreationAsPendingCommands = new ArrayList<CommandState<Model>>();
			
			for(CommandState<Model> modelCreationPart: modelCreation) {
				modelCreationAsPendingCommands.add(((Model.PendingUndoablePair)modelCreationPart).pending);
			}
			
			collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, modelCreationAsPendingCommands) {
				@Override
				public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
					collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, new PendingCommandState<Model>(
						new SetPropertyCommand(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, pendingUndoablePairs), 
						new SetPropertyCommand.AfterSetProperty()
					)));
				}
			});
		}
//		modelBase.setProperty(RestorableModel.PROPERTY_CREATION, modelCreation, propCtx, propDistance, collector);
		// TODO: location changes (in restoreHistory) should be scheduled for execution on collector
		modelBase.restoreHistory(modelHistory, propCtx, propDistance, collector);
		
		if(modelPostCreation != null) {
			// TODO: modelCreation should be scheduled for execution on collector
//			modelBase.playThenReverse(modelCreation, propCtx, propDistance, collector);
			
//			ArrayList<CommandState<Model>> modelPostCreationAsPendingCommands = new ArrayList<CommandState<Model>>();
//			
//			for(CommandState<Model> modelPostCreationPart: modelPostCreation) {
//				modelPostCreationAsPendingCommands.add(((Model.PendingUndoablePair)modelPostCreationPart).pending);
//			}
			
			collector.execute(new SimpleExPendingCommandFactory2<Model>(modelBase, modelPostCreation));
		}
	}
	
	public void restoreCleanupOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		modelBase.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CLEANUP, modelCleanup, propCtx, propDistance, collector);
	}
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model modelBase = unwrapBase(propCtx, propDistance, collector);
		restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
		restoreChangesOnBase(modelBase, propCtx, propDistance, collector);
		return modelBase;
	}
}

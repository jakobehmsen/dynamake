package dynamake.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandSequence;
import dynamake.commands.ExecutionScope;
import dynamake.commands.MappableForwardable;
import dynamake.commands.PURCommand;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.SetPropertyCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.transcription.Collector;
import dynamake.transcription.NullTransactionHandler;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.Trigger;

public class RestorableModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String PROPERTY_ORIGINS = "Origins";
	public static String PROPERTY_CREATION = "Creation";
	
	protected byte[] modelBaseSerialization;
	// Origins must guarantee to not require mapping to new references
	protected List<PURCommand<Model>> modelOrigins;
	protected List<PURCommand<Model>> modelCreation;
	protected MappableForwardable modelHistory;
	
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
		List<PURCommand<Model>> modelOrigins = new ArrayList<PURCommand<Model>>((List<PURCommand<Model>>)model.getProperty(RestorableModel.PROPERTY_ORIGINS));
		@SuppressWarnings("unchecked")
		List<PURCommand<Model>> modelCreation1 = (List<PURCommand<Model>>)model.getProperty(RestorableModel.PROPERTY_CREATION);
		List<PURCommand<Model>> modelCreation = modelCreation1 != null ? new ArrayList<PURCommand<Model>>(modelCreation1) : null;
		
		wrapper.modelBaseSerialization = modelBaseSerialization;
		wrapper.modelOrigins = modelOrigins;
		wrapper.modelCreation = modelCreation;
		wrapper.modelHistory = modelHistory;
	}
	
	protected RestorableModel(byte[] modelBaseSerialization, List<PURCommand<Model>> modelOrigins, List<PURCommand<Model>> modelCreation, MappableForwardable modelHistory) {
		this.modelBaseSerialization = modelBaseSerialization;
		this.modelOrigins = modelOrigins;
		this.modelCreation = modelCreation;
		this.modelHistory = modelHistory;
	}
	
	protected RestorableModel(byte[] modelBaseSerialization, List<PURCommand<Model>> modelOrigins) {
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
			mapped.modelCreation = new ArrayList<PURCommand<Model>>();
			for(PURCommand<Model> modelCreationPart: modelCreation) {
				PURCommand<Model> newModelCreationPart = (PURCommand<Model>) modelCreationPart.mapToReferenceLocation(sourceReference, targetReference);
				mapped.modelCreation.add(newModelCreationPart);
			}
		}
		
		if(modelHistory != null)
			mapped.modelHistory = modelHistory.mapToReferenceLocation(sourceReference, targetReference);
		
		afterMapToReferenceLocation(mapped, sourceReference, targetReference);
	}
	
	public RestorableModel forForwarding() {
		RestorableModel mapped = createRestorableModel(modelBaseSerialization, modelOrigins);
		forForwarding(mapped);
		return mapped;
	}
	
	protected void forForwarding(RestorableModel mapped) {
		if(modelCreation != null) {
			mapped.modelCreation = new ArrayList<PURCommand<Model>>();
			for(PURCommand<Model> modelCreationPart: modelCreation) {
				PURCommand<Model> newModelCreationPart = (PURCommand<Model>) modelCreationPart.forForwarding();
				mapped.modelCreation.add(newModelCreationPart);
			}
		}
		
		if(modelHistory != null)
			mapped.modelHistory = modelHistory.forForwarding();
		
		afterForForwarding(mapped);
	}
	
	protected RestorableModel createRestorableModel(byte[] modelBaseSerialization, List<PURCommand<Model>> modelOrigins) {
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
	
	public void restoreOriginsOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector, ExecutionScope<Model> scope) {
		modelBase.playThenReverse(modelOrigins, propCtx, propDistance, collector, scope);
		modelBase.setProperty(RestorableModel.PROPERTY_ORIGINS, modelOrigins, propCtx, propDistance, collector);
		
//		System.out.println("restoreOriginsOnBase");
	}
	
	@SuppressWarnings("unchecked")
	public void restoreChangesOnBase(final Model modelBase, final PropogationContext propCtx, final int propDistance, Collector<Model> collector) {
		collector.startTransaction(modelBase, (Class<? extends TransactionHandler<Model>>)NullTransactionHandler.class);
		
		ArrayList<PURCommand<Model>> modelCreationAsPendingCommands = new ArrayList<PURCommand<Model>>();
		
		if(modelCreation != null) {
			for(PURCommand<Model> modelCreationPart: modelCreation) {
				modelCreationAsPendingCommands.add(modelCreationPart);
			}
		}
		
		modelCreationAsPendingCommands.addAll(appendedCreation);

//		PendingCommandFactory.Util.executeSequence(collector, modelCreationAsPendingCommands, new ExecutionsHandler<Model>() {
//			@Override
//			public void handleExecutions(List<Execution<Model>> allPendingUndoablePairs, Collector<Model> collector) {
//				collector.execute(new SimplePendingCommandFactory<Model>(new PendingCommandState<Model>(
//					new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, allPendingUndoablePairs), 
//					new SetPropertyCommand.AfterSetProperty()
//				)));
//			}
//		});
		
		collector.execute(modelCreationAsPendingCommands);
		
		collector.execute(new TriStatePURCommand<Model>(
			new CommandSequence<Model>(
				collector.createProduceCommand(RestorableModel.PROPERTY_CREATION),
				collector.createProduceCommand(modelCreationAsPendingCommands),
				new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
			), 
			new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
			new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
		));

		if(modelHistory != null)
			modelBase.restoreHistory(modelHistory, propCtx, propDistance, collector);
		
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				afterRestoreChangesOnBase(modelBase, propCtx, propDistance, collector);
			}
		});
		
		collector.commitTransaction();
		
//		System.out.println("restoreChangesOnBase");
	}
	
	protected void afterMapToReferenceLocation(RestorableModel mapped, Model sourceReference, Model targetReference) { }
	protected void afterForForwarding(RestorableModel forForwarded) { }
	protected void afterRestoreChangesOnBase(final Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) { }
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector, ExecutionScope<Model> scope) {
		Model modelBase = unwrapBase(propCtx, propDistance, collector);
		restoreOriginsOnBase(modelBase, propCtx, propDistance, collector, scope);
		restoreChangesOnBase(modelBase, propCtx, propDistance, collector);
		return modelBase;
	}
	
	private ArrayList<PURCommand<Model>> appendedCreation = new ArrayList<PURCommand<Model>>();

	public void appendCreation(PURCommand<Model> creationPartToAppend) {
		appendedCreation.add(creationPartToAppend);
	}

	public void clearCreation() {
		if(modelCreation != null)
			modelCreation.clear();
	}
}

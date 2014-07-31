package dynamake.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sun.media.sound.ModelChannelMixer;

import dynamake.commands.CommandState;
import dynamake.commands.Mappable;
import dynamake.transcription.Collector;

public class RestorableModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
		List<CommandState<Model>> modelOrigins = (List<CommandState<Model>>)model.getProperty("Origins");
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCreation = (List<CommandState<Model>>)model.getProperty("Inhereted");
//		List<CommandState<Model>> modelLocalChanges = model.getLocalChanges();
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> modelCleanup = (List<CommandState<Model>>)model.getProperty("Cleanup");
		
		return new RestorableModel(modelBaseSerialization, modelOrigins, modelCreation, modelHistory, modelCleanup);
	}
	
	private RestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelOrigins, List<CommandState<Model>> modelCreation, Mappable modelHistory, List<CommandState<Model>> modelCleanup) {
		this.modelBaseSerialization = modelBaseSerialization;
		this.modelOrigins = modelOrigins;
		this.modelCreation = modelCreation;
		this.modelHistory = modelHistory;
		this.modelCleanup = modelCleanup;
	}
	
	public RestorableModel mapToReferenceLocation(Model sourceReference, Model targetReference) {
		ArrayList<CommandState<Model>> mappedModelCreation = new ArrayList<CommandState<Model>>();
		
		for(CommandState<Model> modelCreationPart: modelCreation) {
			CommandState<Model> newModelCreationPart = modelCreationPart.mapToReferenceLocation(sourceReference, targetReference);
			mappedModelCreation.add(newModelCreationPart);
		}
		
		Mappable mappedModelHistory = modelHistory.mapToReferenceLocation(sourceReference, targetReference);
		
//		ArrayList<CommandState<Model>> mappedModelLocalChanges = new ArrayList<CommandState<Model>>();
//		
//		for(CommandState<Model> modelLocalChange: modelLocalChanges) {
//			CommandState<Model> newModelLocalChange = modelLocalChange.mapToReferenceLocation(sourceReference, targetReference);
//			mappedModelLocalChanges.add(newModelLocalChange);
//		}
		
		ArrayList<CommandState<Model>> mappedModelCleanup = new ArrayList<CommandState<Model>>();
		
		if(modelCleanup != null) {
			for(CommandState<Model> mc: modelCleanup) {
				CommandState<Model> newModelCleanup = mc.mapToReferenceLocation(sourceReference, targetReference);
				mappedModelCleanup.add(newModelCleanup);
			}
		}
		
		return new RestorableModel(modelBaseSerialization, modelOrigins, mappedModelCreation, mappedModelHistory, mappedModelCleanup);
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
		modelBase.setProperty("Origins", modelOrigins, propCtx, propDistance, collector);
	}
	
	public void restoreChangesOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		modelBase.playThenReverse(modelCreation, propCtx, propDistance, collector);
		modelBase.setProperty("Inhereted", modelCreation, propCtx, propDistance, collector);
		modelBase.restoreHistory(modelHistory, propCtx, propDistance, collector);
	}
	
	public void restoreCleanupOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		modelBase.setProperty("Cleanup", modelCleanup, propCtx, propDistance, collector);
	}
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model modelBase = unwrapBase(propCtx, propDistance, collector);
		restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
		restoreChangesOnBase(modelBase, propCtx, propDistance, collector);
		return modelBase;
	}
}

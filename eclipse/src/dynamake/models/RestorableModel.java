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
import dynamake.transcription.Collector;

public class RestorableModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private byte[] modelBaseSerialization;
	private List<CommandState<Model>> modelChanges;
	
	public static RestorableModel wrap(Model model, boolean includeLocalHistory) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutputStream out = new ObjectOutputStream(bos);
			Model modelBase = model.cloneBase();
			
			if(includeLocalHistory)
				modelBase.cloneHistory(model);
			
			out.writeObject(modelBase);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] modelBaseSerialization = bos.toByteArray();
		
		ArrayList<CommandState<Model>> modelChanges = new ArrayList<CommandState<Model>>();
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> inhereterInheretedChanges = (List<CommandState<Model>>)model.getProperty("Inhereted");
		if(inhereterInheretedChanges != null)
			modelChanges.addAll(inhereterInheretedChanges);
		List<CommandState<Model>> inhereterLocalChanges = model.getLocalChanges();
		modelChanges.addAll(inhereterLocalChanges);
		
		return new RestorableModel(modelBaseSerialization, modelChanges);
	}
	
	private RestorableModel(byte[] modelBaseSerialization, List<CommandState<Model>> modelChanges) {
		this.modelBaseSerialization = modelBaseSerialization;
		this.modelChanges = modelChanges;
	}
	
	public RestorableModel mapToReferenceLocation(Model sourceReference, Model targetReference) {
		ArrayList<CommandState<Model>> mappedModelChanges = new ArrayList<CommandState<Model>>();
		
		for(CommandState<Model> modelChange: modelChanges) {
			CommandState<Model> newModelChange = modelChange.mapToReferenceLocation(sourceReference, targetReference);
			mappedModelChanges.add(newModelChange);
		}
		
		return new RestorableModel(modelBaseSerialization, mappedModelChanges);
	}
	
	public Model unwrapBase() {
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
	
	public void restoreOnBase(Model modelBase, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		modelBase.playThenReverse(modelChanges, propCtx, propDistance, collector);
		modelBase.setProperty("Inhereted", modelChanges, propCtx, propDistance, collector);
	}
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model modelBase = unwrapBase();
		restoreOnBase(modelBase, propCtx, propDistance, collector);
		return modelBase;
//		Model modelBase = null;
//		ByteArrayInputStream bis = new ByteArrayInputStream(modelBaseSerialization);
//		ObjectInputStream in;
//		try {
//			in = new ObjectInputStream(bis);
//			modelBase = (Model) in.readObject();
//			in.close();
//		} catch (IOException | ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//		
//		// Somehow, each of the command states must be unwrapped around a reference location.
//		// This indicates, a reference location is to be supplied as an argument for this method.
////		ArrayList<CommansState<Model>> 
//		
//		modelBase.playThenReverse(modelChanges, propCtx, propDistance, collector);
//		modelBase.setProperty("Inhereted", modelChanges, propCtx, propDistance, collector);
//		
//		return modelBase;
	}
}

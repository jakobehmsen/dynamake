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
	
	public static RestorableModel wrap(Model model) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutputStream out = new ObjectOutputStream(bos);
			Model modelBase = model.cloneBase();
			
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
	
	public Model unwrap(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
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
		
		modelBase.playThenReverse(modelChanges, propCtx, propDistance, collector);
		modelBase.setProperty("Inhereted", modelChanges, propCtx, propDistance, collector);
		
		return modelBase;
	}
}

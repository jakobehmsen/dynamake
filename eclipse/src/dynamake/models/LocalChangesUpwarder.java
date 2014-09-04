package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.models.LocalChangesForwarder.PushLocalChanges;
import dynamake.transcription.Collector;
import dynamake.tuples.Tuple2;

public class LocalChangesUpwarder extends ObserverAdapter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location<Model> sourceLocation;
	private Location<Model> offsetFromSource;

	public LocalChangesUpwarder(Location<Model> sourceLocation, Location<Model> offsetFromSource) {
		this.sourceLocation = sourceLocation;
		this.offsetFromSource = offsetFromSource;
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof PushLocalChanges) {
			PushLocalChanges pushLocalChanges = (PushLocalChanges)change;
			Model source = (Model)sourceLocation.getChild(sender);
			
			if(sender != source) {
				System.out.println("***Upwarding from " + sender + " to " + source + "***");
				Location<Model> newOffsetFromSource = new CompositeLocation<Model>(offsetFromSource, pushLocalChanges.offset);
				
				source.sendChanged(new PushLocalChanges(newOffsetFromSource, pushLocalChanges.localChangesToRevert, pushLocalChanges.newChanges), propCtx, propDistance, changeDistance, collector);
			}
		}
		
		// TODO: Consider:
		// What about the changes made to the embedded models? This changes should somehow be maintained and using during unplay/replay in forwarder
		if(change instanceof Model.HistoryAppendLogChange) {
			Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			Model source = (Model)sourceLocation.getChild(sender);
			
			// Forward the logged change in source
//			Object firstCommandOutput = null; //historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
			
			ArrayList<PURCommand<Model>> newChanges = new ArrayList<PURCommand<Model>>();
			
			for(ReversibleCommand<Model> rc: historyAppendLogChange.pendingUndoablePairs) {
				// Is this typecast safe?
				newChanges.add((PURCommand<Model>)rc);
			}
			
//			if(firstCommandOutput instanceof UndoCommand.Output) {
////				newChanges.add(((UndoCommand.Output)firstCommandOutput).command);
//			} else if(firstCommandOutput instanceof RedoCommand.Output) {
////				newChanges.add(((RedoCommand.Output)firstCommandOutput).command);
//			} else {
////				for(Execution<Model> pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs)
////					newChanges.add(pendingUndoablePair);
//			}
			
			ArrayList<PURCommand<Model>> offsetNewChanges = new ArrayList<PURCommand<Model>>();
			for(PURCommand<Model> pup: newChanges) {
				// Be sensitive to undo/redo commands here; they should be handled differently
				// Somehow, it is the undone/redone command that should be offset instead
				
				// TODO: Set up the command for upwarding
//				offsetNewChanges.add(pup.forUpwarding());
				
				PURCommand<Model> forwardedCommand = (PURCommand<Model>) pup.forUpwarding();
				if(forwardedCommand != null)
					offsetNewChanges.add(forwardedCommand);
			}
			
			source.sendChanged(new PushLocalChanges(offsetFromSource, new ArrayList<Tuple2<ExecutionScope<Model>, PURCommand<Model>>>(), offsetNewChanges), propCtx, propDistance, changeDistance, collector);			
		} else if (change instanceof CanvasModel.AddedModelChange) {
//			System.out.println("Upwarder observed AddedModelChange!!!");
			CanvasModel source = (CanvasModel)sender;
			
			// Derivations should be added in a special way here: 
			// When a new derivation is added within an upwarder, then, for the forwarding of that derivation, no forwarder and upwarder should be created
			// Like forwarding, but only with the filter effect, no conversion otherwise
			CanvasModel.AddedModelChange addedModelChange = (CanvasModel.AddedModelChange)change;
			Model modelInTarget = addedModelChange.model;
			Location<Model> modelLocationInSource = source.getLocationOf(modelInTarget); 
			Location<Model> modelSourceLocation = new CompositeLocation<Model>(sourceLocation, new ParentLocation());
			Location<Model> modelOffsetFromTarget = new CompositeLocation<Model>(offsetFromSource, modelLocationInSource);
			modelInTarget.addObserver(new LocalChangesUpwarder(modelSourceLocation, modelOffsetFromTarget));
		} else if (change instanceof CanvasModel.RemovedModelChange) {
//			System.out.println("Upwarder observed RemovedModelChange!!!");
		}
	}
}

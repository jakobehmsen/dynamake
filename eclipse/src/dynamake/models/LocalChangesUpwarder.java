package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.UndoCommand;
import dynamake.models.LocalChangesForwarder.PushLocalChanges;
import dynamake.transcription.Collector;
import dynamake.transcription.Execution;

public class LocalChangesUpwarder extends ObserverAdapter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location sourceLocation;
	private Location offsetFromSource;

	public LocalChangesUpwarder(Location sourceLocation, Location offsetFromSource) {
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
				Location newOffsetFromSource = new CompositeLocation(offsetFromSource, pushLocalChanges.offset);
				
				source.sendChanged(new PushLocalChanges(newOffsetFromSource, pushLocalChanges.localChangesToRevert, pushLocalChanges.newChanges), propCtx, propDistance, changeDistance, collector);
			}
		}
		
		// TODO: Consider:
		// What about the changes made to the embedded models? This changes should somehow be maintained and using during unplay/replay in forwarder
		if(change instanceof Model.HistoryAppendLogChange) {
			Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			Model source = (Model)sourceLocation.getChild(sender);
			
			// Forward the logged change in source
			Object firstCommandOutput = null; //historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
			
			ArrayList<CommandState<Model>> newChanges = new ArrayList<CommandState<Model>>();
			
			if(firstCommandOutput instanceof UndoCommand.Output) {
				newChanges.add(((UndoCommand.Output)firstCommandOutput).command);
			} else if(firstCommandOutput instanceof RedoCommand.Output) {
				newChanges.add(((RedoCommand.Output)firstCommandOutput).command);
			} else {
//				for(Execution<Model> pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs)
//					newChanges.add(pendingUndoablePair);
			}
			
			ArrayList<CommandState<Model>> offsetNewChanges = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> pup: newChanges) {
				// Be sensitive to undo/redo commands here; they should be handled differently
				// Somehow, it is the undone/redone command that should be offset instead
				offsetNewChanges.add(pup.forUpwarding());
			}
			
			source.sendChanged(new PushLocalChanges(offsetFromSource, new ArrayList<CommandState<Model>>(), offsetNewChanges), propCtx, propDistance, changeDistance, collector);			
		} else if (change instanceof CanvasModel.AddedModelChange) {
//			System.out.println("Upwarder observed AddedModelChange!!!");
			CanvasModel source = (CanvasModel)sender;
			
			// Derivations should be added in a special way here: 
			// When a new derivation is added within an upwarder, then, for the forwarding of that derivation, no forwarder and upwarder should be created
			// Like forwarding, but only with the filter effect, no conversion otherwise
			CanvasModel.AddedModelChange addedModelChange = (CanvasModel.AddedModelChange)change;
			Model modelInTarget = addedModelChange.model;
			Location modelLocationInSource = source.getLocationOf(modelInTarget); 
			Location modelSourceLocation = new CompositeLocation(sourceLocation, new ParentLocation());
			Location modelOffsetFromTarget = new CompositeLocation(offsetFromSource, modelLocationInSource);
			modelInTarget.addObserver(new LocalChangesUpwarder(modelSourceLocation, modelOffsetFromTarget));
		} else if (change instanceof CanvasModel.RemovedModelChange) {
//			System.out.println("Upwarder observed RemovedModelChange!!!");
		}
	}
}

package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.UndoCommand;
import dynamake.models.LocalChangesForwarder.PushLocalChanges;
import dynamake.transcription.Collector;

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
		// TODO: Consider:
		// What about the changes made to the embedded models? This changes should somehow be maintained and using during unplay/replay in forwarder
		if(change instanceof Model.HistoryAppendLogChange) {
			Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			Model source = (Model)sourceLocation.getChild(sender);
			
			// Forward the logged change in source
			Object firstCommandOutput = historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
			
			ArrayList<CommandState<Model>> newChanges = new ArrayList<CommandState<Model>>();
			
			if(firstCommandOutput instanceof UndoCommand.Output) {
				newChanges.add(((UndoCommand.Output)firstCommandOutput).command);
			} else if(firstCommandOutput instanceof RedoCommand.Output) {
				newChanges.add(((RedoCommand.Output)firstCommandOutput).command);
			} else {
				for(Model.PendingUndoablePair pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs)
					newChanges.add(pendingUndoablePair);
			}
			
			ArrayList<CommandState<Model>> offsetNewChanges = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> pup: newChanges) {
				// Be sensitive to undo/redo commands here; they should be handled differently
				// Somehow, it is the undone/redone command that should be offset instead
//				offsetNewChanges.add(pup.offset(offsetFromSource));
				offsetNewChanges.add(pup);
			}
			
			source.sendChanged(new PushLocalChanges(offsetFromSource, new ArrayList<CommandState<Model>>(), offsetNewChanges), propCtx, propDistance, changeDistance, collector);			
		} else if (change instanceof CanvasModel.AddedModelChange) {
//			System.out.println("Upwarder observed AddedModelChange!!!");
			CanvasModel source = (CanvasModel)sender;
			
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

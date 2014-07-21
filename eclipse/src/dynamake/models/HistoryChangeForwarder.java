package dynamake.models;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.ForwardHistoryCommand;
import dynamake.commands.PendingCommandFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.UndoCommand;
import dynamake.models.CanvasModel.AddModelCommand;
import dynamake.transcription.Collector;
import dynamake.transcription.TranscribeOnlyPendingCommandFactory;

/**
 * Instances each are supposed to forward change made in an inhereter to an inheretee.
 * The relation is not supposed to be one-to-one between inhereter and inheretee; instead
 * inheretee are to support isolated changes which are maintained safely even when changes
 * are forwarded from the inhereter.
 */
public class HistoryChangeForwarder extends ObserverAdapter {
	private Model inhereter;
	private Model inheretee;
	
	public HistoryChangeForwarder(Model inheretee) {
		this.inheretee = inheretee;
	}
	
	@Override
	public void addObservee(Observer observee) {
		this.inhereter = (Model)observee;
	}
	
	@Override
	public void removeObservee(Observer observee) {
		this.inhereter = null;
	}
	
	public boolean forwardsTo(Model model) {
		return inheretee == model;
	}
	
	public boolean forwardsFrom(Model model) {
		return inhereter == model;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof HistoryChangeForwarder) {
			HistoryChangeForwarder historyChangeForwarder = (HistoryChangeForwarder)obj;
			return this.inhereter == historyChangeForwarder.inhereter && this.inheretee == historyChangeForwarder.inheretee;
		}
		return false;
	}

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof Model.HistoryAppendLogChange) {
			final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			
			collector.execute(new PendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return inheretee;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					ArrayList<CommandState<Model>> filteredPendingCommands = new ArrayList<CommandState<Model>>();
					
					for(Model.PendingUndoablePair pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs) {
						Command<Model> command = pendingUndoablePair.pending.getCommand();
						ReversibleCommand<Model> undoable = pendingUndoablePair.undoable;
						
						if(command instanceof AddModelCommand) {
							AddModelCommand addModelCommand = (AddModelCommand)command;
							AddModelCommand.Output addModelCommandOutput = (AddModelCommand.Output)undoable.getOutput();
							// Use same factory
							// Reuse id (of location / IdLocation)

							filteredPendingCommands.add(new PendingCommandState<Model>(
								new CanvasModel.RestoreModelCommand(
									addModelCommandOutput.location, addModelCommand.xCreation, addModelCommand.yCreation, addModelCommand.widthCreation, addModelCommand.heightCreation, 
									addModelCommand.factory, new ArrayList<Command<Model>>()
								), 
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
							
							Location locationOfInhereter = ModelComponent.Util.locationBetween(inheretee, inhereter);
							Location locationOfAddedInInhereter = new CompositeLocation(locationOfInhereter, addModelCommandOutput.location);
							Location locationOfAddedInInheretee = addModelCommandOutput.location;
							
							// Embed a history change forwarder to forward between the added model of inhereter to the added model of inheretee
							filteredPendingCommands.add(new PendingCommandState<Model>(
								new ForwardHistoryCommand(locationOfAddedInInhereter, locationOfAddedInInheretee), 
								new Command.Null<Model>()
//								new UnforwardHistoryCommand(locationOfAddedInInhereter, locationOfAddedInInheretee)
							));
						} else {
							filteredPendingCommands.add(pendingUndoablePair.pending);
						}
					}
					
					commandStates.addAll(filteredPendingCommands);
				}
			});
		} else if(change instanceof Model.HistoryChange) {
			Model.HistoryChange historyChange = (Model.HistoryChange)change;
			
			switch(historyChange.type) {
			case Model.HistoryChange.TYPE_UNDO:
				collector.execute(new TranscribeOnlyPendingCommandFactory<Model>() {
					@Override
					public Model getReference() {
						return inheretee;
					}
					
					@Override
					public void createPendingCommand(List<CommandState<Model>> commandStates) {
						commandStates.add(new PendingCommandState<Model>(
							new UndoCommand(false), 
							new RedoCommand(false)
						));
					}
				});

				break;
			case Model.HistoryChange.TYPE_REDO:
				collector.execute(new TranscribeOnlyPendingCommandFactory<Model>() {
					@Override
					public Model getReference() {
						return inheretee;
					}
					
					@Override
					public void createPendingCommand(List<CommandState<Model>> commandStates) {
						commandStates.add(new PendingCommandState<Model>(
							new RedoCommand(false),
							new UndoCommand(false) 
						));
					}
				});
				
				break;
			}
		} else if(change instanceof Model.HistoryLogChange) {
			Model.HistoryLogChange historyLogChange = (Model.HistoryLogChange)change;
			
			switch(historyLogChange.type) {
			case Model.HistoryLogChange.TYPE_COMMIT_LOG:
//				inheretee.commitLog(historyLogChange.length, propCtx, propDistance, collector);
				break;
			case Model.HistoryLogChange.TYPE_REJECT_LOG:
//				inheretee.rejectLog(historyLogChange.length, propCtx, propDistance, collector);
				break;
			}
		}/* else if(change instanceof CanvasModel.AddedModelChange) {
			final CanvasModel.AddedModelChange addedModelChange = (CanvasModel.AddedModelChange)change;
			
			collector.execute(new PendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return inheretee;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					commandStates.addAll(historyAppendLogChange.pendingCommands);
				}
			});
			
			Model clone = addedModelChange.model
		} else if(change instanceof CanvasModel.RemovedModelChange) {
			CanvasModel.RemovedModelChange removedModelChange = (CanvasModel.RemovedModelChange)change;
		}*/
	}
}

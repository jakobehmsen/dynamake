package dynamake.models;

import java.util.List;

import dynamake.commands.AppendLogCommand;
import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.RemoveLastLogCommand;
import dynamake.commands.UndoCommand;
import dynamake.transcription.Collector;

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
					commandStates.add(new PendingCommandState<Model>(
						new AppendLogCommand(historyAppendLogChange.change), 
						new RemoveLastLogCommand.AfterAppendLog()
					));
				}
			});
		} else if(change instanceof Model.HistoryChange) {
			Model.HistoryChange historyChange = (Model.HistoryChange)change;
			
			switch(historyChange.type) {
			case Model.HistoryChange.TYPE_UNDO:
				collector.execute(new PendingCommandFactory<Model>() {
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
				collector.execute(new PendingCommandFactory<Model>() {
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
		}
	}
}

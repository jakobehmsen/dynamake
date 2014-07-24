package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.PlayCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnplayCommand;
import dynamake.models.CanvasModel.AddModelCommand;
import dynamake.transcription.Collector;
import dynamake.transcription.TranscribeOnlyAndPostNotPendingCommandFactory;
import dynamake.transcription.TranscribeOnlyPendingCommandFactory;

/**
 * Instances each are supposed to forward change made in an inhereter to an inheretee.
 * The relation is not supposed to be one-to-one between inhereter and inheretee; instead
 * inheretee are to support isolated changes which are maintained safely even when changes
 * are forwarded from the inhereter.
 */
public class HistoryChangeForwarder extends ObserverAdapter implements Serializable {
	public static class UndoInhereterCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unchecked")
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			Model inheretee = (Model)location.getChild(prevalentSystem);
			
			Stack<CommandState<Model>> inhereterUndoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterUndoStack");
			Stack<CommandState<Model>> inhereterRedoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterRedoStack");
			
			CommandState<Model> undoable = inhereterUndoStack.pop();
			CommandState<Model> redoable = undoable.executeOn(propCtx, prevalentSystem, collector, location);
			inhereterRedoStack.push(redoable);
			
			return null;
		}
	}
	
	public static class RedoInhereterCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unchecked")
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			Model inheretee = (Model)location.getChild(prevalentSystem);
			
			Stack<CommandState<Model>> inhereterUndoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterUndoStack");
			Stack<CommandState<Model>> inhereterRedoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterRedoStack");
			
			CommandState<Model> redoable = inhereterRedoStack.pop();
			CommandState<Model> undoable = redoable.executeOn(propCtx, prevalentSystem, collector, location);
			inhereterUndoStack.push(undoable);
			
			return null;
		}
	}
	
	public static class CommitLogInhereterCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private CommandState<Model>[] compressedLogPartAsArray;

		public CommitLogInhereterCommand(CommandState<Model>[] compressedLogPartAsArray) {
			this.compressedLogPartAsArray = compressedLogPartAsArray;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			Model inheretee = (Model)location.getChild(prevalentSystem);
			
			Stack<CommandState<Model>> inhereterUndoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterUndoStack");
			Stack<CommandState<Model>> inhereterRedoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterRedoStack");

			RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);

			inhereterUndoStack.add(compressedLogPart);
			inhereterRedoStack.clear();

			return null;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model inhereter;
	private Model inheretee;

	
	private ArrayList<Model.PendingUndoablePair> inhereterNewLog = new ArrayList<Model.PendingUndoablePair>();
	// Is it necessary to keep track of inhereter log, here, at all?
//	private ArrayList<Model.PendingUndoablePair> inhereteeLog = new ArrayList<Model.PendingUndoablePair>();
	private int logSize;
	
	public HistoryChangeForwarder(Model inhereter, Model inheretee) {
		this.inhereter = inhereter;
		// at this point, inheretee is assumed to be clone of inhereter with no local changes
		this.inheretee = inheretee;
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

//	private void appendAllToLocalChanges(List<PendingUndoablePair> pendingUndoablePairs) {
//		if(logSize >= inhereteeLog.size())
//			inhereteeLog.addAll(pendingUndoablePairs);
//		else {
//			for(int i = 0; i < pendingUndoablePairs.size(); i++)
//				inhereteeLog.set(logSize + i, pendingUndoablePairs.get(i));
//		}
//		logSize += pendingUndoablePairs.size();
//	}

	private boolean hasLocalChanges() {
		return logSize > 0;
	}
	
	private void registerUndoInLocalChanges() {
		logSize--;
	}
	
	private void registerRedoInLocalChanges() {
		logSize++;
	}
	
//	private List<Model.PendingUndoablePair> getLocalChanges() {
//		return new ArrayList<Model.PendingUndoablePair>(inhereteeLog.subList(0, logSize));
//	}

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		/*
		How to support maintaining local changes in inhereter in a safe way?
		Should some sort of log (not just undo- and redo stack) be maintained, where the parts of the inhereter
		is kept seperate from the parts of the inheretee?
		Then, when the inhereter is changed, the following procedure occurs:
		- The inheretee parts are rolled back
		- The new inheterer parts are played on the inheretee
		- The inheretee parts are replayed 
		*/
		
		/*
		Somehow, all the changes of embedded models flow up in inhereters and be passed to inheretees
		*/
		
		/*
		How could both of the logs (the log of inhereter and the log of inheretee) be maintained here in this class
		instead of in model? I.e., can the log in model be moved to history change forwarders?
		*/
		
		/*
		How to prevent inheretee to undo inhereted changes? 
		*/
		
		if((change instanceof Model.HistoryAppendLogChange || change instanceof Model.HistoryAppendLogChange2 || change instanceof Model.HistoryChange)) {
			if(sender == inhereter) {
				if(hasLocalChanges()) {
					collector.execute(new TranscribeOnlyPendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return inheretee;
						}
						
						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							@SuppressWarnings("unchecked")
							List<CommandState<Model>> localChanges = (List<CommandState<Model>>)inheretee.getProperty("localChanges");
							commandStates.add(new PendingCommandState<Model>(
								new UnplayCommand(),
								new PlayCommand(localChanges)
							));
						}
					});
				}
				
				collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
					@Override
					public Model getReference() {
						return inheretee;
					}
					
					@Override
					public void createPendingCommand(List<CommandState<Model>> commandStates) {
						commandStates.add(new PendingCommandState<Model>(
							new SetPropertyCommand("observeInheretee", false),
							new SetPropertyCommand.AfterSetProperty()
						));
					}
				});
				
				if(change instanceof Model.HistoryAppendLogChange) {
					final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
					
					collector.execute(new TranscribeOnlyPendingCommandFactory<Model>() {
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
									// THE REUSED ID CANNOT BE USED BY OTHER CREATED MODELS!!!
									
									filteredPendingCommands.add(new PendingCommandState<Model>(
										new CanvasModel.RestoreModelCommand(
											addModelCommandOutput.location, addModelCommand.xCreation, addModelCommand.yCreation, addModelCommand.widthCreation, addModelCommand.heightCreation, 
											addModelCommand.factory, new ArrayList<Command<Model>>()
										), 
										new CanvasModel.RemoveModelCommand.AfterAdd(),
										new CanvasModel.RestoreModelCommand.AfterRemove()
									));

									// Embed a history upwarder
									Location locationOfInhereter = ModelComponent.Util.locationBetween(inheretee, inhereter);
									@SuppressWarnings("unused")
									Location locationOfAddedInInhereter = new CompositeLocation(locationOfInhereter, addModelCommandOutput.location);
									@SuppressWarnings("unused")
									Location locationOfAddedInInheretee = addModelCommandOutput.location;
									
//									// Embed a history change forwarder to forward between the added model of inhereter to the added model of inheretee
//									filteredPendingCommands.add(new PendingCommandState<Model>(
//										new ForwardHistoryCommand(locationOfAddedInInhereter, locationOfAddedInInheretee), 
//										new Command.Null<Model>()
//		//								new UnforwardHistoryCommand(locationOfAddedInInhereter, locationOfAddedInInheretee)
//									));
								} else {
									filteredPendingCommands.add(pendingUndoablePair.pending);
								}
							}
							
							commandStates.addAll(filteredPendingCommands);
						}
					});
				} else if(change instanceof Model.HistoryChange) {
					Model.HistoryChange historyChange = (Model.HistoryChange)change;
					
					collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return inheretee;
						}
						
						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							commandStates.add(new PendingCommandState<Model>(
								new SetPropertyCommand("doingUndoRedo", true),
								new SetPropertyCommand.AfterSetProperty()
							));
						}
					});
					
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
									new UndoInhereterCommand(),
									new RedoInhereterCommand() 
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
									new RedoInhereterCommand(),
									new UndoInhereterCommand()
								));
							}
						});
						
						break;
					}
					
					collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return inheretee;
						}
						
						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							commandStates.add(new PendingCommandState<Model>(
								new SetPropertyCommand("doingUndoRedo", false),
								new SetPropertyCommand.AfterSetProperty()
							));
						}
					});
				} else if(change instanceof Model.HistoryLogChange) {

				}
				
				collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
					@Override
					public Model getReference() {
						return inheretee;
					}
					
					@Override
					public void createPendingCommand(List<CommandState<Model>> commandStates) {
						commandStates.add(new PendingCommandState<Model>(
							new SetPropertyCommand("observeInheretee", true),
							new SetPropertyCommand.AfterSetProperty()
						));
					}
				});
				
				if(hasLocalChanges()) {
					collector.execute(new TranscribeOnlyPendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return inheretee;
						}
						
						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							@SuppressWarnings("unchecked")
							List<CommandState<Model>> localChanges = (List<CommandState<Model>>)inheretee.getProperty("localChanges");
							commandStates.add(new PendingCommandState<Model>(
								new PlayCommand(localChanges),
								new UnplayCommand()
							));
						}
					});
				}
			} else if(sender == inheretee) {
				boolean observeInheretee = (boolean)inheretee.getProperty("observeInheretee");
				if(observeInheretee) {
					if(change instanceof Model.HistoryAppendLogChange) {
//						final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
						
//						appendAllToLocalChanges(historyAppendLogChange.pendingUndoablePairs);
					} else if(change instanceof Model.HistoryChange) {
						Model.HistoryChange historyChange = (Model.HistoryChange)change;
						
						switch(historyChange.type) {
						case Model.HistoryChange.TYPE_UNDO: {
							registerUndoInLocalChanges();
			
							break;
						} case Model.HistoryChange.TYPE_REDO: {
							// TODO:
							registerRedoInLocalChanges();
							
							break;
						}
						}
					}
				} else {
					boolean doingUndoRedo = (boolean)inheretee.getProperty("doingUndoRedo");
					if(!doingUndoRedo) {
						if(change instanceof Model.HistoryAppendLogChange2) {
							final Model.HistoryAppendLogChange2 historyAppendLogChange = (Model.HistoryAppendLogChange2)change;
							
							inhereterNewLog.addAll(historyAppendLogChange.pendingUndoablePairs);
						}
					}
				}
			}
		} else if(change instanceof Model.HistoryLogChange) {
			// If HistoryLogChange (i.e. commit or reject), then it should be safe to assume that it is safe to execute directly
			// instead of via collector
			if(sender == inheretee) {
				Model.HistoryLogChange historyLogChange = (Model.HistoryLogChange)change;
				
				switch(historyLogChange.type) {
				case Model.HistoryLogChange.TYPE_COMMIT_LOG:
					@SuppressWarnings({ "unused", "unchecked" })
					List<CommandState<Model>> localChanges /*list of lists*/ = (List<CommandState<Model>>)inheretee.getProperty("localChanges");
					
//					if(inhereterNewLog.size() > 0) {
//						@SuppressWarnings("unchecked")
//						Stack<CommandState<Model>> inhereterUndoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterUndoStack");
//						@SuppressWarnings("unchecked")
//						Stack<CommandState<Model>> inhereterRedoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterRedoStack");
//						
//						@SuppressWarnings("unchecked")
//						CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[inhereterNewLog.size()];
//						for(int i = 0; i < inhereterNewLog.size(); i++)
//							compressedLogPartAsArray[i] = inhereterNewLog.get(i).undoable;
//						inhereterNewLog.clear();
//						RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
//						inhereterUndoStack.add(compressedLogPart);
//						inhereterRedoStack.clear();
//					}
					
					break;
				case Model.HistoryLogChange.TYPE_REJECT_LOG:
					inhereterNewLog.clear();
					break;
				}
			} else if(sender == inhereter) {
				Model.HistoryLogChange historyLogChange = (Model.HistoryLogChange)change;
				
				switch(historyLogChange.type) {
				case Model.HistoryLogChange.TYPE_COMMIT_LOG:
					
					if(inhereterNewLog.size() > 0) {
						@SuppressWarnings("unchecked")
						Stack<CommandState<Model>> inhereterUndoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterUndoStack");
						@SuppressWarnings("unchecked")
						Stack<CommandState<Model>> inhereterRedoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterRedoStack");
						
						@SuppressWarnings("unchecked")
						CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[inhereterNewLog.size()];
						for(int i = 0; i < inhereterNewLog.size(); i++)
							compressedLogPartAsArray[i] = inhereterNewLog.get(i).undoable;
						inhereterNewLog.clear();
						RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
						inhereterUndoStack.add(compressedLogPart);
						inhereterRedoStack.clear();
					}
					
					break;
				case Model.HistoryLogChange.TYPE_REJECT_LOG:
					inhereterNewLog.clear();
					break;
				}
			}
		}
	}
}

package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.PlayCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UndoCommand;
import dynamake.commands.UnplayCommand;
import dynamake.models.CanvasModel.AddModelCommand;
import dynamake.models.Model.PendingUndoablePair;
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
	public static class ForwardLogChange {
		public final List<List<Model.PendingUndoablePair>> newChanges;

		public ForwardLogChange(List<List<PendingUndoablePair>> localChanges) {
			this.newChanges = localChanges;
		}
	}
	
	public static class UnplayChange {
		public final List<List<Model.PendingUndoablePair>> localChanges;

		public UnplayChange(List<List<PendingUndoablePair>> localChanges) {
			this.localChanges = localChanges;
		}
	}
	
	public static class PlayChange {
		public final List<List<Model.PendingUndoablePair>> localChanges;

		public PlayChange(List<List<PendingUndoablePair>> localChanges) {
			this.localChanges = localChanges;
		}
	}
	
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
			
			return new UndoCommand.Output();
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
			
			return new RedoCommand.Output();
		}
	}
	
//	public static class CommitLogInhereterCommand implements Command<Model> {
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		private CommandState<Model>[] compressedLogPartAsArray;
//
//		public CommitLogInhereterCommand(CommandState<Model>[] compressedLogPartAsArray) {
//			this.compressedLogPartAsArray = compressedLogPartAsArray;
//		}
//
//		@SuppressWarnings("unchecked")
//		@Override
//		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
//			Model inheretee = (Model)location.getChild(prevalentSystem);
//			
//			Stack<CommandState<Model>> inhereterUndoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterUndoStack");
//			Stack<CommandState<Model>> inhereterRedoStack = (Stack<CommandState<Model>>)inheretee.getProperty("inhereterRedoStack");
//
//			RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
//
//			inhereterUndoStack.add(compressedLogPart);
//			inhereterRedoStack.clear();
//
//			return null;
//		}
//	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model inhereter;
	private Model inheretee;
	
	private ArrayList<Model.PendingUndoablePair> inhereterNewLog = new ArrayList<Model.PendingUndoablePair>();
	// Is it necessary to keep track of inhereter log, here, at all?
//	private ArrayList<Model.PendingUndoablePair> inhereteeLog = new ArrayList<Model.PendingUndoablePair>();
//	private int logSize;
	
	public HistoryChangeForwarder(Model inhereter, Model inheretee) {
		this.inhereter = inhereter;
		// at this point, inheretee is assumed to be clone of inhereter with no local changes
		this.inheretee = inheretee;
	}
	
	public void attach(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		inheretee.setProperty("inhereterUndoStack", new Stack<CommandState<Model>>(), propCtx, 0, collector);
		inheretee.setProperty("inhereterRedoStack", new Stack<CommandState<Model>>(), propCtx, 0, collector);
		inheretee.setProperty("observeInheretee", true, propCtx, 0, collector);
		inheretee.setProperty("doingUndoRedo", false, propCtx, 0, collector);
		inheretee.setProperty("localChanges", new ArrayList<List<Model.PendingUndoablePair>>(), propCtx, 0, collector);
		inheretee.setProperty("localChangeCount", 0, propCtx, 0, collector);
	}

	private boolean hasLocalChanges() {
		return (int)inheretee.getProperty("localChangeCount") > 0;
	}
	
	private void registerUndoInLocalChanges(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int localChangeCount = (int)inheretee.getProperty("localChangeCount");
		localChangeCount--;
		inheretee.setProperty("localChangeCount", localChangeCount, propCtx, propDistance, collector);
	}
	
	private void registerRedoInLocalChanges(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int localChangeCount = (int)inheretee.getProperty("localChangeCount");
		localChangeCount++;
		inheretee.setProperty("localChangeCount", localChangeCount, propCtx, propDistance, collector);
	}
	
	private void appendLocalChange(ArrayList<Model.PendingUndoablePair> newLog, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int localChangeCount = (int)inheretee.getProperty("localChangeCount");
		List<List<Model.PendingUndoablePair>> localChanges = (List<List<Model.PendingUndoablePair>>)inheretee.getProperty("localChanges");
		if(localChangeCount >= localChanges.size())
			localChanges.add(newLog);
		else
			localChanges.set(localChangeCount, newLog);
		
		localChangeCount++;
		inheretee.setProperty("localChangeCount", localChangeCount, propCtx, propDistance, collector);
	}
	
	public List<List<Model.PendingUndoablePair>> getLocalChanges() {
		int localChangeCount = (int)inheretee.getProperty("localChangeCount");
		List<List<Model.PendingUndoablePair>> localChanges = (List<List<Model.PendingUndoablePair>>)inheretee.getProperty("localChanges");
		
		return new ArrayList<List<Model.PendingUndoablePair>>(localChanges.subList(0, localChangeCount));
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
		if(change instanceof ForwardLogChange) {
			ForwardLogChange forwardLogChange = (ForwardLogChange)change;
			
			inheretee.unplay2(propCtx, propDistance, collector);
			
			inheretee.play2(forwardLogChange.newChanges, propCtx, propDistance, collector);
			
			inheretee.replay2(propCtx, propDistance, collector);
			
			List<List<PendingUndoablePair>> inhereteeLocalChange = inheretee.getLocalChanges();
			
			ArrayList<List<PendingUndoablePair>> changes = new ArrayList<List<PendingUndoablePair>>();
			changes.addAll(forwardLogChange.newChanges);
			changes.addAll(inhereteeLocalChange);
			
			inheretee.sendChanged(new ForwardLogChange(inhereteeLocalChange), propCtx, propDistance, changeDistance, collector);
		} else if(change instanceof UnplayChange) {
			// Unplay the supplied change plus your local change
			collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return inheretee;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					@SuppressWarnings("unchecked")
					List<List<Model.PendingUndoablePair>> localChanges = getLocalChanges();
					commandStates.add(new PendingCommandState<Model>(
						new UnplayCommand(),
						new PlayCommand(localChanges)
					));
				}
			});
			
//			change = new UnplayChange();
			
			inheretee.sendChanged(change, propCtx, propDistance, changeDistance, collector);
		} if(change instanceof PlayChange) {
			// Play the supplied change plus your local change
			collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return inheretee;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					@SuppressWarnings("unchecked")
					List<List<Model.PendingUndoablePair>> localChanges = getLocalChanges();
					commandStates.add(new PendingCommandState<Model>(
						new PlayCommand(localChanges),
						new UnplayCommand()
					));
				}
			});
			
//			change = new PlayChange();
			
			inheretee.sendChanged(change, propCtx, propDistance, changeDistance, collector);
		} else if((change instanceof Model.HistoryAppendLogChange /*|| change instanceof Model.HistoryAppendLogChange2*/ /*|| change instanceof Model.HistoryChange*/)) {
			if(sender == inhereter) {
				if(hasLocalChanges()) {
					// Send Unplay to self
//					this.changed(inheretee, new UnplayChange(), propCtx, propDistance, changeDistance, collector);
					System.out.println("Unplay local change in inheretee " + inheretee);
					collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return inheretee;
						}
						
						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							@SuppressWarnings("unchecked")
							List<List<Model.PendingUndoablePair>> localChanges = getLocalChanges();
							commandStates.add(new PendingCommandState<Model>(
								new UnplayCommand(),
								new PlayCommand(localChanges)
							));
						}
					});
				}
				
				// If not doing undo or redo
				boolean doingUndoRedo = (boolean)inheretee.getProperty("doingUndoRedo");
				
				if(!doingUndoRedo) {
					// Forward logged change in inhereter to inheretee
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
					
					final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
					
					Command<Model> firstCommand = historyAppendLogChange.pendingUndoablePairs.get(0).pending.getCommand();
					Object firstCommandOutput = historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
					boolean isUndo = firstCommandOutput instanceof UndoCommand.Output;
					boolean isRedo = firstCommandOutput instanceof RedoCommand.Output;
					
					if(isUndo || isRedo) {
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
						
						if(isUndo) {
							System.out.println("Forward undo from inhereter " + inhereter + " to inheretee " + inheretee);
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
						} else if(isRedo) {
							System.out.println("Forward redo from inhereter " + inhereter + " to inheretee " + inheretee);
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
					} else {
						System.out.println("Forward other from inhereter " + inhereter + " to inheretee " + inheretee);
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
									
									if(false && command instanceof AddModelCommand) {
										AddModelCommand addModelCommand = (AddModelCommand)command;
										AddModelCommand.Output addModelCommandOutput = (AddModelCommand.Output)undoable.getOutput();
										// Use same factory
										// Reuse id (of location / IdLocation)
										// Is the above necessary?
										
										// THE REUSED ID CANNOT BE USED BY OTHER CREATED MODELS!!!
										// Is it possible to have some sort of mapping between ids in inhereter and ids in inheretee?
										// Does this implicate a need to map all locations likewise?
										
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
				}
				
				if(hasLocalChanges()) {
					System.out.println("Replay local change in inheretee " + inheretee);
					collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return inheretee;
						}
						
						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							@SuppressWarnings("unchecked")
							List<List<Model.PendingUndoablePair>> localChanges = getLocalChanges();
							commandStates.add(new PendingCommandState<Model>(
								new PlayCommand(localChanges),
								new UnplayCommand()
							));
						}
					});
				}
			} else if(sender == inheretee) {
				// If false, then forwarding 
				boolean observeInheretee = (boolean)inheretee.getProperty("observeInheretee");
				if(observeInheretee) {
					if(change instanceof Model.HistoryAppendLogChange) {
//						final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
						
//						appendAllToLocalChanges(historyAppendLogChange.pendingUndoablePairs);
						final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
						
						Object firstCommandOutput = historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
						boolean isUndo = firstCommandOutput instanceof UndoCommand.Output;
						boolean isRedo = firstCommandOutput instanceof RedoCommand.Output;
						
						if(isUndo) {
							System.out.println("Undo was performed on inheretee " + inheretee);
							registerUndoInLocalChanges(propCtx, propDistance, collector);
						} else if(isRedo) {
							System.out.println("Redo was performed on inheretee " + inheretee);
							registerRedoInLocalChanges(propCtx, propDistance, collector);
						} else {
//							inhereterNewLog.addAll(historyAppendLogChange.pendingUndoablePairs);
						}
					}
				} else {
					boolean doingUndoRedo = (boolean)inheretee.getProperty("doingUndoRedo");
					if(!doingUndoRedo) {
//						if(change instanceof Model.HistoryAppendLogChange2) {
						if(change instanceof Model.HistoryAppendLogChange) {
							System.out.println("Appended to new inhereter log of inhereter " + inhereter);
//							final Model.HistoryAppendLogChange2 historyAppendLogChange = (Model.HistoryAppendLogChange2)change;
							final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
							
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
					if(historyLogChange.newLog.size() > 0) {
						System.out.println("Recognized comitting of local change in inheretee" + inheretee);
						@SuppressWarnings("unchecked")
						List<List<Model.PendingUndoablePair>> localChanges = (List<List<Model.PendingUndoablePair>>)inheretee.getProperty("localChanges");
						appendLocalChange(historyLogChange.newLog, propCtx, propDistance, collector);
					}
					
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
						System.out.println("Comitted new inhereter log of inhereter " + inhereter);
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

package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandFactory;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateSequence;
import dynamake.commands.ForwardLocalChangesCommand;
import dynamake.commands.ForwardLocalChangesUpwards2Command;
import dynamake.commands.ForwardLocalChangesUpwardsCommand;
import dynamake.commands.PlayLocalChangesFromSourceCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.PlayThenReverseCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnforwardLocalChangesCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.ExPendingCommandFactory2;
import dynamake.transcription.HistoryHandler;
import dynamake.transcription.NullHistoryHandler;
import dynamake.transcription.TranscribeOnlyAndPostNotPendingCommandFactory;

public class NewInstanceFactory2 implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location modelLocation;
	
	public NewInstanceFactory2(RectangleF creationBounds, Location modelLocation) {
		this.creationBounds = creationBounds;
		this.modelLocation = modelLocation;
	}
	
	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		final Model source = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model target = createdModel;
				
				pushCreation(source, target, propCtx, propDistance, collector);
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				final Model target = source.cloneBase();
				
				pushOrigins(source, target, propCtx, propDistance, collector);
				
				return target;
			}
		};
	}
	
	private void pushOrigins(Model source, final Model target, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		final List<CommandState<Model>> origins = (List<CommandState<Model>>)source.getProperty(RestorableModel.PROPERTY_ORIGINS);
		
		target.playThenReverse(origins, propCtx, propDistance, collector);
		// How to schedule something for immediate execution on collector?
		// - to replace the usage of playThenReverse
		// - Does it even make sense? Because this is invoked from add command, which needs x, y, width, and height to set (the origins to be run)?
//		collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
//			@Override
//			public Model getReference() {
//				return target;
//			}
//			
//			@Override
//			public void createPendingCommand(List<CommandState<Model>> commandStates) {
//				commandStates.addAll(origins);
//			}
//		});
		target.setProperty(RestorableModel.PROPERTY_ORIGINS, origins, propCtx, propDistance, collector);
	}
	
	private void pushCreation(final Model source, final Model target, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Location locationOfSourceFromTarget = ModelComponent.Util.locationBetween(target, source);
		
		// Setup forwarding
		final ArrayList<CommandState<Model>> creationForwarding = new ArrayList<CommandState<Model>>();
		
		creationForwarding.add(new PendingCommandState<Model>(
			new ForwardLocalChangesCommand(locationOfSourceFromTarget), 
			new UnforwardLocalChangesCommand(locationOfSourceFromTarget)
		));
		
		collector.execute(new ExPendingCommandFactory2<Model>() {
			@Override
			public Model getReference() {
				return target;
			}
			
			@Override
			public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
				pendingCommands.addAll(creationForwarding);
			}
			
			@Override
			public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
				// pendingUndoablePairs are supplied in executed order; thus, the undoable, in reverse order, should fit the purpose of cleanup just right
				final ArrayList<CommandState<Model>> cleanup = new ArrayList<CommandState<Model>>();
				for(int i = pendingUndoablePairs.size() - 1; i >= 0; i--)
					cleanup.add(pendingUndoablePairs.get(i).undoable);

				collector.execute(new ExPendingCommandFactory2<Model>() {
					@Override
					public Model getReference() {
						return target;
					}

					@Override
					public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
						pendingCommands.add(new PendingCommandState<Model>(
							new SetPropertyCommand(RestorableModel.PROPERTY_CLEANUP, cleanup), 
							new SetPropertyCommand.AfterSetProperty()
						));
					}

					@Override
					public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

					}

					@Override
					public HistoryHandler<Model> getHistoryHandler() {
						return new NullHistoryHandler<Model>();
					}
				});
			}

			@Override
			public HistoryHandler<Model> getHistoryHandler() {
				return new NullHistoryHandler<Model>();
			}
		});
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> creation = (List<CommandState<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
		
		final ArrayList<CommandState<Model>> newCreation = new ArrayList<CommandState<Model>>();
		
		if(creation != null) {
			int rootDistanceFromReference = 1;
			
			for(CommandState<Model> creationPart: creation) {
				PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart;
				if(pcsCreationPart.getCommand() instanceof PlayLocalChangesFromSourceCommand)
					rootDistanceFromReference++;
			}
			
			int playCommandCount = 0;
			// Each PlayLocalChangesFromSourceCommand should have its distance incremented by one
			
			// Don't include ForwardHistoryCommand commands in changes to inheret 
			// TODO: Remove the ugly filter hack below; replace with decoupled logic
			for(CommandState<Model> creationPart: creation) {
				PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart;
				
				if(pcsCreationPart.getCommand() instanceof PlayLocalChangesFromSourceCommand) {
					PlayLocalChangesFromSourceCommand playCommand = (PlayLocalChangesFromSourceCommand)pcsCreationPart.getCommand();
					
					int rootDistance = rootDistanceFromReference - playCommandCount;
					playCommand = playCommand.whereRootDistanceIs(rootDistance);
					
					pcsCreationPart = new PendingCommandState<Model>(playCommand, pcsCreationPart.getForthFactory(), pcsCreationPart.getForthFactory());
					
					playCommandCount++;
				}
				
				if(pcsCreationPart.getCommand() instanceof ForwardLocalChangesCommand) {

				} else if(pcsCreationPart.getCommand() instanceof ForwardLocalChangesUpwards2Command) {

				} else
					newCreation.add(pcsCreationPart.mapToReferenceLocation(source, target));
			}
		}
		
		@SuppressWarnings("unchecked")
		final ArrayList<CommandState<Model>> firstCreation = (ArrayList<CommandState<Model>>)newCreation.clone();
		
		collector.execute(new ExPendingCommandFactory2<Model>() {
			@Override
			public Model getReference() {
				return target;
			}
			
			@Override
			public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
				pendingCommands.addAll(firstCreation);
			}
			
			@Override
			public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

			}

			@Override
			public HistoryHandler<Model> getHistoryHandler() {
				return new NullHistoryHandler<Model>();
			}
		});
		
		final ArrayList<CommandState<Model>> newCreationLastParts = new ArrayList<CommandState<Model>>();
		
		newCreationLastParts.add(new PendingCommandState<Model>(new PlayLocalChangesFromSourceCommand(locationOfSourceFromTarget), new PlayThenReverseCommand.AfterPlay()));
		
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
		
		collector.execute(new ExPendingCommandFactory2<Model>() {
			@Override
			public Model getReference() {
				return target;
			}
			
			@Override
			public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
				pendingCommands.addAll(newCreationLastParts);
			}
			
			@Override
			public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public HistoryHandler<Model> getHistoryHandler() {
				return new NullHistoryHandler<Model>();
			}
		});
		
		newCreation.addAll(newCreationLastParts);

//		// TODO: Consider: Inherit cleanup?
//		List<CommandState<Model>> cleanup = target.playThenReverse(creationForwarding, propCtx, propDistance, collector);
//		target.setProperty(RestorableModel.PROPERTY_CLEANUP, cleanup, propCtx, propDistance, collector);
		
//		collector.execute(new ExPendingCommandFactory2<Model>() {
//			@Override
//			public Model getReference() {
//				return target;
//			}
//			
//			@Override
//			public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
//				pendingCommands.addAll(creationForwarding);
//			}
//			
//			@Override
//			public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//				// pendingUndoablePairs are supplied in executed order; thus, the undoable, in reverse order, should fit the purpose of cleanup just right
//				final ArrayList<CommandState<Model>> cleanup = new ArrayList<CommandState<Model>>();
//				for(int i = pendingUndoablePairs.size() - 1; i >= 0; i--)
//					cleanup.add(pendingUndoablePairs.get(i).undoable);
//
//				collector.execute(new ExPendingCommandFactory2<Model>() {
//					@Override
//					public Model getReference() {
//						return target;
//					}
//
//					@Override
//					public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
//						pendingCommands.add(new PendingCommandState<Model>(
//							new SetPropertyCommand(RestorableModel.PROPERTY_CLEANUP, cleanup), 
//							new SetPropertyCommand.AfterSetProperty()
//						));
//					}
//
//					@Override
//					public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//
//					}
//
//					@Override
//					public HistoryHandler<Model> getHistoryHandler() {
//						return new NullHistoryHandler<Model>();
//					}
//				});
//			}
//
//			@Override
//			public HistoryHandler<Model> getHistoryHandler() {
//				return new NullHistoryHandler<Model>();
//			}
//		});
		
		newCreation.addAll(creationForwarding);
		
		collector.execute(new ExPendingCommandFactory2<Model>() {
			@Override
			public Model getReference() {
				return target;
			}

			@Override
			public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
				pendingCommands.add(new PendingCommandState<Model>(
					new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, newCreation), 
					new SetPropertyCommand.AfterSetProperty()
				));
			}

			@Override
			public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

			}

			@Override
			public HistoryHandler<Model> getHistoryHandler() {
				return new NullHistoryHandler<Model>();
			}
		});
		
		// Setup local changes upwarder in source if not already part of creation
		boolean changeUpwarderIsSetup = false;

		if(creation != null) {
			changeUpwarderIsSetup = creation.contains(new ForwardLocalChangesUpwards2Command());
			
			for(CommandState<Model> creationPart: creation) {
				PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart;

				if(pcsCreationPart.getCommand() instanceof ForwardLocalChangesUpwards2Command) {
					changeUpwarderIsSetup = true;
					break;
				}
			}
		} else {
			creation = new ArrayList<CommandState<Model>>();
		}
		
		if(!changeUpwarderIsSetup) {
			// Setup forwarding
			final ArrayList<CommandState<Model>> creationForwardingUpwards = new ArrayList<CommandState<Model>>();
			
			creationForwardingUpwards.add(new PendingCommandState<Model>(
				new ForwardLocalChangesUpwards2Command(), 
				(CommandFactory<Model>)null
			));
			
			collector.execute(new ExPendingCommandFactory2<Model>() {
				@Override
				public Model getReference() {
					return target;
				}
				
				@Override
				public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
					pendingCommands.addAll(creationForwarding);
				}
				
				@Override
				public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
					// Add some conditional cleaning up of ForwardLocalChangesUpwards2Command?
				}

				@Override
				public HistoryHandler<Model> getHistoryHandler() {
					return new NullHistoryHandler<Model>();
				}
			});
			
			creation.addAll(creationForwardingUpwards);
		}
		
		final List<CommandState<Model>> creationF = creation;
		
		collector.execute(new ExPendingCommandFactory2<Model>() {
			@Override
			public Model getReference() {
				return source;
			}

			@Override
			public void createPendingCommands(List<CommandState<Model>> pendingCommands) {
				pendingCommands.add(new PendingCommandState<Model>(
					new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, creationF), 
					new SetPropertyCommand.AfterSetProperty()
				));
			}

			@Override
			public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

			}

			@Override
			public HistoryHandler<Model> getHistoryHandler() {
				return new NullHistoryHandler<Model>();
			}
		});
	}
}

package dynamake;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import dynamake.CanvasModel.IndexLocation;
import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;
import dynamake.LiveModel.ProductionPanel.EditPanelMouseAdapter;

public class PlotTool implements Tool {
	@Override
	public String getName() {
		return "Plot";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
			JPopupMenu factoryPopopMenu = new JPopupMenu();
			
			Point selectionReleasePoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), productionPanel);
			final Rectangle creationBounds = productionPanel.editPanelMouseAdapter.getPlotBounds(productionPanel.editPanelMouseAdapter.selectionMouseDown, selectionReleasePoint);
			
			final Rectangle selectionCreationBounds = SwingUtilities.convertRectangle(productionPanel, creationBounds, ((JComponent)(e.getSource())).getParent());
			
			// Find target model component
			Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			
			// Find components within the creation bounds
			final ArrayList<ModelComponent> componentsWithinBounds = new ArrayList<ModelComponent>();
			for(Component c: ((JComponent)targetModelComponent).getComponents()) {
				if(selectionCreationBounds.contains(c.getBounds())) {
//					componentsWithinBounds.add((ModelComponent)c);
					// Add in reverse order because views are positioned in the reverse order in the CanvasModel
					// This way, the views are sorted ascending index-wise
					componentsWithinBounds.add(0, (ModelComponent)c);
				}
			}
			
			if(componentsWithinBounds.size() > 0) {
				JMenuItem factoryMenuItem = new JMenuItem();
				factoryMenuItem.setText("Wrap");
				
				factoryMenuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						// Find the selected model and attempt an add model transaction
						// HACK: Models can only be added to canvases
						if(productionPanel.editPanelMouseAdapter.selection.getModelBehind() instanceof CanvasModel) {
							PropogationContext propCtx = new PropogationContext();

////							connection.execute(propCtx, new DualCommandFactory<Model>() {
//							branchStep2.branch(propCtx, new DualCommandFactory<Model>() {
//								public DualCommand<Model> createDualCommand() {
//									Location[] modelLocations = new Location[componentsWithinBounds.size()];
//									int[] modelIndexes = new int[componentsWithinBounds.size()];
//									for(int i = 0; i < modelLocations.length; i++) {
//										ModelComponent view = componentsWithinBounds.get(i);
//										modelLocations[i] = view.getTransactionFactory().getModelLocation();
//										modelIndexes[i] = ((CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind()).indexOfModel(view.getModelBehind());
//									}
//									Location outputLocation = productionPanel.livePanel.model.getOutput() != null ? productionPanel.livePanel.model.getOutput().getLocator().locate() : null;
//									int wrapperIndex = ((CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind()).getModelCount() - modelLocations.length;
//
//									return new DualCommandPair<Model>(
//										new WrapTransaction(
//											productionPanel.livePanel.getTransactionFactory().getModelLocation(),
//											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
//											selectionCreationBounds, 
//											modelLocations),
//										new UnwrapTransaction(
//											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
//											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
//											new CanvasModel.IndexLocation(wrapperIndex), 
//											modelIndexes,
//											selectionCreationBounds,
//											outputLocation)
//									);
//								}
//								
//								@Override
//								public void createDualCommands(
//										List<DualCommand<Model>> dualCommands) {
//									dualCommands.add(createDualCommand());
//								}
//							}, new PrevaylerServiceBranchContinuation<Model>() {
//								@Override
//								public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch) {
//									branch.absorb();
//								}
//							});
							
							branchStep2.branch(propCtx, new PrevaylerServiceBranchCreator<Model>() {
								@Override
								public void create(PrevaylerServiceBranchCreation<Model> branchCreation) {
									Location[] modelLocations = new Location[componentsWithinBounds.size()];
									int[] modelIndexes = new int[componentsWithinBounds.size()];
									for(int i = 0; i < modelLocations.length; i++) {
										ModelComponent view = componentsWithinBounds.get(i);
										modelLocations[i] = view.getTransactionFactory().getModelLocation();
										modelIndexes[i] = ((CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind()).indexOfModel(view.getModelBehind());
									}
									Location outputLocation = productionPanel.livePanel.model.getOutput() != null ? productionPanel.livePanel.model.getOutput().getLocator().locate() : null;
									int wrapperIndex = ((CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind()).getModelCount() - modelLocations.length;
									
									// TODO: Must be changed into several transaction. E.g., set output must be a transaction of its own.
									DualCommand<Model> transaction = new DualCommandPair<Model>(
										new WrapTransaction(
											productionPanel.livePanel.getTransactionFactory().getModelLocation(),
											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
											selectionCreationBounds, 
											modelLocations),
										new UnwrapTransaction(
											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
											new CanvasModel.IndexLocation(wrapperIndex), 
											modelIndexes,
											selectionCreationBounds,
											outputLocation)
									);
									
									branchCreation.create(
										transaction, 
										new PrevaylerServiceBranchContinuation<Model>() {
											@Override
											public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch) {
												branch.absorb();
											}
										}
									);
								}
							});
							
//							step2Branch.absorb();
							
//							PropogationContext commitPropCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
//							connection.commit(commitPropCtx);

							productionPanel.editPanelMouseAdapter.resetEffectFrame();
							productionPanel.livePanel.repaint();
						}
					}
				});
				
				factoryPopopMenu.add(factoryMenuItem);
			}
			
			for(final Factory factory: productionPanel.livePanel.getFactories()) {
				JMenuItem factoryMenuItem = new JMenuItem();
				factoryMenuItem.setText(factory.getName());
				
				factoryMenuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						// Find the selected model and attempt an add model transaction
						// HACK: Models can only be added to canvases
						if(productionPanel.editPanelMouseAdapter.selection.getModelBehind() instanceof CanvasModel) {
							PropogationContext propCtx = new PropogationContext();
							
////							connection.execute(propCtx, new DualCommandFactory<Model>() {
//							branchStep2.branch(propCtx, new DualCommandFactory<Model>() {
////								public DualCommand<Model> createDualCommand() {
////									ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
//////									int addIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
//////									ModelComponent output = productionPanel.editPanelMouseAdapter.output;
//////									Location outputLocation = null;
//////									if(output != null)
//////										outputLocation = output.getTransactionFactory().getModelLocation();
//////									
//////									return new DualCommandPair<Model>(
//////										new AddThenOutputBranchTransaction(
//////											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
//////											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
//////											creationBounds, 
//////											factory), 
//////										new SetOutputThenRemoveAtTransaction(
//////											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
//////											outputLocation, 
//////											target.getTransactionFactory().getModelLocation(), 
//////											addIndex
//////										)
//////									);
////								}
//								
//								@Override
//								public void createDualCommands(
//										List<DualCommand<Model>> dualCommands) {
//									ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
//									
//									CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
//									Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
//									int index = canvasModel.getModelCount();
//									Location addedModelLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
//									// The location for Output depends on the side effect of add
//									
//									// Add
//									dualCommands.add(new DualCommandPair<Model>(
//										new CanvasModel.AddModelTransaction(canvasModelLocation, creationBounds, factory), 
//										new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
//									));
//									
//									// Output
//									dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, addedModelLocation)); // Absolute location
//								}
//							}, new PrevaylerServiceBranchContinuation<Model>() {
//								@Override
//								public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch) {
//									branch.absorb();
//								}
//							});
							
//							branchStep2.branch(propCtx, new PrevaylerServiceBranchCreator<Model>() {
//								@Override
//								public void create(PrevaylerServiceBranchCreation<Model> branchCreation) {
//									ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
//									
//									CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
//									Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
//									int index = canvasModel.getModelCount();
//									final Location addedModelLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
//									// The location for Output depends on the side effect of add
//									
//									branchCreation.create(
//										new DualCommandPair<Model>(
//											new CanvasModel.AddModel2Transaction(canvasModelLocation, creationBounds, factory), 
//											new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
//										), 
//										new PrevaylerServiceBranchContinuation<Model>() {
//											@Override
//											public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch) {
//												branch.branch(propCtx, new PrevaylerServiceBranchCreator<Model>() {
//													@Override
//													public void create(PrevaylerServiceBranchCreation<Model> branchCreation) {
//														branchCreation.create(
//															LiveModel.SetOutput.createDual(productionPanel.livePanel, addedModelLocation),
//															new PrevaylerServiceBranchContinuation<Model>() {
//																@Override
//																public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch) {
//																	branch.absorb();
//																}
//															}
//														);
//													}
//												});
//											}
//										}
//									);
//								}
//							});
							
							branchStep2.branch(propCtx, new PrevaylerServiceBranchSequenceCreator<Model>(new DualCommandFactory<Model>() {
								@Override
								public void createDualCommands(List<DualCommand<Model>> dualCommands) {
									ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
									
									CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
									Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
									int index = canvasModel.getModelCount();
									Location addedModelLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
									// The location for Output depends on the side effect of add
									
									dualCommands.add(new DualCommandPair<Model>(
										new CanvasModel.AddModel2Transaction(canvasModelLocation, creationBounds, factory), 
										new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
									));
									
									dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, addedModelLocation));
								}
							}, PrevaylerServiceBranchContinuation.Util.<Model>absorb()));

							productionPanel.editPanelMouseAdapter.resetEffectFrame();
							productionPanel.livePanel.repaint();
						}
					}
				});
				
				factoryPopopMenu.add(factoryMenuItem);
			}
			
			factoryPopopMenu.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) { 
					productionPanel.livePanel.repaint();
				}
				
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
						productionPanel.livePanel.repaint();
					}
				}
				
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_ROLLBACK);
					branch.reject();
				}
			});
			
			Point selectionReleasePointInSelection = SwingUtilities.convertPoint(((JComponent)(e.getSource())), e.getPoint(), productionPanel);
			factoryPopopMenu.show(productionPanel, selectionReleasePointInSelection.x + 10, selectionReleasePointInSelection.y);
		}
	}
	
	private PrevaylerServiceBranch<Model> branch;
	private PrevaylerServiceBranch<Model> branchStep2;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e) {
		if(e.getButton() == 1) {
			PropogationContext propCtx = new PropogationContext();
			branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			
			branchStep2 = branch.branch(propCtx, PrevaylerServiceBranchCreator.Util.<Model>empty());
			
			if(productionPanel.editPanelMouseAdapter.output != null) {
				branch.branch(propCtx, new PrevaylerServiceBranchCreator<Model>() {
					@Override
					public void create(PrevaylerServiceBranchCreation<Model> branchCreation) {
						ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
						
						branchCreation.create(
							new DualCommandPair<Model>(
								new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
								new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
							), 
							PrevaylerServiceBranchContinuation.Util.<Model>absorb()
						);
					}
				});
			}

			Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			if(targetModelComponent != null && targetModelComponent.getModelBehind() instanceof CanvasModel) {
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromEmpty(targetModelComponent, referencePoint, true, branch);
				productionPanel.livePanel.repaint();
			} else {
				productionPanel.editPanelMouseAdapter.selectionMouseDown = e.getPoint();
			}
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
			Point selectionDragPoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), productionPanel);
			Rectangle plotBoundsInSelection = productionPanel.editPanelMouseAdapter.getPlotBounds(productionPanel.editPanelMouseAdapter.selectionMouseDown, selectionDragPoint);
			final Rectangle plotBoundsInProductionPanel = SwingUtilities.convertRectangle((JComponent)productionPanel.editPanelMouseAdapter.selection, plotBoundsInSelection, productionPanel);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.effectFrame.setBounds(plotBoundsInProductionPanel);
					productionPanel.livePanel.repaint();
				}
			});
		}
	}
}

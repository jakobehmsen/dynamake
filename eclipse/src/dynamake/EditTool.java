package dynamake;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class EditTool implements Tool {
	@Override
	public String getName() {
		return "Edit";
	}
	
	@Override
	public void mouseMoved(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(productionPanel.editPanelMouseAdapter.selection == modelOver && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			Point point = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), productionPanel.selectionFrame);
			relativePosition = new RelativePosition(point, productionPanel.selectionFrame.getSize());
			productionPanel.selectionFrame.setCursor(relativePosition.getCursor());
		}
	}

	@Override
	public void mouseExited(final ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(viewPressedOn != null) {
			viewPressedOn = null;
			
			final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			branch.close();
			
			if(!productionPanel.selectionFrame.getBounds().equals(productionPanel.editPanelMouseAdapter.getEffectFrameBounds())) {
				final TransactionFactory selectionTransactionFactory = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory();
				if(relativePosition.isInCenter() &&
				   productionPanel.editPanelMouseAdapter.targetOver.getTransactionFactory() != productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getParent()) {
					// Moving to other canvas
					final Rectangle droppedBounds = SwingUtilities.convertRectangle(
						productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), (JComponent)productionPanel.editPanelMouseAdapter.targetOver);

					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					final ModelComponent targetOver = productionPanel.editPanelMouseAdapter.targetOver;
					
					branchStep2.execute(new PropogationContext(), new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							CanvasModel.appendMoveTransaction(dualCommands, productionPanel.livePanel, selection, targetOver, droppedBounds.getLocation());
						}
					});
				} else {
					// Changing bounds within the same canvas
					JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), parent);
					
					PropogationContext propCtx = new PropogationContext();
					
					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Model selectionModel = selection.getModelBehind();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "X", new Fraction(newBounds.x)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "X", selectionModel.getProperty("X"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Y", new Fraction(newBounds.y)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Y", selectionModel.getProperty("Y"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Width", new Fraction(newBounds.width)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Width", selectionModel.getProperty("Width"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Height", new Fraction(newBounds.height)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Height", selectionModel.getProperty("Height"))
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, selectionTransactionFactory.getModelLocation()));
						}
					});
				}
				
				productionPanel.editPanelMouseAdapter.targetOver = null;
				productionPanel.editPanelMouseAdapter.clearTarget2(branchStep2);
			}
			
			Point point = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), productionPanel.selectionFrame);
			relativePosition = new RelativePosition(point, productionPanel.selectionFrame.getSize());
			final Cursor cursor = relativePosition.getCursor();
			final JPanel localSelectionFrame = productionPanel.selectionFrame;
			
			branchStep2.onFinished(new Runnable() {
				@Override
				public void run() {
					localSelectionFrame.setCursor(cursor);
				}
			});
			
			productionPanel.editPanelMouseAdapter.setEffectFrameCursor2(null, branchStep2);
			
			productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
			branchStep2.close();
			
			mouseDown = null;
		}
	}
	
	private Point mouseDown;
	private ModelComponent viewPressedOn;
	private PrevaylerServiceBranch<Model> branch;
	private RelativePosition relativePosition;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
			branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
			branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			if(productionPanel.editPanelMouseAdapter.output != null) {
				PropogationContext propCtx = new PropogationContext();
				
				branchStep1.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
						dualCommands.add(new DualCommandPair<Model>(
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
						));
					}
				});
			}
			
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
			relativePosition = new RelativePosition(referencePoint, ((JComponent)targetModelComponent).getSize());
			final Cursor cursor = relativePosition.getCursor();
			
			branchStep1.onFinished(new Runnable() {
				@Override
				public void run() {
					productionPanel.selectionFrame.setCursor(cursor);
					productionPanel.editPanelMouseAdapter.setEffectFrameCursor(cursor);
				}
			});
			
			branchStep1.close();
			
			mouseDown = e.getPoint();
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			ModelComponent newTargetOverComponent;
			
			if(relativePosition.isInCenter()) {
				// Moving
				newTargetOverComponent = modelOver;
				
				if(((JComponent)productionPanel.editPanelMouseAdapter.selection).isAncestorOf((JComponent)newTargetOverComponent))
					newTargetOverComponent = productionPanel.editPanelMouseAdapter.selection;
				
				if(newTargetOverComponent == productionPanel.editPanelMouseAdapter.selection)
					newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(((JComponent)newTargetOverComponent).getParent());
			} else {
				// Resizing
				newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent());
			}
			
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null) {
					final JPanel localTargetFrame = productionPanel.targetFrame;
					
					runBuilder.addRunnable(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(localTargetFrame);
						}
					});
				}
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					
					final Color color = ProductionPanel.TARGET_OVER_COLOR;
					final Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
						((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), productionPanel);
					productionPanel.targetFrame.setBorder(
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(Color.BLACK, 1), 
							BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(color, 3), 
								BorderFactory.createLineBorder(Color.BLACK, 1)
							)
						)
					);
					
					productionPanel.targetFrame.setBounds(targetFrameBounds);
					productionPanel.targetFrame.setBackground(new Color(0, 0, 0, 0));

					final JPanel localTargetFrame = productionPanel.targetFrame;
					
					runBuilder.addRunnable(new Runnable() {
						@Override
						public void run() {
							productionPanel.add(localTargetFrame);
						}
					});
				}
			}
			
			Rectangle newEffectBounds = relativePosition.resize(
				productionPanel.selectionFrame.getLocation(), 
				productionPanel.editPanelMouseAdapter.selectionFrameSize, 
				mouseDown, 
				productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), 
				e.getPoint());
			
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(newEffectBounds, runBuilder);
			
			runBuilder.execute();
		}
	}
}

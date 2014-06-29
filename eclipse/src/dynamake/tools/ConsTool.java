package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.DualCommand;
import dynamake.DualCommandFactory;
import dynamake.DualCommandPair;
import dynamake.TranscriberBranch;
import dynamake.RepaintRunBuilder;
import dynamake.TargetPresenter;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;

public class ConsTool implements Tool {
	@Override
	public String getName() {
		return "Cons";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		final ModelComponent targetModelComponent = modelOver;
		
		if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
			TranscriberBranch<Model> branchStep2 = branch.branch();
			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			branch.close();

			targetPresenter.reset(branchStep2);
			targetPresenter = null;
			
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				productionPanel.editPanelMouseAdapter.showPopupForSelectionCons(productionPanel, e.getPoint(), targetModelComponent, branchStep2);
			} else {
				if(productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
					PropogationContext propCtx = new PropogationContext();
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
							Location observerLocation = targetModelComponent.getTransactionFactory().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
								new Model.AddObserver(observableLocation, observerLocation) // Absolute location
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, observerLocation)); // Absolute location
						}
					});
				} else {
					PropogationContext propCtx = new PropogationContext();
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
							Location observerLocation = targetModelComponent.getTransactionFactory().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.AddObserver(observableLocation, observerLocation), // Absolute location
								new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, observerLocation)); // Absolute location
						}
					});
				}
				
				productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);

				branchStep2.close();
			}
		} else {
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				final TranscriberBranch<Model> branchStep2 = branch.branch();
				branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
				targetPresenter.reset(branchStep2);
				targetPresenter = null;
				productionPanel.editPanelMouseAdapter.showPopupForSelectionCons(productionPanel, e.getPoint(), targetModelComponent, branchStep2);
				branch.close();
			} else {
				final TranscriberBranch<Model> branchStep2 = branch.branch();
				branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
				targetPresenter.reset(branchStep2);
				targetPresenter = null;
				productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
				branch.reject();
			}
		}

		mouseDown = null;
	}
	
	private Point mouseDown;
	private TranscriberBranch<Model> branch;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		TranscriberBranch<Model> branchStep1 = branch.branch();
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromDefault(targetModelComponent, referencePoint, branchStep1);
		}
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					if(target.getModelBehind() instanceof CanvasModel) {
						return ProductionPanel.TARGET_OVER_COLOR;
					} else {
						return productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(target.getModelBehind()) 
							? ProductionPanel.UNBIND_COLOR
							: ProductionPanel.BIND_COLOR;
					}
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target != productionPanel.editPanelMouseAdapter.selection;
				}
			}
		);
		
		targetPresenter.update(modelOver, branchStep1);
		
		mouseDown = e.getPoint();
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null) {
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			targetPresenter.update(modelOver, runBuilder);
			
			final int width = productionPanel.editPanelMouseAdapter.getEffectFrameWidth();
			final int height = productionPanel.editPanelMouseAdapter.getEffectFrameHeight();
			
			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = cursorLocationInProductionPanel.x - width / 2;
			final int y = cursorLocationInProductionPanel.y - height / 2;
			
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), runBuilder);
			
			runBuilder.execute();
		}
	}

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}
}

package dynamake;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class ViewTool implements Tool {
	@Override
	public String getName() {
		return "View";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		productionPanel.editPanelMouseAdapter.showPopupForSelectionView(productionPanel, e.getPoint(), null, branchStep2);
		
		branch.close();

		productionPanel.editPanelMouseAdapter.targetOver = null;
	}
	
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			if(productionPanel.editPanelMouseAdapter.output != null) {
				PropogationContext propCtx = new PropogationContext();
				
				branchStep1.execute(propCtx, new DualCommandFactory<Model>() {
					public DualCommand<Model> createDualCommand() {
						ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
						return new DualCommandPair<Model>(
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
						);
					}
					
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(createDualCommand());
					}
				});
			}
			
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
		}
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}
}

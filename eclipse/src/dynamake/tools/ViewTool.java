package dynamake.tools;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;

public class ViewTool implements Tool {
	@Override
	public String getName() {
		return "View";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		final TranscriberBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		interactionPresenter.showPopupForSelectionView(productionPanel, e.getPoint(), null, branchStep2);
		
		interactionPresenter.reset(branchStep2);
		interactionPresenter = null;
		
		branch.close();
	}
	
	private TranscriberBranch<Model> branch;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		branch = productionPanel.livePanel.getModelTranscriber().createBranch();
		
		TranscriberBranch<Model> branchStep1 = branch.branch();
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, branchStep1);
		}
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {

	}

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
		final TranscriberBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		interactionPresenter.reset(branchStep2);
		interactionPresenter = null;
		
		branchStep2.close();
		
		branch.reject();
	}
}

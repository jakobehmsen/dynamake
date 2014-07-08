package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;

public class DragTool implements Tool {
	@Override
	public String getName() {
		return "Drag";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
//		System.out.println("Drag tool mouseReleased");
		
//		System.out.println("Ree");
		ModelComponent targetModelComponent = modelOver;
		
//		final TranscriberBranch<Model> branchStep2 = branch.branch();
//		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		targetPresenter.reset(collector);
		targetPresenter = null;
		
		if(targetModelComponent != null && interactionPresenter.getSelection() != targetModelComponent) {
//			productionPanel.editPanelMouseAdapter.showPopupForSelectionObject(productionPanel, e.getPoint(), targetModelComponent, branchStep2);
			interactionPresenter.showPopupForSelectionObject(productionPanel, e.getPoint(), targetModelComponent, connection);
		} else {
//			productionPanel.editPanelMouseAdapter.showPopupForSelectionObject(productionPanel, e.getPoint(), null, branchStep2);
			interactionPresenter.showPopupForSelectionObject(productionPanel, e.getPoint(), targetModelComponent, connection);
		}
		
		interactionPresenter.reset(collector);
		interactionPresenter = null;
		
//		branch.close();

		mouseDown = null;
	}
	
	private Point mouseDown;
//	private TranscriberBranch<Model> branch;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
//		System.out.println("Drag tool mousePressed");
		
//		branch = productionPanel.livePanel.getModelTranscriber().createBranch();
//		
//		TranscriberBranch<Model> branchStep1 = branch.branch();
//		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent = ModelComponent.Util.closestModelComponent(target);
		
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
		}
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return ProductionPanel.TARGET_OVER_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target != interactionPresenter.getSelection();
				}
			}
		);
		
		targetPresenter.update(modelOver, collector);
		
//		branchStep1.close();
		
		mouseDown = e.getPoint();
		
		collector.flush();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector, TranscriberConnection<Model> connection) {
//		System.out.println("Drag tool mouseDragged (mouseDown=" + mouseDown + ")");
		
		if(mouseDown != null) {
//			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			targetPresenter.update(modelOver, collector);
			
			final int width = interactionPresenter.getEffectFrameWidth();
			final int height = interactionPresenter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = interactionPresenter.getSelectionFrameLocation().x + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = interactionPresenter.getSelectionFrameLocation().y + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			interactionPresenter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), collector);
			
//			runBuilder.execute();
			collector.flush();
		}
	}

	@Override
	public void paint(Graphics g) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
		// TODO Auto-generated method stub
		
	}
}

package dynamake;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import dynamake.LiveModel.LivePanel;
import dynamake.LiveModel.ProductionPanel;

public class TrimTool implements Tool {
	@Override
	public String getName() {
		return "Trim";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		canvas = null;
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		targetPresenter.reset(runBuilder);
		
		runBuilder.execute();
		
		targetPresenter = null;
	}
	
	private ModelComponent canvas;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		canvas = ModelComponent.Util.closestCanvasModelComponent(modelOver);
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(target.getModelBehind()) 
						? ProductionPanel.UNBIND_COLOR 
						: ProductionPanel.BIND_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target != productionPanel.editPanelMouseAdapter.selection;
				}
			}
		);
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		targetPresenter.update(modelOver, runBuilder);
		
		runBuilder.execute();
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(modelOver != canvas) {
			ModelComponent modelOverParent = ModelComponent.Util.getParent(modelOver);
			
			if(modelOverParent == canvas) {
				System.out.println("Should be deleted");
			}
		}
	}

	@Override
	public void paint(Graphics g) {

	}
}

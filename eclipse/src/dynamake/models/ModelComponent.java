package dynamake.models;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import dynamake.delegates.Action1;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
import dynamake.transcription.DualCommandFactory;

/**
 * Assumed only to be implemented by JComponent (or JFrame) class extensions.
 */
public interface ModelComponent {
	Model getModelBehind();
	ModelTranscriber getModelTranscriber();
	void appendContainerTransactions(LivePanel livePanel, CompositeMenuBuilder menuBuilder, ModelComponent child);
	void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder);
	void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder);
	void appendDropTargetTransactions(ModelComponent livePanel, ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, CompositeMenuBuilder menuBuilder);
	void initialize();
	
	public static class Util {
		public static ModelComponent getParent(ModelComponent view) {
			Component parent = ((Component)view).getParent();
			while(parent != null && !(parent instanceof ModelComponent))
				parent = parent.getParent();
			return (ModelComponent)parent;
		}
		
		public static ModelComponent closestModelComponent(Component component) {
			while(component != null && !(component instanceof ModelComponent))
				component = component.getParent();
			return (ModelComponent)component;
		}

		public static ModelComponent closestCanvasModelComponent(ModelComponent view) {
			while(!(((ModelComponent)view).getModelBehind() instanceof CanvasModel))
				view = closestModelComponent(((Component)view).getParent());
			return (ModelComponent)view;
		}
	}

	DualCommandFactory<Model> getImplicitDropAction(ModelComponent target);
	void visitTree(Action1<ModelComponent> visitAction);
}

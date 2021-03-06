package dynamake.models;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;

import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;

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
			Component parent = (Component)view;
			
			while(true) {
				parent = parent.getParent();
				
				if(parent == null)
					return null;
				
				if(parent instanceof ModelComponent)
					return (ModelComponent)parent;
			}
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

		public static ModelComponent closestCommonAncestor(ModelComponent first, ModelComponent second) {
			HashSet<ModelComponent> secondAncestors = new HashSet<ModelComponent>();

			// Start at second, because first may be contained within second
			// and, thus, second may be an ancestor of first
			ModelComponent secondParent = second;//getParent(second);
			
			while(secondParent != null) {
				secondAncestors.add(secondParent);
				secondParent = getParent(secondParent);
			}

			// Start at first, because second may be contained within first
			// and, thus, first may be an ancestor of second
			ModelComponent firstParent = first;//getParent(first);
			
			while(firstParent != null) {
				if(secondAncestors.contains(firstParent))
					return firstParent;

				firstParent = getParent(firstParent);
			}

			return null;
		}

		public static Model closestCommonAncestor(Model first, Model second) {
			HashSet<Model> secondAncestors = new HashSet<Model>();

			// Start at second, because first may be contained within second
			// and, thus, second may be an ancestor of first
			Model secondParent = second;//getParent(second);
			
			while(secondParent != null) {
				secondAncestors.add(secondParent);
				secondParent = secondParent.getParent();
			}

			// Start at first, because second may be contained within first
			// and, thus, first may be an ancestor of second
			Model firstParent = first;//getParent(first);
			
			while(firstParent != null) {
				if(secondAncestors.contains(firstParent))
					return firstParent;

				firstParent = firstParent.getParent();
			}

			return null;
		}
		
		public static Location<Model> locationBetween(Model from, Model to) {
			Model cca = ModelComponent.Util.closestCommonAncestor(from, to);
			
			Location<Model> locationFromSelfToCca = ModelComponent.Util.locationToAncestor(cca, from);
			Location<Model> locationFromCcaToObserver = ModelComponent.Util.locationFromAncestor(cca, to);
			
			return new CompositeLocation<Model>(locationFromSelfToCca, locationFromCcaToObserver);
		}

		public static Location<Model> locationFromAncestor(ModelComponent ancestor, ModelComponent child) {
			return locationFromAncestor(ancestor.getModelBehind(), child.getModelBehind());
		}

		public static Location<Model> locationFromAncestor(Model ancestor, Model child) {
			if(child == ancestor)
				return new ModelRootLocation<Model>();
			
			ArrayList<Model> ancestorsClosestToFarthest = new ArrayList<Model>();

			Model parent = child.getParent();
			while(true) {
				ancestorsClosestToFarthest.add(parent);
				if(parent == ancestor)
					break;
				parent = parent.getParent();
			}
			
			Location<Model> location = ((CanvasModel)ancestorsClosestToFarthest.get(0)).getLocationOf(child);
			
			for(int i = 1; i < ancestorsClosestToFarthest.size(); i++) {
				Model currentAncestor = ancestorsClosestToFarthest.get(i - 1);
				location = new CompositeLocation<Model>(
					((CanvasModel)ancestorsClosestToFarthest.get(i)).getLocationOf(currentAncestor),
					location
				);
			}
					
			return location;
		}

		public static Location<Model> locationToAncestor(ModelComponent ancestor, ModelComponent child) {
			return locationToAncestor(ancestor.getModelBehind(), child.getModelBehind());
		}

		public static Location<Model> locationToAncestor(Model ancestor, Model child) {
			if(child == ancestor)
				return new ModelRootLocation<Model>();

			Location<Model> location = new ParentLocation();
			
			Model parent = child.getParent();
			while(true) {
				if(parent == ancestor)
					break;
				
				parent = parent.getParent();
				
				location = new CompositeLocation<Model>(
					new ParentLocation(),
					location
				);
			}
					
			return location;
		}
	}
}

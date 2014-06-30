package dynamake.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.models.Binding;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.TranscriberBranch;

public class InteractionPresenter {
	public static final Color SELECTION_COLOR = Color.GRAY;
	
	private ProductionPanel productionPanel;
	private Binding<Component> selectionBoundsBinding;
	private ModelComponent selection;
	private JPanel selectionFrame;
	private JPanel effectFrame;
	
	public InteractionPresenter(ProductionPanel productionPanel) {
		this.productionPanel = productionPanel;
	}
	
	public ModelComponent getSelection() {
		return selection;
	}
	
	public void selectFromView(final ModelComponent view, final Point initialMouseDown, TranscriberBranch<Model> branch) {
		Rectangle effectBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
		select(view, branch);
		createEffectFrame(effectBounds, branch);
	}
	
	private void select(final ModelComponent view, TranscriberBranch<Model> branch) {
		// <Don't remove>
		// Whether the following check is necessary or not has not been decided yet, so don't remove the code
//		if(this.selection == view)
//			return;
		// </Don't remove>
		
		this.selection = view;
		
		if(selectionBoundsBinding != null)
			selectionBoundsBinding.releaseBinding();
		
		if(this.selection != null) {
			if(selectionFrame == null) {
				final JPanel localSelectionFrame = new JPanel();
				
				localSelectionFrame.setBackground(new Color(0, 0, 0, 0));

				localSelectionFrame.setBorder(
					BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(Color.BLACK, 1), 
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(SELECTION_COLOR, 3), 
							BorderFactory.createLineBorder(Color.BLACK, 1)
						)
					)
				);
				
				// DON'T ADD MOUSE ADAPTER, SINCE MOUSE EVENTS WILL THEN BUBBLE UP TO THE PRODUCTION PANEL
				// AND WILL BE HANDLED THERE (SENT TO THE TOOL).
				
//				MouseAdapter mouseAdapter = new MouseAdapter() {
//					@Override
//					public void mouseMoved(MouseEvent e) {
////						System.out.println("Selection forwarding moved");
//						
//						e.translatePoint(localSelectionFrame.getX(), localSelectionFrame.getY());
//						e.setSource(productionPanel);
//						
//						for(MouseMotionListener l: productionPanel.getMouseMotionListeners())
//							l.mouseMoved(e);
//					}
//
//					public void mouseExited(MouseEvent e) {
//
//					}
//
//					@Override
//					public void mousePressed(MouseEvent e) {
//						System.out.println("Selection forwarding pressed");
//						
//						e.translatePoint(localSelectionFrame.getX(), localSelectionFrame.getY());
//						e.setSource(productionPanel);
//						
//						for(MouseListener l: productionPanel.getMouseListeners())
//							l.mousePressed(e);
//					}
//
//					@Override
//					public void mouseDragged(MouseEvent e) {
//						System.out.println("Selection forwarding dragged");
//						
//						e.translatePoint(localSelectionFrame.getX(), localSelectionFrame.getY());
//						e.setSource(productionPanel);
//						
//						for(MouseMotionListener l: productionPanel.getMouseMotionListeners())
//							l.mouseDragged(e);
//					}
//
//					@Override
//					public void mouseReleased(MouseEvent e) {
//						System.out.println("Selection forwarding released");
//						
//						e.translatePoint(localSelectionFrame.getX(), localSelectionFrame.getY());
//						e.setSource(productionPanel);
//						
//						for(MouseListener l: productionPanel.getMouseListeners())
//							l.mouseReleased(e);
//					}
//				};
//				
//				localSelectionFrame.addMouseListener(mouseAdapter);
//				localSelectionFrame.addMouseMotionListener(mouseAdapter);
				
				if(effectFrame != null)
					System.out.println("Effect frame was there before selection was added");

				selectionFrame = localSelectionFrame;
				
				branch.onFinished(new Runnable() {
					@Override
					public void run() {
						productionPanel.add(localSelectionFrame);
//						System.out.println("Added selectionFrame");
					}
				});
			}

			final JPanel localSelectionFrame = this.selectionFrame;
			
			// Wait deriving the Swing based bounds because the adding of the component is postponed to the next repaint sync
			branch.onFinished(new Runnable() {
				@Override
				public void run() {
					final Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
					localSelectionFrame.setBounds(selectionBounds);
//					System.out.println("Changed selection bounds");
				}
			});
			
			selectionBoundsBinding = new Binding<Component>() {
				private Component component;
				private ComponentListener listener;
				
				{
					component = (JComponent)selection;
					listener = new ComponentListener() {
						@Override
						public void componentShown(ComponentEvent arg0) { }
						
						@Override
						public void componentResized(ComponentEvent arg0) {
							Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
							localSelectionFrame.setBounds(selectionBounds);
							productionPanel.livePanel.repaint();
						}
						
						@Override
						public void componentMoved(ComponentEvent arg0) {
							Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
							localSelectionFrame.setBounds(selectionBounds);
							productionPanel.livePanel.repaint();
						}
						
						@Override
						public void componentHidden(ComponentEvent arg0) { }
					};
					((JComponent)selection).addComponentListener(listener);
				}
				
				@Override
				public void releaseBinding() {
					component.removeComponentListener(listener);
				}
				
				@Override
				public Component getBindingTarget() {
					return component;
				}
			};
		} else {
			if(selectionFrame != null)
				clearFocus(branch);
		}
	}
	
	private void clearFocus(TranscriberBranch<Model> branch) {
		if(selectionFrame != null) {
			if(selectionBoundsBinding != null)
				selectionBoundsBinding.releaseBinding();
			
			final JPanel localSelectionFrame = selectionFrame;
			branch.onFinished(new Runnable() {
				@Override
				public void run() {
					productionPanel.remove(localSelectionFrame);
//					System.out.println("Removed selectionFrame");
				}
			});
			selectionFrame = null;
		}
	}
	
	private void clearEffectFrameOnBranch(TranscriberBranch<Model> branch) {
		if(effectFrame != null) {
			final JPanel localEffectFrame = effectFrame;
			effectFrame = null;
//			initialEffectBounds = null;
			branch.onFinished(new Runnable() {
				@Override
				public void run() {
					productionPanel.remove(localEffectFrame);
//					System.out.println("Removed effect frame");
				}
			});
		} else {
			System.out.println("Attempted to clear effect frame when it hasn't been created.");
		}
	}
	
	private void createEffectFrame(Rectangle creationBounds, TranscriberBranch<Model> branch) {
		if(effectFrame == null) {
			final JPanel localEffectFrame = new JPanel();
			localEffectFrame.setBackground(new Color(0, 0, 0, 0));
			localEffectFrame.setBounds(creationBounds);
			
			Color effectColor = LiveModel.ToolButton.getColorForButton(productionPanel.editPanelMouseAdapter.buttonPressed);
			effectColor = effectColor.darker();
			localEffectFrame.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createDashedBorder(effectColor, 2.0f, 2.0f, 1.5f, false),
				BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
			));
			
//			addFocusMouseListener(productionPanel, localEffectFrame, "effectFrame");
			
			effectFrame = localEffectFrame;
//			initialEffectBounds = creationBounds;
			
			// Ensure effect frame is shown in front of selection frame
			if(productionPanel.selectionFrame != null) {
				final JPanel localSelectionFrame = productionPanel.selectionFrame; 
				branch.onFinished(new Runnable() {
					@Override
					public void run() {
						// NOTICE: DON'T REMOVE THE COMMENTED FOLLOWING CODE
						// IF THE BELOW CODE IS UNCOMMENTED TO REPLACE THE
						// APPROACH BELOW TO INSERT THE EFFECT FRAME, THEN
						// mouseReleased IS NEVER INVOKED ON selectionFrame
						// ON JAR RELEASES.
						
//						productionPanel.remove(localSelectionFrame);
//						productionPanel.add(localEffectFrame);
//						productionPanel.add(localSelectionFrame);
//						System.out.println("Created effect frame (after reordering).");
						
						int indexOfSelectionFrame = 0;
						for(int i = 0; i < productionPanel.getComponents().length; i++) {
							if(productionPanel.getComponents()[i] == localSelectionFrame)
								indexOfSelectionFrame = i;
						}
						productionPanel.add(localEffectFrame, indexOfSelectionFrame);
//						System.out.println("Created effect frame (after reordering).");
					}
				});
			} else {
				branch.onFinished(new Runnable() {
					@Override
					public void run() {
						productionPanel.add(localEffectFrame);
//						System.out.println("Created effect frame.");
					}
				});
			}
		} else {
			System.out.println("Attempted to created an effect frame when it has already been created.");
		}
	}
	
	public void reset(TranscriberBranch<Model> branch) {
		clearFocus(branch);
		clearEffectFrameOnBranch(branch);
	}
}

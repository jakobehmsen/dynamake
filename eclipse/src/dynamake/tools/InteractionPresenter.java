package dynamake.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import dynamake.delegates.Runner;
import dynamake.dragndrop.ConsDragDropPopupBuilder;
import dynamake.dragndrop.DragDragDropPopupBuilder;
import dynamake.dragndrop.DragDropPopupBuilder;
import dynamake.dragndrop.TellDragDropPopupBuilder;
import dynamake.dragndrop.ViewDragDropPopupBuilder;
import dynamake.models.Binding;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.RunBuilder;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberOnFlush;

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
	
	public void selectFromView(final ModelComponent view, final Point initialMouseDown, final TranscriberCollector<Model> collector) {
		selectFromView(view, initialMouseDown, new Runner() {
			@Override
			public void run(final Runnable runnable) {
				collector.afterNextFlush(new TranscriberOnFlush<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						runnable.run();
					}
				});
			}
		});
	}
	
	public void selectFromView(final ModelComponent view, final Point initialMouseDown, final TranscriberBranch<Model> branch) {
		selectFromView(view, initialMouseDown, new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
	}
	
	public void selectFromView(final ModelComponent view, final Point initialMouseDown, Runner runner) {
		Rectangle effectBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
		select(view, runner);
		createEffectFrame(effectBounds, runner);
	}
	
	public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, final TranscriberCollector<Model> collector) {
		selectFromDefault(view, initialMouseDown, new Runner() {
			@Override
			public void run(final Runnable runnable) {
				collector.afterNextFlush(new TranscriberOnFlush<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						runnable.run();
					}
				});
			}
		});
	}
	
	public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, final TranscriberBranch<Model> branch) {
		selectFromDefault(view, initialMouseDown, new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
	}
	
	public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, Runner runner) {
		Dimension sourceBoundsSize = new Dimension(125, 33);
		Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
		Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
		Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, productionPanel);
		select(view, runner);
		createEffectFrame(selectionBounds, runner);
	}
	
//	private void select(final ModelComponent view, final TranscriberBranch<Model> branch) {
//		select(view, new Runner() {
//			@Override
//			public void run(Runnable runnable) {
//				branch.onFinished(runnable);
//			}
//		});
//	}
	
	private void select(final ModelComponent view, Runner runner) {
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
				
//				branch.onFinished(new Runnable() {
//					@Override
//					public void run() {
//						productionPanel.add(localSelectionFrame);
////						System.out.println("Added selectionFrame");
//					}
//				});
				
				runner.run(new Runnable() {
					@Override
					public void run() {
						productionPanel.add(localSelectionFrame);
//						System.out.println("Added selectionFrame");
					}
				});
			}

			final JPanel localSelectionFrame = this.selectionFrame;
			
			// Wait deriving the Swing based bounds because the adding of the component is postponed to the next repaint sync
//			branch.onFinished(new Runnable() {
//				@Override
//				public void run() {
//					final Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
//					localSelectionFrame.setBounds(selectionBounds);
////					System.out.println("Changed selection bounds");
//				}
//			});

			// Wait deriving the Swing based bounds because the adding of the component is postponed to the next repaint sync
			runner.run(new Runnable() {
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
				clearFocus(runner);
		}
	}
	
//	private void clearFocus(final TranscriberBranch<Model> branch) {
//		clearFocus(new Runner() {
//			@Override
//			public void run(Runnable runnable) {
//				branch.onFinished(runnable);
//			}
//		});
//	}
	
	private void clearFocus(Runner runner) {
		if(selectionFrame != null) {
			if(selectionBoundsBinding != null)
				selectionBoundsBinding.releaseBinding();
			
			final JPanel localSelectionFrame = selectionFrame;
//			branch.onFinished(new Runnable() {
//				@Override
//				public void run() {
//					productionPanel.remove(localSelectionFrame);
////					System.out.println("Removed selectionFrame");
//				}
//			});
			runner.run(new Runnable() {
				@Override
				public void run() {
					productionPanel.remove(localSelectionFrame);
//					System.out.println("Removed selectionFrame");
				}
			});
			selectionFrame = null;
		}
	}
	
//	private void clearEffectFrameOnBranch(final TranscriberBranch<Model> branch) {
//		clearEffectFrameOnBranch(new Runner() {
//			@Override
//			public void run(Runnable runnable) {
//				branch.onFinished(runnable);
//			}
//		});
//	}
	
	private void clearEffectFrameOnBranch(Runner runner) {
		if(effectFrame != null) {
			final JPanel localEffectFrame = effectFrame;
			effectFrame = null;
//			initialEffectBounds = null;
			runner.run(new Runnable() {
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
	
	private void createEffectFrame(Rectangle creationBounds, Runner runner) {
		if(effectFrame == null) {
			final JPanel localEffectFrame = new JPanel();
			localEffectFrame.setBackground(new Color(0, 0, 0, 0));
			localEffectFrame.setBounds(creationBounds);
			
//			Color effectColor = LiveModel.ToolButton.getColorForButton(productionPanel.editPanelMouseAdapter.buttonPressed);
			Color effectColor = LiveModel.ToolButton.avgColorOfButtons(productionPanel.editPanelMouseAdapter.buttonsPressed);
			effectColor = effectColor.darker();
			localEffectFrame.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createDashedBorder(effectColor, 2.0f, 2.0f, 1.5f, false),
				BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
			));
			
//			addFocusMouseListener(productionPanel, localEffectFrame, "effectFrame");
			
			effectFrame = localEffectFrame;
//			initialEffectBounds = creationBounds;
			
			// Ensure effect frame is shown in front of selection frame
			if(selectionFrame != null) {
				final JPanel localSelectionFrame = selectionFrame; 
				runner.run(new Runnable() {
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
				runner.run(new Runnable() {
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

	public void reset(final TranscriberCollector<Model> collector) {
		reset(new Runner() {
			@Override
			public void run(final Runnable runnable) {
				collector.afterNextFlush(new TranscriberOnFlush<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						runnable.run();
					}
				});
			}
		});
	}

	public void reset(final TranscriberBranch<Model> branch) {
		reset(new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
	}
	
	public void reset(Runner runner) {
		clearFocus(runner);
		clearEffectFrameOnBranch(runner);
	}

	public Rectangle getEffectFrameBounds() {
		return effectFrame.getBounds();
	}
	
	public void changeEffectFrameDirect2(final Rectangle newBounds, final TranscriberCollector<Model> collector) {
		changeEffectFrameDirect2(newBounds, new Runner() {
			@Override
			public void run(final Runnable runnable) {
				collector.afterNextFlush(new TranscriberOnFlush<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						runnable.run();
					}
				});
			}
		});
	}
	
	public void changeEffectFrameDirect2(final Rectangle newBounds, final RunBuilder runBuilder) {
		changeEffectFrameDirect2(newBounds, new Runner() {
			@Override
			public void run(Runnable runnable) {
				runBuilder.addRunnable(runnable);
			}
		});
	}
	
	public void changeEffectFrameDirect2(final Rectangle newBounds, Runner runner) {
		if(effectFrame != null) {
			final JPanel localEffectFrame = effectFrame;
			
			runner.run(new Runnable() {
				@Override
				public void run() {
					localEffectFrame.setBounds(newBounds);
				}
			});
		}
	}

	public void setEffectFrameCursor(final Cursor cursor) {
		effectFrame.setCursor(cursor);
	}

	public void setSelectionFrameCursor(Cursor cursor) {
		selectionFrame.setCursor(cursor);
	}

	public Point getSelectionFrameLocation() {
		return selectionFrame.getLocation();
	}

	public Dimension getSelectionFrameSize() {
		return selectionFrame.getSize();
	}

	public Rectangle getSelectionFrameBounds() {
		return selectionFrame.getBounds();
	}

	public int getEffectFrameWidth() {
		return effectFrame.getWidth();
	}

	public int getEffectFrameHeight() {
		return effectFrame.getHeight();
	}

	public void showPopupForSelectionObject(JComponent popupMenuInvoker, Point pointOnInvoker, ModelComponent targetOver, TranscriberBranch<Model> branch) {
		showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new DragDragDropPopupBuilder(branch));
	}

	public void showPopupForSelectionCons(JComponent popupMenuInvoker, Point pointOnInvoker, ModelComponent targetOver, TranscriberConnection<Model> connection) {
		showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ConsDragDropPopupBuilder(connection));
	}

	public void showPopupForSelectionTell(JComponent popupMenuInvoker, Point pointOnInvoker, ModelComponent targetOver, TranscriberBranch<Model> branch) {
		showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new TellDragDropPopupBuilder(branch));
	}

	public void showPopupForSelectionView(JComponent popupMenuInvoker, Point pointOnInvoker, ModelComponent targetOver, TranscriberBranch<Model> branch) {
		showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ViewDragDropPopupBuilder(branch));
	}

	public void selectFromEmpty(ModelComponent view, Point initialMouseDown, final TranscriberBranch<Model> branch) {
		selectFromEmpty(view, initialMouseDown, new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
	}

	public void selectFromEmpty(ModelComponent view, Point initialMouseDown, Runner runner) {
		select(view, runner);
		createEffectFrame(new Rectangle(0, 0, 0, 0), runner);
	}
	
	private void showPopupForSelection(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, final DragDropPopupBuilder popupBuilder) {
		if(selection != null) {
//			System.out.println("Here@popup");
			JPopupMenu transactionsPopupMenu = new JPopupMenu() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				private boolean ignoreNextPaint;
				
				public void paint(java.awt.Graphics g) {
					super.paint(g);
					if(!ignoreNextPaint) {
						productionPanel.livePanel.repaint();
						ignoreNextPaint = true;
					} else {
						ignoreNextPaint = false;
					}
				}
			};

			Point pointOnTargetOver = SwingUtilities.convertPoint(popupMenuInvoker, pointOnInvoker, (JComponent)targetOver);
			Rectangle droppedBounds = SwingUtilities.convertRectangle(productionPanel, effectFrame.getBounds(), (JComponent)targetOver);
			popupBuilder.buildFromSelectionAndTarget(productionPanel.livePanel, transactionsPopupMenu, selection, targetOver, pointOnTargetOver, droppedBounds);

			transactionsPopupMenu.show(popupMenuInvoker, pointOnInvoker.x, pointOnInvoker.y);
			productionPanel.livePanel.repaint();
			
			transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
					
				}
				
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {

				}
				
				@Override
				public void popupMenuCanceled(PopupMenuEvent arg0) {
					popupBuilder.cancelPopup(productionPanel.livePanel);
				}
			});
		}
	}

	public Rectangle getPlotBounds(Point firstPoint, Point secondPoint) {
		int left = Math.min(firstPoint.x, secondPoint.x);
		int right = Math.max(firstPoint.x, secondPoint.x);
		int top = Math.min(firstPoint.y, secondPoint.y);
		int bottom = Math.max(firstPoint.y, secondPoint.y);
		
		return new Rectangle(left, top, right - left, bottom - top);
	}

	public JComponent getSelectionFrame() {
		return selectionFrame;
	}
}

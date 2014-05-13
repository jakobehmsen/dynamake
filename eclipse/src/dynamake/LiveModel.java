package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.prevayler.Transaction;

public class LiveModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class SelectionChanged {
		
	}

	public static class OutputChanged {
		
	}

	public static class StateChanged {
		
	}
	
	public static final int STATE_USE = 0;
	public static final int STATE_EDIT = 1;
	public static final int STATE_PLOT = 2;
	public static final int STATE_BIND = 3;
	public static final int STATE_DRAG = 4;
	public static final int STATE_CONS = 5;
	
	private int state;
	private Model content;
	private Model selection;
	private Model output;
	
	public LiveModel(Model content) {
		this.content = content;
	}
	
	@Override
	public Model modelCloneIsolated() {
		LiveModel clone = new LiveModel(content.cloneIsolated());
		
		clone.state = state;
		clone.selection = this.selection.cloneIsolated();
		
		return clone;
	}
	
	public void setSelection(Model selection, PropogationContext propCtx, int propDistance) {
		this.selection = selection;
		sendChanged(new SelectionChanged(), propCtx, propDistance, 0);
	}

	public void setOutput(Model output, PropogationContext propCtx, int propDistance) {
		this.output = output;
		
		sendChanged(new SelectionChanged(), propCtx, propDistance, 0);
	}

	public int getState() {
		return state;
	}
	
	public void setState(int state, PropogationContext propCtx, int propDistance) {
		this.state = state;
		sendChanged(new StateChanged(), propCtx, propDistance, 0);
	}
	
	public static class SetSelection implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location liveModelLocation;
		private Location modelLocation;

		public SetSelection(Location liveModelLocation, Location modelLocation) {
			this.liveModelLocation = liveModelLocation;
			this.modelLocation = modelLocation;
		}
		
		@Override
		public void executeOn(Model prevalentSystem, Date executionTime) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
			if(modelLocation != null) {
				Model selection = (Model)modelLocation.getChild(prevalentSystem);
				liveModel.setSelection(selection, new PropogationContext(), 0);
			} else {
				liveModel.setSelection(null, new PropogationContext(), 0);
			}
		}
	}
	
	public static class SetOutput implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location liveModelLocation;
		private Location modelLocation;

		public SetOutput(Location liveModelLocation, Location modelLocation) {
			this.liveModelLocation = liveModelLocation;
			this.modelLocation = modelLocation;
		}
		
		@Override
		public void executeOn(Model prevalentSystem, Date executionTime) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
			if(modelLocation != null) {
				Model selection = (Model)modelLocation.getChild(prevalentSystem);
				liveModel.setOutput(selection, new PropogationContext(), 0);
			} else {
				liveModel.setOutput(null, new PropogationContext(), 0);
			}
		}
	}
	
	// TODO: Consider: Should be renamed to SetMode instead?
	// Or to SetTool
	// Or to SetRole
	// - and reflect this naming a appropriate locations
	public static class SetState implements Transaction<LiveModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int state;

		public SetState(int state) {
			this.state = state;
		}
		
		@Override
		public void executeOn(LiveModel prevalentSystem, Date executionTime) {
			prevalentSystem.setState(state, new PropogationContext(), 0);
		}
	}
	
	public static class ContentLocator implements dynamake.Locator {
		@Override
		public Location locate() {
			return new ContentLocation();
		}
	}
	
	private static class ContentLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object getChild(Object holder) {
			return ((LiveModel)holder).content;
		}

		@Override
		public void setChild(Object holder, Object child) {
			((LiveModel)holder).content = (Model)child;
		}
	}
	
	private static final Color TOP_BACKGROUND_COLOR = Color.GRAY;
	private static final Color TOP_FOREGROUND_COLOR = Color.WHITE;
	
	private static JRadioButton createStateRadioButton(final TransactionFactory transactionFactory, ButtonGroup group, int currentState, final int state, String text) {
		JRadioButton radioButton = new JRadioButton(text);
		radioButton.setBackground(TOP_BACKGROUND_COLOR);
		radioButton.setForeground(TOP_FOREGROUND_COLOR);
		radioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				transactionFactory.execute(new SetState(state));
			}
		});
		radioButton.setFocusable(false);
		group.add(radioButton);
		if(currentState == state) {
			radioButton.setSelected(true);
		}
		return radioButton;
	}
	
	public static class ProductionPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public JPanel effectFrame;
		public JPanel selectionFrame;
		public JPanel targetFrame;
		public JPanel outputFrame;

		public static final Color TARGET_OVER_COLOR = new Color(35, 89, 184);
		public static final Color BIND_COLOR = new Color(25, 209, 89);
		public static final Color UNBIND_COLOR = new Color(240, 34, 54);
		public static final Color SELECTION_COLOR = Color.GRAY;
//		public static final Color OUTPUT_COLOR = new Color(155, 235, 235);
//		public static final Color OUTPUT_COLOR = Color.GREEN;
//		public static final Color OUTPUT_COLOR = new Color(180, 232, 111);
//		public static final Color OUTPUT_COLOR = new Color(217, 240, 173);
		public static final Color OUTPUT_COLOR = new Color(54, 240, 17);
		
		public static class EditPanelMouseAdapter extends MouseAdapter {
			public ProductionPanel productionPanel;
			
			public ModelComponent selection;
			public boolean effectFrameMoving;
			public Point selectionMouseDown;
			public Point initialEffectLocation;
			public Rectangle initialEffectBounds;
			public Dimension selectionFrameSize;
			public int selectionFrameHorizontalPosition;
			public int selectionFrameVerticalPosition;
			public ModelComponent targetOver;
			
			public ModelComponent output;
			
			public static final int HORIZONTAL_REGION_WEST = 0;
			public static final int HORIZONTAL_REGION_CENTER = 1;
			public static final int HORIZONTAL_REGION_EAST = 2;
			public static final int VERTICAL_REGION_NORTH = 0;
			public static final int VERTICAL_REGION_CENTER = 1;
			public static final int VERTICAL_REGION_SOUTH = 2;
			
//			private Model.RemovableListener removableListener;
			
			public EditPanelMouseAdapter(ProductionPanel productionPanel) {
				this.productionPanel = productionPanel;
				
//				removableListener = Model.RemovableListener.addObserver(productionPanel.livePanel.model, new Observer() {
//					@Override
//					public void changed(Model sender, Object change,
//							PropogationContext propCtx, int propDistance,
//							int changeDistance) {
//						if(change instanceof SetSelection) {
//							ModelComponent view = productionPanel.livePanel.model.getProperty("Selected");
//							select2();
//						}
//					}
//
//					@Override
//					public void addObservee(Observer observee) { }
//
//					@Override
//					public void removeObservee(Observer observee) { }
//				});
			}
			
			private Tool getTool() {
				return productionPanel.livePanel.viewManager.getTools()[productionPanel.livePanel.model.state - 1];
			}

			public void resetEffectFrame() {
				productionPanel.effectFrame.setBounds(new Rectangle(0, 0, 0, 0));
				productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionEffectBounds", productionPanel.effectFrame.getBounds()));
			}
			
			public void updateRelativeCursorPosition(Point point, Dimension size) {
				int resizeWidth = 5;
				
				int leftPositionEnd = resizeWidth;
				int rightPositionStart = size.width - resizeWidth;

				int topPositionEnd = resizeWidth;
				int bottomPositionStart = size.height - resizeWidth;
				
				selectionFrameHorizontalPosition = 1;
				selectionFrameVerticalPosition = 1;
				
				if(point.x <= leftPositionEnd)
					selectionFrameHorizontalPosition = HORIZONTAL_REGION_WEST;
				else if(point.x < rightPositionStart)
					selectionFrameHorizontalPosition = HORIZONTAL_REGION_CENTER;
				else
					selectionFrameHorizontalPosition = HORIZONTAL_REGION_EAST;
				
				if(point.y <= topPositionEnd)
					selectionFrameVerticalPosition = VERTICAL_REGION_NORTH;
				else if(point.y < bottomPositionStart)
					selectionFrameVerticalPosition = VERTICAL_REGION_CENTER;
				else
					selectionFrameVerticalPosition = VERTICAL_REGION_SOUTH;
			}
			
			public void selectFromView(final ModelComponent view, final Point initialMouseDown, boolean moving) {
				Rectangle effectBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
				select(view, initialMouseDown, moving, effectBounds);
			}
			
			public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, boolean moving) {
				Dimension sourceBoundsSize = new Dimension(125, 33);
				Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
				Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
				Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, productionPanel);
				select(view, initialMouseDown, moving, selectionBounds);
			}
			
			public void selectFromEmpty(final ModelComponent view, final Point initialMouseDown, boolean moving) {
				select(view, initialMouseDown, moving, new Rectangle(0, 0, 0, 0));
			}
			
//			private void requestSelect(final ModelComponent view, final Point initialMouseDown, boolean moving, Rectangle effectBounds) {
//				productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionInitialMouseDown", initialMouseDown));
//				productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionMoving", moving));
//				productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionEffectBounds", effectBounds));
//				productionPanel.livePanel.getTransactionFactory().executeOnRoot(new SetSelection(productionPanel.livePanel.getTransactionFactory().getLocation(), view.getTransactionFactory().getLocation()));
//			}
			
			private void select(final ModelComponent view, final Point initialMouseDown, boolean moving, Rectangle effectBounds) {
				// <Don't remove>
				// Whether the following check is necessary or not has not been decided yet, so don't remove the code
//				if(this.selection == view)
//					return;
				// </Don't remove>
				
				this.selection = view;
				
				if(this.selection != null) {
					if(productionPanel.effectFrame == null) {
						productionPanel.effectFrame = new JPanel();
						productionPanel.effectFrame.setBackground(new Color(0, 0, 0, 0));
						
						productionPanel.effectFrame.setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createDashedBorder(Color.BLACK, 2.0f, 2.0f, 1.5f, false),
							BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
						));
						productionPanel.add(productionPanel.effectFrame);
					}
					
					if(productionPanel.selectionFrame == null) {
						productionPanel.selectionFrame = new JPanel();
						productionPanel.selectionFrame.setBackground(new Color(0, 0, 0, 0));

						productionPanel.selectionFrame.setBorder(
							BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(Color.BLACK, 1), 
								BorderFactory.createCompoundBorder(
									BorderFactory.createLineBorder(SELECTION_COLOR, 3), 
									BorderFactory.createLineBorder(Color.BLACK, 1)
								)
							)
						);
						
						MouseAdapter mouseAdapter = new MouseAdapter() {
							@Override
							public void mouseMoved(MouseEvent e) {
								getTool().mouseMoved(productionPanel, e);
							}

							public void mouseExited(MouseEvent e) {
								getTool().mouseExited(productionPanel, e);
							}

							@Override
							public void mousePressed(MouseEvent e) {
								if(EditPanelMouseAdapter.this.output != null) {
									setOutput(null);
									productionPanel.livePanel.getTransactionFactory().executeOnRoot(new SetOutput(productionPanel.livePanel.getTransactionFactory().getLocation(), null));
								}
								
								getTool().mousePressed(productionPanel, e);
							}

							@Override
							public void mouseDragged(MouseEvent e) {
								getTool().mouseDragged(productionPanel, e);
							}

							@Override
							public void mouseReleased(MouseEvent e) {
								getTool().mouseReleased(productionPanel, e);
							}
						};
						
						productionPanel.selectionFrame.addMouseListener(mouseAdapter);
						productionPanel.selectionFrame.addMouseMotionListener(mouseAdapter);

						productionPanel.add(productionPanel.selectionFrame);
					}
					
					selectionMouseDown = initialMouseDown;
					selectionFrameSize = ((JComponent)view).getSize();
					effectFrameMoving = moving;
					updateRelativeCursorPosition(initialMouseDown, ((JComponent)view).getSize());
					productionPanel.effectFrame.setBounds(effectBounds);
					initialEffectLocation = effectBounds.getLocation();
					this.initialEffectBounds = effectBounds;
					
					Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
					productionPanel.selectionFrame.setBounds(selectionBounds);
					
					// final ModelComponent view, final Point initialMouseDown, boolean moving, Rectangle effectBounds
					productionPanel.livePanel.getTransactionFactory().executeOnRoot(new SetSelection(productionPanel.livePanel.getTransactionFactory().getLocation(), view.getTransactionFactory().getLocation()));
					productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionInitialMouseDown", initialMouseDown));
					productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionMoving", moving));
					productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionEffectBounds", effectBounds));
				} else {
					if(productionPanel.effectFrame != null) {
						productionPanel.clearFocus();
					}
					productionPanel.livePanel.getTransactionFactory().executeOnRoot(new SetSelection(productionPanel.livePanel.getTransactionFactory().getLocation(), null));
				}
			}
			
			public void showPopupForSelectionObject(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new DragDragDropPopupBuilder());
			}
			
			public void showPopupForSelectionCons(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ConsDragDropPopupBuilder());
			}
			
			public void showPopupForSelectionTell(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new TellDragDropPopupBuilder());
			}

			public void showPopupForSelectionView(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ViewDragDropPopupBuilder());
			}
			
			private void showPopupForSelection(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, DragDropPopupBuilder popupBuilder) {
				if(selection != null) {
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
					Rectangle droppedBounds = SwingUtilities.convertRectangle(productionPanel, productionPanel.effectFrame.getBounds(), (JComponent)targetOver);
					popupBuilder.buildFromSelectionAndTarget(productionPanel.livePanel, transactionsPopupMenu, selection, targetOver, pointOnTargetOver, droppedBounds);

					transactionsPopupMenu.show(popupMenuInvoker, pointOnInvoker.x, pointOnInvoker.y);
					productionPanel.livePanel.repaint();
					
					transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
						@Override
						public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
							
						}
						
						@Override
						public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
							if(productionPanel.targetFrame != null) {
								productionPanel.remove(productionPanel.targetFrame);
								productionPanel.targetFrame = null;
							}

							resetEffectFrame();
							productionPanel.livePanel.repaint();
						}
						
						@Override
						public void popupMenuCanceled(PopupMenuEvent arg0) {

						}
					});
				}
			}
			
			public ModelComponent closestModelComponent(Component component) {
				while(component != null && !(component instanceof ModelComponent))
					component = component.getParent();
				return (ModelComponent)component;
			}
			
			public Rectangle getPlotBounds(Point firstPoint, Point secondPoint) {
				int left = Math.min(firstPoint.x, secondPoint.x);
				int right = Math.max(firstPoint.x, secondPoint.x);
				int top = Math.min(firstPoint.y, secondPoint.y);
				int bottom = Math.max(firstPoint.y, secondPoint.y);
				
				return new Rectangle(left, top, right - left, bottom - top);
			}
			
			public void mousePressed(MouseEvent e) {
				if(this.output != null) {
					setOutput(null);
					productionPanel.livePanel.getTransactionFactory().executeOnRoot(new SetOutput(productionPanel.livePanel.getTransactionFactory().getLocation(), null));
				}
				getTool().mousePressed(productionPanel, e);
			}

			public void mouseDragged(MouseEvent e) {
				e.translatePoint(-productionPanel.selectionFrame.getX(), -productionPanel.selectionFrame.getY());
				e.setSource(productionPanel.selectionFrame);
				for(MouseMotionListener l: productionPanel.selectionFrame.getMouseMotionListeners()) {
					l.mouseDragged(e);
				}
			}

			public void mouseReleased(MouseEvent e) {
				if(productionPanel.selectionFrame != null) {
					e.translatePoint(-productionPanel.selectionFrame.getX(), -productionPanel.selectionFrame.getY());
					e.setSource(productionPanel.selectionFrame);
					for(MouseListener l: productionPanel.selectionFrame.getMouseListeners()) {
						l.mouseReleased(e);
					}
				}
			}

			public void setOutput(ModelComponent view) {
				this.output = view;
				if(view != null) {
					if(productionPanel.outputFrame == null) {
						productionPanel.outputFrame = new JPanel();
						productionPanel.outputFrame.setBackground(new Color(0, 0, 0, 0));
						
//						Color color = Color.GRAY;
						
						productionPanel.outputFrame.setBorder(
							BorderFactory.createBevelBorder(
								BevelBorder.RAISED, OUTPUT_COLOR.darker().darker(), OUTPUT_COLOR.darker(), OUTPUT_COLOR.darker().darker().darker(), OUTPUT_COLOR.darker().darker())
						);
						
//						productionPanel.outputFrame.setBorder(
//							BorderFactory.createCompoundBorder(
//								BorderFactory.createBevelBorder(BevelBorder.RAISED, OUTPUT_COLOR.darker().darker(), OUTPUT_COLOR.darker(), OUTPUT_COLOR.darker().darker(), OUTPUT_COLOR.darker()),
//								BorderFactory.createBevelBorder(BevelBorder.RAISED, OUTPUT_COLOR.darker(), OUTPUT_COLOR, OUTPUT_COLOR.darker().darker(), OUTPUT_COLOR.darker())
//							)
//						);
		
//						productionPanel.outputFrame.setBorder(
//							BorderFactory.createCompoundBorder(
//								BorderFactory.createLineBorder(Color.BLACK, 2), 
//								BorderFactory.createCompoundBorder(
//									BorderFactory.createLineBorder(new Color(155, 235, 235), 2), 
//									BorderFactory.createLineBorder(Color.BLACK, 2)
//								)
//							)
//						);
						
//						Color outputColorDarkened = OUTPUT_COLOR.darker().darker();
//						productionPanel.outputFrame.setBorder(
//							BorderFactory.createCompoundBorder(
//								BorderFactory.createLineBorder(Color.BLACK, 1), 
//								BorderFactory.createCompoundBorder(
//									BorderFactory.createLineBorder(OUTPUT_COLOR, 2), 
//									BorderFactory.createCompoundBorder(
//										BorderFactory.createLineBorder(outputColorDarkened, 2), 
//										BorderFactory.createCompoundBorder(
//											BorderFactory.createLineBorder(OUTPUT_COLOR, 2), 
//											BorderFactory.createLineBorder(Color.BLACK, 1)
//										)
//									)
//								)
//							)
//						);
						
//						productionPanel.outputFrame.setBorder(
//							BorderFactory.createCompoundBorder(
//								BorderFactory.createLineBorder(Color.BLACK, 1), 
//								BorderFactory.createCompoundBorder(
//									BorderFactory.createLineBorder(new Color(155, 235, 235), 2), 
//									BorderFactory.createCompoundBorder(
////										BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
//										BorderFactory.createLineBorder(new Color(71, 92, 92), 2), 
//										BorderFactory.createCompoundBorder(
//											BorderFactory.createLineBorder(new Color(155, 235, 235), 2), 
//											BorderFactory.createLineBorder(Color.BLACK, 1)
//										)
//									)
//								)
//							)
//						);
						
//						productionPanel.outputFrame.setBorder(
//							BorderFactory.createCompoundBorder(
//								BorderFactory.createMatteBorder(2, 0, 0, 0, OUTPUT_COLOR.darker().darker()), 
//								BorderFactory.createCompoundBorder(
//									BorderFactory.createMatteBorder(0, 2, 0, 0, OUTPUT_COLOR.darker().darker()), 
//									BorderFactory.createCompoundBorder(
//										BorderFactory.createMatteBorder(0, 0, 2, 0, OUTPUT_COLOR.darker()), 
//										BorderFactory.createMatteBorder(0, 0, 0, 2, OUTPUT_COLOR.darker())
//									)
//								)
//							)
//						);
						
//						productionPanel.outputFrame.setBorder(
//							BorderFactory.createCompoundBorder(
//								BorderFactory.createLineBorder(Color.BLACK, 2),
//								BorderFactory.createCompoundBorder(
//									BorderFactory.createMatteBorder(2, 2, 12, 2, Color.WHITE),
//									BorderFactory.createLineBorder(Color.BLACK, 2)
//								)
//							)
//						);
						
						productionPanel.add(productionPanel.outputFrame);
					}
					
					Rectangle outputBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
					productionPanel.outputFrame.setBounds(outputBounds);
				} else {
					productionPanel.remove(productionPanel.outputFrame);
					productionPanel.outputFrame = null;
				}
			}
		}
		
		public LivePanel livePanel;
		public Binding<ModelComponent> contentView;
		public EditPanelMouseAdapter editPanelMouseAdapter;
		
		public ProductionPanel(final LivePanel livePanel, final Binding<ModelComponent> contentView) {
			this.setLayout(null);
			effectFrame = null;
			this.livePanel = livePanel;
			this.contentView = contentView;
			
			// TODO: Consider the following:
			// For a selected frame, it should be possible to scroll upwards to select its immediate parent
			// - and scroll downwards to select its root parents
			editPanelMouseAdapter = new EditPanelMouseAdapter(this);

			this.addMouseListener(editPanelMouseAdapter);
			this.addMouseMotionListener(editPanelMouseAdapter);
			
			this.setOpaque(true);
			this.setBackground(new Color(0, 0, 0, 0));
		}

		public void clearFocus() {
			if(effectFrame != null) {
				this.remove(effectFrame);
				effectFrame = null;
				this.remove(selectionFrame);
				selectionFrame = null;
			}
			
			if(selectionFrame != null) {
				this.remove(selectionFrame);
				selectionFrame = null;
			}
			
			this.livePanel.transactionFactory.executeOnRoot(new SetSelection(this.livePanel.transactionFactory.getLocation(), null));
		}
	}
	
	public static class LivePanel extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private LiveModel model;
		private JPanel topPanel;
		private JLayeredPane contentPane;
		private RemovableListener removableListener;
		private ProductionPanel productionPanel;
		public ViewManager viewManager;
		private TransactionFactory transactionFactory;
		
		public LivePanel(LiveModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.setLayout(new BorderLayout());
			this.model = model;
			this.viewManager = viewManager;
			this.transactionFactory = transactionFactory;

			final ModelComponent[] selectedViewHolder = new ModelComponent[1];
			final ModelComponent[] outputViewHolder = new ModelComponent[1];
			
			ViewManager newViewManager = new ViewManager() {
				@Override
				public void setFocus(JComponent component) { }
				
				@Override
				public void unFocus(ModelComponent view) {
					if(productionPanel.editPanelMouseAdapter.selection == view)
						productionPanel.clearFocus();
				}
				
				@Override
				public void selectAndActive(ModelComponent view, int x, int y) { }
				
				@Override
				public int getState() {
					return LivePanel.this.model.getState();
				}
				
				@Override
				public Factory[] getFactories() {
					return viewManager.getFactories();
				}
				
				@Override
				public void clearFocus() {
					productionPanel.clearFocus();
				}
				
				@Override
				public void repaint(JComponent view) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							LivePanel.this.repaint();
						}
					});
				}
				
				@Override
				public void refresh(ModelComponent view) {
					LivePanel.this.repaint();
				}
				
				@Override
				public void wasCreated(ModelComponent view) {
//					PropogationContext propCtx = new PropogationContext(); // As arg
//					PropogationContext addedPropCtx = new PropogationContext(); // As local
//					
//					if(propCtx.isOrDerivesFrom(addedPropCtx)) {
//						// Select the view 
//					}
					
//					if(productionPanel != null && LivePanel.this.model.output == view.getModelBehind()) {
//						// Output was created as a view
////						new String();
//						productionPanel.editPanelMouseAdapter.setOutput(view);
//					}
					
					if(LivePanel.this.model.selection != null && LivePanel.this.model.selection == view.getModelBehind()) {
						selectedViewHolder[0] = view;
					}
					
					if(LivePanel.this.model.output != null && LivePanel.this.model.output == view.getModelBehind()) {
						outputViewHolder[0] = view;
					}
				}
				
				@Override
				public void becameVisible(ModelComponent view) {
					if(productionPanel != null && LivePanel.this.model.output == view.getModelBehind()) {
						// Output was created as a view
//						new String();
						productionPanel.editPanelMouseAdapter.setOutput(view);
					}
				}
				
				@Override
				public void becameInvisible(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public Tool[] getTools() {
					// TODO Auto-generated method stub
					return null;
				}
			};
//			this.viewManager = newViewManager;
			final Binding<ModelComponent> contentView = model.getContent().createView(newViewManager, transactionFactory.extend(new ContentLocator()));

			productionPanel = new ProductionPanel(this, contentView);
			
			topPanel = new JPanel();
			topPanel.setBackground(TOP_BACKGROUND_COLOR);
			ButtonGroup group = new ButtonGroup();
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_USE, "Use"));
			
			Tool[] tools = viewManager.getTools();
			for(int i = 0; i < tools.length; i++) {
				Tool tool = tools[i];
				topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), i + 1, tool.getName()));
			}
			topPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			
			contentPane = new JLayeredPane();
			productionPanel.setSize(contentPane.getSize().width, contentPane.getSize().height - 1);
			
			contentPane.addComponentListener(new ComponentListener() {
				@Override
				public void componentShown(ComponentEvent e) { }
				
				@Override
				public void componentResized(ComponentEvent e) {
					((JComponent)contentView.getBindingTarget()).setSize(((JComponent)e.getSource()).getSize());
					if(productionPanel != null) {
						productionPanel.setSize(((JComponent)e.getSource()).getSize().width, ((JComponent)e.getSource()).getSize().height - 1);
					}
				}
				
				@Override
				public void componentMoved(ComponentEvent e) { }
				
				@Override
				public void componentHidden(ComponentEvent e) { }
			});
			
			contentPane.add((JComponent)contentView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
			
			this.add(topPanel, BorderLayout.NORTH);
			this.add(contentPane, BorderLayout.CENTER);
			
			removableListener = Model.RemovableListener.addObserver(model, new ObserverAdapter() {
				int previousState;
				
				{
					initialize();
				}
				
				private void initialize() {
					int state = LivePanel.this.model.getState();
					if(state != LiveModel.STATE_USE) {
						contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
						contentPane.revalidate();
						contentPane.repaint();
					}
					previousState = state;
				}
				
				@Override
				public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof LiveModel.StateChanged) {
						if(previousState == LiveModel.STATE_USE && LivePanel.this.model.getState() != LiveModel.STATE_USE) {
							contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
							contentPane.revalidate();
							contentPane.repaint();
						} else if(previousState != LiveModel.STATE_USE && LivePanel.this.model.getState() == LiveModel.STATE_USE) {
							contentPane.remove(productionPanel);
							contentPane.revalidate();
							contentPane.repaint();
						}
						
						previousState = LivePanel.this.model.getState();
					}
				}
			});
			
			if(selectedViewHolder[0] != null) {
				Point initialMouseDown = (Point)model.getProperty("SelectionInitialMouseDown");
				boolean moving = (boolean)model.getProperty("SelectionMoving");
				Rectangle effectBounds = (Rectangle)model.getProperty("SelectionEffectBounds");

				LivePanel.this.productionPanel.editPanelMouseAdapter.select(selectedViewHolder[0], initialMouseDown, moving, effectBounds);
			}
			
			if(outputViewHolder[0] != null) {
				LivePanel.this.productionPanel.editPanelMouseAdapter.setOutput(outputViewHolder[0]);
			}
		}
		
		public Factory[] getFactories() {
			return viewManager.getFactories();
		}

		@Override
		public Model getModelBehind() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		public void releaseBinding() {
			removableListener.releaseBinding();
		}

		@Override
		public Transaction<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager, TransactionFactory transactionFactory) {
		final LivePanel view = new LivePanel(this, transactionFactory, viewManager);
		
		viewManager.wasCreated(view);
		
		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
				view.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}

	public Model getContent() {
		return content;
	}
}

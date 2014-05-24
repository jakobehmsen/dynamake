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
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

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
		
		sendChanged(new OutputChanged(), propCtx, propDistance, 0);
	}

	public int getState() {
		return state;
	}
	
	public void setState(int state, PropogationContext propCtx, int propDistance) {
		this.state = state;
		sendChanged(new StateChanged(), propCtx, propDistance, 0);
	}
	
	public static class SetSelection implements Command<Model> {
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
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
			if(modelLocation != null) {
				Model selection = (Model)modelLocation.getChild(prevalentSystem);
				liveModel.setSelection(selection, new PropogationContext(), 0);
			} else {
				liveModel.setSelection(null, new PropogationContext(), 0);
			}
		}
	}
	
	public static class SetOutput implements Command<Model> {
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
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
			if(modelLocation != null) {
				Model selection = (Model)modelLocation.getChild(prevalentSystem);
				liveModel.setOutput(selection, new PropogationContext(), 0);
			} else {
				liveModel.setOutput(null, new PropogationContext(), 0);
			}
		}
		
		public static DualCommand<Model> createDual(LiveModel.LivePanel livePanel, Location outputLocation) {
			Location currentOutputLocation = null;
			if(livePanel.productionPanel.editPanelMouseAdapter.output != null)
				currentOutputLocation = livePanel.productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
			
			return new DualCommandPair<Model>(
				new SetOutput(livePanel.getTransactionFactory().getModelLocation(), outputLocation), 
				new SetOutput(livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
			);
		}
	}
	
	// TODO: Consider: Should be renamed to SetMode instead?
	// Or to SetTool
	// Or to SetRole
	// - and reflect this naming a appropriate locations
	public static class SetState implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		private int state;

		public SetState(Location modelLocation, int state) {
			this.modelLocation = modelLocation;
			this.state = state;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
			LiveModel model = (LiveModel)modelLocation.getChild(prevalentSystem);
			model.setState(state, propCtx, 0);
		}
	}
	
	public static class ContentLocator implements dynamake.ModelLocator {
		@Override
		public ModelLocation locate() {
			return new FieldContentLocation();
		}
	}
	
	private static class FieldContentLocation implements ModelLocation {
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

		@Override
		public Location getModelComponentLocation() {
			// TODO Auto-generated method stub
			return new ViewFieldContentLocation();
		}
	}
	
	private static class ViewFieldContentLocation implements Location {
		@Override
		public Object getChild(Object holder) {
			return ((LivePanel)holder).contentView.getBindingTarget();
		}
		
		@Override
		public void setChild(Object holder, Object child) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private static final Color TOP_BACKGROUND_COLOR = Color.GRAY;
	private static final Color TOP_FOREGROUND_COLOR = Color.WHITE;
	
	public static final int TAG_CAUSED_BY_UNDO = 0;
	public static final int TAG_CAUSED_BY_REDO = 1;
	public static final int TAG_CAUSED_BY_TOGGLE_BUTTON = 2;
	public static final int TAG_CAUSED_BY_ROLLBACK = 3;
	public static final int TAG_CAUSED_BY_COMMIT = 4;
	
	private static JToggleButton createToolButton(final LiveModel model, final TransactionFactory transactionFactory, ButtonGroup group, int currentState, final int state, String text) {
//		JRadioButton radioButton = new JRadioButton(text);
		JToggleButton buttonTool = new JToggleButton(text);
		buttonTool.setBackground(TOP_BACKGROUND_COLOR);
		buttonTool.setForeground(TOP_FOREGROUND_COLOR);
		buttonTool.setBorderPainted(false);
		
		buttonTool.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Indicate this is an radio button toggle context
				PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_TOGGLE_BUTTON);
				
				transactionFactory.executeOnRoot(propCtx, new DualCommandFactory<Model>() {
					public DualCommand<Model> createDualCommand() {
						Location modelLocation = transactionFactory.getModelLocation();
						int previousState = model.getState();
						return new DualCommandPair<Model>(new SetState(modelLocation, state), new SetState(modelLocation, previousState));
					}
					
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(createDualCommand());
					}
				});
			}
		});
		buttonTool.setFocusable(false);
		group.add(buttonTool);
		if(currentState == state) {
			buttonTool.setSelected(true);
		}
		return buttonTool;
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
		public Binding<Component> selectionBoundsBinding;

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
			
			public EditPanelMouseAdapter(ProductionPanel productionPanel) {
				this.productionPanel = productionPanel;
			}
			
			private Tool getTool() {
				return productionPanel.livePanel.viewManager.getTools()[productionPanel.livePanel.model.state - 1];
			}

			public void resetEffectFrame() {
				productionPanel.effectFrame.setBounds(new Rectangle(0, 0, 0, 0));
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
				requestSelect(view, initialMouseDown, moving, effectBounds);
			}
			
			public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, boolean moving) {
				Dimension sourceBoundsSize = new Dimension(125, 33);
				Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
				Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
				Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, productionPanel);
				requestSelect(view, initialMouseDown, moving, selectionBounds);
			}
			
			public void selectFromEmpty(final ModelComponent view, final Point initialMouseDown, boolean moving) {
				requestSelect(view, initialMouseDown, moving, new Rectangle(0, 0, 0, 0));
			}
			
			private void requestSelect(final ModelComponent view, final Point initialMouseDown, final boolean moving, final Rectangle effectBounds) {
				// Notice: executes a transaction

				PropogationContext propCtx = new PropogationContext();

				productionPanel.livePanel.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
					public DualCommand<Model> createDualCommand() {
						Point currentInitialMouseDown = (Point)productionPanel.livePanel.model.getProperty("SelectionInitialMouseDown");
						Boolean currentSelectionMoving = (Boolean)productionPanel.livePanel.model.getProperty("SelectionMoving");
						Rectangle currentSelectionEffectBounds = (Rectangle)productionPanel.livePanel.model.getProperty("SelectionEffectBounds");

						return new DualCommandPair<Model>(
							new SetSelectionAndLocalsTransaction(
								productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
								view.getTransactionFactory().getModelLocation(),
								initialMouseDown,
								moving,
								effectBounds
							),
							new SetSelectionAndLocalsTransaction(
								productionPanel.livePanel.getTransactionFactory().getModelLocation(),
								productionPanel.editPanelMouseAdapter.selection != null ? productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation() : null,
								currentInitialMouseDown,
								currentSelectionMoving,
								currentSelectionEffectBounds
							)
						);
					}
					
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(createDualCommand());
					}
				});
			}
			
			private static class SetSelectionAndLocalsTransaction implements Command<Model> {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				private Location liveModelLocation;
				private Location selectionLocation;
				private Point initialMouseDown;
				private Boolean moving;
				private Rectangle effectBounds;
				
				public SetSelectionAndLocalsTransaction(
						Location liveModelLocation, Location selectionLocation,
						Point initialMouseDown, Boolean moving,
						Rectangle effectBounds) {
					this.liveModelLocation = liveModelLocation;
					this.selectionLocation = selectionLocation;
					this.initialMouseDown = initialMouseDown;
					this.moving = moving;
					this.effectBounds = effectBounds;
				}

				@Override
				public void executeOn(PropogationContext propCtx,
						Model prevalentSystem, Date executionTime) {
					LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
					
					if(selectionLocation != null) {
						Model selection = (Model)selectionLocation.getChild(prevalentSystem);
						
						propCtx.define("SelectionInitialMouseDown", initialMouseDown);
						propCtx.define("SelectionMoving", moving);
						propCtx.define("SelectionEffectBounds", effectBounds);
						
						liveModel.setSelection(selection, propCtx, 0);
					} else {
						liveModel.setSelection(null, propCtx, 0);
					}
					liveModel.setProperty("SelectionInitialMouseDown", initialMouseDown, propCtx, 0);
					liveModel.setProperty("SelectionMoving", moving, propCtx, 0);
					liveModel.setProperty("SelectionEffectBounds", effectBounds, propCtx, 0);
				}
			}
			
			private void select(final ModelComponent view, final Point initialMouseDown, boolean moving, Rectangle effectBounds) {
				// <Don't remove>
				// Whether the following check is necessary or not has not been decided yet, so don't remove the code
//				if(this.selection == view)
//					return;
				// </Don't remove>
				
				this.selection = view;
				
				if(productionPanel.selectionBoundsBinding != null)
					productionPanel.selectionBoundsBinding.releaseBinding();
				
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
					
					productionPanel.selectionBoundsBinding = new Binding<Component>() {
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
									productionPanel.selectionFrame.setBounds(selectionBounds);
									productionPanel.livePanel.repaint();
								}
								
								@Override
								public void componentMoved(ComponentEvent arg0) {
									Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
									productionPanel.selectionFrame.setBounds(selectionBounds);
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
					productionPanel.livePanel.repaint();
				} else {
					if(productionPanel.effectFrame != null) {
						productionPanel.clearFocus();
					}
					
					productionPanel.livePanel.repaint();
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
			
			private void showPopupForSelection(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, final DragDropPopupBuilder popupBuilder) {
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
							clearTarget();

							resetEffectFrame();
							productionPanel.livePanel.repaint();
						}
						
						@Override
						public void popupMenuCanceled(PopupMenuEvent arg0) {
							popupBuilder.cancelPopup(productionPanel.livePanel);
						}
					});
				}
			}

			public void clearTarget() {
				if(productionPanel.targetFrame != null) {
					productionPanel.remove(productionPanel.targetFrame);
					productionPanel.targetFrame = null;
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
			
			public void mousePressed(final MouseEvent e) {
				productionPanel.livePanel.getTransactionFactory().executeTransient(new Runnable() {
					@Override
					public void run() {
						getTool().mousePressed(productionPanel, e);
					}
				});
			}

			public void mouseDragged(final MouseEvent e) {
				productionPanel.livePanel.getTransactionFactory().executeTransient(new Runnable() {
					@Override
					public void run() {
						e.translatePoint(-productionPanel.selectionFrame.getX(), -productionPanel.selectionFrame.getY());
						e.setSource(productionPanel.selectionFrame);
						for(MouseMotionListener l: productionPanel.selectionFrame.getMouseMotionListeners()) {
							l.mouseDragged(e);
						}
					}
				});
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
					if(productionPanel.outputFrame != null) {
						productionPanel.remove(productionPanel.outputFrame);
						productionPanel.outputFrame = null;
					}
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
				
				if(selectionBoundsBinding != null)
					selectionBoundsBinding.releaseBinding();
				
				this.remove(selectionFrame);
				selectionFrame = null;
			}
			
			if(selectionFrame != null) {
				if(selectionBoundsBinding != null)
					selectionBoundsBinding.releaseBinding();
				
				this.remove(selectionFrame);
				selectionFrame = null;
			}
			
//			PropogationContext propCtx = new PropogationContext();
//			this.livePanel.transactionFactory.executeOnRoot(propCtx, new SetSelection(this.livePanel.transactionFactory.getLocation(), null));
		}
	}
	
	public static class LivePanel extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public LiveModel model;
		private JPanel topPanel;
		private JLayeredPane contentPane;
		private RemovableListener removableListener;
		public ProductionPanel productionPanel;
		public ViewManager viewManager;
		private TransactionFactory transactionFactory;
		private JToggleButton[] buttonTools;
		private final Binding<ModelComponent> contentView;
		private ModelComponent rootView;
		
		public LivePanel(final ModelComponent rootView, LiveModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.rootView = rootView;
			this.setLayout(new BorderLayout());
			this.model = model;
			this.viewManager = viewManager;
			this.transactionFactory = transactionFactory;
			
			ViewManager newViewManager = new ViewManager() {
				@Override
				public void setFocus(JComponent component) { }
				
				@Override
				public void unFocus(PropogationContext propCtx, ModelComponent view) {
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
				public void clearFocus(PropogationContext propCtx) {
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
				public void wasCreated(ModelComponent view) { }
				
				@Override
				public void becameVisible(ModelComponent view) { }
				
				@Override
				public void becameInvisible(PropogationContext propCtx, ModelComponent view) { }
				
				@Override
				public Tool[] getTools() {
					return null;
				}
			};

			contentView = model.getContent().createView(rootView, newViewManager, transactionFactory.extend(new ContentLocator()));

			productionPanel = new ProductionPanel(this, contentView);
			
			topPanel = new JPanel();
			topPanel.setBackground(TOP_BACKGROUND_COLOR);
			
			JButton undo = new JButton("Undo");
			undo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_UNDO);
					// Indicate this is an undo context
					getTransactionFactory().undo(propCtx);
				}
			});
			topPanel.add(undo);
			topPanel.setFocusable(false);
			JButton redo = new JButton("Redo");
			redo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_REDO);
					// Indicate this is an redo context
					getTransactionFactory().redo(propCtx);
				}
			});
			topPanel.add(redo);
			topPanel.setFocusable(false);

			Tool[] tools = viewManager.getTools();
			buttonTools = new JToggleButton[1 + tools.length];
			ButtonGroup group = new ButtonGroup();
			
			buttonTools[0] = createToolButton(model, transactionFactory, group, this.model.getState(), STATE_USE, "Use");

			for(int i = 0; i < tools.length; i++) {
				Tool tool = tools[i];
				buttonTools[i + 1] = createToolButton(model, transactionFactory, group, this.model.getState(), i + 1, tool.getName());
			}
			for(JToggleButton buttonTool: buttonTools) {
				JPanel buttonToolWrapper = new JPanel();
				buttonToolWrapper.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
				buttonToolWrapper.setLayout(new BorderLayout());
				buttonToolWrapper.add(buttonTool, BorderLayout.CENTER);
				topPanel.add(buttonToolWrapper);
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
					initializeObserverAdapter();
				}
				
				private void initializeObserverAdapter() {
					int state = LivePanel.this.model.getState();
					if(state != LiveModel.STATE_USE) {
						contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
						contentPane.revalidate();
						contentPane.repaint();
					}
					previousState = state;
				}
				
				@Override
				public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof LiveModel.StateChanged) {
						if(!propCtx.isTagged(TAG_CAUSED_BY_TOGGLE_BUTTON)) {
							JToggleButton buttonNewTool = buttonTools[LivePanel.this.model.getState()];
							buttonNewTool.setSelected(true);
						}
						
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
					} else if(change instanceof LiveModel.OutputChanged) {
						if(LivePanel.this.model.output == null) {
							productionPanel.editPanelMouseAdapter.setOutput(null);
						} else {
							ModelLocator locator = LivePanel.this.model.output.getLocator();
							ModelLocation modelLocation = locator.locate();
							Location modelComponentLocation = modelLocation.getModelComponentLocation();
							ModelComponent view = (ModelComponent)modelComponentLocation.getChild(rootView);
							productionPanel.editPanelMouseAdapter.setOutput(view);
						}
						
						productionPanel.livePanel.repaint();
					} else if(change instanceof LiveModel.SelectionChanged) {
						if(LivePanel.this.model.selection != null) {
							// TODO: Consider whether this is a safe manner in which location of selection if derived.
							ModelLocator locator = LivePanel.this.model.selection.getLocator();
							ModelLocation modelLocation = locator.locate();
							Location modelComponentLocation = modelLocation.getModelComponentLocation();
							final ModelComponent view = (ModelComponent)modelComponentLocation.getChild(rootView);
							final Point initialMouseDown = (Point)propCtx.lookup("SelectionInitialMouseDown");
							final boolean moving = (boolean)propCtx.lookup("SelectionMoving");
							final Rectangle effectBounds = (Rectangle)propCtx.lookup("SelectionEffectBounds");
							
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									productionPanel.editPanelMouseAdapter.select(view, initialMouseDown, moving, effectBounds);
									productionPanel.livePanel.repaint();
								}
							});
						} else {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									productionPanel.editPanelMouseAdapter.select(null, null, false, null);
								}
							});
						}
					}
				}
			});
		}
		
		@Override
		public void initialize() {
			if(LivePanel.this.model.selection != null) {
				Point initialMouseDown = (Point)LivePanel.this.model.getProperty("SelectionInitialMouseDown");
				boolean moving = (boolean)LivePanel.this.model.getProperty("SelectionMoving");
				Rectangle effectBounds = (Rectangle)LivePanel.this.model.getProperty("SelectionEffectBounds");

				ModelComponent selectionView = (ModelComponent)LivePanel.this.model.selection.getLocator().locate().getModelComponentLocation().getChild(rootView);
				LivePanel.this.productionPanel.editPanelMouseAdapter.select(selectionView, initialMouseDown, moving, effectBounds);
			}
			
			if(LivePanel.this.model.output != null) {
				ModelComponent outputView = (ModelComponent)LivePanel.this.model.output.getLocator().locate().getModelComponentLocation().getChild(rootView);
				LivePanel.this.productionPanel.editPanelMouseAdapter.setOutput(outputView);
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
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions) {
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
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			visitAction.run(this);
		}
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final LivePanel view = new LivePanel(rootView, this, transactionFactory, viewManager);
		
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

	public Model getOutput() {
		return output;
	}
}

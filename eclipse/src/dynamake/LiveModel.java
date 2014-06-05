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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
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
		public final Point selectionInitialMouseDown;
		public final boolean selectionMoving;
		public final Rectangle selectionEffectBounds;
		
		public SelectionChanged(Point selectionInitialMouseDown, boolean selectionMoving, Rectangle selectionEffectBounds) {
			this.selectionInitialMouseDown = selectionInitialMouseDown;
			this.selectionMoving = selectionMoving;
			this.selectionEffectBounds = selectionEffectBounds;
		}
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
	
	private int tool;
	private Model content;
	private Model selection;
	private Model output;
	
	public LiveModel(Model content) {
		this.content = content;
	}
	
	@Override
	public Model modelCloneIsolated() {
		LiveModel clone = new LiveModel(content.cloneIsolated());
		
		clone.tool = tool;
		clone.selection = this.selection.cloneIsolated();
		
		return clone;
	}
	
	public void setSelection(Model selection, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		this.selection = selection;
		
		if(this.selection != null) {
			Point selectionInitialMouseDown = (Point)getProperty("SelectionInitialMouseDown");
			boolean selectionMoving = (boolean)getProperty("SelectionMoving");
			Rectangle selectionEffectBounds = (Rectangle)getProperty("SelectionEffectBounds");
			
			sendChanged(new SelectionChanged(selectionInitialMouseDown, selectionMoving, selectionEffectBounds), propCtx, propDistance, 0, branch);
		} else {
			sendChanged(new SelectionChanged(null, false, null), propCtx, propDistance, 0, branch);
		}
	}

	public void setOutput(Model output, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		this.output = output;
		
		sendChanged(new OutputChanged(), propCtx, propDistance, 0, branch);
	}

	public int getTool() {
		return tool;
	}
	
	public void setTool(int tool, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		this.tool = tool;
		sendChanged(new StateChanged(), propCtx, propDistance, 0, branch);
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
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
//			System.out.println("SetSelection");
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
			if(modelLocation != null) {
				Model selection = (Model)modelLocation.getChild(prevalentSystem);
				liveModel.setSelection(selection, new PropogationContext(), 0, branch);
			} else {
				liveModel.setSelection(null, new PropogationContext(), 0, branch);
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
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
			if(modelLocation != null) {
				Model selection = (Model)modelLocation.getChild(prevalentSystem);
				liveModel.setOutput(selection, new PropogationContext(), 0, branch);
			} else {
				liveModel.setOutput(null, new PropogationContext(), 0, branch);
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

	public static class SetTool implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		private int tool;

		public SetTool(Location modelLocation, int tool) {
			this.modelLocation = modelLocation;
			this.tool = tool;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			LiveModel model = (LiveModel)modelLocation.getChild(prevalentSystem);
			model.setTool(tool, propCtx, 0, branch);
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
				
				PrevaylerServiceBranch<Model> branch = transactionFactory.createBranch();
//				branch.branch(propCtx, new PrevaylerServiceBranchCreator<Model>() {
//					@Override
//					public void create(PrevaylerServiceBranchCreation<Model> branchCreation) {
//						Location modelLocation = transactionFactory.getModelLocation();
//						int previousState = model.getTool();
//						
//						branchCreation.create(
//							new DualCommandPair<Model>(new SetTool(modelLocation, state), new SetTool(modelLocation, previousState)), 
//							new PrevaylerServiceBranchContinuation<Model>() {
//								@Override
//								public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch) {
//									branch.absorb();
//								}
//							}
//						);
//					}
//				});
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Location modelLocation = transactionFactory.getModelLocation();
						int previousTool = model.getTool();
						
						dualCommands.add(
							new DualCommandPair<Model>(new SetTool(modelLocation, state), new SetTool(modelLocation, previousTool))
						);
					}
				});
				branch.close();
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
				return productionPanel.livePanel.viewManager.getTools()[productionPanel.livePanel.model.tool - 1];
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
			
			public void selectFromView(final ModelComponent view, final Point initialMouseDown, boolean moving, PrevaylerServiceBranch<Model> branch) {
				Rectangle effectBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
				requestSelect(view, initialMouseDown, moving, effectBounds, branch);
			}
			
			public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, boolean moving, PrevaylerServiceBranch<Model> branch) {
				// TODO: Once finished with replace all usages of beginTransaction, commitTransaction, and rollbackTransaction:
				// Put a PrevayerServiceConnection<Model> as parameter to this method and forward the connection in the flow.
				Dimension sourceBoundsSize = new Dimension(125, 33);
				Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
				Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
				Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, productionPanel);
				requestSelect(view, initialMouseDown, moving, selectionBounds, branch);
			}
			
			public void selectFromEmpty(final ModelComponent view, final Point initialMouseDown, boolean moving, PrevaylerServiceBranch<Model> branch) {
				requestSelect(view, initialMouseDown, moving, new Rectangle(0, 0, 0, 0), branch);
			}
			
			private void requestSelect(final ModelComponent view, final Point initialMouseDown, final boolean moving, final Rectangle effectBounds, PrevaylerServiceBranch<Model> branch) {
				// Notice: executes a transaction
				PropogationContext propCtx = new PropogationContext();
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						createSelectCommands(view, initialMouseDown, moving, effectBounds, dualCommands);
					}
				});
			}
			
			public void createSelectCommands(final ModelComponent view, final Point initialMouseDown, final boolean moving, final Rectangle effectBounds, List<DualCommand<Model>> dualCommands) {
				final LiveModel liveModel = productionPanel.livePanel.model;
				final Location liveModelLocation = productionPanel.livePanel.getTransactionFactory().getModelLocation();
				
				Location currentSelectionLocation = EditPanelMouseAdapter.this.selection != null 
						? EditPanelMouseAdapter.this.selection.getTransactionFactory().getModelLocation() : null; 
				Location selectionLocation = view != null ? view.getTransactionFactory().getModelLocation() : null;
				
				dualCommands.add(new DualCommandPair<Model>(
					new SetPropertyOnRootTransaction(liveModelLocation, "SelectionInitialMouseDown", initialMouseDown), 
					new SetSelection(liveModelLocation, currentSelectionLocation)
				));
				dualCommands.add(new DualCommandPair<Model>(
					new SetPropertyOnRootTransaction(liveModelLocation, "SelectionMoving", moving), 
					new SetPropertyOnRootTransaction(liveModelLocation, "SelectionEffectBounds", liveModel.getProperty("SelectionEffectBounds"))
				));
				dualCommands.add(new DualCommandPair<Model>(
					new SetPropertyOnRootTransaction(liveModelLocation, "SelectionEffectBounds", effectBounds), 
					new SetPropertyOnRootTransaction(liveModelLocation, "SelectionMoving", liveModel.getProperty("SelectionMoving"))
				));
				
				dualCommands.add(new DualCommandPair<Model>(
					new SetSelection(liveModelLocation, selectionLocation), 
					new SetPropertyOnRootTransaction(liveModelLocation, "SelectionInitialMouseDown", liveModel.getProperty("SelectionInitialMouseDown"))
				));
			}
			
			private void select(final ModelComponent view, final Point initialMouseDown, boolean moving, Rectangle effectBounds) {
//				System.out.println("in select method");
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
			
			public void showPopupForSelectionObject(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, PrevaylerServiceBranch<Model> branch) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new DragDragDropPopupBuilder(branch));
			}
			
			public void showPopupForSelectionCons(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, PrevaylerServiceBranch<Model> branch) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ConsDragDropPopupBuilder(branch));
			}
			
			public void showPopupForSelectionTell(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, PrevaylerServiceBranch<Model> branch) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new TellDragDropPopupBuilder(branch));
			}

			public void showPopupForSelectionView(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver, PrevaylerServiceBranch<Model> branch) {
				showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ViewDragDropPopupBuilder(branch));
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
				/*
				
				For further implementations of tools, when branches are used in all tools:
				
				Create a branch here, through which executions are scheduled and flushed immediately, such that
				it is ensured that selections have been made before drag and release events.
				This branch is then provided to the respective tool.
				
				NOTICE: This requires that each tool must ensure selecting a model during each press event.
				- NOTICE FURTHER: In some cases, this guarantee may not make sense.
				
				*/
				
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
				public void unFocus(PropogationContext propCtx, ModelComponent view, PrevaylerServiceBranch<Model> branch) {
					if(productionPanel.editPanelMouseAdapter.selection == view) {
//						productionPanel.clearFocus();
						
						productionPanel.editPanelMouseAdapter.requestSelect(null, null, false, null, branch);
					}
				}
				
				@Override
				public void selectAndActive(ModelComponent view, int x, int y) { }
				
				@Override
				public int getState() {
					return LivePanel.this.model.getTool();
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
			undo.setBackground(TOP_FOREGROUND_COLOR);
			undo.setForeground(TOP_BACKGROUND_COLOR);
			undo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_UNDO);
					// Indicate this is an undo context
					getTransactionFactory().undo(propCtx);
				}
			});
			undo.setFocusable(false);
			topPanel.add(undo);
			JButton redo = new JButton("Redo");
			redo.setBackground(TOP_FOREGROUND_COLOR);
			redo.setForeground(TOP_BACKGROUND_COLOR);
			redo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_REDO);
					// Indicate this is an redo context
					getTransactionFactory().redo(propCtx);
				}
			});
			redo.setFocusable(false);
			topPanel.add(redo);
			
			topPanel.add(new JSeparator(JSeparator.VERTICAL));
			topPanel.add(new JSeparator(JSeparator.VERTICAL));

			Tool[] tools = viewManager.getTools();
			buttonTools = new JToggleButton[1 + tools.length];
			ButtonGroup group = new ButtonGroup();
			
			buttonTools[0] = createToolButton(model, transactionFactory, group, this.model.getTool(), STATE_USE, "Use");

			for(int i = 0; i < tools.length; i++) {
				Tool tool = tools[i];
				buttonTools[i + 1] = createToolButton(model, transactionFactory, group, this.model.getTool(), i + 1, tool.getName());
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
					int state = LivePanel.this.model.getTool();
					if(state != LiveModel.STATE_USE) {
						contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
						contentPane.revalidate();
						contentPane.repaint();
					}
					previousState = state;
				}
				
				@Override
				public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof LiveModel.StateChanged) {
						if(!propCtx.isTagged(TAG_CAUSED_BY_TOGGLE_BUTTON)) {
							JToggleButton buttonNewTool = buttonTools[LivePanel.this.model.getTool()];
							buttonNewTool.setSelected(true);
						}
						
						if(previousState == LiveModel.STATE_USE && LivePanel.this.model.getTool() != LiveModel.STATE_USE) {
							contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
							contentPane.revalidate();
							contentPane.repaint();
						} else if(previousState != LiveModel.STATE_USE && LivePanel.this.model.getTool() == LiveModel.STATE_USE) {
							contentPane.remove(productionPanel);
							contentPane.revalidate();
							contentPane.repaint();
						}
						
						previousState = LivePanel.this.model.getTool();
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
							SelectionChanged selectionChanged = (SelectionChanged)change;
							
							// TODO: Consider whether this is a safe manner in which location of selection if derived.
							ModelLocator locator = LivePanel.this.model.selection.getLocator();
							ModelLocation modelLocation = locator.locate();
							Location modelComponentLocation = modelLocation.getModelComponentLocation();
							final ModelComponent view = (ModelComponent)modelComponentLocation.getChild(rootView);
							final Point initialMouseDown = selectionChanged.selectionInitialMouseDown;
							final boolean moving = selectionChanged.selectionMoving;
							final Rectangle effectBounds = selectionChanged.selectionEffectBounds;

							productionPanel.editPanelMouseAdapter.select(view, initialMouseDown, moving, effectBounds);
							
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									productionPanel.livePanel.repaint();
								}
							});
						} else {
							productionPanel.editPanelMouseAdapter.select(null, null, false, null);
							
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									productionPanel.livePanel.repaint();
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
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child, PrevaylerServiceBranch<Model> branch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions, branch);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
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

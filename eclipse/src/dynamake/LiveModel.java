package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.prevayler.Transaction;

public class LiveModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	
	public LiveModel(Model content) {
		this.content = content;
	}

	public int getState() {
		return state;
	}
	
	public void setState(int state, PropogationContext propCtx, int propDistance) {
		this.state = state;
		sendChanged(new StateChanged(), propCtx, propDistance, 0);
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
	
	private static class ProductionPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JPanel effectFrame;
		private JPanel selectionFrame;
		private JPanel targetFrame;
		
		public ProductionPanel(final LivePanel livePanel, final Binding<ModelComponent> contentView) {
			this.setLayout(null);
			effectFrame = null;
			
			// TODO: Consider the following:
			// For a selected frame, it should be possible to scroll upwards to select its immediate parent
			// - and scroll downwards to select its root parents
			
			MouseAdapter editPanelMouseAdapter = new MouseAdapter() {
				private ModelComponent selection;
				private boolean effectFrameMoving;
				private Point selectionMouseDown;
				private Point initialEffectLocation;
				private Rectangle initialEffectBounds;
				private Dimension selectionFrameSize;
				private int selectionFrameHorizontalPosition;
				private int selectionFrameVerticalPosition;
				private ModelComponent targetOver;
				
				private static final int HORIZONTAL_REGION_WEST = 0;
				private static final int HORIZONTAL_REGION_CENTER = 1;
				private static final int HORIZONTAL_REGION_EAST = 2;
				private static final int VERTICAL_REGION_NORTH = 0;
				private static final int VERTICAL_REGION_CENTER = 1;
				private static final int VERTICAL_REGION_SOUTH = 2;

				private void resetEffectFrame() {
//					effectFrame.setBounds(selectionFrame.getBounds());
					effectFrame.setBounds(new Rectangle(0, 0, 0, 0));
				}
				
				private void updateRelativeCursorPosition(Point point, Dimension size) {
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
				
				private void selectFromView(final ModelComponent view, final Point initialMouseDown, boolean moving) {
					Rectangle effectBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), ProductionPanel.this);
					select(view, initialMouseDown, moving, effectBounds);
				}
				
				private void selectFromDefault(final ModelComponent view, final Point initialMouseDown, boolean moving) {
					Dimension sourceBoundsSize = new Dimension(120, 40);
					Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
					Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
					Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, ProductionPanel.this);
					select(view, initialMouseDown, moving, selectionBounds);
				}
				
				private void selectFromEmpty(final ModelComponent view, final Point initialMouseDown, boolean moving) {
					select(view, initialMouseDown, moving, new Rectangle(0, 0, 0, 0));
				}
				
				private void select(final ModelComponent view, final Point initialMouseDown, boolean moving, Rectangle effectBounds) {
					// <Don't remove>
					// Where the following check is necessary or not has not been decided yet, so don't remove the code
//					if(this.selection == view)
//						return;
					// </Don't remove>
					
					this.selection = view;
					
					if(this.selection != null) {
						if(effectFrame == null) {
							effectFrame = new JPanel();
							effectFrame.setBackground(new Color(0, 0, 0, 0));
							
							effectFrame.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createDashedBorder(Color.BLACK, 2.0f, 2.0f, 1.5f, false),
								BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
							));
							ProductionPanel.this.add(effectFrame);
						}
						
						if(selectionFrame == null) {
							selectionFrame = new JPanel();
							selectionFrame.setBackground(new Color(0, 0, 0, 0));
							
//							selectionFrame.setBorder(BorderFactory.createCompoundBorder(
//								BorderFactory.createDashedBorder(Color.BLACK, 2.0f, 2.0f, 1.5f, false),
//								BorderFactory.createDashedBorder(Color.BLACK, 2.0f, 2.0f, 1.5f, false)
//							));
							
//							selectionFrame.setBorder(BorderFactory.createDashedBorder(Color.BLACK, 4.0f, 4.0f, 1.5f, false));
//							selectionFrame.setBorder(BorderFactory.createLineBorder(Color.BLACK, 4));
							
							Color color = Color.GRAY;

							selectionFrame.setBorder(
								BorderFactory.createCompoundBorder(
									BorderFactory.createLineBorder(Color.BLACK, 1), 
									BorderFactory.createCompoundBorder(
										BorderFactory.createLineBorder(color, 3), 
										BorderFactory.createLineBorder(Color.BLACK, 1)
									)
								)
							);
							
							MouseAdapter mouseAdapter = new MouseAdapter() {
								@Override
								public void mouseMoved(MouseEvent e) {
									switch(livePanel.model.state) {
									case LiveModel.STATE_EDIT:
										mouseMovedEdit(e);
										break;
									case LiveModel.STATE_PLOT:
										mouseMovedPlot(e);
										break;
									case LiveModel.STATE_BIND:
										mouseMovedBind(e);
										break;
									case LiveModel.STATE_DRAG:
										mouseMovedDrag(e);
										break;
									case LiveModel.STATE_CONS:
										mouseMovedCons(e);
										break;
									}
								}

								private void mouseMovedEdit(MouseEvent e) {
									if(selection != contentView.getBindingTarget()) {
										Point point = e.getPoint();
										
										updateRelativeCursorPosition(point, effectFrame.getSize());
										
										Cursor cursor = null;
										
										switch(selectionFrameHorizontalPosition) {
										case HORIZONTAL_REGION_WEST:
											switch(selectionFrameVerticalPosition) {
											case VERTICAL_REGION_NORTH:
												cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
												break;
											case VERTICAL_REGION_CENTER:
												cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
												break;
											case VERTICAL_REGION_SOUTH:
												cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
												break;
											}
											break;
										case HORIZONTAL_REGION_CENTER:
											switch(selectionFrameVerticalPosition) {
											case VERTICAL_REGION_NORTH:
												cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
												break;
											case VERTICAL_REGION_SOUTH:
												cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
												break;
											}
											break;
										case HORIZONTAL_REGION_EAST:
											switch(selectionFrameVerticalPosition) {
											case VERTICAL_REGION_NORTH:
												cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
												break;
											case VERTICAL_REGION_CENTER:
												cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
												break;
											case VERTICAL_REGION_SOUTH:
												cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
												break;
											}
											break;
										}
										
										if(effectFrame.getCursor() != cursor) {
											effectFrame.setCursor(cursor);
										}
									}
								}

								private void mouseMovedPlot(MouseEvent e) {

								}
								
								private void mouseMovedBind(MouseEvent e) {

								}
								
								private void mouseMovedDrag(MouseEvent e) {

								}
								
								private void mouseMovedCons(MouseEvent e) {

								}

								public void mouseExited(MouseEvent e) {
									switch(livePanel.model.state) {
									case LiveModel.STATE_EDIT:
										mouseExitedEdit(e);
										break;
									case LiveModel.STATE_PLOT:
										mouseExitedPlot(e);
										break;
									case LiveModel.STATE_BIND:
										mouseExitedBind(e);
										break;
									case LiveModel.STATE_DRAG:
										mouseExitedDrag(e);
										break;
									case LiveModel.STATE_CONS:
										mouseExitedCons(e);
										break;
									}
								}
								
								private void mouseExitedEdit(MouseEvent e) {
									if(selectionMouseDown == null) {
										effectFrame.setCursor(null);
									}
								}

								private void mouseExitedPlot(MouseEvent e) {

								}

								private void mouseExitedBind(MouseEvent e) {

								}

								private void mouseExitedDrag(MouseEvent e) {

								}

								private void mouseExitedCons(MouseEvent e) {

								}

								@Override
								public void mouseReleased(MouseEvent e) {
									switch(livePanel.model.state) {
									case LiveModel.STATE_EDIT:
										mouseReleasedEdit(e);
										break;
									case LiveModel.STATE_PLOT:
										mouseReleasedPlot(e);
										break;
									case LiveModel.STATE_BIND:
										mouseReleasedBind(e);
										break;
									case LiveModel.STATE_DRAG:
										mouseReleasedDrag(e);
										break;
									case LiveModel.STATE_CONS:
										mouseReleasedCons(e);
										break;
									}
								}
								
								private void mouseReleasedEdit(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
										selectionMouseDown = null;
										
										TransactionFactory transactionFactory = selection.getTransactionFactory();
										
										JComponent parent = (JComponent)((JComponent)selection).getParent();
										Rectangle newBounds = SwingUtilities.convertRectangle(effectFrame.getParent(), effectFrame.getBounds(), parent);
										selectionFrame.setBounds(effectFrame.getBounds());
										livePanel.repaint();
										
										@SuppressWarnings("unchecked")
										Transaction<Model> changeBoundsTransaction = new Model.CompositeTransaction((Transaction<Model>[])new Transaction<?>[] {
											new Model.SetPropertyTransaction("X", (int)newBounds.getX()),
											new Model.SetPropertyTransaction("Y", (int)newBounds.getY()),
											new Model.SetPropertyTransaction("Width", (int)newBounds.getWidth()),
											new Model.SetPropertyTransaction("Height", (int)newBounds.getHeight())
										});
										transactionFactory.execute(changeBoundsTransaction);
									}
								}

								private void mouseReleasedPlot(MouseEvent e) {
									if(selectionMouseDown != null) {
										JPopupMenu factoryPopopMenu = new JPopupMenu();
										
										Point selectionReleasePoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), ProductionPanel.this);
										final Rectangle creationBounds = getPlotBounds(selectionMouseDown, selectionReleasePoint);
										
										for(final Factory factory: livePanel.getFactories()) {
											JMenuItem factoryMenuItem = new JMenuItem();
											factoryMenuItem.setText(factory.getName());
											
											factoryMenuItem.addActionListener(new ActionListener() {
												@Override
												public void actionPerformed(ActionEvent e) {
													// Find the selected model and attempt an add model transaction
													// HACK: Models can only be added to canvases
													if(selection.getModel() instanceof CanvasModel) {
														selection.getTransactionFactory().executeOnRoot(
															new CanvasModel.AddModelTransaction(selection.getTransactionFactory().getLocation(), creationBounds, factory)
														);
													}
												}
											});
											
											factoryPopopMenu.add(factoryMenuItem);
										}
										
										factoryPopopMenu.addPopupMenuListener(new PopupMenuListener() {
											@Override
											public void popupMenuWillBecomeVisible(PopupMenuEvent e) { 
												livePanel.repaint();
											}
											
											@Override
											public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
												if(selectionMouseDown != null) {
													resetEffectFrame();
													livePanel.repaint();
												}
											}
											
											@Override
											public void popupMenuCanceled(PopupMenuEvent e) { }
										});
										
										Point selectionReleasePointInSelection = SwingUtilities.convertPoint(((JComponent)(e.getSource())), e.getPoint(), ProductionPanel.this);
										factoryPopopMenu.show(ProductionPanel.this, selectionReleasePointInSelection.x + 10, selectionReleasePointInSelection.y);
									}
								}

								private void mouseReleasedBind(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(releasePoint);
										ModelComponent targetModelComponent = closestModelComponent(target);
										
										if(targetModelComponent != null && selection != targetModelComponent) {
											if(selection.getModel().isObservedBy(targetModelComponent.getModel())) {
												targetModelComponent.getTransactionFactory().executeOnRoot(
													new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), targetModelComponent.getTransactionFactory().getLocation()));
											} else {
												targetModelComponent.getTransactionFactory().executeOnRoot(
													new Model.AddObserver(selection.getTransactionFactory().getLocation(), targetModelComponent.getTransactionFactory().getLocation()));
											}
										}

										resetEffectFrame();

										if(targetFrame != null)
											ProductionPanel.this.remove(targetFrame);
										
										targetOver = null;
										livePanel.repaint();
									}
								}

								private void mouseReleasedDrag(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(releasePoint);
										ModelComponent targetModelComponent = closestModelComponent(target);
										
										if(targetModelComponent != null && selection != targetModelComponent) {
											showPopupForSelectionObject(selectionFrame, e.getPoint(), targetModelComponent);
										} else {
											showPopupForSelectionObject(selectionFrame, e.getPoint(), null);
										}

										targetOver = null;
										livePanel.repaint();
									}
								}

								private void mouseReleasedCons(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(releasePoint);
										ModelComponent targetModelComponent = closestModelComponent(target);
										
										if(targetModelComponent != null && selection != targetModelComponent) {
											if(targetModelComponent.getModel() instanceof CanvasModel) {
												showPopupForSelectionCons(selectionFrame, e.getPoint(), targetModelComponent);
											} else {
												if(selection.getModel().isObservedBy(targetModelComponent.getModel())) {
													targetModelComponent.getTransactionFactory().executeOnRoot(
														new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), targetModelComponent.getTransactionFactory().getLocation()));
												} else {
													targetModelComponent.getTransactionFactory().executeOnRoot(
														new Model.AddObserver(selection.getTransactionFactory().getLocation(), targetModelComponent.getTransactionFactory().getLocation()));
												}
												
												if(targetFrame != null)
													ProductionPanel.this.remove(targetFrame);
												
												resetEffectFrame();
												livePanel.repaint();
											}
										} else {
											if(targetModelComponent.getModel() instanceof CanvasModel) {
												showPopupForSelectionCons(selectionFrame, e.getPoint(), targetModelComponent);
											} else {
												resetEffectFrame();
											}
										}

										targetOver = null;
										livePanel.repaint();
									}
								}

								@Override
								public void mousePressed(MouseEvent e) {
									switch(livePanel.model.state) {
									case LiveModel.STATE_EDIT:
										mousePressedEdit(e);
										break;
									case LiveModel.STATE_PLOT:
										mousePressedPlot(e);
										break;
									case LiveModel.STATE_BIND:
										mousePressedBind(e);
										break;
									case LiveModel.STATE_DRAG:
										mousePressedDrag(e);
										break;
									case LiveModel.STATE_CONS:
										mousePressedCons(e);
										break;
									}
								}
								
								private void mousePressedEdit(MouseEvent e) {
									Point pointInContentView = SwingUtilities.convertPoint((JComponent)selection, e.getPoint(), (JComponent)contentView.getBindingTarget());
									JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(pointInContentView);
									ModelComponent targetModelComponent = closestModelComponent(target);
									
									if(e.getButton() == MouseEvent.BUTTON1 && targetModelComponent != contentView.getBindingTarget()) {
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromView(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									}
								}

								private void mousePressedPlot(MouseEvent e) {
									if(e.getButton() == 1) {
										Point pointInContentView = SwingUtilities.convertPoint((JComponent)selection, e.getPoint(), (JComponent)contentView.getBindingTarget());
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(pointInContentView);
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null && targetModelComponent.getModel() instanceof CanvasModel) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromEmpty(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										} else {
											selectionMouseDown = e.getPoint();
										}
									}
								}

								private void mousePressedBind(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point pointInContentView = SwingUtilities.convertPoint((JComponent)selection, e.getPoint(), (JComponent)contentView.getBindingTarget());
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(pointInContentView);
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromView(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									}
								}

								private void mousePressedDrag(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point pointInContentView = SwingUtilities.convertPoint((JComponent)selection, e.getPoint(), (JComponent)contentView.getBindingTarget());
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(pointInContentView);
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromView(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									}
								}

								private void mousePressedCons(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point pointInContentView = SwingUtilities.convertPoint((JComponent)selection, e.getPoint(), (JComponent)contentView.getBindingTarget());
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(pointInContentView);
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromDefault(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									}
								}

								@Override
								public void mouseDragged(MouseEvent e) {
									switch(livePanel.model.state) {
									case LiveModel.STATE_EDIT:
										mouseDraggedEdit(e);
										break;
									case LiveModel.STATE_PLOT:
										mouseDraggedPlot(e);
										break;
									case LiveModel.STATE_BIND:
										mouseDraggedBind(e);
										break;
									case LiveModel.STATE_DRAG:
										mouseDraggedDrag(e);
										break;
									case LiveModel.STATE_CONS:
										mouseDraggedCons(e);
										break;
									}
								}

								private void mouseDraggedEdit(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving && selection != contentView.getBindingTarget()) {
										int x = effectFrame.getX();
										int y = effectFrame.getY();
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();
										
										Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										
										switch(selectionFrameHorizontalPosition) {
										case HORIZONTAL_REGION_WEST: {
											int currentX = x;
											x = cursorLocationInProductionPanel.x - selectionMouseDown.x;
											width += currentX - x;
											
											break;
										}
										case HORIZONTAL_REGION_EAST: {
											width = selectionFrameSize.width + e.getX() - selectionMouseDown.x;
											
											break;
										}
										case HORIZONTAL_REGION_CENTER:
											switch(selectionFrameVerticalPosition) {
											case VERTICAL_REGION_CENTER:
												x = cursorLocationInProductionPanel.x - selectionMouseDown.x;
												y = cursorLocationInProductionPanel.y - selectionMouseDown.y;
												break;
											}
											break;
										}
										
										switch(selectionFrameVerticalPosition) {
										case VERTICAL_REGION_NORTH: {
											int currentY = y;
											y = cursorLocationInProductionPanel.y - selectionMouseDown.y;
											height += currentY - y;
											
											break;
										}
										case VERTICAL_REGION_SOUTH: {
											height = selectionFrameSize.height + e.getY() - selectionMouseDown.y;
											
											break;
										}
										}

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}

								private void mouseDraggedPlot(MouseEvent e) {
									if(selectionMouseDown != null) {
										Point selectionDragPoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), ProductionPanel.this);
										Rectangle plotBoundsInSelection = getPlotBounds(selectionMouseDown, selectionDragPoint);
										Rectangle plotBoundsInProductionPanel = SwingUtilities.convertRectangle((JComponent)selection, plotBoundsInSelection, ProductionPanel.this);
										effectFrame.setBounds(plotBoundsInProductionPanel);
										livePanel.repaint();
									}
								}

								private void mouseDraggedBind(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving) {
										Point mouseOverPoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent newTargetOver = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
										ModelComponent newTargetOverComponent = closestModelComponent(newTargetOver);
										if(newTargetOverComponent != targetOver) {
											targetOver = newTargetOverComponent;
											if(targetFrame != null)
												ProductionPanel.this.remove(targetFrame);
											
											if(newTargetOverComponent != null && newTargetOverComponent != selection) {
												targetFrame = new JPanel();
												Color color = 
													// Red if selection already forwards to target
													// Otherwise green
													selection.getModel().isObservedBy(newTargetOverComponent.getModel()) ? Color.RED
													: Color.GREEN;

												targetFrame.setBorder(
													BorderFactory.createCompoundBorder(
														BorderFactory.createLineBorder(Color.BLACK, 1), 
														BorderFactory.createCompoundBorder(
															BorderFactory.createLineBorder(color, 3), 
															BorderFactory.createLineBorder(Color.BLACK, 1)
														)
													)
												);
												
												Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
													((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), ProductionPanel.this);
												targetFrame.setBounds(targetFrameBounds);
												targetFrame.setBackground(new Color(0, 0, 0, 0));
												ProductionPanel.this.add(targetFrame);
											}
										}
										
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();

										Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										
										int x = cursorLocationInProductionPanel.x - selectionMouseDown.x;
										int y = cursorLocationInProductionPanel.y - selectionMouseDown.y;

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
								
								private void mouseDraggedDrag(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving) {
										Point mouseOverPoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent newTargetOver = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
										ModelComponent newTargetOverComponent = closestModelComponent(newTargetOver);
										if(newTargetOverComponent != targetOver) {
											targetOver = newTargetOverComponent;
											if(targetFrame != null)
												ProductionPanel.this.remove(targetFrame);
											
											if(newTargetOverComponent != null && newTargetOverComponent != selection) {
												targetFrame = new JPanel();
												
												Color color = Color.BLUE;

												targetFrame.setBorder(
													BorderFactory.createCompoundBorder(
														BorderFactory.createLineBorder(Color.BLACK, 1), 
														BorderFactory.createCompoundBorder(
															BorderFactory.createLineBorder(color, 3), 
															BorderFactory.createLineBorder(Color.BLACK, 1)
														)
													)
												);
												
												Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
													((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), ProductionPanel.this);
												targetFrame.setBounds(targetFrameBounds);
												targetFrame.setBackground(new Color(0, 0, 0, 0));
												ProductionPanel.this.add(targetFrame);
											}
										}
										
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();

										Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										
										int x = cursorLocationInProductionPanel.x - selectionMouseDown.x;
										int y = cursorLocationInProductionPanel.y - selectionMouseDown.y;

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
								
								private void mouseDraggedCons(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving) {
										Point mouseOverPoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent newTargetOver = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
										ModelComponent newTargetOverComponent = closestModelComponent(newTargetOver);
										if(newTargetOverComponent != targetOver) {
											targetOver = newTargetOverComponent;
											if(targetFrame != null)
												ProductionPanel.this.remove(targetFrame);
											
											if(newTargetOverComponent != null && newTargetOverComponent != selection) {
												targetFrame = new JPanel();
												Color color;
												
												if(newTargetOverComponent.getModel() instanceof CanvasModel) {
													color = Color.BLUE;
												} else {
													// Red if selection already forwards to target
													// Otherwise green
													color = 
														selection.getModel().isObservedBy(newTargetOverComponent.getModel()) ? Color.RED
														: Color.GREEN;
												}

												targetFrame.setBorder(
													BorderFactory.createCompoundBorder(
														BorderFactory.createLineBorder(Color.BLACK, 1), 
														BorderFactory.createCompoundBorder(
															BorderFactory.createLineBorder(color, 3), 
															BorderFactory.createLineBorder(Color.BLACK, 1)
														)
													)
												);
												
												Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
													((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), ProductionPanel.this);
												targetFrame.setBounds(targetFrameBounds);
												targetFrame.setBackground(new Color(0, 0, 0, 0));
												ProductionPanel.this.add(targetFrame);
											}
										}
										
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();
										
										Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										
										int x = cursorLocationInProductionPanel.x - initialEffectBounds.width / 2;
										int y = cursorLocationInProductionPanel.y - initialEffectBounds.height / 2;

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
							};
							
							selectionFrame.addMouseListener(mouseAdapter);
							selectionFrame.addMouseMotionListener(mouseAdapter);

							ProductionPanel.this.add(selectionFrame);
						}
						
						selectionMouseDown = initialMouseDown;
						selectionFrameSize = ((JComponent)view).getSize();
						effectFrameMoving = moving;
						updateRelativeCursorPosition(initialMouseDown, ((JComponent)view).getSize());
						effectFrame.setBounds(effectBounds);
						initialEffectLocation = effectBounds.getLocation();
						this.initialEffectBounds = effectBounds;
						
						Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), ProductionPanel.this);
						selectionFrame.setBounds(selectionBounds);
					} else {
						if(effectFrame != null) {
							clearFocus();
						}
					}
				}
				
				private void showPopupForSelectionObject(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
					showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new DragDragDropPopupBuilder());
				}
				
				private void showPopupForSelectionCons(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
					showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new ConsDragDropPopupBuilder());
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
									livePanel.repaint();
									ignoreNextPaint = true;
								} else {
									ignoreNextPaint = false;
								}
							}
						};

						Point pointOnTargetOver = SwingUtilities.convertPoint(popupMenuInvoker, pointOnInvoker, (JComponent)targetOver);
						Rectangle droppedBounds = SwingUtilities.convertRectangle(ProductionPanel.this, effectFrame.getBounds(), (JComponent)targetOver);
						popupBuilder.buildFromSelectionAndTarget(transactionsPopupMenu, selection, targetOver, pointOnTargetOver, droppedBounds);

						transactionsPopupMenu.show(popupMenuInvoker, pointOnInvoker.x, pointOnInvoker.y);
						livePanel.repaint();
						
						transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
							@Override
							public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
								
							}
							
							@Override
							public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
								// In some cases the focus should be cleared (when a transaction was performed) - THIS SHOULD BE UP TO THE INDIVIDUAL TRANSACTIONS TO DECIDE
								// Otherwise, only the effect frame should be removed
								if(targetFrame != null) {
									ProductionPanel.this.remove(targetFrame);
									targetFrame = null;
								}

								resetEffectFrame();
								livePanel.repaint();
							}
							
							@Override
							public void popupMenuCanceled(PopupMenuEvent arg0) {

							}
						});
					}
				}
				
				private ModelComponent closestModelComponent(Component component) {
					while(component != null && !(component instanceof ModelComponent))
						component = component.getParent();
					return (ModelComponent)component;
				}
				
				public void mousePressed(MouseEvent e) {
					switch(livePanel.model.state) {
					case LiveModel.STATE_EDIT:
						mousePressedEdit(e);
						break;
					case LiveModel.STATE_PLOT:
						mousePressedPlot(e);
						break;
					case LiveModel.STATE_BIND:
						mousePressedBind(e);
						break;
					case LiveModel.STATE_DRAG:
						mousePressedDrag(e);
						break;
					case LiveModel.STATE_CONS:
						mousePressedCons(e);
						break;
					}
				}
				
				private void mousePressedEdit(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						selectFromView(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					}
				}
				
				private Rectangle getPlotBounds(Point firstPoint, Point secondPoint) {
					int left = Math.min(firstPoint.x, secondPoint.x);
					int right = Math.max(firstPoint.x, secondPoint.x);
					int top = Math.min(firstPoint.y, secondPoint.y);
					int bottom = Math.max(firstPoint.y, secondPoint.y);
					
					return new Rectangle(left, top, right - left, bottom - top);
				}

				private void mousePressedPlot(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						selectFromEmpty(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					}
				}

				private void mousePressedBind(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						selectFromView(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					}
				}

				private void mousePressedDrag(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						selectFromView(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					}
				}

				private void mousePressedCons(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						selectFromDefault(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					}
				}

				public void mouseReleased(MouseEvent e) {
					switch(livePanel.model.state) {
					case LiveModel.STATE_EDIT:
						mouseReleasedEdit(e);
						break;
					case LiveModel.STATE_PLOT:
						mouseReleasedPlot(e);
						break;
					case LiveModel.STATE_BIND:
						mouseReleasedBind(e);
						break;
					case LiveModel.STATE_DRAG:
						mouseReleasedDrag(e);
						break;
					case LiveModel.STATE_CONS:
						mouseReleasedCons(e);
						break;
					}
				}
				
				private void mouseReleasedEdit(MouseEvent e) {
					if(selectionFrame != null) {
						e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
						e.setSource(selectionFrame);
						for(MouseListener l: selectionFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedPlot(MouseEvent e) {
					if(selectionFrame != null) {
						e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
						e.setSource(selectionFrame);
						for(MouseListener l: selectionFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedBind(MouseEvent e) {
					if(selectionFrame != null) {
						e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
						e.setSource(selectionFrame);
						for(MouseListener l: selectionFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedDrag(MouseEvent e) {
					if(selectionFrame != null) {
						e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
						e.setSource(selectionFrame);
						for(MouseListener l: selectionFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedCons(MouseEvent e) {
					if(selectionFrame != null) {
						e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
						e.setSource(selectionFrame);
						for(MouseListener l: selectionFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				public void mouseDragged(MouseEvent e) {
					switch(livePanel.model.state) {
					case LiveModel.STATE_EDIT:
						mouseDraggedEdit(e);
						break;
					case LiveModel.STATE_PLOT:
						mouseDraggedPlot(e);
						break;
					case LiveModel.STATE_BIND:
						mouseDraggedBind(e);
						break;
					case LiveModel.STATE_DRAG:
						mouseDraggedDrag(e);
						break;
					case LiveModel.STATE_CONS:
						mouseDraggedCons(e);
						break;
					}
				}

				private void mouseDraggedEdit(MouseEvent e) {
					e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
					e.setSource(selectionFrame);
					for(MouseMotionListener l: selectionFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedPlot(MouseEvent e) {
					e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
					e.setSource(selectionFrame);
					for(MouseMotionListener l: selectionFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedBind(MouseEvent e) {
					e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
					e.setSource(selectionFrame);
					for(MouseMotionListener l: selectionFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedDrag(MouseEvent e) {
					e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
					e.setSource(selectionFrame);
					for(MouseMotionListener l: selectionFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedCons(MouseEvent e) {
					e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
					e.setSource(selectionFrame);
					for(MouseMotionListener l: selectionFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}
			};
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
		}
	}
	
	private static class LivePanel extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private LiveModel model;
		private JPanel topPanel;
		private JLayeredPane contentPane;
		private RemovableListener removableListener;
		private ProductionPanel productionPanel;
		private ViewManager viewManager;
		
		public LivePanel(LiveModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.setLayout(new BorderLayout());
			this.model = model;
			this.viewManager = viewManager;
			
			ViewManager newViewManager = new ViewManager() {
				@Override
				public void setFocus(JComponent component) { }
				
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
			};
			final Binding<ModelComponent> contentView = model.getContent().createView(newViewManager, transactionFactory.extend(new ContentLocator()));

			productionPanel = new ProductionPanel(this, contentView);
			
			topPanel = new JPanel();
			topPanel.setBackground(TOP_BACKGROUND_COLOR);
			ButtonGroup group = new ButtonGroup();
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_USE, "Use"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_EDIT, "Edit"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_PLOT, "Plot"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_BIND, "Bind"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_DRAG, "Drag"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_CONS, "Cons"));
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
		}
		
		public Factory[] getFactories() {
			return viewManager.getFactories();
		}

		@Override
		public Model getModel() {
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
		public void appendDroppedTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent dropped,
				Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			// TODO Auto-generated method stub
			return null;
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

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
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.prevayler.Transaction;

import dynamake.CanvasModel.AddModelTransaction;

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
	
	private int state;
	private Model content;
	
	public LiveModel(Model content) {
		this.content = content;
	}

	public int getState() {
		return state;
	}
	
	public void setState(int state, PropogationContext propCtx) {
		this.state = state;
		sendChanged(new StateChanged(), propCtx);
	}
	
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
			prevalentSystem.setState(state, new PropogationContext());
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
		private JPanel selectionFrame;
		private JPanel targetFrame;
		
		public ProductionPanel(final LivePanel livePanel, final Binding<ModelComponent> contentView) {
			this.setLayout(null);
			selectionFrame = null;
			
			// TODO: Consider the following:
			// For a selected frame, it should be possible to scroll upwards to select its immediate parent
			// - and scroll downwards to select its root parents
			
			MouseAdapter editPanelMouseAdapter = new MouseAdapter() {
				private ModelComponent selection;
				private boolean selectionFrameMoving;
				private Point selectionFrameMouseDown;
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
				
				private void updatePosition(Point point, Dimension size) {
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
				
				private void select(final ModelComponent view, final Point initialMouseDown, boolean moving) {
					if(this.selection == view)
						return;
					
					this.selection = view;
					
					if(this.selection != null) {
						if(selectionFrame == null) {
							selectionFrame = new JPanel();
							selectionFrame.setBackground(new Color(0, 0, 0, 0));
							
							selectionFrame.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createDashedBorder(Color.BLACK, 2.0f, 2.0f, 1.5f, false),
								BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
							));
							
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
									}
								}

								private void mouseMovedEdit(MouseEvent e) {
									if(selection != contentView.getBindingTarget()) {
										Point point = e.getPoint();
										
										updatePosition(point, selectionFrame.getSize());
										
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
										
										if(selectionFrame.getCursor() != cursor) {
											selectionFrame.setCursor(cursor);
										}
									}
								}

								private void mouseMovedPlot(MouseEvent e) {

								}
								
								private void mouseMovedBind(MouseEvent e) {

								}
								
								private void mouseMovedDrag(MouseEvent e) {

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
									}
								}
								
								private void mouseExitedEdit(MouseEvent e) {
									if(selectionFrameMouseDown == null) {
										selectionFrame.setCursor(null);
									}
								}

								private void mouseExitedPlot(MouseEvent e) {

								}

								private void mouseExitedBind(MouseEvent e) {

								}

								private void mouseExitedDrag(MouseEvent e) {

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
									}
								}
								
								private void mouseReleasedEdit(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
										selectionFrameMouseDown = null;
										
										TransactionFactory transactionFactory = selection.getTransactionFactory();
										TransactionFactory metaTransactionFactory = transactionFactory.extend(new Model.MetaModelLocator());
//										TransactionFactory metaTransactionFactory = selection.getMetaTransactionFactory();
										
										JComponent parent = (JComponent)((JComponent)selection).getParent();
										Rectangle newBounds = SwingUtilities.convertRectangle(selectionFrame.getParent(), selectionFrame.getBounds(), parent);
										
										@SuppressWarnings("unchecked")
										Transaction<Model> changeBoundsTransaction = new Model.CompositeTransaction((Transaction<Model>[])new Transaction<?>[] {
											new Map.SetPropertyTransaction("X", (int)newBounds.getX()),
											new Map.SetPropertyTransaction("Y", (int)newBounds.getY()),
											new Map.SetPropertyTransaction("Width", (int)newBounds.getWidth()),
											new Map.SetPropertyTransaction("Height", (int)newBounds.getHeight())
										});
										metaTransactionFactory.execute(changeBoundsTransaction);

										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												livePanel.invalidate();
											}
										});
									}
								}

								private void mouseReleasedPlot(MouseEvent e) {
									if(plotMouseDownLocation != null) {
										Point releasePoint = e.getPoint();
										
										JPopupMenu factoryPopopMenu = new JPopupMenu();
										final Rectangle creationBounds = getPlotBounds(plotMouseDownLocation, releasePoint);
										
										for(final Factory factory: livePanel.getFactories()) {
											JMenuItem factoryMenuItem = new JMenuItem();
											factoryMenuItem.setText("New " + factory.getName());
											
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
												if(plotMouseDownLocation != null) {
													plotMouseDownLocation = null;
													ProductionPanel.this.remove(plotFrame);
													plotFrame = null;
													livePanel.repaint();
												}
											}
											
											@Override
											public void popupMenuCanceled(PopupMenuEvent e) { }
										});

										releasePoint = SwingUtilities.convertPoint(selectionFrame, releasePoint, ProductionPanel.this);
										factoryPopopMenu.show(ProductionPanel.this, releasePoint.x + 10, releasePoint.y);
									}
								}

								private void mouseReleasedBind(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(selectionFrame, e.getPoint(), ProductionPanel.this);
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(releasePoint);
										ModelComponent targetModelComponent = closestModelComponent(target);
										
										if(targetModelComponent != null && selection != targetModelComponent) {
											if(targetModelComponent.getModel().isObservedBy(selection.getModel())) {
												targetModelComponent.getTransactionFactory().executeOnRoot(
													new Model.RemoveObserver(targetModelComponent.getTransactionFactory().getLocation(), selection.getTransactionFactory().getLocation()));
											} else {
												targetModelComponent.getTransactionFactory().executeOnRoot(
													new Model.AddObserver(targetModelComponent.getTransactionFactory().getLocation(), selection.getTransactionFactory().getLocation()));
											}
										}
										
										Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
										selectionFrame.setLocation(originSelectionFrameLocation);

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
											showPopupForSelection(selectionFrame, e.getPoint(), targetModelComponent);
											
											Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
											selectionFrame.setLocation(originSelectionFrameLocation);

											if(targetFrame != null)
												ProductionPanel.this.remove(targetFrame);
										} else {
											showPopupForSelection(selectionFrame, e.getPoint(), null);
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
									}
								}
								
								private void mousePressedEdit(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
										selectionFrameMouseDown = e.getPoint();
										selectionFrameSize = selectionFrame.getSize();
										selectionFrameMoving = true;
									}
								}

								private void mousePressedPlot(MouseEvent e) {
									if(e.getButton() == 1) {
										plotMouseDownLocation = e.getPoint();
										plotFrame = new JPanel();
										plotFrame.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
										plotFrame.setBackground(Color.GRAY);
										
										ProductionPanel.this.add(plotFrame);
										livePanel.repaint();
									}
								}

								private void mousePressedBind(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										selectionFrameMouseDown = e.getPoint();
										selectionFrameSize = selectionFrame.getSize();
										selectionFrameMoving = true;
									}
								}

								private void mousePressedDrag(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										selectionFrameMouseDown = e.getPoint();
										selectionFrameSize = selectionFrame.getSize();
										selectionFrameMoving = true;
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
									}
								}

								private void mouseDraggedEdit(MouseEvent e) {
									if(selectionFrameMouseDown != null && selectionFrameMoving && selection != contentView.getBindingTarget()) {
										int x = selectionFrame.getX();
										int y = selectionFrame.getY();
										int width = selectionFrame.getWidth();
										int height = selectionFrame.getHeight();
										
										switch(selectionFrameHorizontalPosition) {
										case HORIZONTAL_REGION_WEST: {
											int currentX = x;
											x = selectionFrame.getX() + e.getX() - selectionFrameMouseDown.x;
											width += currentX - x;
											
											break;
										}
										case HORIZONTAL_REGION_EAST: {
											width = selectionFrameSize.width + e.getX() - selectionFrameMouseDown.x;
											
											break;
										}
										case HORIZONTAL_REGION_CENTER:
											switch(selectionFrameVerticalPosition) {
											case VERTICAL_REGION_CENTER:
												x += e.getX() - selectionFrameMouseDown.x;
												y += e.getY() - selectionFrameMouseDown.y;
												break;
											}
											break;
										}
										
										switch(selectionFrameVerticalPosition) {
										case VERTICAL_REGION_NORTH: {
											int currentY = y;
											y = selectionFrame.getY() + e.getY() - selectionFrameMouseDown.y;
											height += currentY - y;
											
											break;
										}
										case VERTICAL_REGION_SOUTH: {
											height = selectionFrameSize.height + e.getY() - selectionFrameMouseDown.y;
											
											break;
										}
										}

										selectionFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}

								private void mouseDraggedPlot(MouseEvent e) {
									if(plotMouseDownLocation != null) {
										Rectangle plotBounds = getPlotBounds(plotMouseDownLocation, e.getPoint());
										plotBounds = SwingUtilities.convertRectangle(selectionFrame, plotBounds, ProductionPanel.this);
										plotFrame.setBounds(plotBounds);
										livePanel.repaint();
									}
								}

								private void mouseDraggedBind(MouseEvent e) {
									if(selectionFrameMouseDown != null && selectionFrameMoving) {
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
													newTargetOverComponent.getModel().isObservedBy(selection.getModel()) ? Color.RED
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
										
										int x = selectionFrame.getX();
										int y = selectionFrame.getY();
										int width = selectionFrame.getWidth();
										int height = selectionFrame.getHeight();

										x += e.getX() - selectionFrameMouseDown.x;
										y += e.getY() - selectionFrameMouseDown.y;

										selectionFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
								
								private void mouseDraggedDrag(MouseEvent e) {
									if(selectionFrameMouseDown != null && selectionFrameMoving) {
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
										
										int x = selectionFrame.getX();
										int y = selectionFrame.getY();
										int width = selectionFrame.getWidth();
										int height = selectionFrame.getHeight();

										x += e.getX() - selectionFrameMouseDown.x;
										y += e.getY() - selectionFrameMouseDown.y;

										selectionFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
							};
							
							selectionFrame.addMouseListener(mouseAdapter);
							selectionFrame.addMouseMotionListener(mouseAdapter);
							
							selectionFrame.addMouseListener(new MouseAdapter() {
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
									}
								}
								
								public void mousePressedEdit(MouseEvent e) {
									if(e.getButton() == 1) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											select(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									} else if(e.getButton() == 3) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), livePanel);
											select(targetModelComponent, e.getPoint(), false);
											Point restoredPoint = SwingUtilities.convertPoint(livePanel, referencePoint, (JComponent)targetModelComponent);
											e.translatePoint(restoredPoint.x - e.getX(), restoredPoint.y - e.getY());
											e.setSource(selectionFrame);
											livePanel.repaint();
										}
									}
								}
								
								public void mousePressedPlot(MouseEvent e) {
									if(e.getButton() == 1) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											select(targetModelComponent, referencePoint, true);

											plotMouseDownLocation = referencePoint;
											plotFrame = new JPanel();
											plotFrame.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
											plotFrame.setBackground(Color.GRAY);
											
											ProductionPanel.this.add(plotFrame);
											livePanel.repaint();
										}
									}
								}
								
								public void mousePressedBind(MouseEvent e) {
									if(e.getButton() == 1) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											select(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									}
								}
								
								public void mousePressedDrag(MouseEvent e) {
									if(e.getButton() == 1) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											select(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
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
									}
								}
								
								public void mouseReleasedEdit(MouseEvent e) {
									if(e.getButton() == 3) {
										showPopupForSelection((JComponent)e.getSource(), e.getPoint(), null);
										livePanel.repaint();
									}
								}
								
								public void mouseReleasedPlot(MouseEvent e) {
									
								}
								
								public void mouseReleasedBind(MouseEvent e) {
									
								}
								
								public void mouseReleasedDrag(MouseEvent e) {
//									if(e.getButton() == 1) {
//										showPopupForSelection(selectionFrame, e.getPoint());
//										livePanel.repaint();
//									}
								}
							});
							
							ProductionPanel.this.add(selectionFrame);
						}
						
						selectionFrameMouseDown = initialMouseDown;
						selectionFrameSize = ((JComponent)view).getSize();
						selectionFrameMoving = moving;
						updatePosition(initialMouseDown, ((JComponent)view).getSize());
						Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), ProductionPanel.this);
						selectionFrame.setBounds(selectionBounds);
					} else {
						if(selectionFrame != null) {
							ProductionPanel.this.remove(selectionFrame);
						}
					}
				}
				
				private void showPopupForSelection(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
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
						
						if(targetOver == null || targetOver == selection) {
							ModelComponent parentModelComponent = closestModelComponent(((JComponent)selection).getParent()); 
							
							TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
							if(parentModelComponent != null)
								parentModelComponent.appendContainerTransactions(containerTransactionMapBuilder, selection);
							
							TransactionMapBuilder transactionSelectionMapBuilder = new TransactionMapBuilder();
							selection.appendTransactions(transactionSelectionMapBuilder);
	
							containerTransactionMapBuilder.appendTo(transactionsPopupMenu, "Container");
							if(!containerTransactionMapBuilder.isEmpty() && !transactionSelectionMapBuilder.isEmpty())
								transactionsPopupMenu.addSeparator();
							transactionSelectionMapBuilder.appendTo(transactionsPopupMenu, "Selection");
						} else {
							TransactionMapBuilder transactionSelectionGeneralMapBuilder = new TransactionMapBuilder();
							final Point pointOnTargetOver = SwingUtilities.convertPoint(popupMenuInvoker, pointOnInvoker, (JComponent)targetOver);
//							final Rectangle droppedBounds = selectionFrame.getBounds();
							
							// TODO: Keep selection frame visible till popup menu is hidden!!!
							
							final Rectangle droppedBounds = SwingUtilities.convertRectangle(ProductionPanel.this, selectionFrame.getBounds(), (JComponent)targetOver);
							
							if(targetOver.getModel().isObservedBy(selection.getModel())) {
								transactionSelectionGeneralMapBuilder.addTransaction("Unbind", 
									new Runnable() {
										@Override
										public void run() {
											targetOver.getTransactionFactory().executeOnRoot(
												new Model.RemoveObserver(targetOver.getTransactionFactory().getLocation(), selection.getTransactionFactory().getLocation())
											);
										}
									}
								);
							} else {
								transactionSelectionGeneralMapBuilder.addTransaction("Bind", 
									new Runnable() {
										@Override
										public void run() {
											targetOver.getTransactionFactory().executeOnRoot(
												new Model.AddObserver(targetOver.getTransactionFactory().getLocation(), selection.getTransactionFactory().getLocation())
											);
										}
									}
								);
							}
							
							transactionSelectionGeneralMapBuilder.addTransaction("Mark Visit",
								new Runnable() {
									@Override
									public void run() {
										// Find the selected model and attempt an add model transaction
										// HACK: Models can only be added to canvases
										if(targetOver.getModel() instanceof CanvasModel) {
											Dimension size = new Dimension(80, 50);
											Rectangle bounds = new Rectangle(pointOnTargetOver, size);
											targetOver.getTransactionFactory().executeOnRoot(
												new CanvasModel.AddModelTransaction(
													targetOver.getTransactionFactory().getLocation(), bounds, new MarkVisitFactory(selection.getTransactionFactory().getLocation())));
										}
									}
								}
							);
							
							transactionSelectionGeneralMapBuilder.addTransaction("Not Visited",
								new Runnable() {
									@Override
									public void run() {
										// Find the selected model and attempt an add model transaction
										// HACK: Models can only be added to canvases
										if(targetOver.getModel() instanceof CanvasModel) {
											Dimension size = new Dimension(80, 50);
											Rectangle bounds = new Rectangle(pointOnTargetOver, size);
											targetOver.getTransactionFactory().executeOnRoot(
												new CanvasModel.AddModelTransaction(
													targetOver.getTransactionFactory().getLocation(), bounds, new NotVisitedFactory(selection.getTransactionFactory().getLocation())));
										}
									}
								}
							);
							
							transactionSelectionGeneralMapBuilder.addTransaction("Meta Model",
								new Runnable() {
									@Override
									public void run() {
										// Find the selected model and attempt an add model transaction
										// HACK: Models can only be added to canvases
										if(targetOver.getModel() instanceof CanvasModel) {
											Dimension size = new Dimension(80, 50);
											Rectangle bounds = new Rectangle(pointOnTargetOver, size);
											targetOver.getTransactionFactory().executeOnRoot(
												new CanvasModel.AddModelTransaction(
													targetOver.getTransactionFactory().getLocation(), bounds, new MetaModelFactory(selection.getTransactionFactory().getLocation())));
										}
									}
								}
							);
							
							// Only available for canvases:
							TransactionMapBuilder transactionObserverMapBuilder = new TransactionMapBuilder();
							TransactionMapBuilder transactionObserverContentMapBuilder = new TransactionMapBuilder();
							for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
								final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
								transactionObserverContentMapBuilder.addTransaction(primImpl.getName(), new Runnable() {
									@Override
									public void run() {
										targetOver.getTransactionFactory().executeOnRoot(new AddThenBindTransaction(
											selection.getTransactionFactory().getLocation(), 
											targetOver.getTransactionFactory().getLocation(), 
											new PrimitiveSingletonFactory(primImpl), 
											droppedBounds
										));
									}
								});
							}
//							transactionObserverContentMapBuilder.addTransaction("BG Setter", new Runnable() {
//								@Override
//								public void run() {
//									// TODO Auto-generated method stub
//									
//								}
//							});
							transactionObserverMapBuilder.addTransaction("Then", transactionObserverContentMapBuilder);
							transactionObserverMapBuilder.appendTo(transactionsPopupMenu, "Observation");
							
							TransactionMapBuilder transactionTargetMapBuilder = new TransactionMapBuilder();
							targetOver.appendDropTargetTransactions(selection, droppedBounds, pointOnTargetOver, transactionTargetMapBuilder);
							
							transactionSelectionGeneralMapBuilder.appendTo(transactionsPopupMenu, "General");
							if(!transactionSelectionGeneralMapBuilder.isEmpty() && !transactionTargetMapBuilder.isEmpty())
								transactionsPopupMenu.addSeparator();
							transactionTargetMapBuilder.appendTo(transactionsPopupMenu, "Target");
							
							TransactionMapBuilder transactionDroppedMapBuilder = new TransactionMapBuilder();
							selection.appendDroppedTransactions(transactionDroppedMapBuilder);
							if(!transactionTargetMapBuilder.isEmpty() && !transactionDroppedMapBuilder.isEmpty())
								transactionsPopupMenu.addSeparator();
							transactionDroppedMapBuilder.appendTo(transactionsPopupMenu, "Dropped");
						}
						
						Point point = SwingUtilities.convertPoint(popupMenuInvoker, pointOnInvoker, ProductionPanel.this);

//						transactionsPopupMenu.show((JComponent)selection, e.getPoint().x, e.getPoint().y);
//						transactionsPopupMenu.show(ProductionPanel.this, point.x, point.y);
						transactionsPopupMenu.show(popupMenuInvoker, pointOnInvoker.x, pointOnInvoker.y);
						livePanel.repaint();
						
						transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
							@Override
							public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
							}
							
							@Override
							public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
								livePanel.repaint();
							}
							
							@Override
							public void popupMenuCanceled(PopupMenuEvent arg0) {
								livePanel.repaint();
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
					}
				}
				
				private void mousePressedEdit(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						select(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					} else if(e.getButton() == 3) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						select(targetModelComponent, targetComponentMouseDown, false);
						livePanel.repaint();
					}
				}
				
				private Point plotMouseDownLocation;
				private JPanel plotFrame;
				
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
						select(targetModelComponent, targetComponentMouseDown, true);
						
						plotMouseDownLocation = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						plotFrame = new JPanel();
						plotFrame.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
						plotFrame.setBackground(Color.GRAY);
						
						ProductionPanel.this.add(plotFrame);
						livePanel.repaint();
					}
				}

				private void mousePressedBind(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						select(targetModelComponent, targetComponentMouseDown, true);
						livePanel.repaint();
					}
				}

				private void mousePressedDrag(MouseEvent e) {
					if(e.getButton() == 1) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						select(targetModelComponent, targetComponentMouseDown, true);
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
			};
			this.addMouseListener(editPanelMouseAdapter);
			this.addMouseMotionListener(editPanelMouseAdapter);
			
			this.setOpaque(true);
			this.setBackground(new Color(0, 0, 0, 0));
		}

		public void clearFocus() {
			this.remove(selectionFrame);
			selectionFrame = null;
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
			
			removableListener = Model.RemovableListener.addObserver(model, new Observer() {
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
				public void changed(Model sender, Object change, PropogationContext propCtx) {
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
		public TransactionFactory getTransactionFactory() {
			// TODO Auto-generated method stub
			return null;
		}

		public void releaseBinding() {
			removableListener.releaseBinding();
		}
		
		@Override
		public Transaction<Model> getDefaultDropTransaction(
				ModelComponent dropped, Point dropPoint) {
			// TODO Auto-generated method stub
			return null;
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

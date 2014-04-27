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
		private JPanel targetFrame;
		
		public ProductionPanel(final LivePanel livePanel, final Binding<ModelComponent> contentView) {
			this.setLayout(null);
			effectFrame = null;
			
			// TODO: Consider the following:
			// For a selected frame, it should be possible to scroll upwards to select its immediate parent
			// - and scroll downwards to select its root parents
			
			MouseAdapter editPanelMouseAdapter = new MouseAdapter() {
				private ModelComponent selection;
				private JPanel selectionFrame;
				private boolean effectFrameMoving;
				private Point selectionMouseDown;
				private Point initialEffectLocation;
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
				
				// ((JComponent)view).getBounds()
				
				private void selectFromView(final ModelComponent view, final Point initialMouseDown, boolean moving) {
					Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), ProductionPanel.this);
					select(view, initialMouseDown, moving, selectionBounds);
				}
				
				private void selectFromDefault(final ModelComponent view, final Point initialMouseDown, boolean moving) {
					Dimension sourceBoundsSize = new Dimension(120, 40);
					Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
					Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
					Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, ProductionPanel.this);
					select(view, initialMouseDown, moving, selectionBounds);
				}

//				private void removeSelection() {
//					ProductionPanel.this.remove(selectionFrame);
//					this.selection = null;
//					selectionFrame = null;
//				}
				
				private void select(final ModelComponent view, final Point initialMouseDown, boolean moving, final Rectangle initialSelectionBounds) {
//					if(this.selection == view)
//						return;
					
					this.selection = view;
					
					if(this.selection != null) {
						if(effectFrame == null) {
							effectFrame = new JPanel();
							effectFrame.setBackground(new Color(0, 0, 0, 0));
							
							effectFrame.setBorder(BorderFactory.createCompoundBorder(
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
//										TransactionFactory metaTransactionFactory = transactionFactory.extend(new Model.MetaModelLocator());
//										TransactionFactory metaTransactionFactory = selection.getMetaTransactionFactory();
										
										JComponent parent = (JComponent)((JComponent)selection).getParent();
										Rectangle newBounds = SwingUtilities.convertRectangle(effectFrame.getParent(), effectFrame.getBounds(), parent);
										
										@SuppressWarnings("unchecked")
										Transaction<Model> changeBoundsTransaction = new Model.CompositeTransaction((Transaction<Model>[])new Transaction<?>[] {
											new Model.SetPropertyTransaction("X", (int)newBounds.getX()),
											new Model.SetPropertyTransaction("Y", (int)newBounds.getY()),
											new Model.SetPropertyTransaction("Width", (int)newBounds.getWidth()),
											new Model.SetPropertyTransaction("Height", (int)newBounds.getHeight())
										});
										transactionFactory.execute(changeBoundsTransaction);

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

										releasePoint = SwingUtilities.convertPoint(effectFrame, releasePoint, ProductionPanel.this);
										factoryPopopMenu.show(ProductionPanel.this, releasePoint.x + 10, releasePoint.y);
									}
								}

								private void mouseReleasedBind(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
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
										
										Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
										effectFrame.setLocation(originSelectionFrameLocation);

										if(targetFrame != null)
											ProductionPanel.this.remove(targetFrame);
										
										targetOver = null;
										livePanel.repaint();
									}
								}

								private void mouseReleasedDrag(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(releasePoint);
										ModelComponent targetModelComponent = closestModelComponent(target);
										
										if(targetModelComponent != null && selection != targetModelComponent) {
											showPopupForSelectionObject(effectFrame, e.getPoint(), targetModelComponent);
											
//											Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
//											effectFrame.setLocation(originSelectionFrameLocation);
//
//											if(targetFrame != null)
//												ProductionPanel.this.remove(targetFrame);
										} else {
											Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
											effectFrame.setLocation(originSelectionFrameLocation);
											
											showPopupForSelectionObject(effectFrame, e.getPoint(), null);
										}

										targetOver = null;
										livePanel.repaint();
									}
								}

								private void mouseReleasedCons(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										Point releasePoint = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
										JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(releasePoint);
										ModelComponent targetModelComponent = closestModelComponent(target);
										
										if(targetModelComponent != null && selection != targetModelComponent) {
											if(targetModelComponent.getModel() instanceof CanvasModel) {
												showPopupForSelectionCons(effectFrame, e.getPoint(), targetModelComponent);
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
												clearFocus();
												livePanel.repaint();
											}
											
//											Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
//											selectionFrame.setLocation(originSelectionFrameLocation);

//											if(targetFrame != null)
//												ProductionPanel.this.remove(targetFrame);
//											clearFocus();
										} else {
											clearFocus();
//											Point originSelectionFrameLocation = SwingUtilities.convertPoint(((JComponent)selection).getParent(), ((JComponent)selection).getLocation(), ProductionPanel.this);
//											selectionFrame.setLocation(originSelectionFrameLocation);
//											
//											showPopupForSelectionCons(selectionFrame, e.getPoint(), null);
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
									if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
										selectionMouseDown = e.getPoint();
										selectionFrameSize = effectFrame.getSize();
										effectFrameMoving = true;
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
										selectionMouseDown = e.getPoint();
										selectionFrameSize = effectFrame.getSize();
										effectFrameMoving = true;
									}
								}

								private void mousePressedDrag(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										selectionMouseDown = e.getPoint();
										selectionFrameSize = effectFrame.getSize();
										effectFrameMoving = true;
									}
								}

								private void mousePressedCons(MouseEvent e) {
									if(e.getButton() == MouseEvent.BUTTON1) {
										selectionMouseDown = e.getPoint();
										selectionFrameSize = effectFrame.getSize();
										effectFrameMoving = true;
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
										
										switch(selectionFrameHorizontalPosition) {
										case HORIZONTAL_REGION_WEST: {
											int currentX = x;
											x = effectFrame.getX() + e.getX() - selectionMouseDown.x;
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
												x += e.getX() - selectionMouseDown.x;
												y += e.getY() - selectionMouseDown.y;
												break;
											}
											break;
										}
										
										switch(selectionFrameVerticalPosition) {
										case VERTICAL_REGION_NORTH: {
											int currentY = y;
											y = effectFrame.getY() + e.getY() - selectionMouseDown.y;
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
									if(plotMouseDownLocation != null) {
										Rectangle plotBounds = getPlotBounds(plotMouseDownLocation, e.getPoint());
										plotBounds = SwingUtilities.convertRectangle(effectFrame, plotBounds, ProductionPanel.this);
										plotFrame.setBounds(plotBounds);
										livePanel.repaint();
									}
								}

								private void mouseDraggedBind(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving) {
										Point mouseOverPoint = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
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
										
										int x = effectFrame.getX();
										int y = effectFrame.getY();
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();

										x += e.getX() - selectionMouseDown.x;
										y += e.getY() - selectionMouseDown.y;

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
								
								private void mouseDraggedDrag(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving) {
										Point mouseOverPoint = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
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
										
										int x = effectFrame.getX();
										int y = effectFrame.getY();
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();

										x += e.getX() - selectionMouseDown.x;
										y += e.getY() - selectionMouseDown.y;

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
								
								private void mouseDraggedCons(MouseEvent e) {
									if(selectionMouseDown != null && effectFrameMoving) {
										Point mouseOverPoint = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
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
										
										int x = effectFrame.getX() - initialEffectLocation.x;
										int y = effectFrame.getY() - initialEffectLocation.y;
										int width = effectFrame.getWidth();
										int height = effectFrame.getHeight();

//										Point initialSelectionLocationInSelection = SwingUtilities.convertPoint(ProductionPanel.this, initialSelectionLocation, (JComponent)selection);
//										Point selectionFrameMouseDownInSelection = SwingUtilities.convertPoint(ProductionPanel.this, selectionFrameMouseDown, (JComponent)selection);
//										System.out.println("initialSelectionLocationInSelection=" + initialSelectionLocationInSelection);
//										System.out.println("e.getX() - selectionFrameMouseDownInSelection.x=" + (e.getX() - selectionFrameMouseDownInSelection.x));
										
										Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(effectFrame, e.getPoint(), ProductionPanel.this);
//										System.out.println("cursorLocationInProductionPanel=" + cursorLocationInProductionPanel);
										
										x = cursorLocationInProductionPanel.x - initialSelectionBounds.width / 2;
										y = cursorLocationInProductionPanel.y - initialSelectionBounds.height / 2;
										
										
//										x = initialSelectionLocation.x + (e.getX() - selectionFrameMouseDown.x);
//										y = initialSelectionLocation.y + (e.getY() - selectionFrameMouseDown.y);

//										x += e.getX() + initialSelectionLocation.x + initialSelectionBounds.width / 2;
//										y += e.getY() + initialSelectionLocation.y + initialSelectionBounds.height / 2;
//										System.out.println(initialSelectionLocation);
//										System.out.println(new Point(x, y));
//										System.out.println();

										effectFrame.setBounds(new Rectangle(x, y, width, height));
										livePanel.repaint();
									}
								}
							};
							
							effectFrame.addMouseListener(mouseAdapter);
							effectFrame.addMouseMotionListener(mouseAdapter);
							
							effectFrame.addMouseListener(new MouseAdapter() {
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
								
								public void mousePressedEdit(MouseEvent e) {
									if(e.getButton() == 1) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromView(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									} else if(e.getButton() == 3) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), livePanel);
											selectFromView(targetModelComponent, e.getPoint(), false);
											Point restoredPoint = SwingUtilities.convertPoint(livePanel, referencePoint, (JComponent)targetModelComponent);
											e.translatePoint(restoredPoint.x - e.getX(), restoredPoint.y - e.getY());
											e.setSource(effectFrame);
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
											selectFromView(targetModelComponent, referencePoint, true);

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
											selectFromView(targetModelComponent, referencePoint, true);
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
											selectFromView(targetModelComponent, referencePoint, true);
											livePanel.repaint();
										}
									}
								}
								
								public void mousePressedCons(MouseEvent e) {
									if(e.getButton() == 1) {
										JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
										ModelComponent targetModelComponent = closestModelComponent(target);
										if(targetModelComponent != null) {
											Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
											selectFromDefault(targetModelComponent, referencePoint, true);
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
									case LiveModel.STATE_CONS:
										mouseReleasedCons(e);
										break;
									}
								}
								
								public void mouseReleasedEdit(MouseEvent e) {
									if(e.getButton() == 3) {
										showPopupForSelectionObject((JComponent)e.getSource(), e.getPoint(), null);
										livePanel.repaint();
									}
								}
								
								public void mouseReleasedPlot(MouseEvent e) {
									
								}
								
								public void mouseReleasedBind(MouseEvent e) {
									
								}
								
								public void mouseReleasedDrag(MouseEvent e) {
									
								}
								
								public void mouseReleasedCons(MouseEvent e) {

								}
							});
							
							ProductionPanel.this.add(effectFrame);
						}
						
						selectionMouseDown = initialMouseDown;
						selectionFrameSize = ((JComponent)view).getSize();
						effectFrameMoving = moving;
						updateRelativeCursorPosition(initialMouseDown, ((JComponent)view).getSize());
//						Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), initialSelectionBounds, ProductionPanel.this);
						Rectangle selectionBounds = initialSelectionBounds;
						effectFrame.setBounds(selectionBounds);
						initialEffectLocation = selectionBounds.getLocation();
					} else {
						if(effectFrame != null) {
							clearFocus();
						}
					}
				}
				
				private void showPopupForSelectionObject(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
//					showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new Func1<ModelComponent, TransactionPublisher>() {
//						@Override
//						public TransactionPublisher call(ModelComponent view) {
//							return view.getObjectTransactionPublisher();
//						}
//					});
					
					showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new DragDragDropPopupBuilder());
				}
				
				private void showPopupForSelectionCons(final JComponent popupMenuInvoker, final Point pointOnInvoker, final ModelComponent targetOver) {
//					showPopupForSelection(popupMenuInvoker, pointOnInvoker, targetOver, new Func1<ModelComponent, TransactionPublisher>() {
//						@Override
//						public TransactionPublisher call(ModelComponent view) {
////							return view.getConsTransactionPublisher();
//							return view.getObjectTransactionPublisher();
//						}
//					});
					
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
						
						// Build popup menu
						if(targetOver == null || targetOver == selection) {
							// Build popup menu for dropping onto selection
							popupBuilder.buildFromSelectionToSelection(transactionsPopupMenu, selection);
						} else {
							// TODO: Keep selection frame visible till popup menu is hidden!!!
							
							// Build popup menu for dropping onto other
							Point pointOnTargetOver = SwingUtilities.convertPoint(popupMenuInvoker, pointOnInvoker, (JComponent)targetOver);
							Rectangle droppedBounds = SwingUtilities.convertRectangle(ProductionPanel.this, effectFrame.getBounds(), (JComponent)targetOver);
							popupBuilder.buildFromSelectionToOther(transactionsPopupMenu, selection, targetOver, pointOnTargetOver, droppedBounds);
						}

						transactionsPopupMenu.show(popupMenuInvoker, pointOnInvoker.x, pointOnInvoker.y);
						livePanel.repaint();
						
						transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
							@Override
							public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
							}
							
							@Override
							public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
								if(targetFrame != null)
									ProductionPanel.this.remove(targetFrame);
								clearFocus();
								livePanel.repaint();
							}
							
							@Override
							public void popupMenuCanceled(PopupMenuEvent arg0) {
//								if(targetFrame != null)
//									ProductionPanel.this.remove(targetFrame);
//								clearFocus();
//								livePanel.repaint();
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
					} else if(e.getButton() == 3) {
						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
						ModelComponent targetModelComponent = closestModelComponent(target);
						Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
						selectFromView(targetModelComponent, targetComponentMouseDown, false);
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
						selectFromView(targetModelComponent, targetComponentMouseDown, true);
						
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
					if(effectFrame != null) {
						e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
						e.setSource(effectFrame);
						for(MouseListener l: effectFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedPlot(MouseEvent e) {
					if(effectFrame != null) {
						e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
						e.setSource(effectFrame);
						for(MouseListener l: effectFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedBind(MouseEvent e) {
					if(effectFrame != null) {
						e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
						e.setSource(effectFrame);
						for(MouseListener l: effectFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedDrag(MouseEvent e) {
					if(effectFrame != null) {
						e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
						e.setSource(effectFrame);
						for(MouseListener l: effectFrame.getMouseListeners()) {
							l.mouseReleased(e);
						}
					}
				}

				private void mouseReleasedCons(MouseEvent e) {
					if(effectFrame != null) {
						e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
						e.setSource(effectFrame);
						for(MouseListener l: effectFrame.getMouseListeners()) {
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
					e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
					e.setSource(effectFrame);
					for(MouseMotionListener l: effectFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedPlot(MouseEvent e) {
					e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
					e.setSource(effectFrame);
					for(MouseMotionListener l: effectFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedBind(MouseEvent e) {
					e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
					e.setSource(effectFrame);
					for(MouseMotionListener l: effectFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedDrag(MouseEvent e) {
					e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
					e.setSource(effectFrame);
					for(MouseMotionListener l: effectFrame.getMouseMotionListeners()) {
						l.mouseDragged(e);
					}
				}

				private void mouseDraggedCons(MouseEvent e) {
//					e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
					e.translatePoint(-effectFrame.getX(), -effectFrame.getY());
					e.setSource(effectFrame);
					for(MouseMotionListener l: effectFrame.getMouseMotionListeners()) {
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
//				System.out.println("Cleared focus");
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
		public TransactionPublisher getObjectTransactionPublisher() {
			return new TransactionPublisher() {
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
			};
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

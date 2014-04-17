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
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
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
	
	private Factory[] factories;
	private int state;
	private Model content;
	
	public LiveModel(Model content, Factory[] factories) {
		this.content = content;
		this.factories = factories;
	}

	public int getState() {
		return state;
	}
	
	public void setState(int state) {
		this.state = state;
		sendChanged(new StateChanged());
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
			prevalentSystem.setState(state);
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
	
	private static class LivePanel extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private LiveModel model;
		private JPanel topPanel;
		private JLayeredPane contentPane;
		private JPanel editPanel;
		private JPanel selectionFrame;
		
		public LivePanel(LiveModel model, TransactionFactory transactionFactory, ViewManager viewManager) {
			this.setLayout(new BorderLayout());
			this.model = model;
			
			ViewManager newViewManager = new ViewManager() {
				
				@Override
				public void unregisterView(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void setFocus(JComponent component) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void selectAndActive(ModelComponent view, int x, int y) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void registerView(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public int getState() {
					return LivePanel.this.model.getState();
				}
				
				@Override
				public Factory[] getFactories() {
					return LivePanel.this.model.getFactories();
				}
				
				@Override
				public void clearFocus() {
					editPanel.remove(selectionFrame);
					selectionFrame = null;
				}
				
				@Override
				public void repaint(JTextPane view) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
//							LivePanel.this.revalidate();
							LivePanel.this.repaint();
						}
					});
				}
			};
			final Binding<ModelComponent> contentView = model.getContent().createView(newViewManager, transactionFactory.extend(new ContentLocator()));
			
			topPanel = new JPanel();
			topPanel.setBackground(TOP_BACKGROUND_COLOR);
			ButtonGroup group = new ButtonGroup();
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_USE, "Use"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_EDIT, "Edit"));
			topPanel.add(createStateRadioButton(transactionFactory, group, this.model.getState(), STATE_PLOT, "Plot"));
			topPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			
			contentPane = new JLayeredPane();
			
			contentPane.addComponentListener(new ComponentListener() {
				@Override
				public void componentShown(ComponentEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void componentResized(ComponentEvent e) {
					((JComponent)contentView.getBindingTarget()).setSize(((JComponent)e.getSource()).getSize());
					if(editPanel != null) {
						editPanel.setSize(((JComponent)e.getSource()).getSize().width, ((JComponent)e.getSource()).getSize().height - 1);
					}
				}
				
				@Override
				public void componentMoved(ComponentEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void componentHidden(ComponentEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			
			contentPane.add((JComponent)contentView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
			
			this.add(topPanel, BorderLayout.NORTH);
			this.add(contentPane, BorderLayout.CENTER);
			
			final RemovableListener removableListener = Model.RemovableListener.addObserver(model, new Observer() {
				int previousState;
				
//				JPanel editPanel;
				
				{
					update();
				}
				
				void update() {
					switch(LivePanel.this.model.getState()) {
					case LiveModel.STATE_USE:
						break;
					case LiveModel.STATE_EDIT:
						editPanel = new JPanel();
						editPanel.setLayout(null);
						selectionFrame = null;
						
						// For a selected frame, it should be possible to scroll upwards to select its immediate parent
						// - and scroll downwards to select its root parents
						
//						JPopupMenu editPopupMenu = new JPopupMenu();
//						editPopupMenu.add("Test");
//						editPanel.setComponentPopupMenu(editPopupMenu);
						
						MouseAdapter editPanelMouseAdapter = new MouseAdapter() {
							private ModelComponent selection;
//							private JPanel selectionFrame;
							private boolean selectionFrameMoving;
							private Point selectionFrameMouseDown;
							private Dimension selectionFrameSize;
							private int selectionFrameHorizontalPosition;
							private int selectionFrameVerticalPosition;
							
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
//								if(this.selection != null) {
//									
//								}
								if(this.selection == view)
									return;
								
								this.selection = view;
								
								if(this.selection != null) {
//									if(selectionFrame != null) {
//										editPanel.remove(selectionFrame);
//										selectionFrame = null;
//									}
									
									if(selectionFrame == null) {
										selectionFrame = new JPanel();
										selectionFrame.setBackground(new Color(0, 0, 0, 0));
//										selectionFrame.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
//										selectionFrame.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.GREEN, Color.BLUE, Color.BLACK, Color.DARK_GRAY));
										
//										selectionFrame.setBorder(new StrokeBorder(new BasicStroke(BasicStroke.CAP_BUTT), Color.RED));
										
//										selectionFrame.setBorder(BorderFactory.createCompoundBorder(
//											BorderFactory.createLineBorder(Color.BLACK, 1),
//											BorderFactory.createMatteBorder(1, 3, 1, 3, Color.BLUE)
//										));
										
//										selectionFrame.setBorder(BorderFactory.createCompoundBorder(
//											BorderFactory.createLineBorder(Color.BLACK, 1),
//											BorderFactory.createCompoundBorder(
//												BorderFactory.createMatteBorder(1, 3, 1, 3, Color.BLUE),
//												BorderFactory.createLineBorder(Color.WHITE, 1)
//											)
//										));
										
										selectionFrame.setBorder(BorderFactory.createCompoundBorder(
											BorderFactory.createDashedBorder(Color.BLACK, 2.0f, 2.0f, 1.5f, false),
											BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
										));
										
										MouseAdapter mouseAdapter = new MouseAdapter() {
											@Override
											public void mouseMoved(MouseEvent e) {
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
											
											public void mouseExited(MouseEvent e) {
												if(selectionFrameMouseDown == null) {
													selectionFrame.setCursor(null);
												}
											}
											
											@Override
											public void mouseReleased(MouseEvent e) {
												if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
													selectionFrameMouseDown = null;
													
													TransactionFactory transactionFactory = selection.getTransactionFactory();
													
													JComponent parent = (JComponent)((JComponent)selection).getParent();
													Rectangle newBounds = SwingUtilities.convertRectangle(selectionFrame.getParent(), selectionFrame.getBounds(), parent);
													
													transactionFactory.execute(new Model.SetPropertyTransaction("X", (int)newBounds.getX()));
													transactionFactory.execute(new Model.SetPropertyTransaction("Y", (int)newBounds.getY()));
													transactionFactory.execute(new Model.SetPropertyTransaction("Width", (int)newBounds.getWidth()));
													transactionFactory.execute(new Model.SetPropertyTransaction("Height", (int)newBounds.getHeight()));
													
													LivePanel.this.repaint();
												}
											}
											
											@Override
											public void mousePressed(MouseEvent e) {
												if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
													selectionFrameMouseDown = e.getPoint();
													selectionFrameSize = selectionFrame.getSize();
													selectionFrameMoving = true;
												}
											}
											
											@Override
											public void mouseDragged(MouseEvent e) {
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
													LivePanel.this.repaint();
												}
											}
										};
										
										selectionFrame.addMouseListener(mouseAdapter);
										selectionFrame.addMouseMotionListener(mouseAdapter);
										
										selectionFrame.addMouseListener(new MouseAdapter() {
											public void mousePressed(MouseEvent e) {
												if(e.getButton() == 1) {
													JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
													ModelComponent targetModelComponent = closestModelComponent(target);
													if(targetModelComponent != null) {
														Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
														select(targetModelComponent, referencePoint, true);
														LivePanel.this.repaint();
													}
												} else if(e.getButton() == 3) {
													JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
													ModelComponent targetModelComponent = closestModelComponent(target);
													if(targetModelComponent != null) {
														Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), LivePanel.this);
														select(targetModelComponent, e.getPoint(), false);
														Point restoredPoint = SwingUtilities.convertPoint(LivePanel.this, referencePoint, (JComponent)targetModelComponent);
														e.translatePoint(restoredPoint.x - e.getX(), restoredPoint.y - e.getY());
														e.setSource(selectionFrame);
														LivePanel.this.repaint();
													}
												}
											}
											
											public void mouseReleased(MouseEvent e) {
												if(e.getButton() == 3) {
													showPopupForSelection(e);
													LivePanel.this.repaint();
												}
											}
										});
										
										editPanel.add(selectionFrame);
									}
									
									selectionFrameMouseDown = initialMouseDown;
									selectionFrameSize = ((JComponent)view).getSize();
									selectionFrameMoving = moving;
									updatePosition(initialMouseDown, ((JComponent)view).getSize());
									Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), editPanel);
									selectionFrame.setBounds(selectionBounds);
								} else {
									if(selectionFrame != null) {
										editPanel.remove(selectionFrame);
									}
								}
							}
							
							private void showPopupForSelection(MouseEvent e) {
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
												LivePanel.this.repaint();
												ignoreNextPaint = true;
											} else {
												ignoreNextPaint = false;
											}
										}
									};
									
									ModelComponent parentModelComponent = closestModelComponent(((JComponent)selection).getParent()); 
									
									TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
									if(parentModelComponent != null)
										parentModelComponent.appendContainerTransactions(containerTransactionMapBuilder, selection);
									TransactionMapBuilder transactionMapBuilder = new TransactionMapBuilder();
									selection.appendTransactions(transactionMapBuilder);

									containerTransactionMapBuilder.appendTo(transactionsPopupMenu, "Container");
									if(!containerTransactionMapBuilder.isEmpty() && !transactionMapBuilder.isEmpty())
										transactionsPopupMenu.addSeparator();
									
									transactionMapBuilder.appendTo(transactionsPopupMenu, "Item");

									transactionsPopupMenu.show((JComponent)e.getSource(), e.getPoint().x, e.getPoint().y);
									LivePanel.this.repaint();
									
									transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
										@Override
										public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
										}
										
										@Override
										public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
											LivePanel.this.repaint();
										}
										
										@Override
										public void popupMenuCanceled(PopupMenuEvent arg0) {
											LivePanel.this.repaint();
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
								if(e.getButton() == 1) {
									JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
									ModelComponent targetModelComponent = closestModelComponent(target);
									Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
									select(targetModelComponent, targetComponentMouseDown, true);
									LivePanel.this.repaint();
								} else if(e.getButton() == 3) {
									JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
									ModelComponent targetModelComponent = closestModelComponent(target);
									Point targetComponentMouseDown = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
									select(targetModelComponent, targetComponentMouseDown, false);
									LivePanel.this.repaint();
								}
							}
							
							public void mouseReleased(MouseEvent e) {
								if(selectionFrame != null) {
									e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
									e.setSource(selectionFrame);
									for(MouseListener l: selectionFrame.getMouseListeners()) {
										l.mouseReleased(e);
									}
								}
							}
							
							public void mouseDragged(MouseEvent e) {
								e.translatePoint(-selectionFrame.getX(), -selectionFrame.getY());
								e.setSource(selectionFrame);
								for(MouseMotionListener l: selectionFrame.getMouseMotionListeners()) {
									l.mouseDragged(e);
								}
							}
						};
						editPanel.addMouseListener(editPanelMouseAdapter);
						editPanel.addMouseMotionListener(editPanelMouseAdapter);
						
						editPanel.setSize(contentPane.getSize().width, contentPane.getSize().height - 1);
						editPanel.setOpaque(true);
						editPanel.setBackground(new Color(0, 0, 0, 0));
						contentPane.add(editPanel, JLayeredPane.MODAL_LAYER);
						contentPane.revalidate();
						contentPane.repaint();
						break;
					case LiveModel.STATE_PLOT:
						break;
					}
					
					previousState = LivePanel.this.model.getState();
				}
				
				@Override
				public void changed(Model sender, Object change) {
					if(change instanceof LiveModel.StateChanged) {
						switch(previousState) {
						case LiveModel.STATE_USE:
							break;
						case LiveModel.STATE_EDIT:
							contentPane.remove(editPanel);
							editPanel = null;
							contentPane.revalidate();
							contentPane.repaint();
							break;
						case LiveModel.STATE_PLOT:
							break;
						}
						
						update();
					}
				}
			});
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
		public Color getPrimaryColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void create(Factory factory, Rectangle creationBounds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager, TransactionFactory transactionFactory) {
		final LivePanel view = new LivePanel(this, transactionFactory, null);
		
		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
				// TODO: Relase bindings in view
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}

	protected Factory[] getFactories() {
		return factories;
	}

	public Model getContent() {
		return content;
	}
}

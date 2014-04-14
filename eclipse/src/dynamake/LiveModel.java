package dynamake;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

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
import javax.swing.plaf.PopupMenuUI;

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
	
	private static class FocusViewManager implements ViewManager {
		private LiveModel liveModel;
		private TransactionFactory transactionFactory;
		private ModelLayeredPane view;
		private JComponent focusWrapper;
		private boolean canChangeFocus = true;
		private int pathIndex;
		private ModelComponent[] path;
		
		public FocusViewManager(LiveModel liveModel, TransactionFactory transactionFactory, ModelLayeredPane view) {
			this.transactionFactory = transactionFactory;
			this.liveModel = liveModel;
			this.view = view;
		}

		@Override
		public void setFocus(JComponent component) {
			path = buildPath(component);
			pathIndex = 0;
			updateFocus();
		}
		
		private ModelComponent getFocusedModelComponent() {
			return path[pathIndex];
		}
		
		private JComponent getFocusedComponent() {
			return (JComponent)getFocusedModelComponent();
		}
		
		private ModelComponent[] buildPath(Component component) {
			ArrayList<ModelComponent> pathBuilder = new ArrayList<ModelComponent>();
			
			pathBuilder.add((ModelComponent)component);
			component = component.getParent();
			while(component != null) {
				if(component instanceof ModelComponent)
					pathBuilder.add((ModelComponent)component);
				component = component.getParent();
			}
			
			ModelComponent[] path = new ModelComponent[pathBuilder.size()];
			pathBuilder.toArray(path);
			return path;
		}
		
		private Color getContrast(Color color) {
			boolean isBright = (color.getRed() + color.getGreen() + color.getBlue()) / 3 >= 128;
			if(isBright)
				return Color.BLACK;
			return Color.WHITE;
		}
		
		private void updateFocus() {
			if(liveModel.getState() != LiveModel.STATE_EDIT)
				return;
			
//			System.out.println("UPDATE FOCUS: " + (i++));
			if(visiblePopupMenu != null) {
				visiblePopupMenu.setVisible(false);
			}
			
			final JComponent component = getFocusedComponent();
//			System.out.println(component.getClass() + ":" + component.getBounds());
			
			if(canChangeFocus) {
				final boolean[] isShowingPopupMenu = new boolean[]{false};
				
				if(focusWrapper == null) {
					focusWrapper = new JPanel();
//					focusWrapper.setBorder(BorderFactory.createLineBorder(new Color(32,64,64,64), 4));

					focusWrapper.setBorder(
//						BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(32,64,64,64), 2), BorderFactory.createLineBorder(new Color(96,160,196,128), 2)));
							BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(32,64,64,64), 2), BorderFactory.createDashedBorder(new Color(128,16,16), 3.0f, 1.0f, 1.0f, true)));
					
					focusWrapper.setFocusable(true);
					focusWrapper.setBackground(new Color(0,0,0,64));
					
					final boolean[] isResizing = new boolean[]{false};
					
					focusWrapper.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseExited(MouseEvent arg0) {
							if(visiblePopupMenu == null && !isResizing[0])
								clearFocus();
							
//							if(!isResizing[0] && !isShowingPopupMenu[0])
//								clearFocus();
//							
//							// When isShowingPopupMenu[0], then popup menu just changed to being visible
//							// which provokes the focusWrapper to dispatch a mouseExited once just after
//							// the visible property changed on the shown popup menu.
//							if(isShowingPopupMenu[0])
//								isShowingPopupMenu[0] = false;
						}
					});
					
//					view.add(focusWrapper);
					view.setLayer(focusWrapper, JLayeredPane.PALETTE_LAYER);
					
					MouseAdapter mouseAdapter = new MouseAdapter() {
						private static final int HORIZONTAL_REGION_WEST = 0;
						private static final int HORIZONTAL_REGION_CENTER = 1;
						private static final int HORIZONTAL_REGION_EAST = 2;
						private static final int VERTICAL_REGION_NORTH = 0;
						private static final int VERTICAL_REGION_CENTER = 1;
						private static final int VERTICAL_REGION_SOUTH = 2;
						
						private Point mouseDown;
						private int horizontalPosition;
						private int verticalPosition;
						private Dimension size;
						
						@Override
						public void mouseMoved(MouseEvent e) {
							Point point = e.getPoint();
							
							Cursor cursor = null;
							
							int resizeWidth = 5;
							
							int leftPositionEnd = resizeWidth;
							int rightPositionStart = FocusViewManager.this.focusWrapper.getWidth() - resizeWidth;
			
							int topPositionEnd = resizeWidth;
							int bottomPositionStart = FocusViewManager.this.focusWrapper.getHeight() - resizeWidth;
							
							horizontalPosition = 1;
							verticalPosition = 1;
							
							if(point.x <= leftPositionEnd)
								horizontalPosition = HORIZONTAL_REGION_WEST;
							else if(point.x < rightPositionStart)
								horizontalPosition = HORIZONTAL_REGION_CENTER;
							else
								horizontalPosition = HORIZONTAL_REGION_EAST;
							
							if(point.y <= topPositionEnd)
								verticalPosition = VERTICAL_REGION_NORTH;
							else if(point.y < bottomPositionStart)
								verticalPosition = VERTICAL_REGION_CENTER;
							else
								verticalPosition = VERTICAL_REGION_SOUTH;
							
							switch(horizontalPosition) {
							case HORIZONTAL_REGION_WEST:
								switch(verticalPosition) {
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
								switch(verticalPosition) {
								case VERTICAL_REGION_NORTH:
									cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
									break;
								case VERTICAL_REGION_SOUTH:
									cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
									break;
								}
								break;
							case HORIZONTAL_REGION_EAST:
								switch(verticalPosition) {
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
							
							if(FocusViewManager.this.focusWrapper.getCursor() != cursor) {
								FocusViewManager.this.focusWrapper.setCursor(cursor);
							}
						}
						
						@Override
						public void mouseReleased(MouseEvent e) {
							if(e.getButton() == MouseEvent.BUTTON1) {
								mouseDown = null;
								isResizing[0] = false;
								FocusViewManager.this.canChangeFocus = true;
								
								TransactionFactory transactionFactory = getFocusedModelComponent().getTransactionFactory();
								
								JComponent parent = pathIndex < path.length - 1 ? (JComponent)path[pathIndex + 1] : view;
								Rectangle newBounds = SwingUtilities.convertRectangle(view, focusWrapper.getBounds(), parent);
								
								transactionFactory.execute(new Model.SetPropertyTransaction("X", (int)newBounds.getX()));
								transactionFactory.execute(new Model.SetPropertyTransaction("Y", (int)newBounds.getY()));
								transactionFactory.execute(new Model.SetPropertyTransaction("Width", (int)newBounds.getWidth()));
								transactionFactory.execute(new Model.SetPropertyTransaction("Height", (int)newBounds.getHeight()));
							}
						}
						
						@Override
						public void mousePressed(MouseEvent e) {
							if(e.getButton() == MouseEvent.BUTTON1) {
								mouseDown = e.getPoint();
								size = FocusViewManager.this.focusWrapper.getSize();
								FocusViewManager.this.canChangeFocus = false;
								isResizing[0] = true;
							}
						}
						
						@Override
						public void mouseDragged(MouseEvent e) {
							if(mouseDown != null) {
								int x = FocusViewManager.this.focusWrapper.getX();
								int y = FocusViewManager.this.focusWrapper.getY();
								int width = FocusViewManager.this.focusWrapper.getWidth();
								int height = FocusViewManager.this.focusWrapper.getHeight();
								
								switch(horizontalPosition) {
								case HORIZONTAL_REGION_WEST: {
									int currentX = x;
									x = FocusViewManager.this.focusWrapper.getX() + e.getX() - mouseDown.x;
									width += currentX - x;
									
									break;
								}
								case HORIZONTAL_REGION_EAST: {
									width = size.width + e.getX() - mouseDown.x;
									
									break;
								}
								case HORIZONTAL_REGION_CENTER:
									switch(verticalPosition) {
									case VERTICAL_REGION_CENTER:
										x += e.getX() - mouseDown.x;
										y += e.getY() - mouseDown.y;
										break;
									}
									break;
								}
								
								switch(verticalPosition) {
								case VERTICAL_REGION_NORTH: {
									int currentY = y;
									y = FocusViewManager.this.focusWrapper.getY() + e.getY() - mouseDown.y;
									height += currentY - y;
									
									break;
								}
								case VERTICAL_REGION_SOUTH: {
									height = size.height + e.getY() - mouseDown.y;
									
									break;
								}
								}

								FocusViewManager.this.focusWrapper.setBounds(new Rectangle(x, y, width, height));
								
								component.setPreferredSize(FocusViewManager.this.focusWrapper.getBounds().getSize());
								component.repaint();
							}
						}
					};
					focusWrapper.addMouseListener(mouseAdapter);
					focusWrapper.addMouseMotionListener(mouseAdapter);
					
					focusWrapper.addMouseWheelListener(new MouseWheelListener() {
						@Override
						public void mouseWheelMoved(MouseWheelEvent e) {
							if(e.getWheelRotation() < 0) {
								pathIndex++;
								if(pathIndex >= path.length)
									pathIndex = 0;
								updateFocus();
							} else if(e.getWheelRotation() > 0) {
								pathIndex--;
								if(pathIndex < 0)
									pathIndex = path.length - 1;
								updateFocus();
							}
						}
					});
				}
				
//				Color contrastBackground = getContrast(component.getBackground());
//				focusWrapper.setBorder(
//					BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(32,64,64,64), 2), BorderFactory.createDashedBorder(contrastBackground, 3.0f, 1.0f, 1.0f, true)));
//				Color contrastBackground = getContrast(getFocusedModelComponent().getPrimaryColor());
//				focusWrapper.setBorder(BorderFactory.createLineBorder(contrastBackground, 2));
				
				Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), view);
				focusWrapper.setBounds(bounds);
				focusWrapper.setPreferredSize(bounds.getSize());

				JPopupMenu transactionsPopupMenu = new JPopupMenu();
				
				TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
				ModelComponent parent = pathIndex + 1 < path.length ? path[pathIndex + 1] : null;
				if(parent != null)
					parent.appendContainerTransactions(containerTransactionMapBuilder, (ModelComponent)getFocusedComponent());
				TransactionMapBuilder transactionMapBuilder = new TransactionMapBuilder();
				((ModelComponent)getFocusedComponent()).appendTransactions(transactionMapBuilder);

				containerTransactionMapBuilder.appendTo(transactionsPopupMenu);
				if(!containerTransactionMapBuilder.isEmpty() && !transactionMapBuilder.isEmpty())
					transactionsPopupMenu.addSeparator();
				
				transactionMapBuilder.appendTo(transactionsPopupMenu);
				
//				focusWrapper.setComponentPopupMenu(transactionsPopupMenu);
				transactionsPopupMenu.addPropertyChangeListener("visible", new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						if(((JPopupMenu)e.getSource()).isVisible())
							isShowingPopupMenu[0] = true;
					}
				});
				transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						isShowingPopupMenu[0] = true;
						visiblePopupMenu = (JPopupMenu)e.getSource();
					}
					
					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						visiblePopupMenu = null;
						focusedPopupMenu = null;
					}
					
					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						visiblePopupMenu = null;
						focusedPopupMenu = null;
					}
				});
				
				view.repaint();
//				focusWrapper.validate();
//				focusWrapper.repaint();
			}
		}
		
		private JPopupMenu visiblePopupMenu;
		private JPopupMenu focusedPopupMenu;
		
		@Override
		public void clearFocus() {
			if(focusWrapper != null) {
				view.remove(focusWrapper);
				view.repaint();
				focusWrapper = null;
				path = null;
				pathIndex = -1;
				if(visiblePopupMenu != null) {
					visiblePopupMenu.setVisible(false);
				}
			}
		}

		@Override
		public int getState() {
			return liveModel.state;
		}

		@Override
		public Factory[] getFactories() {
			return liveModel.factories;
		}

		@Override
		public void registerView(ModelComponent view) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unregisterView(ModelComponent view) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void selectAndActive(ModelComponent view, int x, int y) {
			JPopupMenu transactionsPopupMenu = new JPopupMenu();
			
			ModelComponent[] path = buildPath((JComponent)view);
			
			TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
			ModelComponent parent = pathIndex + 1 < path.length ? path[pathIndex + 1] : null;
			if(parent != null)
				parent.appendContainerTransactions(containerTransactionMapBuilder, view);
			TransactionMapBuilder transactionMapBuilder = new TransactionMapBuilder();
			view.appendTransactions(transactionMapBuilder);

			containerTransactionMapBuilder.appendTo(transactionsPopupMenu);
			if(!containerTransactionMapBuilder.isEmpty() && !transactionMapBuilder.isEmpty())
				transactionsPopupMenu.addSeparator();
			
			transactionMapBuilder.appendTo(transactionsPopupMenu);
			
			transactionsPopupMenu.show((JComponent)view, x, y);
			
//			focusWrapper.setComponentPopupMenu(transactionsPopupMenu);
//			transactionsPopupMenu.addPropertyChangeListener("visible", new PropertyChangeListener() {
//				@Override
//				public void propertyChange(PropertyChangeEvent e) {
//					if(((JPopupMenu)e.getSource()).isVisible())
//						isShowingPopupMenu[0] = true;
//				}
//			});
//			transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
//				@Override
//				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//					isShowingPopupMenu[0] = true;
//					visiblePopupMenu = (JPopupMenu)e.getSource();
//				}
//				
//				@Override
//				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
//					visiblePopupMenu = null;
//					focusedPopupMenu = null;
//				}
//				
//				@Override
//				public void popupMenuCanceled(PopupMenuEvent e) {
//					visiblePopupMenu = null;
//					focusedPopupMenu = null;
//				}
//			});
//			
//			view.repaint();
		}

		@Override
		public void repaint(JTextPane view) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private static class LivePanel extends JPanel implements ModelComponent {
		private LiveModel model;
		private JPanel topPanel;
		private JLayeredPane contentPane;
		private ViewManager viewManager;
		private JPanel editPanel;
		
		public LivePanel(LiveModel model, TransactionFactory transactionFactory, ViewManager viewManager) {
			this.setLayout(new BorderLayout());
			this.model = model;
//			this.viewManager = new FocusViewManager(this, transactionFactory, this);
			
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
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void repaint(JTextPane view) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							LivePanel.this.revalidate();
							LivePanel.this.repaint();
						}
					});
				}
			};
			final Binding<ModelComponent> contentView = model.getContent().createView(newViewManager, transactionFactory.extend(new ContentLocator()));
			
//			final FocusViewManager newViewManager = new FocusViewManager(this, transactionFactory, view);
			
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
//					contentPane.revalidate();
//					contentPane.repaint();
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
			
//			contentPane.setLayout(new BorderLayout());
//			contentPane.add((JComponent)contentView.getBindingTarget(), BorderLayout.CENTER);
//			contentPane.setLayer((JComponent)contentView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
			contentPane.add((JComponent)contentView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
			
//			contentPane.setOpaque(true);
//			contentPane.addMouseListener(new MouseAdapter() {
//				private ModelComponent closestModelComponent(Component component) {
//					while(component != null && !(component instanceof ModelComponent))
//						component = component.getParent();
//					return (ModelComponent)component;
//				}
//				
//				public void mouseClicked(MouseEvent e) {
//					if(e.getButton() == 3) {
//						JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
////						while(target != null && !(target instanceof ModelComponent))
////							target = (JComponent)target.getParent();
//						ModelComponent targetModelComponent = closestModelComponent(target);
//						
//						if(targetModelComponent != null) {
////							ModelComponent targetModelComponent = (ModelComponent)target;
//							JPopupMenu transactionsPopupMenu = new JPopupMenu();
//							
//							ModelComponent parentModelComponent = closestModelComponent(((JComponent)targetModelComponent).getParent()); 
//							
////							ModelComponent[] path = buildPath((JComponent)view);
//							
//							TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
////							ModelComponent parent = pathIndex + 1 < path.length ? path[pathIndex + 1] : null;
//							if(parentModelComponent != null)
//								parentModelComponent.appendContainerTransactions(containerTransactionMapBuilder, targetModelComponent);
//							TransactionMapBuilder transactionMapBuilder = new TransactionMapBuilder();
//							targetModelComponent.appendTransactions(transactionMapBuilder);
//
//							containerTransactionMapBuilder.appendTo(transactionsPopupMenu);
//							if(!containerTransactionMapBuilder.isEmpty() && !transactionMapBuilder.isEmpty())
//								transactionsPopupMenu.addSeparator();
//							
//							transactionMapBuilder.appendTo(transactionsPopupMenu);
//							
////							transactionsPopupMenu.show((JComponent)e.getSource(), e.getX(), e.getY());
////							contentPane.repaint();
//							
//							transactionsPopupMenu.show((JComponent)targetModelComponent, e.getX(), e.getY());
//							
////							Point targetPoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), LivePanel.this);
//							
////							transactionsPopupMenu.show(LivePanel.this, targetPoint.x, targetPoint.y);
////							LivePanel.this.repaint();
//							
//							transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
//								
//								@Override
//								public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
////									contentPane.remove(editPanel);
////									editPanel = null;
////									contentPane.revalidate();
////									contentPane.repaint();
//									
////									contentPane.setLayer(editPanel, JLayeredPane.FRAME_CONTENT_LAYER);
////									contentPane.repaint();
//								}
//								
//								@Override
//								public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
////									contentPane.setLayer(editPanel, JLayeredPane.PALETTE_LAYER);
////									contentPane.repaint();
////									contentPane.add(editPanel, JLayeredPane.PALETTE_LAYER);
////									contentPane.revalidate();
////									contentPane.repaint();
//								}
//								
//								@Override
//								public void popupMenuCanceled(PopupMenuEvent arg0) {
////									contentPane.repaint();
//								}
//							});
//							
//							transactionsPopupMenu.addComponentListener(new ComponentListener() {
//								
//								@Override
//								public void componentShown(ComponentEvent arg0) {
//									contentPane.repaint();
//								}
//								
//								@Override
//								public void componentResized(ComponentEvent arg0) {
//									contentPane.repaint();
//								}
//								
//								@Override
//								public void componentMoved(ComponentEvent arg0) {
//									// TODO Auto-generated method stub
//									
//								}
//								
//								@Override
//								public void componentHidden(ComponentEvent arg0) {
//									// TODO Auto-generated method stub
//									
//								}
//							});
//						}
//					}
//				}
//			});
			
			this.add(topPanel, BorderLayout.NORTH);
			this.add(contentPane, BorderLayout.CENTER);
			
			topPanel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
//					if(LivePanel.this.model.getState() == LiveModel.STATE_EDIT)
//						LivePanel.this.viewManager.clearFocus();
				}
			});
			
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
						
//						JPopupMenu editPopupMenu = new JPopupMenu();
//						editPopupMenu.add("Test");
//						editPanel.setComponentPopupMenu(editPopupMenu);
						
						editPanel.addMouseListener(new MouseAdapter() {
							private ModelComponent selection;
							private JPanel selectionFrame;
							
							private void select(ModelComponent view) {
//								if(this.selection != null) {
//									
//								}
								if(this.selection == view)
									return;
								
								this.selection = view;
								
								if(this.selection != null) {
									if(selectionFrame == null) {
										selectionFrame = new JPanel();
										selectionFrame.setBackground(new Color(0, 0, 0, 0));
										selectionFrame.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
										
										// Somehow, the events sent to selectionFrame should be forwarded to editPanel, if
										// the clicked target component is different from the current selection
										MouseAdapter mouseAdapter = new MouseAdapter() {
											private static final int HORIZONTAL_REGION_WEST = 0;
											private static final int HORIZONTAL_REGION_CENTER = 1;
											private static final int HORIZONTAL_REGION_EAST = 2;
											private static final int VERTICAL_REGION_NORTH = 0;
											private static final int VERTICAL_REGION_CENTER = 1;
											private static final int VERTICAL_REGION_SOUTH = 2;
											
											private Point mouseDown;
											private int horizontalPosition;
											private int verticalPosition;
											private Dimension size;
											
											@Override
											public void mouseMoved(MouseEvent e) {
												if(selection != contentView.getBindingTarget()) {
													Point point = e.getPoint();
													
													Cursor cursor = null;
													
													int resizeWidth = 5;
													
													int leftPositionEnd = resizeWidth;
													int rightPositionStart = selectionFrame.getWidth() - resizeWidth;
									
													int topPositionEnd = resizeWidth;
													int bottomPositionStart = selectionFrame.getHeight() - resizeWidth;
													
													horizontalPosition = 1;
													verticalPosition = 1;
													
													if(point.x <= leftPositionEnd)
														horizontalPosition = HORIZONTAL_REGION_WEST;
													else if(point.x < rightPositionStart)
														horizontalPosition = HORIZONTAL_REGION_CENTER;
													else
														horizontalPosition = HORIZONTAL_REGION_EAST;
													
													if(point.y <= topPositionEnd)
														verticalPosition = VERTICAL_REGION_NORTH;
													else if(point.y < bottomPositionStart)
														verticalPosition = VERTICAL_REGION_CENTER;
													else
														verticalPosition = VERTICAL_REGION_SOUTH;
													
													switch(horizontalPosition) {
													case HORIZONTAL_REGION_WEST:
														switch(verticalPosition) {
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
														switch(verticalPosition) {
														case VERTICAL_REGION_NORTH:
															cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
															break;
														case VERTICAL_REGION_SOUTH:
															cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
															break;
														}
														break;
													case HORIZONTAL_REGION_EAST:
														switch(verticalPosition) {
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
											
											@Override
											public void mouseReleased(MouseEvent e) {
												if(e.getButton() == MouseEvent.BUTTON1 && selection != contentView.getBindingTarget()) {
													mouseDown = null;
//													isResizing[0] = false;
//													FocusViewManager.this.canChangeFocus = true;
													
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
													mouseDown = e.getPoint();
													size = selectionFrame.getSize();
//													FocusViewManager.this.canChangeFocus = false;
//													isResizing[0] = true;
												}
											}
											
											@Override
											public void mouseDragged(MouseEvent e) {
												if(mouseDown != null && selection != contentView.getBindingTarget()) {
													int x = selectionFrame.getX();
													int y = selectionFrame.getY();
													int width = selectionFrame.getWidth();
													int height = selectionFrame.getHeight();
													
													switch(horizontalPosition) {
													case HORIZONTAL_REGION_WEST: {
														int currentX = x;
														x = selectionFrame.getX() + e.getX() - mouseDown.x;
														width += currentX - x;
														
														break;
													}
													case HORIZONTAL_REGION_EAST: {
														width = size.width + e.getX() - mouseDown.x;
														
														break;
													}
													case HORIZONTAL_REGION_CENTER:
														switch(verticalPosition) {
														case VERTICAL_REGION_CENTER:
															x += e.getX() - mouseDown.x;
															y += e.getY() - mouseDown.y;
															break;
														}
														break;
													}
													
													switch(verticalPosition) {
													case VERTICAL_REGION_NORTH: {
														int currentY = y;
														y = selectionFrame.getY() + e.getY() - mouseDown.y;
														height += currentY - y;
														
														break;
													}
													case VERTICAL_REGION_SOUTH: {
														height = size.height + e.getY() - mouseDown.y;
														
														break;
													}
													}

													selectionFrame.setBounds(new Rectangle(x, y, width, height));
													LivePanel.this.repaint();
												}
											}
											
											public void mouseClicked(MouseEvent e) {
												if(e.getButton() == 1) {
													JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
													ModelComponent targetModelComponent = closestModelComponent(target);
													select(targetModelComponent);
													LivePanel.this.repaint();
												} else if(e.getButton() == 3) {
													JComponent target = (JComponent)((JComponent)selection).findComponentAt(e.getPoint());
													ModelComponent targetModelComponent = closestModelComponent(target);
													if(targetModelComponent != null) {
														Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), LivePanel.this);
														select(targetModelComponent);
														Point restoredPoint = SwingUtilities.convertPoint(LivePanel.this, referencePoint, (JComponent)targetModelComponent);
														e.translatePoint(restoredPoint.x - e.getX(), restoredPoint.y - e.getY());
														showPopupForSelection(e);
													}
												}
											}
										};
										selectionFrame.addMouseListener(mouseAdapter);
										selectionFrame.addMouseMotionListener(mouseAdapter);
										
										editPanel.add(selectionFrame);
									}
									
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

									containerTransactionMapBuilder.appendTo(transactionsPopupMenu);
									if(!containerTransactionMapBuilder.isEmpty() && !transactionMapBuilder.isEmpty())
										transactionsPopupMenu.addSeparator();
									
									transactionMapBuilder.appendTo(transactionsPopupMenu);
									
//									JComponent targetInvoker = LivePanel.this;
									
//									Point targetPoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), LivePanel.this);
//									
//									transactionsPopupMenu.show(targetInvoker, targetPoint.x, targetPoint.y);
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
							
							public void mouseClicked(MouseEvent e) {
								if(e.getButton() == 1) {
									JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
									ModelComponent targetModelComponent = closestModelComponent(target);
									select(targetModelComponent);
									LivePanel.this.repaint();
								} else if(e.getButton() == 3) {
									JComponent target = (JComponent)((JComponent)contentView.getBindingTarget()).findComponentAt(e.getPoint());
									ModelComponent targetModelComponent = closestModelComponent(target);
									
									select(targetModelComponent);
									showPopupForSelection(e);
//									if(targetModelComponent != null) {
//										JPopupMenu transactionsPopupMenu = new JPopupMenu() {
//											private boolean ignoreNextPaint;
//											
//											public void paint(java.awt.Graphics g) {
//												super.paint(g);
//												if(!ignoreNextPaint) {
//													LivePanel.this.repaint();
//													ignoreNextPaint = true;
//												} else {
//													ignoreNextPaint = false;
//												}
//											}
//										};
//										
//										ModelComponent parentModelComponent = closestModelComponent(((JComponent)targetModelComponent).getParent()); 
//										
//										TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
//										if(parentModelComponent != null)
//											parentModelComponent.appendContainerTransactions(containerTransactionMapBuilder, targetModelComponent);
//										TransactionMapBuilder transactionMapBuilder = new TransactionMapBuilder();
//										targetModelComponent.appendTransactions(transactionMapBuilder);
//
//										containerTransactionMapBuilder.appendTo(transactionsPopupMenu);
//										if(!containerTransactionMapBuilder.isEmpty() && !transactionMapBuilder.isEmpty())
//											transactionsPopupMenu.addSeparator();
//										
//										transactionMapBuilder.appendTo(transactionsPopupMenu);
//										
//										JComponent targetInvoker = LivePanel.this;
//										
//										Point targetPoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), LivePanel.this);
//										
//										transactionsPopupMenu.show(targetInvoker, targetPoint.x, targetPoint.y);
//										LivePanel.this.repaint();
//										
//										transactionsPopupMenu.addPopupMenuListener(new PopupMenuListener() {
//											
//											@Override
//											public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
//											}
//											
//											@Override
//											public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
//												LivePanel.this.repaint();
//											}
//											
//											@Override
//											public void popupMenuCanceled(PopupMenuEvent arg0) {
//												LivePanel.this.repaint();
//											}
//										});
//									}
								}
							}
						});
						
//						editPanel.setSize(contentPane.getSize());
//						editPanel.setSize(320, 280);
						editPanel.setSize(contentPane.getSize().width, contentPane.getSize().height - 1);
//						editPanel.setBackground(new Color(255, 0, 0, 0));
//						editPanel.setBackground(new Color(128,255,0,0));
						editPanel.setOpaque(true);
//						editPanel.setFocusable(true);
//						editPanel.setBackground(new Color(0, 255, 0, 64));
						editPanel.setBackground(new Color(0, 0, 0, 0));
//						contentPane.add(editPanel, BorderLayout.CENTER);
//						contentPane.setLayer(editPanel, JLayeredPane.PALETTE_LAYER);
						contentPane.add(editPanel, JLayeredPane.PALETTE_LAYER);
						contentPane.revalidate();
						contentPane.repaint();
//						((JComponent)contentView.getBindingTarget()).revalidate();
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
//							JComponent c = (JComponent)contentView.getBindingTarget();
//							((JComponent)contentView.getBindingTarget()).invalidate();
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
	
	private static class ModelLayeredPane extends JLayeredPane implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private LiveModel model;
		private TransactionFactory transactionFactory;
		private JPanel contentHolder;

		public ModelLayeredPane(LiveModel model, TransactionFactory transactionFactory, JPanel contentHolder) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			this.contentHolder = contentHolder;
		}

		@Override
		public Model getModel() {
			return model;
		}
		
		@Override
		public Color getPrimaryColor() {
			return getBackground();
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
		public void create(Factory factory, Rectangle creationBounds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
		
//		@Override
//		protected void processComponentEvent(ComponentEvent e) {
//			// TODO Auto-generated method stub
//			super.processComponentEvent(e);
//		}
//		
//		@Override
//		protected void processEvent(AWTEvent e) {
//			if(e instanceof MouseEvent && model.getState() == STATE_PLOT) {
//				processMouseEvent((MouseEvent)e);
//			} else {
//				super.processEvent(e);
//			}
//		}
//		
//		@Override
//		protected void processMouseEvent(MouseEvent e) {
//			// TODO Auto-generated method stub
//			super.processMouseEvent(e);
//		}
		
//		public void dispatchMouseEvent(MouseEvent e) {
////			processMouseEvent(e);
//			for()
//		}
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager, TransactionFactory transactionFactory) {
		
//		final JPanel contentHolder = new JPanel();
//		final ModelLayeredPane view = new ModelLayeredPane(this, transactionFactory, contentHolder);

		
//		final FocusViewManager newViewManager = new FocusViewManager(this, transactionFactory, view);
		
//		final Binding<ModelComponent> contentView = content.createView(newViewManager, transactionFactory.extend(new ContentLocator()));
		final LivePanel view = new LivePanel(this, transactionFactory, null);
		
//		view.setLayout(new BorderLayout());
//		
//		view.add(contentHolder, BorderLayout.CENTER);
//		view.setLayer(contentHolder, JLayeredPane.DEFAULT_LAYER);
		
//		long eventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK;
//		
//		final boolean[] ignorePlotMouseEvent = new boolean[]{false};
//		
//		AWTEventListener plotMouseAdapter2 = new AWTEventListener() {
//			@Override
//			public void eventDispatched(AWTEvent e) {
//				if(e instanceof MouseEvent && getState() == STATE_PLOT && !ignorePlotMouseEvent[0]) {
////					view.dispatchMouseEvent((MouseEvent)e);
////					view.pro
////					view.getmou
//					MouseEvent mouseEvent = (MouseEvent)e;
//
//					JComponent content = (JComponent)((BorderLayout)contentHolder.getLayout()).getLayoutComponent(BorderLayout.CENTER);
//					System.out.println(content.getLocation());
//					mouseEvent.translatePoint(content.getX(), content.getY());
//					
//					switch(mouseEvent.getID()) {
//					case MouseEvent.MOUSE_CLICKED:
//						for(MouseListener l: view.getMouseListeners())
//							l.mouseClicked(mouseEvent);
//						break;
//					case MouseEvent.MOUSE_PRESSED:
//						for(MouseListener l: view.getMouseListeners())
//							l.mousePressed(mouseEvent);
//						break;
//					case MouseEvent.MOUSE_RELEASED:
//						for(MouseListener l: view.getMouseListeners())
//							l.mouseReleased(mouseEvent);
//						break;
//					case MouseEvent.MOUSE_MOVED:
//						for(MouseMotionListener l: view.getMouseMotionListeners())
//							l.mouseMoved(mouseEvent);
//						break;
//					case MouseEvent.MOUSE_DRAGGED:
//						for(MouseMotionListener l: view.getMouseMotionListeners())
//							l.mouseDragged(mouseEvent);
//						break;
//					}
////					MouseEvent.
////					mouseEvent.getID()
//				} /*else {
//					view.dispatchEvent(e);
//				}*/
//			}
//		};

//		Toolkit.getDefaultToolkit().addAWTEventListener(plotMouseAdapter2, eventMask);
//		
//		MouseAdapter plotMouseAdapter = new MouseAdapter() {
//			private Point mouseDownLocation;
//			private JPanel plotFrame;
//			
//			@Override
//			public void mousePressed(MouseEvent e) {
//				if(getState() == STATE_PLOT) {
//					mouseDownLocation = e.getPoint();
//					plotFrame = createPlotFrame();
//					
//					view.add(plotFrame);
//					view.setLayer(plotFrame, JLayeredPane.PALETTE_LAYER);
//					
////					System.out.println("Created plot frame");
//				}
//			}
//			
//			@Override
//			public void mouseReleased(MouseEvent e) {
//				if(getState() == STATE_PLOT) {
//					if(mouseDownLocation != null) {
//						final Point releasePoint = e.getPoint();
//	//					mouseDownLocation = null;
//	//					view.remove(plotFrame);
//	//					plotFrame = null;
//	//					view.repaint();
//						
//						JPopupMenu factoryPopopMenu = new JPopupMenu();
//						final Rectangle creationBounds = getPlotBounds(mouseDownLocation, releasePoint);
//						
//						for(final Factory factory: factories) {
//							JMenuItem factoryMenuItem = new JMenuItem();
//							factoryMenuItem.setText("New " + factory.getName());
//							
//							factoryMenuItem.addActionListener(new ActionListener() {
//								@Override
//								public void actionPerformed(ActionEvent e) {
////									Rectangle creationBounds = getPlotBounds(mouseDownLocation, releasePoint);
//									
//									JComponent content = (JComponent)((BorderLayout)contentHolder.getLayout()).getLayoutComponent(BorderLayout.CENTER);
////									creationBounds.translate(content.getX(), content.getY());
//									creationBounds.translate(-content.getX(), -content.getY());
////									Model model = (Model)factory.create();
//									
//									contentView.getBindingTarget().create(factory, creationBounds);
//								}
//							});
//							
//							factoryPopopMenu.add(factoryMenuItem);
//						}
//	
//						JComponent content = (JComponent)((BorderLayout)contentHolder.getLayout()).getLayoutComponent(BorderLayout.CENTER);
//						
//						factoryPopopMenu.addPopupMenuListener(new PopupMenuListener() {
//							@Override
//							public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
//								ignorePlotMouseEvent[0] = true;
//							}
//							
//							@Override
//							public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
//								ignorePlotMouseEvent[0] = false;
//								
//								if(mouseDownLocation != null) {
//									mouseDownLocation = null;
//									view.remove(plotFrame);
//									plotFrame = null;
//									view.repaint();
//								}
//							}
//							
//							@Override
//							public void popupMenuCanceled(PopupMenuEvent arg0) {
//								// TODO Auto-generated method stub
//								
//							}
//						});
//						
//						mouseDownLocation.translate(-content.getX(), -content.getY());
//						
//						factoryPopopMenu.show(content, mouseDownLocation.x, mouseDownLocation.y);
//					}
//				}
//			}
//			
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				if(getState() == STATE_PLOT) {
//					System.out.println(mouseDownLocation);
//					if(mouseDownLocation != null) {
//						Rectangle plotBounds = getPlotBounds(mouseDownLocation, e.getPoint());
//						plotFrame.setBounds(plotBounds);
////						System.out.println("Modifying plot frame");
//					}
//				}
//			}
//			
//			private JPanel createPlotFrame() {
//				JPanel plotFrame = new JPanel();
//				plotFrame.setBackground(Color.RED);
//				return plotFrame;
//			}
//			
//			private Rectangle getPlotBounds(Point firstPoint, Point secondPoint) {
//				int left = Math.min(firstPoint.x, secondPoint.x);
//				int right = Math.max(firstPoint.x, secondPoint.x);
//				int top = Math.min(firstPoint.y, secondPoint.y);
//				int bottom = Math.max(firstPoint.y, secondPoint.y);
//				
//				return new Rectangle(left, top, right - left, bottom - top);
//			}
//		};
//		view.addMouseListener(plotMouseAdapter);
//		view.addMouseMotionListener(plotMouseAdapter);
		
//		contentHolder.setLayout(new BorderLayout());
//		
//		final JPanel topPanel = new JPanel();
//		topPanel.setBackground(TOP_BACKGROUND_COLOR);
//		ButtonGroup group = new ButtonGroup();
//		topPanel.add(createStateRadioButton(transactionFactory, group, STATE_USE, "Use"));
//		topPanel.add(createStateRadioButton(transactionFactory, group, STATE_EDIT, "Edit"));
//		topPanel.add(createStateRadioButton(transactionFactory, group, STATE_PLOT, "Plot"));
//		topPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//		
//		contentHolder.add(topPanel, BorderLayout.NORTH);
//		
//		topPanel.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseEntered(MouseEvent e) {
//				if(getState() == LiveModel.STATE_EDIT)
//					newViewManager.clearFocus();
//			}
//		});
//		
//		contentHolder.add((JComponent)contentView.getBindingTarget(), BorderLayout.CENTER);
		
//		final RemovableListener removableListener = Model.RemovableListener.addObserver(this, new Observer() {
//			int previousState;
//			
//			JPanel editPanel;
//			
//			{
//				update();
//			}
//			
//			void update() {
//				switch(getState()) {
//				case LiveModel.STATE_USE:
//					break;
//				case LiveModel.STATE_EDIT:
//					editPanel = new JPanel();
//					editPanel.setBackground(Color.LIGHT_GRAY);
//					editPanel.setOpaque(true);
//					view.add(editPanel);
//					view.setLayer(editPanel, JLayeredPane.PALETTE_LAYER);
//					view.repaint();
//					break;
//				case LiveModel.STATE_PLOT:
//					break;
//				}
//				
//				previousState = getState();
//			}
//			
//			@Override
//			public void changed(Model sender, Object change) {
//				if(change instanceof LiveModel.StateChanged) {
//					switch(previousState) {
//					case LiveModel.STATE_USE:
//						break;
//					case LiveModel.STATE_EDIT:
//						view.remove(editPanel);
//						editPanel = null;
//						break;
//					case LiveModel.STATE_PLOT:
//						break;
//					}
//					
//					update();
//				}
//			}
//		});
		
		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
//				contentView.releaseBinding();
//				removableListener.releaseBinding();
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

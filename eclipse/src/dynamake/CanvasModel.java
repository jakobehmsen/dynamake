package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.prevayler.Transaction;

import dynamake.Model.RemovableListener;
import dynamake.Model.SetPropertyTransaction;

public class CanvasModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Model> models = new ArrayList<Model>();
	
	public static class AddedModelChange {
		public final int index;
		public final Model model;
		
		public AddedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	public static class RemovedModelChange {
		public final int index;
		public final Model model;
		
		public RemovedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	public static class AddModelTransaction implements Transaction<CanvasModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
//		private int index;
		private Rectangle creationBounds;
		private Factory factory;
		
		public AddModelTransaction(/*int index,*/ Rectangle creationBounds, Factory factory) {
//			this.index = index;
			this.creationBounds = creationBounds;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(CanvasModel prevalentSystem, Date executionTime) {
			Model model = (Model)factory.create();

			model.setProperty("X", creationBounds.x);
			model.setProperty("Y", creationBounds.y);
			model.setProperty("Width", creationBounds.width);
			model.setProperty("Height", creationBounds.height);
			
			prevalentSystem.addModel(model);
		}
	}
	
	public static class RemoveModelTransaction implements Transaction<CanvasModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;
		
		public RemoveModelTransaction(int index) {
			this.index = index;
		}
		
		@Override
		public void executeOn(CanvasModel prevalentSystem, Date executionTime) {
			prevalentSystem.removeModel(index);
		}
	}
	
	public void addModel(Model model) {
		addModel(models.size(), model);
	}
	
	public void addModel(int index, Model model) {
		models.add(index, model);
		sendChanged(new AddedModelChange(index, model));
	}
	
	public void removeModel(Model model) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel);
	}
	
	public void removeModel(int index) {
		Model model = models.get(index);
		models.remove(index);
		sendChanged(new RemovedModelChange(index, model));
	}
	
	public int indexOfModel(Model model) {
		return models.indexOf(model);
	}
	
	private static class CanvasPanel extends JLayeredPane implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private CanvasModel model;
		private TransactionFactory transactionFactory;
		
		public CanvasPanel(CanvasModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			setLayout(null);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setOpaque(true);
			
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == 3)
						viewManager.selectAndActive(CanvasPanel.this, e.getX(), e.getY());
				}
			});
		}

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, final ModelComponent child) {
			transactions.addTransaction("Remove", new Runnable() {
				@Override
				public void run() {
					
//					String region = (String)((BorderLayout)ModelPanel.this.getLayout()).getConstraints((JComponent)child);
					int indexOfModel = model.indexOfModel(child.getModel());
					transactionFactory.execute(new RemoveModelTransaction(indexOfModel));
				}
			});
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(transactionFactory, transactions);
		}

		@Override
		public Color getPrimaryColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void create(Factory factory, Rectangle creationBounds) {
			
//			int index 
			transactionFactory.execute(new AddModelTransaction(creationBounds, factory));
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
	}
	
	private static class IndexLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;
		
		public IndexLocation(int index) {
			this.index = index;
		}

		@Override
		public Object getChild(Object holder) {
			return ((CanvasModel)holder).models.get(index);
		}

		@Override
		public void setChild(Object holder, Object child) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		final CanvasPanel view = new CanvasPanel(this, transactionFactory, viewManager);
		Model.wrapForFocus(viewManager, view, view, new Func0<Boolean>() {
			@Override
			public Boolean call() {
				return models.size() == 0;
//				return true;
			}
		});
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view);
		Model.loadComponentProperties(this, view);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view);
		
		for(final Model model: models) {
			Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new Locator() {
				@Override
				public Location locate() {
					int index = indexOfModel(model);
					return new IndexLocation(index);
				}
			}));
			
			Rectangle bounds = new Rectangle(
				(int)model.getProperty("X"),
				(int)model.getProperty("Y"),
				(int)model.getProperty("Width"),
				(int)model.getProperty("Height")
			);
			
			((JComponent)modelView.getBindingTarget()).setBounds(bounds);
			
			view.add((JComponent)modelView.getBindingTarget());
			view.setLayer((JComponent)modelView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
		}
		
		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new Observer() {
			@Override
			public void changed(Model sender, Object change) {
				if(change instanceof CanvasModel.AddedModelChange) {
					final Model model = ((CanvasModel.AddedModelChange)change).model;
					
					Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new Locator() {
						@Override
						public Location locate() {
							int index = indexOfModel(model);
							return new IndexLocation(index);
						}
					}));
					
					Rectangle bounds = new Rectangle(
						(int)model.getProperty("X"),
						(int)model.getProperty("Y"),
						(int)model.getProperty("Width"),
						(int)model.getProperty("Height")
					);
					
					((JComponent)modelView.getBindingTarget()).setBounds(bounds);
					
					view.add((JComponent)modelView.getBindingTarget());
					view.setLayer((JComponent)modelView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
				} else if(change instanceof CanvasModel.RemovedModelChange) {
//					final Model model = ((CanvasModel.AddedModelChange)change).model;
					view.remove(((CanvasModel.RemovedModelChange)change).index);
					view.validate();
					view.repaint();
					viewManager.clearFocus();
				}
			}
		});
		
		MouseAdapter plotMouseAdapter = new MouseAdapter() {
			private Point mouseDownLocation;
			private JPanel plotFrame;
			
			@Override
			public void mousePressed(MouseEvent e) {
				if(viewManager.getState() == LiveModel.STATE_PLOT) {
					mouseDownLocation = e.getPoint();
					plotFrame = createPlotFrame();
					
					view.add(plotFrame);
					view.setLayer(plotFrame, JLayeredPane.PALETTE_LAYER);
					
//					System.out.println("Created plot frame");
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if(viewManager.getState() == LiveModel.STATE_PLOT) {
					if(mouseDownLocation != null) {
						final Point releasePoint = e.getPoint();
	//					mouseDownLocation = null;
	//					view.remove(plotFrame);
	//					plotFrame = null;
	//					view.repaint();
						
						JPopupMenu factoryPopopMenu = new JPopupMenu();
						final Rectangle creationBounds = getPlotBounds(mouseDownLocation, releasePoint);
						
						for(final Factory factory: viewManager.getFactories()) {
							JMenuItem factoryMenuItem = new JMenuItem();
							factoryMenuItem.setText("New " + factory.getName());
							
							factoryMenuItem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
//									Rectangle creationBounds = getPlotBounds(mouseDownLocation, releasePoint);
									
//									JComponent content = (JComponent)((BorderLayout)view.getLayout()).getLayoutComponent(BorderLayout.CENTER);
////									creationBounds.translate(content.getX(), content.getY());
//									creationBounds.translate(-content.getX(), -content.getY());
//									Model model = (Model)factory.create();
									
//									contentView.getBindingTarget().create(factory, creationBounds);
									transactionFactory.execute(new AddModelTransaction(creationBounds, factory));
								}
							});
							
							factoryPopopMenu.add(factoryMenuItem);
						}
	
//						JComponent content = (JComponent)((BorderLayout)view.getLayout()).getLayoutComponent(BorderLayout.CENTER);
						
						factoryPopopMenu.addPopupMenuListener(new PopupMenuListener() {
							@Override
							public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
//								ignorePlotMouseEvent[0] = true;
							}
							
							@Override
							public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
//								ignorePlotMouseEvent[0] = false;
								
								if(mouseDownLocation != null) {
									mouseDownLocation = null;
									view.remove(plotFrame);
									plotFrame = null;
									view.repaint();
								}
							}
							
							@Override
							public void popupMenuCanceled(PopupMenuEvent arg0) {
								// TODO Auto-generated method stub
								
							}
						});
						
//						mouseDownLocation.translate(-content.getX(), -content.getY());
						
						factoryPopopMenu.show(view, mouseDownLocation.x, mouseDownLocation.y);
					}
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if(viewManager.getState() == LiveModel.STATE_PLOT) {
//					System.out.println(mouseDownLocation);
					if(mouseDownLocation != null) {
						Rectangle plotBounds = getPlotBounds(mouseDownLocation, e.getPoint());
						plotFrame.setBounds(plotBounds);
//						System.out.println("Modifying plot frame");
					}
				}
			}
			
			private JPanel createPlotFrame() {
				JPanel plotFrame = new JPanel();
				plotFrame.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
				plotFrame.setBackground(Color.GRAY);
				return plotFrame;
			}
			
			private Rectangle getPlotBounds(Point firstPoint, Point secondPoint) {
				int left = Math.min(firstPoint.x, secondPoint.x);
				int right = Math.max(firstPoint.x, secondPoint.x);
				int top = Math.min(firstPoint.y, secondPoint.y);
				int bottom = Math.max(firstPoint.y, secondPoint.y);
				
				return new Rectangle(left, top, right - left, bottom - top);
			}
		};
		view.addMouseListener(plotMouseAdapter);
		view.addMouseMotionListener(plotMouseAdapter);

		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
				removableListenerForComponentPropertyChanges.releaseBinding();
				removableListenerForBoundsChanges.releaseBinding();
				removableListener.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}

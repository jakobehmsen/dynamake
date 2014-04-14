package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.prevayler.Transaction;

public class ContainerModel extends Model {
//	public static class AddedModelChange {
//		public final Model model;
//
//		public AddedModelChange(Model model) {
//			this.model = model;
//		}
//	}
	
//	public static final int REGION_NORTH = 0;
//	public static final int REGION_CENTER = 1;
//	public static final int REGION_SOUTH = 2;
//	public static final int REGION_WEST = 3;
//	public static final int REGION_EAST = 4;
	
//	private Model[] regions = new Model[5];
	private Hashtable<String, Model> regions = new Hashtable<String, Model>();
	
	public static class AddedModelChange {
		public final String region;
		public final Model model;

		public AddedModelChange(String region, Model model) {
			this.region = region;
			this.model = model;
		}
	}
	
	public static class RemovedModelChange {
		public final String region;
		public final Model model;

		public RemovedModelChange(String region, Model model) {
			this.region = region;
			this.model = model;
		}
	}
	
//	private ArrayList<Model> models = new ArrayList<Model>();
	
	public void addModel(String region, Model model) {
//		models.add(model);
		regions.put(region, model);
		sendChanged(new AddedModelChange(region, model));
	}
	
	public void removeModel(String region) {
//		models.remove(model);
//		regions[region] = model;
		Model model = regions.get(region);
		regions.remove(region);
		sendChanged(new RemovedModelChange(region, model));
	}
	
	public static class RegionLocator implements dynamake.Locator {
		private String region;
		
		public RegionLocator(String region) {
			this.region = region;
		}

		@Override
		public Location locate() {
			return new RegionLocation(region);
		}
	}
	
	private static class RegionLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String region;
		
		public RegionLocation(String region) {
			this.region = region;
		}

		@Override
		public Object getChild(Object holder) {
			return ((ContainerModel)holder).regions.get(region);
		}

		@Override
		public void setChild(Object holder, Object child) {
			((ContainerModel)holder).regions.put(region, (Model)child);
		}
	}

//	@Override
//	public void appendTransactions(JComponent view,
//			TransactionMapBuilder transactions) {
//		// TODO Auto-generated method stub
//		
//	}
	
	private static class AddContainerTransaction implements Transaction<ContainerModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String region;

		public AddContainerTransaction(String region) {
			this.region = region;
		}

		@Override
		public void executeOn(ContainerModel prevalentSystem, Date executionTime) {
			ContainerModel model = new ContainerModel();
			prevalentSystem.addModel(region, model);
		}
	}
	
	private static class AddTextTransaction implements Transaction<ContainerModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String region;

		public AddTextTransaction(String region) {
			this.region = region;
		}

		@Override
		public void executeOn(ContainerModel prevalentSystem, Date executionTime) {
			TextModel model = new TextModel();
			prevalentSystem.addModel(region, model);
		}
	}
	
	private static class RemoveTransaction implements Transaction<ContainerModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String region;

		public RemoveTransaction(String region) {
			this.region = region;
		}

		@Override
		public void executeOn(ContainerModel prevalentSystem, Date executionTime) {
			prevalentSystem.removeModel(region);
		}
	}
	
	private static class ModelPanel extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ContainerModel model;
		private TransactionFactory transactionFactory;

		public ModelPanel(ContainerModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
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
				TransactionMapBuilder transactions, final ModelComponent child) {
			if(!(child instanceof ModelToolBar)) {
				transactions.addTransaction("Remove", new Runnable() {
					@Override
					public void run() {
						String region = (String)((BorderLayout)ModelPanel.this.getLayout()).getConstraints((JComponent)child);
						transactionFactory.execute(new RemoveTransaction(region));
					}
				});
			}
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
	}
	
	private static class ModelToolBar extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String region;
		private Model model;
		private TransactionFactory transactionFactory;

		public ModelToolBar(String region, Model model, TransactionFactory transactionFactory) {
			this.region = region;
			this.model = model;
			this.transactionFactory = transactionFactory;
			setBackground(Color.LIGHT_GRAY);
			setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
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
			transactions.addTransaction("Add container", new Runnable() {
				@Override
				public void run() {
					transactionFactory.execute(new AddContainerTransaction(region));
				}
			});
			transactions.addTransaction("Add text", new Runnable() {
				@Override
				public void run() {
					transactionFactory.execute(new AddTextTransaction(region));
				}
			});
		}

		@Override
		public void create(Factory factory, Rectangle creationBounds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
	}
	
	private ModelToolBar createRegionPlaceHolder(ViewManager viewManager, final String region, final TransactionFactory transactionFactory) {
		Model containerAreaModel = null; // TODO: Make a model, which can represent an area of a container
		final ModelToolBar toolBar = new ModelToolBar(region, containerAreaModel, transactionFactory);
		Model.wrapForFocus(viewManager, toolBar, toolBar);
		
//		JButton btnAddContainer = new JButton("Add container");
//		Model.wrap(viewManager, toolBar, btnAddContainer);
//		btnAddContainer.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				transactionFactory.execute(new AddContainerTransaction(region));
//			}
//		});
//		toolBar.add(btnAddContainer);
//		Model.wrap(viewManager, toolBar, toolBar);
//		
//		JButton btnAddText = new JButton("Add text");
//		Model.wrap(viewManager, toolBar, btnAddText);
//		btnAddText.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				transactionFactory.execute(new AddTextTransaction(region));
//			}
//		});
//		toolBar.add(btnAddText);
		
		return toolBar;
	}

	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		final ModelPanel view = new ModelPanel(this, transactionFactory);
		
		Model.wrapForFocus(viewManager, view, view);
		
		view.setLayout(new BorderLayout());
		
		String[] allRegions = new String[]{BorderLayout.NORTH, BorderLayout.CENTER, BorderLayout.SOUTH, BorderLayout.WEST, BorderLayout.EAST};
		for(final String region: allRegions) {
			Model model = regions.get(region);
			if(model != null) {
				Binding<ModelComponent> childComponent = model.createView(viewManager, transactionFactory.extend(new ContainerModel.RegionLocator(region)));
				view.add((JComponent)childComponent.getBindingTarget(), region);
			} else {
				ModelToolBar toolBar = createRegionPlaceHolder(viewManager, region, transactionFactory);
				view.add(toolBar, region);
			}
		}

		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new Observer() {
			@Override
			public void changed(Model sender, Object change) {
				if(change instanceof ContainerModel.AddedModelChange) {
					Binding<ModelComponent> childComponent = 
						((AddedModelChange)change).model.createView(viewManager, transactionFactory.extend(new ContainerModel.RegionLocator(((ContainerModel.AddedModelChange) change).region)));
					Component containedComponent = ((BorderLayout)view.getLayout()).getLayoutComponent(view, ((AddedModelChange)change).region);
					if(containedComponent != null)
						view.remove(containedComponent);
					view.add((JComponent)childComponent.getBindingTarget(), ((AddedModelChange)change).region);
//					view.revalidate();
					view.validate();
					view.repaint();
				} else if(change instanceof ContainerModel.RemovedModelChange) {
					Component containedComponent = ((BorderLayout)view.getLayout()).getLayoutComponent(((ContainerModel.RemovedModelChange)change).region);
					view.remove(containedComponent);
					ModelToolBar toolBar = createRegionPlaceHolder(viewManager, ((ContainerModel.RemovedModelChange)change).region, transactionFactory);
					view.add(toolBar, ((ContainerModel.RemovedModelChange)change).region);
//					viewManager.suspendFocusOnce();
//					viewManager.clearFocus();
					viewManager.setFocus(toolBar);
					view.validate();
					view.repaint();
				}
			}
		});
		
		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
				removableListener.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}

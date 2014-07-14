package dynamake.models;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import dynamake.commands.Command;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.delegates.Action1;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.factories.Factory;
import dynamake.tools.Tool;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.Trigger;

public class LiveModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class ButtonsToolBindingChanged {
		public final List<Integer> buttons;
		public final int tool;
		
		public ButtonsToolBindingChanged(List<Integer> buttons, int tool) {
			this.buttons = buttons;
			this.tool = tool;
		}
	}

	private Model content;
	private Hashtable<List<Integer>, Integer> buttonsToToolMap = new Hashtable<List<Integer>, Integer>();
	
	public LiveModel(Model content) {
		this.content = content;
	}
	
	@Override
	public Model modelCloneIsolated() {
		LiveModel clone = new LiveModel(content.cloneIsolated());
		
		clone.buttonsToToolMap.putAll(clone.buttonsToToolMap);
		
		return clone;
	}
	
	public int getToolForButtons(List<Integer> buttons) {
		Integer tool = buttonsToToolMap.get(buttons);
		return tool != null ? tool : -1;
	}

	public List<Integer> getButtonsForTool(int tool) {
		for(Map.Entry<List<Integer>, Integer> entry: buttonsToToolMap.entrySet()) {
			if(entry.getValue() == tool)
				return entry.getKey();
		}
		
		return Collections.emptyList();
	}
	
	public void removeButtonsToToolBinding(List<Integer> buttons, int tool, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		buttonsToToolMap.remove(buttons);
		sendChanged(new ButtonsToolBindingChanged(Collections.<Integer>emptyList(), tool), propCtx, propDistance, 0, collector);
	}
	
	public void bindButtonsToTool(List<Integer> buttons, int tool, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		buttonsToToolMap.put(buttons, tool);
		sendChanged(new ButtonsToolBindingChanged(buttons, tool), propCtx, propDistance, 0, collector);
	}
	
	public static class BindButtonsToToolCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;
		private List<Integer> buttons;
		private int tool;

		public BindButtonsToToolCommand(Location modelLocation, List<Integer> buttons, int tool) {
			this.modelLocation = modelLocation;
			this.buttons = buttons;
			this.tool = tool;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			LiveModel liveModel = (LiveModel)modelLocation.getChild(prevalentSystem);
			liveModel.bindButtonsToTool(buttons, tool, propCtx, 0, collector);
		}
	}
	
	public static class RemoveButtonsToToolBindingCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;
		private List<Integer> buttons;
		private int tool;

		public RemoveButtonsToToolBindingCommand(Location modelLocation, List<Integer> buttons, int tool) {
			this.modelLocation = modelLocation;
			this.buttons = buttons;
			this.tool = tool;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			LiveModel liveModel = (LiveModel)modelLocation.getChild(prevalentSystem);
			liveModel.removeButtonsToToolBinding(buttons, tool, propCtx, 0, collector);
		}
	}
	
	public static class ContentLocator implements dynamake.models.ModelLocator {
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
		public Location getModelComponentLocation() {
			return new ViewFieldContentLocation();
		}
	}
	
	private static class ViewFieldContentLocation implements Location {
		@Override
		public Object getChild(Object holder) {
			return ((LivePanel)holder).contentView.getBindingTarget();
		}
	}
	
	private static final int BUTTON_FONT_SIZE = 13;
	private static final Color TOP_BACKGROUND_COLOR = new Color(90, 90, 90);
	private static final Color TOP_BUTTON_BACKGROUND_COLOR = TOP_BACKGROUND_COLOR;
	private static final Color TOP_FOREGROUND_COLOR = Color.WHITE;
	
	public static class ToolButton extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int tool;
		private List<Integer> buttons;
		private String text;
		private LiveModel liveModel;
		private ModelTranscriber modelTranscriber;
		private JLabel labelToolName;
		private JPanel panelButtons;
		
		public ToolButton(int tool, List<Integer> buttons, String text, LiveModel liveModel, ModelTranscriber modelTranscriber) {
			this.tool = tool;
			this.buttons = buttons;
			this.text = text;
			this.liveModel = liveModel;
			this.modelTranscriber = modelTranscriber;

			setLayout(new BorderLayout(0, 0));
			setBackground(TOP_BUTTON_BACKGROUND_COLOR);
			labelToolName = new JLabel();
			labelToolName.setHorizontalAlignment(SwingConstants.CENTER);
			labelToolName.setForeground(TOP_FOREGROUND_COLOR);
			labelToolName.setFont(new Font(labelToolName.getFont().getFontName(), Font.BOLD, BUTTON_FONT_SIZE));
			add(labelToolName, BorderLayout.CENTER);
			
			panelButtons = new JPanel();
			panelButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
			panelButtons.setOpaque(false);
			add(panelButtons, BorderLayout.NORTH);
			
			this.setPreferredSize(new Dimension(72, 45));
			
			update();
			
			this.addMouseListener(new MouseAdapter() {
				int buttonsDown = 0;
				ArrayList<Integer> buttonsPressed = new ArrayList<Integer>();
				
				@Override
				public void mousePressed(MouseEvent e) {
					int newButton = e.getButton();
					
					buttonsDown++;
					if(!buttonsPressed.contains(newButton)) {
						buttonsPressed.add(newButton);
						Collections.sort(buttonsPressed);
					}
					
					if(buttonsDown == 1) {
						setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
					}
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							update(buttonsPressed);
							ToolButton.this.repaint();
						}
					});
				}
				
				@Override
				public void mouseReleased(MouseEvent e) {
					buttonsDown--;
					
					if(buttonsDown == 0) {
						setBackground(TOP_BUTTON_BACKGROUND_COLOR);
						
						@SuppressWarnings("unchecked")
						final ArrayList<Integer> localButtonsPressed = (ArrayList<Integer>)buttonsPressed.clone();
						
						Connection<Model> connection = ToolButton.this.modelTranscriber.createConnection();
						
						connection.trigger(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
								collector.execute(new DualCommandFactory<Model>() {
									@Override
									public Model getReference() {
										return ToolButton.this.liveModel;
									}
									
									@Override
									public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
										List<Integer> currentButtons = ToolButton.this.buttons;
										
										if(localButtonsPressed.equals(currentButtons)) {
											// If the indicated combination is the same as the current combination, then remove
											// the current binding
											dualCommands.add(new DualCommandPair<Model>(
												new RemoveButtonsToToolBindingCommand(location, localButtonsPressed, ToolButton.this.tool),
												new BindButtonsToToolCommand(location, localButtonsPressed, ToolButton.this.tool)
											));
										} else {
											int previousToolForNewButton = ToolButton.this.liveModel.getToolForButtons(localButtonsPressed);
											
											if(previousToolForNewButton != -1) {
												// If the new buttons are associated to another tool, then remove that binding
												dualCommands.add(new DualCommandPair<Model>(
													new RemoveButtonsToToolBindingCommand(location, localButtonsPressed, previousToolForNewButton), 
													new BindButtonsToToolCommand(location, localButtonsPressed, previousToolForNewButton))
												);
											}
											
											if(currentButtons.size() > 0) {
												// If this tool is associated to buttons, then remove that binding before
												dualCommands.add(new DualCommandPair<Model>(
													new RemoveButtonsToToolBindingCommand(location, currentButtons, ToolButton.this.tool), 
													new BindButtonsToToolCommand(location, currentButtons, ToolButton.this.tool))
												);
												
												// adding the replacement binding
												dualCommands.add(new DualCommandPair<Model>(
													new BindButtonsToToolCommand(location, localButtonsPressed, ToolButton.this.tool), 
													new RemoveButtonsToToolBindingCommand(location, localButtonsPressed, ToolButton.this.tool))
												);
											} else {
												dualCommands.add(new DualCommandPair<Model>(
													new BindButtonsToToolCommand(location, localButtonsPressed, ToolButton.this.tool), 
													new RemoveButtonsToToolBindingCommand(location, localButtonsPressed, ToolButton.this.tool)
												));
											}
										}
									}
								});
								collector.commit();
							}
						});
						
						buttonsPressed.clear();
					}
				}
			});
			
//			// Support for binding a key combination with a tool
//			// It should be possible to both bind a key combination AND a mouse button to the same tool at the same time
//			KeyListener keyListener = new KeyAdapter() {
//				@Override
//				public void keyPressed(KeyEvent e) {
////					System.out.println(e.isControlDown() + ":" + e.getKeyCode());
//				}
//				
//				@Override
//				public void keyTyped(KeyEvent e) {
////					System.out.println(e.isControlDown() + ":" + e.getKeyCode());
//				}
//			};
			
			setFocusable(true);
			
//			this.addKeyListener(keyListener);
//			labelToolName.addKeyListener(keyListener);
//			labelButton.addKeyListener(keyListener);
		}
		
		private static final Color[] BUTTON_COLORS = new Color[] {
			new Color(255, 120, 10),//Color.RED,
			new Color(10, 220, 10), //Color.GREEN,
			new Color(10, 10, 220), //Color.BLUE,
			new Color(10, 220, 220), //Color.CYAN,
			new Color(220, 220, 10), //Color.ORANGE
			new Color(220, 10, 220),
		};
		
		public static Color avgColorOfButtons(List<Integer> buttons) {
			ArrayList<Color> buttonColors = new ArrayList<Color>();
			for(int buttonPressed: buttons)
				buttonColors.add(getColorForButton(buttonPressed));
			return avgColor(buttonColors);
		}
		
		public static Color getColorForButton(int button) {
			return BUTTON_COLORS[button - 1];
		}
		
		public static Color avgColor(List<Color> colors) {
			int rSum = 0;
			int gSum = 0;
			int bSum = 0;
			
			for(Color c: colors) {
				rSum += c.getRed();
				gSum += c.getGreen();
				bSum += c.getBlue();
			}
			
			return new Color(rSum / colors.size(), gSum / colors.size(), bSum / colors.size());
		}
		
		private void update(List<Integer> buttons) {
			labelToolName.setText(text);
			
			Border innerBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
			Border outerBorder;
			
			panelButtons.removeAll();
			
			if(buttons.size() > 0) {
				for(int button: buttons) {
					JLabel buttonLabel = new JLabel();
					buttonLabel.setHorizontalAlignment(SwingConstants.CENTER);
					buttonLabel.setForeground(getColorForButton(button));
					buttonLabel.setFont(new Font(buttonLabel.getFont().getFontName(), Font.ITALIC | Font.BOLD, 16));
					buttonLabel.setText("" + button);
					
					panelButtons.add(buttonLabel);
				}
				
				panelButtons.revalidate();
				
				outerBorder = BorderFactory.createLoweredSoftBevelBorder();
			} else {
				JLabel buttonLabel = new JLabel();
				buttonLabel.setText(" ");
				buttonLabel.setForeground(null);
				
				panelButtons.add(buttonLabel);

				outerBorder = BorderFactory.createRaisedSoftBevelBorder();
			}
			
			setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		}
		
		private void update() {
			update(buttons);
		}
		
		public void setButtons(List<Integer> buttons) {
			this.buttons = buttons;
			update();
		}

		public void showAsActive() {
			setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
		}

		public void showAsPassive() {
			setBackground(TOP_BUTTON_BACKGROUND_COLOR);
		}
	}
	
	private static ToolButton createToolButton(final LiveModel model, final ModelTranscriber modelTranscriber, ButtonGroup group, List<Integer> buttons, final int tool, final String text) {
		return new ToolButton(tool, buttons, text, model, modelTranscriber);
	}
	
	private static void updateToolButton(JComponent toolButton, List<Integer> buttons) {
		((ToolButton)toolButton).setButtons(buttons);
	}

	public static class ProductionPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final Color TARGET_OVER_COLOR = new Color(35, 89, 184);
		public static final Color BIND_COLOR = new Color(25, 209, 89);
		public static final Color UNBIND_COLOR = new Color(240, 34, 54);
		public static final Color SELECTION_COLOR = Color.GRAY;
		
		public static class EditPanelMouseAdapter extends MouseAdapter {
			public ProductionPanel productionPanel;

			public int buttonPressed;
			
			public static final int HORIZONTAL_REGION_WEST = 0;
			public static final int HORIZONTAL_REGION_CENTER = 1;
			public static final int HORIZONTAL_REGION_EAST = 2;
			public static final int VERTICAL_REGION_NORTH = 0;
			public static final int VERTICAL_REGION_CENTER = 1;
			public static final int VERTICAL_REGION_SOUTH = 2;
			
			public EditPanelMouseAdapter(ProductionPanel productionPanel) {
				this.productionPanel = productionPanel;
				toolConnection = productionPanel.livePanel.getModelTranscriber().createConnection();
			}
			
			private Tool getTool(List<Integer> buttons) {
				int toolForButton = productionPanel.livePanel.model.getToolForButtons(buttons);
				if(toolForButton != -1) {
					return productionPanel.livePanel.viewManager.getTools()[toolForButton];
				} else {
					return new Tool() {
						@Override
						public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }
						
						@Override
						public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }
						
						@Override
						public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }
						
						@Override
						public void mouseExited(ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector) { }
						
						@Override
						public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) { }
						
						@Override
						public String getName() { return null; }
						
						@Override
						public void rollback(ProductionPanel productionPanel, Collector<Model> collector) { }
					};
				}
			}
			
			private ToolButton getToolButton(List<Integer> buttons) {
				int toolForButton = productionPanel.livePanel.model.getToolForButtons(buttons);
				if(toolForButton != -1) {
					return productionPanel.livePanel.buttonTools[toolForButton];
				} else {
					return null;
				}
			}
			
			public ModelComponent getModelOver(MouseEvent e) {
				Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
				JComponent componentOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
				return ModelComponent.Util.closestModelComponent(componentOver);
			}
			
			private Tool toolBeingApplied;
			private int buttonsDown;
			public ArrayList<Integer> buttonsPressed = new ArrayList<Integer>();
			
			private Connection<Model> toolConnection;
			
			public void mousePressed(final MouseEvent e) {
				// Check whether there is an active tool which must be rolled back
				// because a new combination of button is recognized
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						int button = e.getButton();
						
						if(!buttonsPressed.contains(button) && buttonsPressed.size() > 0) {
							final Tool toolToRollback = toolBeingApplied;
							
							toolToRollback.rollback(productionPanel, collector);
							collector.reject();
							
							ToolButton toolButton = getToolButton(buttonsPressed);
							if(toolButton != null)
								toolButton.showAsPassive();
						}
					}
				});
				
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						buttonsDown++;
						
						int button = e.getButton();
						
						if(!buttonsPressed.contains(button)) {
							buttonsPressed.add(button);
							Collections.sort(buttonsPressed);
							toolBeingApplied = getTool(buttonsPressed);
							
							final ModelComponent modelOver = getModelOver(e);
							final Tool toolToApply = toolBeingApplied;
							
							toolToApply.mousePressed(productionPanel, e, modelOver, toolConnection, collector);
							
							ToolButton toolButton = getToolButton(buttonsPressed);
							if(toolButton != null)
								toolButton.showAsActive();
						}
					}
				});
			}

			public void mouseDragged(final MouseEvent e) {
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						final ModelComponent modelOver = getModelOver(e);

						if(modelOver != null) {
							final Tool toolToApply = toolBeingApplied;
							toolToApply.mouseDragged(productionPanel, e, modelOver, collector, toolConnection);
						}
					}
				});
			}

			public void mouseReleased(final MouseEvent e) {
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						buttonsDown--;
						
						final ModelComponent modelOver = getModelOver(e);

						if(modelOver != null) {
							if(buttonsDown == 0) {
								final Tool toolToApply = toolBeingApplied;
								toolToApply.mouseReleased(productionPanel, e, modelOver, toolConnection, collector);
								
								ToolButton toolButton = getToolButton(buttonsPressed);
								if(toolButton != null)
									toolButton.showAsPassive();
								
								buttonsPressed.clear();
								toolBeingApplied = null;
							}
						}
					}
				});
			}
			
			@Override
			public void mouseMoved(final MouseEvent e) {

			}
		}
		
		public LivePanel livePanel;
		public Binding<ModelComponent> contentView;
		public EditPanelMouseAdapter editPanelMouseAdapter;
		
		public ProductionPanel(final LivePanel livePanel, final Binding<ModelComponent> contentView) {
			this.setLayout(null);
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
		private ModelTranscriber modelTranscriber;
		private ToolButton[] buttonTools;
		private final Binding<ModelComponent> contentView;
		
		public LivePanel(final ModelComponent rootView, LiveModel model, ModelTranscriber modelTranscriber, final ViewManager viewManager) {
			modelTranscriber.setComponentToRepaint(this);
			this.setLayout(new BorderLayout());
			this.model = model;
			this.viewManager = viewManager;
			this.modelTranscriber = modelTranscriber;
			
			ViewManager newViewManager = new ViewManager() {
				@Override
				public Factory[] getFactories() {
					return viewManager.getFactories();
				}
				
				@Override
				public Tool[] getTools() {
					return null;
				}
			};

			contentView = model.getContent().createView(rootView, newViewManager, modelTranscriber.extend(new ContentLocator()));

			productionPanel = new ProductionPanel(this, contentView);
			
			topPanel = new JPanel();
			
			topPanel.setBackground(TOP_BACKGROUND_COLOR);
			
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
				{
					initializeObserverAdapter();
				}
				
				private void initializeObserverAdapter() {

				}
				
				@Override
				public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof LiveModel.ButtonsToolBindingChanged) {
						LiveModel.ButtonsToolBindingChanged bindButtonChanged = (LiveModel.ButtonsToolBindingChanged)change;
						
						if(bindButtonChanged.tool != -1) {
							JComponent buttonNewTool = buttonTools[bindButtonChanged.tool];
							updateToolButton(buttonNewTool, bindButtonChanged.buttons);
						}
					}
				}
			});
			
			contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
		}
		
		@Override
		public void initialize() {
			Tool[] tools = viewManager.getTools();
			buttonTools = new ToolButton[tools.length];
			ButtonGroup group = new ButtonGroup();

			for(int i = 0; i < tools.length; i++) {
				Tool tool = tools[i];
				List<Integer> buttons = model.getButtonsForTool(i);
				buttonTools[i] = createToolButton(model, modelTranscriber, group, buttons, i, tool.getName());
			}
			
			for(JComponent buttonTool: buttonTools) {
				topPanel.add(buttonTool);
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
		public void appendContainerTransactions(LivePanel livePanel, CompositeMenuBuilder menuBuilder, ModelComponent child) {

		}

		@Override
		public void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder) {

		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, menuBuilder);
		}

		@Override
		public void appendDropTargetTransactions(
			ModelComponent livePanel, ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, CompositeMenuBuilder menuBuilder) {

		}

		@Override
		public ModelTranscriber getModelTranscriber() {
			return modelTranscriber;
		}

		public void releaseBinding() {
			removableListener.releaseBinding();
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			visitAction.run(this);
		}
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, ModelTranscriber modelTranscriber) {
		this.setLocator(modelTranscriber.getModelLocator());
		
		final LivePanel view = new LivePanel(rootView, this, modelTranscriber, viewManager);
		
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

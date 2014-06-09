package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
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

	public static class SelectionChanged { }

	public static class OutputChanged { }

	public static class StateChanged { }

	public static class ButtonToolBindingChanged {
		public final int button;
		public final int tool;
		
		public ButtonToolBindingChanged(int button, int tool) {
			this.button = button;
			this.tool = tool;
		}
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
	
	private Hashtable<Integer, Integer> buttonToToolMap = new Hashtable<Integer, Integer>();
//	private Hashtable<Integer, Integer> toolToButtonMap = new Hashtable<Integer, Integer>();
	
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

		sendChanged(new SelectionChanged(), propCtx, propDistance, 0, branch);
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
	
	public int getToolForButton(int button) {
		Integer tool = buttonToToolMap.get(button);
		return tool != null ? tool : -1;
	}

	public int getButtonForTool(int tool) {
		for(Map.Entry<Integer, Integer> entry: buttonToToolMap.entrySet()) {
			if(entry.getValue() == tool)
				return entry.getKey();
		}
		
		return -1;
		
//		Integer button = toolToButtonMap.get(tool);
//		return button != null ? button : -1;
	}
	
//	public int[] getButtonsForTool(int tool) {
//		ArrayList<Integer> toolButtons = toolToButtonsMap.get(tool);
//		if(toolButtons == null)
//			return new int[0];
//		
//		int[] toolButtonsArray = new int[toolButtons.size()];
//		for(int i = 0; i < toolButtons.size(); i++)
//			toolButtonsArray[i] = toolButtons.get(i);
//		
//		return toolButtonsArray;
//	}
	
	public void removeButtonToToolBinding(int button, int tool, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		buttonToToolMap.remove(button);
//		System.out.println("Removed binding from button " + button + " and tool " + tool);
//		System.out.println("buttonToToolMap: " + buttonToToolMap);
		
		sendChanged(new ButtonToolBindingChanged(-1, tool), propCtx, propDistance, 0, branch);
	}
	
	public void bindButtonToTool(int button, int tool, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
//		ArrayList<Integer> toolButtons = toolToButtonsMap.get(button);
//		if(toolButtons == null) {
//			toolButtons = new ArrayList<Integer>();
//			toolButtons.add(button);
//		}
//		
//		Integer currentToolForButton = buttonToToolMap.get(button);
//		if(currentToolForButton != null) {
//			ArrayList<Integer> currentToolButtons = toolToButtonsMap.get(tool);
//			currentToolButtons.remove((Integer)button);
//		}
		
		buttonToToolMap.put(button, tool);
//		System.out.println("Bound button " + button + " to tool " + tool);
//			
//		System.out.println("buttonToToolMap: " + buttonToToolMap);
//		toolToButtonMap.put(tool, button);
		sendChanged(new ButtonToolBindingChanged(button, tool), propCtx, propDistance, 0, branch);
	}
	
	public static class BindButtonToToolCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;
		private int button;
		private int tool;

		public BindButtonToToolCommand(Location modelLocation, int button, int tool) {
			this.modelLocation = modelLocation;
			this.button = button;
			this.tool = tool;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			LiveModel liveModel = (LiveModel)modelLocation.getChild(prevalentSystem);
			liveModel.bindButtonToTool(button, tool, propCtx, 0, branch);
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return false;
		}
	}
	
	public static class RemoveButtonToToolBindingCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;
		private int button;
		private int tool;

		public RemoveButtonToToolBindingCommand(Location modelLocation, int button, int tool) {
			this.modelLocation = modelLocation;
			this.button = button;
			this.tool = tool;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			LiveModel liveModel = (LiveModel)modelLocation.getChild(prevalentSystem);
			liveModel.removeButtonToToolBinding(button, tool, propCtx, 0, branch);
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return false;
		}
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
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
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
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
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
		
		@Override
		public boolean occurredWithin(Location location) {
			return false;
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
	
	private static final int BUTTON_FONT_SIZE = 13;
	private static final Color TOP_BACKGROUND_COLOR = Color.GRAY;
	private static final Color TOP_FOREGROUND_COLOR = Color.WHITE;
	
	public static final int TAG_CAUSED_BY_UNDO = 0;
	public static final int TAG_CAUSED_BY_REDO = 1;
	public static final int TAG_CAUSED_BY_TOGGLE_BUTTON = 2;
	public static final int TAG_CAUSED_BY_ROLLBACK = 3;
	public static final int TAG_CAUSED_BY_COMMIT = 4;
	
	private static class ToolButton extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int tool;
		private int button;
		private String text;
		private LiveModel liveModel;
		private TransactionFactory transactionFactory;
		private JLabel labelToolName;
		private JLabel labelButton;
		
		public ToolButton(int tool, int button, String text, LiveModel liveModel, TransactionFactory transactionFactory) {
			this.tool = tool;
			this.button = button;
			this.text = text;
			this.liveModel = liveModel;
			this.transactionFactory = transactionFactory;

			/*
			buttonTool.setBackground(TOP_BACKGROUND_COLOR);
			buttonTool.setForeground(TOP_FOREGROUND_COLOR);
			*/
			
			setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			setLayout(new BorderLayout());
			setBackground(TOP_BACKGROUND_COLOR);
			labelToolName = new JLabel();
			labelToolName.setAlignmentY(JLabel.CENTER_ALIGNMENT);
			labelToolName.setForeground(TOP_FOREGROUND_COLOR);
			labelToolName.setFont(new Font(labelToolName.getFont().getFontName(), Font.BOLD, BUTTON_FONT_SIZE));
			add(labelToolName, BorderLayout.CENTER);
			labelButton = new JLabel();
			add(labelButton, BorderLayout.EAST);
			labelButton.setFont(new Font(labelButton.getFont().getFontName(), Font.ITALIC | Font.BOLD, 18));
			labelButton.setAlignmentY(JLabel.CENTER_ALIGNMENT);
			this.setPreferredSize(new Dimension(60, 25));
			
			update();
			
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
//					System.out.println("Mouse button:" + e.getButton());
					
					final int newButton = e.getButton();
					
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_TOGGLE_BUTTON);
					
					PrevaylerServiceBranch<Model> branch = ToolButton.this.transactionFactory.createBranch();
					
					branch.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							setBackground(TOP_BACKGROUND_COLOR.brighter());
							
							int currentButton = ToolButton.this.button;
							
							Location modelLocation = ToolButton.this.transactionFactory.getModelLocation();
							
							int previousToolForNewButton = ToolButton.this.liveModel.getToolForButton(newButton);
							
							if(previousToolForNewButton != -1) {
								// If the new button is associated to another tool, then remove that binding
								dualCommands.add(new DualCommandPair<Model>(
									new RemoveButtonToToolBindingCommand(modelLocation, newButton, previousToolForNewButton), 
									new BindButtonToToolCommand(modelLocation, newButton, previousToolForNewButton))
								);
							}
							
							if(currentButton != -1) {
								// If this tool is associated to button, then remove that binding before
								dualCommands.add(new DualCommandPair<Model>(
									new RemoveButtonToToolBindingCommand(modelLocation, currentButton, ToolButton.this.tool), 
									new BindButtonToToolCommand(modelLocation, currentButton, ToolButton.this.tool))
								);
								
								// adding the replacement binding
								dualCommands.add(new DualCommandPair<Model>(
									new BindButtonToToolCommand(modelLocation, newButton, ToolButton.this.tool), 
									new RemoveButtonToToolBindingCommand(modelLocation, newButton, ToolButton.this.tool))
								);
							} else {
								dualCommands.add(new DualCommandPair<Model>(
									new BindButtonToToolCommand(modelLocation, newButton, ToolButton.this.tool), 
									new RemoveButtonToToolBindingCommand(modelLocation, newButton, ToolButton.this.tool)
								));
							}
						}
					});
					branch.close();
				}
				
				@Override
				public void mouseReleased(MouseEvent e) {
					setBackground(TOP_BACKGROUND_COLOR);
				}
			});
		}
		
		private static final Color[] BUTTON_COLORS = new Color[] {
			new Color(220, 10, 10),//Color.RED,
			new Color(10, 220, 10), //Color.GREEN,
			new Color(10, 10, 220), //Color.BLUE,
			new Color(10, 220, 220), //Color.CYAN,
			new Color(220, 220, 10), //Color.ORANGE
			new Color(220, 10, 220),
		};
		
		public static Color getColorForButton(int button) {
			return BUTTON_COLORS[button - 1];
		}
		
		private void update() {
			labelToolName.setText(text);
			if(button != -1) {
				labelButton.setText("" + button);
//				label.setText(text + "(" + button + ")");
				labelButton.setForeground(getColorForButton(button));
			} else {
				labelButton.setText("");
				labelButton.setForeground(null);
			}
		}
		
		public void setButton(int button) {
			this.button = button;
			update();
		}
	}
	
	private static JComponent createToolButton(final LiveModel model, final TransactionFactory transactionFactory, ButtonGroup group, int button, int currentTool, final int tool, final String text) {
		final ToolButton buttonTool = new ToolButton(tool, button, text, model, transactionFactory);
		
//		buttonTool.setBackground(TOP_BACKGROUND_COLOR);
//		buttonTool.setForeground(TOP_FOREGROUND_COLOR);
//		buttonTool.setBorderPainted(false);
		
		/*
		Somehow, when clicking on a button, the mouse button which was used to click on the button
		should be associated to the particular tool the clicked button represent.
		
		This means that, then a mouse button is used to click on the production panel, then tool, associated
		to the button, should be looked up and used.
		
		If the button is already associated to a tool, this associated is removed.
		*/
		
//		buttonTool.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				System.out.println("Mouse button:" + e.getButton());
//			}
//		});
		
//		buttonTool.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mousePressed(MouseEvent e) {
////				System.out.println("Mouse button:" + e.getButton());
//				
//				final int newButton = e.getButton();
//				
//				PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_TOGGLE_BUTTON);
//				
//				PrevaylerServiceBranch<Model> branch = transactionFactory.createBranch();
//				
//				branch.execute(propCtx, new DualCommandFactory<Model>() {
//					@Override
//					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
//						int currentButton = buttonTool.button;
//						
//						Location modelLocation = transactionFactory.getModelLocation();
//						
//						int previousToolForNewButton = model.getToolForButton(newButton);
//						
//						if(previousToolForNewButton != -1) {
//							// If the new button is associated to another tool, then remove that binding
//							dualCommands.add(new DualCommandPair<Model>(
//								new RemoveButtonToToolBindingCommand(modelLocation, newButton, previousToolForNewButton), 
//								new BindButtonToToolCommand(modelLocation, newButton, previousToolForNewButton))
//							);
//						}
//						
//						if(currentButton != -1) {
//							// If this tool is associated to button, then remove that binding before
//							dualCommands.add(new DualCommandPair<Model>(
//								new RemoveButtonToToolBindingCommand(modelLocation, currentButton, tool), 
//								new BindButtonToToolCommand(modelLocation, currentButton, tool))
//							);
//							
//							// adding the replacement binding
//							dualCommands.add(new DualCommandPair<Model>(
//								new BindButtonToToolCommand(modelLocation, newButton, tool), 
//								new RemoveButtonToToolBindingCommand(modelLocation, newButton, tool))
//							);
//						} else {
//							dualCommands.add(new DualCommandPair<Model>(
//								new BindButtonToToolCommand(modelLocation, newButton, tool), 
//								new RemoveButtonToToolBindingCommand(modelLocation, newButton, tool)
//							));
//						}
//					}
//				});
//				branch.close();
//			}
//		});
		
//		buttonTool.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				System.out.println("Change tool to " + text);
//				// Indicate this is an radio button toggle context
//				PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_TOGGLE_BUTTON);
//				
//				PrevaylerServiceBranch<Model> branch = transactionFactory.createBranch();
//				
//				branch.execute(propCtx, new DualCommandFactory<Model>() {
//					@Override
//					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
//						Location modelLocation = transactionFactory.getModelLocation();
//						int previousTool = model.getTool();
//						
//						dualCommands.add(
//							new DualCommandPair<Model>(new SetTool(modelLocation, tool), new SetTool(modelLocation, previousTool))
//						);
//					}
//				});
//				branch.close();
//			}
//		});
		buttonTool.setFocusable(false);
//		group.add(buttonTool);
//		if(currentTool == tool) {
//			buttonTool.setSelected(true);
//		}
		return buttonTool;
	}
	
	private static void updateToolButton(JComponent toolButton, int button) {
		((ToolButton)toolButton).setButton(button);
	}

	public static class ProductionPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		// Temporary frames
		private JPanel effectFrame;
		public JPanel targetFrame;
		
		// Persistent frames
		public JPanel selectionFrame;
		public JPanel outputFrame;
		
		public Binding<Component> selectionBoundsBinding;

		public static final Color TARGET_OVER_COLOR = new Color(35, 89, 184);
		public static final Color BIND_COLOR = new Color(25, 209, 89);
		public static final Color UNBIND_COLOR = new Color(240, 34, 54);
		public static final Color SELECTION_COLOR = Color.GRAY;
		public static final Color OUTPUT_COLOR = new Color(54, 240, 17);
		
		public static class EditPanelMouseAdapter extends MouseAdapter {
			public ProductionPanel productionPanel;
			
			public ModelComponent selection;
			public Point selectionMouseDown;
			public Rectangle initialEffectBounds;
			public Dimension selectionFrameSize;
			public int selectionFrameHorizontalPosition;
			public int selectionFrameVerticalPosition;
			public ModelComponent targetOver;
			
			public ModelComponent output;

			protected int buttonPressed;
			
			public static final int HORIZONTAL_REGION_WEST = 0;
			public static final int HORIZONTAL_REGION_CENTER = 1;
			public static final int HORIZONTAL_REGION_EAST = 2;
			public static final int VERTICAL_REGION_NORTH = 0;
			public static final int VERTICAL_REGION_CENTER = 1;
			public static final int VERTICAL_REGION_SOUTH = 2;
			
			public EditPanelMouseAdapter(ProductionPanel productionPanel) {
				this.productionPanel = productionPanel;
			}
			
			private Tool getTool(int button) {
				int toolForButton = productionPanel.livePanel.model.getToolForButton(button);
				if(toolForButton != -1) {
//					return productionPanel.livePanel.viewManager.getTools()[productionPanel.livePanel.model.tool - 1];
					return productionPanel.livePanel.viewManager.getTools()[toolForButton - 1];
				} else {
					return new Tool() {
						@Override
						public void mouseReleased(ProductionPanel productionPanel, MouseEvent e) { }
						
						@Override
						public void mousePressed(ProductionPanel productionPanel, MouseEvent e) { }
						
						@Override
						public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) { }
						
						@Override
						public void mouseExited(ProductionPanel productionPanel, MouseEvent e) { }
						
						@Override
						public void mouseDragged(ProductionPanel productionPanel, MouseEvent e) { }
						
						@Override
						public String getName() { return null; }
					};
				}
			}
			
			public void createEffectFrame(Rectangle creationBounds, Point initialMouseDown) {
				if(productionPanel.effectFrame == null) {
					final JPanel localEffectFrame = new JPanel();
					localEffectFrame.setBackground(new Color(0, 0, 0, 0));
					localEffectFrame.setBounds(creationBounds);
					
//					Color effectColor = Color.BLACK;
					Color effectColor = ToolButton.getColorForButton(buttonPressed);
					effectColor = effectColor.darker();
					localEffectFrame.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createDashedBorder(effectColor, 2.0f, 2.0f, 1.5f, false),
						BorderFactory.createDashedBorder(Color.WHITE, 2.0f, 2.0f, 1.5f, false)
					));
					
					productionPanel.effectFrame = localEffectFrame;
					selectionMouseDown = initialMouseDown;
					initialEffectBounds = creationBounds;
					
					// Ensure effect frame is shown in front of selection frame
					if(productionPanel.selectionFrame != null) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								productionPanel.remove(productionPanel.selectionFrame);
								productionPanel.add(localEffectFrame);
								productionPanel.add(productionPanel.selectionFrame);
							}
						});
					} else {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								productionPanel.add(localEffectFrame);
							}
						});
					}
				} else {
					System.out.println("Attempted to created an effect frame when it has already been created.");
				}
			}
			
//			public void changeEffectFrame(final Rectangle newBounds) {
//				if(productionPanel.effectFrame != null) {
//					final JPanel localEffectFrame = productionPanel.effectFrame;
//					SwingUtilities.invokeLater(new Runnable() {
//						@Override
//						public void run() {
//							localEffectFrame.setBounds(newBounds);
//						}
//					});
//				} else {
//					System.out.println("Attempted to change effect frame when it hasn't been created.");
//				}
//			}
			
			public void changeEffectFrameDirect(final Rectangle newBounds) {
				productionPanel.effectFrame.setBounds(newBounds);
			}
			
			public void clearEffectFrame() {
				if(productionPanel.effectFrame != null) {
					final JPanel localEffectFrame = productionPanel.effectFrame;
					productionPanel.effectFrame = null;
					selectionMouseDown = null;
					initialEffectBounds = null;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(localEffectFrame);
						}
					});
				} else {
					System.out.println("Attempted to clear effect frame when it hasn't been created.");
				}
			}

			public int getEffectFrameX() {
				return productionPanel.effectFrame.getX();
			}

			public int getEffectFrameY() {
				return productionPanel.effectFrame.getY();
			}

			public int getEffectFrameWidth() {
				return productionPanel.effectFrame.getWidth();
			}

			public int getEffectFrameHeight() {
				return productionPanel.effectFrame.getHeight();
			}

			public Rectangle getEffectFrameBounds() {
				return productionPanel.effectFrame.getBounds();
			}

			public void setEffectFrameCursor(final Cursor cursor) {
				final JPanel localEffectFrame = productionPanel.effectFrame;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						localEffectFrame.setCursor(cursor);
					}
				});
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
			
			public Cursor getCursorFromRelativePosition() {
				return getCursorFromRelativePosition(selectionFrameHorizontalPosition, selectionFrameVerticalPosition);
			}
			
			public static Cursor getCursorFromRelativePosition(int horizontalPosition, int verticalPosition) {
				final Cursor cursor;
				
				switch(horizontalPosition) {
				case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_WEST:
					switch(verticalPosition) {
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
						cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
						break;
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
						cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
						break;
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
						cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
						break;
					default:
						cursor = null;
						break;
					}
					break;
				case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_CENTER:
					switch(verticalPosition) {
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
						cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
						break;
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
						cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
						break;
					default:
						cursor = null;
						break;
					}
					break;
				case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_EAST:
					switch(verticalPosition) {
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
						cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
						break;
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
						cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
						break;
					case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
						cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
						break;
					default:
						cursor = null;
						break;
					}
					break;
				default:
					cursor = null;
					break;
				}
				
				return cursor;
			}
			
			public void selectFromView(final ModelComponent view, final Point initialMouseDown, PrevaylerServiceBranch<Model> branch) {
				Rectangle effectBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
				requestSelect(view, effectBounds, branch);
				createEffectFrame(effectBounds, initialMouseDown);
			}
			
			public void selectFromDefault(final ModelComponent view, final Point initialMouseDown, PrevaylerServiceBranch<Model> branch) {
				Dimension sourceBoundsSize = new Dimension(125, 33);
				Point sourceBoundsLocation = new Point(initialMouseDown.x - sourceBoundsSize.width / 2, initialMouseDown.y - sourceBoundsSize.height / 2);
				Rectangle sourceBounds = new Rectangle(sourceBoundsLocation, sourceBoundsSize);
				Rectangle selectionBounds = SwingUtilities.convertRectangle((JComponent)view, sourceBounds, productionPanel);
				requestSelect(view, selectionBounds, branch);
				createEffectFrame(selectionBounds, initialMouseDown);
			}
			
			public void selectFromEmpty(final ModelComponent view, final Point initialMouseDown, PrevaylerServiceBranch<Model> branch) {
				requestSelect(view, new Rectangle(0, 0, 0, 0), branch);
				createEffectFrame(new Rectangle(0, 0, 0, 0), initialMouseDown);
			}
			
			private void requestSelect(final ModelComponent view, final Rectangle effectBounds, PrevaylerServiceBranch<Model> branch) {
				// Notice: executes a transaction
				PropogationContext propCtx = new PropogationContext();
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						createSelectCommands(view, dualCommands);
					}
				});
			}
			
			public void createSelectCommands(final ModelComponent view, List<DualCommand<Model>> dualCommands) {
				Location selectionLocation = view != null ? view.getTransactionFactory().getModelLocation() : null;
				createSelectCommandsFromLocation(selectionLocation, dualCommands);
			}
			
			public void createSelectCommandsFromLocation(final Location selectionLocation, List<DualCommand<Model>> dualCommands) {
				final Location liveModelLocation = productionPanel.livePanel.getTransactionFactory().getModelLocation();
				
				Location currentSelectionLocation = EditPanelMouseAdapter.this.selection != null 
						? EditPanelMouseAdapter.this.selection.getTransactionFactory().getModelLocation() : null; 
						
				dualCommands.add(new DualCommandPair<Model>(
					new SetSelection(liveModelLocation, selectionLocation), 
					new SetSelection(liveModelLocation, currentSelectionLocation)
				));
			}
			
			private void select(final ModelComponent view) {
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
								// The tool associated to button 1 it used here
								getTool(1).mouseMoved(productionPanel, e);
							}

							public void mouseExited(MouseEvent e) {
								// The tool associated to button 1 it used here
								getTool(1).mouseExited(productionPanel, e);
							}

							@Override
							public void mousePressed(MouseEvent e) {
								productionPanel.editPanelMouseAdapter.buttonPressed = e.getButton();
								getTool(productionPanel.editPanelMouseAdapter.buttonPressed).mousePressed(productionPanel, e);
							}

							@Override
							public void mouseDragged(MouseEvent e) {
								getTool(productionPanel.editPanelMouseAdapter.buttonPressed).mouseDragged(productionPanel, e);
							}

							@Override
							public void mouseReleased(MouseEvent e) {
								getTool(productionPanel.editPanelMouseAdapter.buttonPressed).mouseReleased(productionPanel, e);
								productionPanel.editPanelMouseAdapter.buttonPressed = -1;
							}
						};
						
						productionPanel.selectionFrame.addMouseListener(mouseAdapter);
						productionPanel.selectionFrame.addMouseMotionListener(mouseAdapter);

						final JPanel selectionFrame = productionPanel.selectionFrame; 
						
						if(productionPanel.effectFrame != null)
							System.out.println("Effect frame was there before selection was added");
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								productionPanel.add(selectionFrame);
							}
						});
					}
					
					selectionFrameSize = ((JComponent)view).getSize();
					
					final Rectangle selectionBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);

					final JPanel selectionFrame = productionPanel.selectionFrame; 
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							selectionFrame.setBounds(selectionBounds);
						}
					});
					
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
				} else {
					if(productionPanel.selectionFrame != null)
						productionPanel.clearFocus();
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
					final JPanel targetFrame = productionPanel.targetFrame;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(targetFrame);
						}
					});
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
				
				final int button = e.getButton();
				productionPanel.livePanel.getTransactionFactory().executeTransient(new Runnable() {
					@Override
					public void run() {
						productionPanel.editPanelMouseAdapter.buttonPressed = button;
						getTool(button).mousePressed(productionPanel, e);
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
			
			@Override
			public void mouseMoved(MouseEvent e) {
//				getTool().mouseMoved(productionPanel, e);
			}

			public void setOutput(ModelComponent view) {
				this.output = view;
				if(view != null) {
					if(productionPanel.outputFrame == null) {
						productionPanel.outputFrame = new JPanel();
						productionPanel.outputFrame.setBackground(new Color(0, 0, 0, 0));
						
						productionPanel.outputFrame.setBorder(
							BorderFactory.createBevelBorder(
								BevelBorder.RAISED, OUTPUT_COLOR.darker().darker(), OUTPUT_COLOR.darker(), OUTPUT_COLOR.darker().darker().darker(), OUTPUT_COLOR.darker().darker())
						);
						
						final JPanel outputFrame = productionPanel.outputFrame;
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								productionPanel.add(outputFrame);
							}
						});
					}
					
					final Rectangle outputBounds = SwingUtilities.convertRectangle(((JComponent)view).getParent(), ((JComponent)view).getBounds(), productionPanel);
					final JPanel outputFrame = productionPanel.outputFrame;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							outputFrame.setBounds(outputBounds);
						}
					});
				} else {
					if(productionPanel.outputFrame != null) {
						final JPanel outputFrame = productionPanel.outputFrame;
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								productionPanel.remove(outputFrame);
							}
						});
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
			if(selectionFrame != null) {
				if(selectionBoundsBinding != null)
					selectionBoundsBinding.releaseBinding();
				
				this.remove(selectionFrame);
				selectionFrame = null;
			}
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
		private JComponent[] buttonTools;
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
						productionPanel.editPanelMouseAdapter.requestSelect(null, null, branch);
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
			undo.setFont(new Font(undo.getFont().getFontName(), Font.BOLD, BUTTON_FONT_SIZE));
			undo.setBackground(TOP_FOREGROUND_COLOR);
			undo.setForeground(TOP_BACKGROUND_COLOR);
			undo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_UNDO);
					Location location = getTransactionFactory().getModelLocation();
					// Indicate this is an undo context
					getTransactionFactory().undo(propCtx, location);
				}
			});
			undo.setFocusable(false);
			topPanel.add(undo);
			JButton redo = new JButton("Redo");
			redo.setFont(new Font(redo.getFont().getFontName(), Font.BOLD, BUTTON_FONT_SIZE));
			redo.setBackground(TOP_FOREGROUND_COLOR);
			redo.setForeground(TOP_BACKGROUND_COLOR);
			redo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_REDO);
					Location location = getTransactionFactory().getModelLocation();
					// Indicate this is an redo context
					getTransactionFactory().redo(propCtx, location);
				}
			});
			redo.setFocusable(false);
			topPanel.add(redo);
			
			topPanel.add(new JSeparator(JSeparator.VERTICAL));
			topPanel.add(new JSeparator(JSeparator.VERTICAL));

//			Tool[] tools = viewManager.getTools();
//			buttonTools = new JComponent[1 + tools.length];
//			ButtonGroup group = new ButtonGroup();
//			
//			buttonTools[0] = createToolButton(model, transactionFactory, group, -1, this.model.getTool(), STATE_USE, "Use");
//
//			for(int i = 0; i < tools.length; i++) {
//				Tool tool = tools[i];
//				int button = model.getButtonForTool(i);
//				buttonTools[i + 1] = createToolButton(model, transactionFactory, group, button, this.model.getTool(), i + 1, tool.getName());
//			}
//			for(JComponent buttonTool: buttonTools) {
//				JPanel buttonToolWrapper = new JPanel();
//				buttonToolWrapper.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
//				buttonToolWrapper.setLayout(new BorderLayout());
//				buttonToolWrapper.add(buttonTool, BorderLayout.CENTER);
//				topPanel.add(buttonToolWrapper);
//			}
			
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
					if(change instanceof LiveModel.ButtonToolBindingChanged) {
						LiveModel.ButtonToolBindingChanged bindButtonChanged = (LiveModel.ButtonToolBindingChanged)change;
						
//						if(!propCtx.isTagged(TAG_CAUSED_BY_TOGGLE_BUTTON)) {
//							JComponent buttonNewTool = buttonTools[bindButtonChanged.tool];
//							updateToolButton(buttonNewTool, bindButtonChanged.button);
////							buttonNewTool.setSelected(true);
//						}
						
						if(bindButtonChanged.tool != -1) {
							JComponent buttonNewTool = buttonTools[bindButtonChanged.tool];
							updateToolButton(buttonNewTool, bindButtonChanged.button);
						}
						
//						if(previousState == LiveModel.STATE_USE && LivePanel.this.model.getTool() != LiveModel.STATE_USE) {
//							SwingUtilities.invokeLater(new Runnable() {
//								@Override
//								public void run() {
//									contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
//									contentPane.revalidate();
//									contentPane.repaint();
//								}
//							});
//						} else if(previousState != LiveModel.STATE_USE && LivePanel.this.model.getTool() == LiveModel.STATE_USE) {
//							SwingUtilities.invokeLater(new Runnable() {
//								@Override
//								public void run() {
//									contentPane.remove(productionPanel);
//									contentPane.revalidate();
//									contentPane.repaint();
//								}
//							});
//						}
//						
//						previousState = LivePanel.this.model.getTool();
					}
					
					/*if(change instanceof LiveModel.StateChanged) {
						if(!propCtx.isTagged(TAG_CAUSED_BY_TOGGLE_BUTTON)) {
							JComponent buttonNewTool = buttonTools[LivePanel.this.model.getTool()];
//							buttonNewTool.setSelected(true);
						}
						
						if(previousState == LiveModel.STATE_USE && LivePanel.this.model.getTool() != LiveModel.STATE_USE) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
									contentPane.revalidate();
									contentPane.repaint();
								}
							});
						} else if(previousState != LiveModel.STATE_USE && LivePanel.this.model.getTool() == LiveModel.STATE_USE) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									contentPane.remove(productionPanel);
									contentPane.revalidate();
									contentPane.repaint();
								}
							});
						}
						
						previousState = LivePanel.this.model.getTool();
					}*/ else if(change instanceof LiveModel.OutputChanged) {
						if(LivePanel.this.model.output == null) {
							productionPanel.editPanelMouseAdapter.setOutput(null);
						} else {
							ModelLocator locator = LivePanel.this.model.output.getLocator();
							ModelLocation modelLocation = locator.locate();
							Location modelComponentLocation = modelLocation.getModelComponentLocation();
							ModelComponent view = (ModelComponent)modelComponentLocation.getChild(rootView);
							productionPanel.editPanelMouseAdapter.setOutput(view);
						}
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								productionPanel.livePanel.repaint();
							}
						});
					} else if(change instanceof LiveModel.SelectionChanged) {
						if(LivePanel.this.model.selection != null) {
							// TODO: Consider whether this is a safe manner in which location of selection if derived.
							ModelLocator locator = LivePanel.this.model.selection.getLocator();
							ModelLocation modelLocation = locator.locate();
							Location modelComponentLocation = modelLocation.getModelComponentLocation();
							final ModelComponent view = (ModelComponent)modelComponentLocation.getChild(rootView);

							productionPanel.editPanelMouseAdapter.select(view);
							
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									productionPanel.livePanel.repaint();
								}
							});
						} else {
							productionPanel.editPanelMouseAdapter.select(null);
							
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
			
			contentPane.add(productionPanel, JLayeredPane.MODAL_LAYER);
		}
		
		@Override
		public void initialize() {
			Tool[] tools = viewManager.getTools();
			buttonTools = new JComponent[1 + tools.length];
			ButtonGroup group = new ButtonGroup();
			
			buttonTools[0] = createToolButton(model, transactionFactory, group, -1, this.model.getTool(), STATE_USE, "Use");

			for(int i = 0; i < tools.length; i++) {
				Tool tool = tools[i];
				int button = model.getButtonForTool(i + 1);
				buttonTools[i + 1] = createToolButton(model, transactionFactory, group, button, this.model.getTool(), i + 1, tool.getName());
			}
			for(JComponent buttonTool: buttonTools) {
				JPanel buttonToolWrapper = new JPanel();
				buttonToolWrapper.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
				buttonToolWrapper.setLayout(new BorderLayout());
				buttonToolWrapper.add(buttonTool, BorderLayout.CENTER);
				topPanel.add(buttonToolWrapper);
			}
			
			if(LivePanel.this.model.selection != null) {
				ModelComponent selectionView = (ModelComponent)LivePanel.this.model.selection.getLocator().locate().getModelComponentLocation().getChild(rootView);
				LivePanel.this.productionPanel.editPanelMouseAdapter.select(selectionView);
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
				LivePanel livePanel, TransactionMapBuilder transactions, ModelComponent child, PrevaylerServiceBranch<Model> branch) {
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

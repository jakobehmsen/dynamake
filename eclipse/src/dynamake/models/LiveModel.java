package dynamake.models;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.delegates.Action1;
import dynamake.delegates.Action2;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.tools.Tool;
import dynamake.tools.ToolFactory;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.LocalHistoryHandler;
import dynamake.transcription.Trigger;

public class LiveModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class ButtonsToolBindingChanged {
		public final List<InputButton> buttons;
		public final int tool;
		
		public ButtonsToolBindingChanged(List<InputButton> buttons, int tool) {
			this.buttons = buttons;
			this.tool = tool;
		}
	}

	private Model content;
	private Hashtable<List<InputButton>, Integer> buttonsToToolMap = new Hashtable<List<InputButton>, Integer>();
	
	public LiveModel(Model content) {
		this.content = content;
		content.setParent(this);
	}
	
	@Override
	public Model cloneBase() {
		return new LiveModel(content.cloneBase());
	}
	
	public int getToolForButtons(List<InputButton> buttons) {
		Integer tool = buttonsToToolMap.get(buttons);
		return tool != null ? tool : -1;
	}

	public List<InputButton> getButtonsForTool(int tool) {
		for(Map.Entry<List<InputButton>, Integer> entry: buttonsToToolMap.entrySet()) {
			if(entry.getValue() == tool)
				return entry.getKey();
		}
		
		return Collections.emptyList();
	}
	
	public void removeButtonsToToolBinding(List<InputButton> buttons, int tool, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		buttonsToToolMap.remove(buttons);
		sendChanged(new ButtonsToolBindingChanged(Collections.<InputButton>emptyList(), tool), propCtx, propDistance, 0, collector);
	}
	
	public void bindButtonsToTool(List<InputButton> buttons, int tool, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		buttonsToToolMap.put(buttons, tool);
		sendChanged(new ButtonsToolBindingChanged(buttons, tool), propCtx, propDistance, 0, collector);
	}
	
	public static class BindButtonsToToolCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private List<InputButton> buttons;
		private int tool;

		public BindButtonsToToolCommand(List<InputButton> buttons, int tool) {
			this.buttons = buttons;
			this.tool = tool;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			LiveModel liveModel = (LiveModel)location.getChild(prevalentSystem);
			liveModel.bindButtonsToTool(buttons, tool, propCtx, 0, collector);
			
			return null;
		}
	}
	
	public static class RemoveButtonsToToolBindingCommand2 implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private List<InputButton> buttons;
		private int tool;

		public RemoveButtonsToToolBindingCommand2(List<InputButton> buttons, int tool) {
			this.buttons = buttons;
			this.tool = tool;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			LiveModel liveModel = (LiveModel)location.getChild(prevalentSystem);
			liveModel.removeButtonsToToolBinding(buttons, tool, propCtx, 0, collector);
			
			return null;
		}
	}
	
	public static class ContentLocator implements dynamake.models.Locator {
		@Override
		public Location locate() {
			return new FieldContentLocation();
		}
	}
	
	private static class FieldContentLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object getChild(Object holder) {
			return ((LiveModel)holder).content;
		}
		
		@Override
		public Location forForwarding() {
			return this;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof FieldContentLocation;
		}
		
		@Override
		public int hashCode() {
			return 7;
		}
	}
	
	private static final int BUTTON_FONT_SIZE = 13;
	private static final Color TOP_BACKGROUND_COLOR = new Color(90, 90, 90);
	private static final Color TOP_BUTTON_BACKGROUND_COLOR = TOP_BACKGROUND_COLOR;
	private static final Color TOP_FOREGROUND_COLOR = Color.WHITE;
	
	private interface InputListener {
		void pressed(Point point, InputButton inputButton);
		void released(Point point, InputButton inputButton);
		void dragged(Point point);
	}
	
	private interface InputButton extends Comparable<InputButton>, Serializable {
		Color getColor();
	}
	
	private static class MouseButton implements InputButton {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int button;

		public MouseButton(int button) {
			this.button = button;
		}
		
		@Override
		public boolean equals(Object arg0) {
			if(arg0 instanceof MouseButton) {
				MouseButton otherMouseButton = (MouseButton)arg0;
				return this.button == otherMouseButton.button;
			}
			
			return false;
		}
		
		@Override
		public int hashCode() {
			return 1 * button;
		}
		
		@Override
		public String toString() {
			return "" + button;
		}
		
		@Override
		public Color getColor() {
			return Color.BLUE;
		}
		
		@Override
		public int compareTo(InputButton o) {
			if(o instanceof MouseButton)
				return this.button - ((MouseButton)o).button;
			return -1;
		}
	}
	
	private static class KeyboardButton implements InputButton {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int keyCode;

		public KeyboardButton(int keyCode) {
			this.keyCode = keyCode;
		}
		
		@Override
		public boolean equals(Object arg0) {
			if(arg0 instanceof KeyboardButton) {
				KeyboardButton otherKeyboardButton = (KeyboardButton)arg0;
				return this.keyCode == otherKeyboardButton.keyCode;
			}
			
			return false;
		}
		
		@Override
		public int hashCode() {
			return 11 * keyCode;
		}
		
		@Override
		public String toString() {
			return KeyEvent.getKeyText(keyCode);
		}
		
		@Override
		public Color getColor() {
			return Color.GREEN;
		}
		
		@Override
		public int compareTo(InputButton o) {
			if(o instanceof KeyboardButton)
				return this.keyCode - ((KeyboardButton)o).keyCode;
			return 1;
		}
	}
	
	public static class ToolButton extends JPanel implements InputListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int tool;
		private List<InputButton> buttons;
		private String text;
		private LivePanel livePanel;
		private ModelTranscriber modelTranscriber;
		private JLabel labelToolName;
		private JPanel panelButtons;

		private int buttonsDown;
		private ArrayList<InputButton> newButtonCombination = new ArrayList<InputButton>();
		
		public final MouseAdapter mouseAdapter;
		public final KeyAdapter keyAdapter;
		
		public ToolButton(int tool, List<InputButton> buttons, String text, LivePanel liveModel, ModelTranscriber modelTranscriber) {
			this.tool = tool;
			this.buttons = buttons;
			this.text = text;
			this.livePanel = liveModel;
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
			
			mouseAdapter = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int newButton = e.getButton();
					
					buttonsDown++;
					if(!newButtonCombination.contains(new MouseButton(newButton))) {
						newButtonCombination.add(new MouseButton(newButton));
						Collections.sort(newButtonCombination);
					}
					
					if(buttonsDown == 1) {
						setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
					}
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							update(newButtonCombination);
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
						final ArrayList<InputButton> localButtonsPressed = (ArrayList<InputButton>)newButtonCombination.clone();
						
						Connection<Model> connection = ToolButton.this.modelTranscriber.createConnection();
						
						connection.trigger(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
								
								List<InputButton> currentButtons = ToolButton.this.buttons;
								
								if(localButtonsPressed.equals(currentButtons)) {
									// If the indicated combination is the same as the current combination, then remove
									// the current binding
									pendingCommands.add(new PendingCommandState<Model>(
										new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool),
										new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool)
									));
								} else {
									int previousToolForNewButton = ToolButton.this.livePanel.model.getToolForButtons(localButtonsPressed);
									
									if(previousToolForNewButton != -1) {
										// If the new buttons are associated to another tool, then remove that binding
										pendingCommands.add(new PendingCommandState<Model>(
											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, previousToolForNewButton), 
											new BindButtonsToToolCommand(localButtonsPressed, previousToolForNewButton))
										);
									}
									
									if(currentButtons.size() > 0) {
										// If this tool is associated to buttons, then remove that binding before
										pendingCommands.add(new PendingCommandState<Model>(
											new RemoveButtonsToToolBindingCommand2(currentButtons, ToolButton.this.tool), 
											new BindButtonsToToolCommand(currentButtons, ToolButton.this.tool))
										);
										
										// adding the replacement binding
										pendingCommands.add(new PendingCommandState<Model>(
											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool))
										);
									} else {
										pendingCommands.add(new PendingCommandState<Model>(
											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool)
										));
									}
								}
								
								PendingCommandFactory.Util.sequence(collector, ToolButton.this.livePanel.model, pendingCommands, LocalHistoryHandler.class);
								collector.commit();
							}
						});
						
						newButtonCombination.clear();
					}
				}
			};
			
			keyAdapter = new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if(ToolButton.this.livePanel.keysDown.contains(e.getKeyCode()))
						return;
					
					ToolButton.this.livePanel.keysDown.add(e.getKeyCode());
					
					if(!newButtonCombination.contains(new KeyboardButton(e.getKeyCode()))) {
						buttonsDown++;
						
						newButtonCombination.add(new KeyboardButton(e.getKeyCode()));
						Collections.sort(newButtonCombination);
						
						if(buttonsDown == 1) {
							setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
						}
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								update(newButtonCombination);
								ToolButton.this.repaint();
							}
						});

//						System.out.println("Pressed " + e.getKeyChar());
						System.out.println("Current buttons pressed " + keyPressedAsString());
					}
				}
				
				@Override
				public void keyReleased(KeyEvent e) {
					ToolButton.this.livePanel.keysDown.remove(e.getKeyCode());
//					System.out.println("Released " + e.getKeyChar());
					
//					newButtonCombination.remove(new KeyboardButton(e.getKeyCode()));
//					
//					if(newButtonCombination.size() == 0) {
//						System.out.println("New button combination " + keyPressedAsString());
//						newButtonCombination.clear();
//					}
					
					buttonsDown--;
					
					if(buttonsDown == 0) {
						setBackground(TOP_BUTTON_BACKGROUND_COLOR);
						
						@SuppressWarnings("unchecked")
						final ArrayList<InputButton> localButtonsPressed = (ArrayList<InputButton>)newButtonCombination.clone();
						
						Connection<Model> connection = ToolButton.this.modelTranscriber.createConnection();
						
						connection.trigger(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
								
								List<InputButton> currentButtons = ToolButton.this.buttons;
								
								if(localButtonsPressed.equals(currentButtons)) {
									// If the indicated combination is the same as the current combination, then remove
									// the current binding
									pendingCommands.add(new PendingCommandState<Model>(
										new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool),
										new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool)
									));
								} else {
									int previousToolForNewButton = ToolButton.this.livePanel.model.getToolForButtons(localButtonsPressed);
									
									if(previousToolForNewButton != -1) {
										// If the new buttons are associated to another tool, then remove that binding
										pendingCommands.add(new PendingCommandState<Model>(
											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, previousToolForNewButton), 
											new BindButtonsToToolCommand(localButtonsPressed, previousToolForNewButton))
										);
									}
									
									if(currentButtons.size() > 0) {
										// If this tool is associated to buttons, then remove that binding before
										pendingCommands.add(new PendingCommandState<Model>(
											new RemoveButtonsToToolBindingCommand2(currentButtons, ToolButton.this.tool), 
											new BindButtonsToToolCommand(currentButtons, ToolButton.this.tool))
										);
										
										// adding the replacement binding
										pendingCommands.add(new PendingCommandState<Model>(
											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool))
										);
									} else {
										pendingCommands.add(new PendingCommandState<Model>(
											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool)
										));
									}
								}
								
								PendingCommandFactory.Util.sequence(collector, ToolButton.this.livePanel.model, pendingCommands, LocalHistoryHandler.class);
								collector.commit();
							}
						});
						
						newButtonCombination.clear();
					}
				}
				
				private String keyPressedAsString() {
					String allKeys = "";
					for(Object keyPressed: newButtonCombination) {
						allKeys += keyPressed + " ";
					}
					return allKeys;
				}
			};
			
//			this.addMouseListener(new MouseAdapter() {
////				int buttonsDown = 0;
////				ArrayList<Integer> buttonsPressed = new ArrayList<Integer>();
//				
//				@Override
//				public void mouseEntered(MouseEvent e) {
////					requestFocusInWindow();
//				}
//				
//				@Override
//				public void mouseExited(MouseEvent e) {
//					// TODO: End current binding action, if any
////					getParent().requestFocusInWindow();
//				}
//				
//				@Override
//				public void mousePressed(MouseEvent e) {
//					int newButton = e.getButton();
//					
//					buttonsDown++;
//					if(!newButtonCombination.contains(new MouseButton(newButton))) {
//						newButtonCombination.add(new MouseButton(newButton));
//						Collections.sort(newButtonCombination);
//					}
//					
//					if(buttonsDown == 1) {
//						setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
//					}
//					
//					SwingUtilities.invokeLater(new Runnable() {
//						@Override
//						public void run() {
//							update(newButtonCombination);
//							ToolButton.this.repaint();
//						}
//					});
//				}
//				
//				@Override
//				public void mouseReleased(MouseEvent e) {
//					buttonsDown--;
//					
//					if(buttonsDown == 0) {
//						setBackground(TOP_BUTTON_BACKGROUND_COLOR);
//						
//						@SuppressWarnings("unchecked")
//						final ArrayList<InputButton> localButtonsPressed = (ArrayList<InputButton>)newButtonCombination.clone();
//						
//						Connection<Model> connection = ToolButton.this.modelTranscriber.createConnection();
//						
//						connection.trigger(new Trigger<Model>() {
//							@Override
//							public void run(Collector<Model> collector) {
//								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
//								
//								List<InputButton> currentButtons = ToolButton.this.buttons;
//								
//								if(localButtonsPressed.equals(currentButtons)) {
//									// If the indicated combination is the same as the current combination, then remove
//									// the current binding
//									pendingCommands.add(new PendingCommandState<Model>(
//										new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool),
//										new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool)
//									));
//								} else {
//									int previousToolForNewButton = ToolButton.this.livePanel.model.getToolForButtons(localButtonsPressed);
//									
//									if(previousToolForNewButton != -1) {
//										// If the new buttons are associated to another tool, then remove that binding
//										pendingCommands.add(new PendingCommandState<Model>(
//											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, previousToolForNewButton), 
//											new BindButtonsToToolCommand(localButtonsPressed, previousToolForNewButton))
//										);
//									}
//									
//									if(currentButtons.size() > 0) {
//										// If this tool is associated to buttons, then remove that binding before
//										pendingCommands.add(new PendingCommandState<Model>(
//											new RemoveButtonsToToolBindingCommand2(currentButtons, ToolButton.this.tool), 
//											new BindButtonsToToolCommand(currentButtons, ToolButton.this.tool))
//										);
//										
//										// adding the replacement binding
//										pendingCommands.add(new PendingCommandState<Model>(
//											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
//											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool))
//										);
//									} else {
//										pendingCommands.add(new PendingCommandState<Model>(
//											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
//											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool)
//										));
//									}
//								}
//								
//								PendingCommandFactory.Util.sequence(collector, ToolButton.this.livePanel.model, pendingCommands, LocalHistoryHandler.class);
//								collector.commit();
//							}
//						});
//						
//						newButtonCombination.clear();
//					}
//				}
//			});
			
			// Support for binding a key combination with a tool
			// It should be possible to both bind a key combination AND a mouse button to the same tool at the same time
//			KeyListener keyListener = new KeyAdapter() {
//				@Override
//				public void keyPressed(KeyEvent e) {
//					if(ToolButton.this.livePanel.keysDown.contains(e.getKeyCode()))
//						return;
//					
//					ToolButton.this.livePanel.keysDown.add(e.getKeyCode());
//					
//					if(!newButtonCombination.contains(new KeyboardButton(e.getKeyCode()))) {
//						buttonsDown++;
//						
//						newButtonCombination.add(new KeyboardButton(e.getKeyCode()));
//						Collections.sort(newButtonCombination);
//						
//						if(buttonsDown == 1) {
//							setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
//						}
//						
//						SwingUtilities.invokeLater(new Runnable() {
//							@Override
//							public void run() {
//								update(newButtonCombination);
//								ToolButton.this.repaint();
//							}
//						});
//
////						System.out.println("Pressed " + e.getKeyChar());
//						System.out.println("Current buttons pressed " + keyPressedAsString());
//					}
//				}
//				
//				@Override
//				public void keyReleased(KeyEvent e) {
//					ToolButton.this.livePanel.keysDown.remove(e.getKeyCode());
////					System.out.println("Released " + e.getKeyChar());
//					
////					newButtonCombination.remove(new KeyboardButton(e.getKeyCode()));
////					
////					if(newButtonCombination.size() == 0) {
////						System.out.println("New button combination " + keyPressedAsString());
////						newButtonCombination.clear();
////					}
//					
//					buttonsDown--;
//					
//					if(buttonsDown == 0) {
//						setBackground(TOP_BUTTON_BACKGROUND_COLOR);
//						
//						@SuppressWarnings("unchecked")
//						final ArrayList<InputButton> localButtonsPressed = (ArrayList<InputButton>)newButtonCombination.clone();
//						
//						Connection<Model> connection = ToolButton.this.modelTranscriber.createConnection();
//						
//						connection.trigger(new Trigger<Model>() {
//							@Override
//							public void run(Collector<Model> collector) {
//								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
//								
//								List<InputButton> currentButtons = ToolButton.this.buttons;
//								
//								if(localButtonsPressed.equals(currentButtons)) {
//									// If the indicated combination is the same as the current combination, then remove
//									// the current binding
//									pendingCommands.add(new PendingCommandState<Model>(
//										new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool),
//										new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool)
//									));
//								} else {
//									int previousToolForNewButton = ToolButton.this.livePanel.model.getToolForButtons(localButtonsPressed);
//									
//									if(previousToolForNewButton != -1) {
//										// If the new buttons are associated to another tool, then remove that binding
//										pendingCommands.add(new PendingCommandState<Model>(
//											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, previousToolForNewButton), 
//											new BindButtonsToToolCommand(localButtonsPressed, previousToolForNewButton))
//										);
//									}
//									
//									if(currentButtons.size() > 0) {
//										// If this tool is associated to buttons, then remove that binding before
//										pendingCommands.add(new PendingCommandState<Model>(
//											new RemoveButtonsToToolBindingCommand2(currentButtons, ToolButton.this.tool), 
//											new BindButtonsToToolCommand(currentButtons, ToolButton.this.tool))
//										);
//										
//										// adding the replacement binding
//										pendingCommands.add(new PendingCommandState<Model>(
//											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
//											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool))
//										);
//									} else {
//										pendingCommands.add(new PendingCommandState<Model>(
//											new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
//											new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool)
//										));
//									}
//								}
//								
//								PendingCommandFactory.Util.sequence(collector, ToolButton.this.livePanel.model, pendingCommands, LocalHistoryHandler.class);
//								collector.commit();
//							}
//						});
//						
//						newButtonCombination.clear();
//					}
//				}
//				
//				private String keyPressedAsString() {
//					String allKeys = "";
//					for(Object keyPressed: newButtonCombination) {
//						allKeys += keyPressed + " ";
//					}
//					return allKeys;
//				}
//			};
			
//			this.addKeyListener(keyListener);
//			labelToolName.addKeyListener(keyListener);
//			labelButton.addKeyListener(keyListener);
			
			setFocusable(true);
		}
		
		private static final Color[] BUTTON_COLORS = new Color[] {
			new Color(255, 120, 10),//Color.RED,
			new Color(10, 220, 10), //Color.GREEN,
			new Color(10, 10, 220), //Color.BLUE,
			new Color(10, 220, 220), //Color.CYAN,
			new Color(220, 220, 10), //Color.ORANGE
			new Color(220, 10, 220),
		};
		
		public static Color avgColorOfButtons(List<InputButton> buttons) {
			ArrayList<Color> buttonColors = new ArrayList<Color>();
			for(InputButton buttonPressed: buttons)
				buttonColors.add(buttonPressed.getColor());
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
		
		private void update(List<InputButton> buttons) {
			labelToolName.setText(text);
			
			Border innerBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
			Border outerBorder;
			
			panelButtons.removeAll();
			
			if(buttons.size() > 0) {
				for(InputButton button: buttons) {
					JLabel buttonLabel = new JLabel();
					buttonLabel.setHorizontalAlignment(SwingConstants.CENTER);
					buttonLabel.setForeground(button.getColor());
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
		
		public void setButtons(List<InputButton> buttons) {
			this.buttons = buttons;
			update();
		}

		public void showAsActive() {
			setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
		}

		public void showAsPassive() {
			setBackground(TOP_BUTTON_BACKGROUND_COLOR);
		}

		@Override
		public void pressed(Point point, InputButton inputButton) {
			buttonsDown++;
			if(!newButtonCombination.contains(inputButton)) {
				newButtonCombination.add(inputButton);
				Collections.sort(newButtonCombination);
			}
			
			if(buttonsDown == 1) {
				setBackground(TOP_BUTTON_BACKGROUND_COLOR.brighter());
			}
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					update(newButtonCombination);
					ToolButton.this.repaint();
				}
			});
		}

		@Override
		public void released(Point point, InputButton inputButton) {
			buttonsDown--;
			
			if(buttonsDown == 0) {
				setBackground(TOP_BUTTON_BACKGROUND_COLOR);
				
				@SuppressWarnings("unchecked")
				final ArrayList<InputButton> localButtonsPressed = (ArrayList<InputButton>)newButtonCombination.clone();
				
				Connection<Model> connection = ToolButton.this.modelTranscriber.createConnection();
				
				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
						
						List<InputButton> currentButtons = ToolButton.this.buttons;
						
						if(localButtonsPressed.equals(currentButtons)) {
							// If the indicated combination is the same as the current combination, then remove
							// the current binding
							pendingCommands.add(new PendingCommandState<Model>(
								new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool),
								new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool)
							));
						} else {
							int previousToolForNewButton = ToolButton.this.livePanel.model.getToolForButtons(localButtonsPressed);
							
							if(previousToolForNewButton != -1) {
								// If the new buttons are associated to another tool, then remove that binding
								pendingCommands.add(new PendingCommandState<Model>(
									new RemoveButtonsToToolBindingCommand2(localButtonsPressed, previousToolForNewButton), 
									new BindButtonsToToolCommand(localButtonsPressed, previousToolForNewButton))
								);
							}
							
							if(currentButtons.size() > 0) {
								// If this tool is associated to buttons, then remove that binding before
								pendingCommands.add(new PendingCommandState<Model>(
									new RemoveButtonsToToolBindingCommand2(currentButtons, ToolButton.this.tool), 
									new BindButtonsToToolCommand(currentButtons, ToolButton.this.tool))
								);
								
								// adding the replacement binding
								pendingCommands.add(new PendingCommandState<Model>(
									new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
									new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool))
								);
							} else {
								pendingCommands.add(new PendingCommandState<Model>(
									new BindButtonsToToolCommand(localButtonsPressed, ToolButton.this.tool), 
									new RemoveButtonsToToolBindingCommand2(localButtonsPressed, ToolButton.this.tool)
								));
							}
						}
						
						PendingCommandFactory.Util.sequence(collector, ToolButton.this.livePanel.model, pendingCommands, LocalHistoryHandler.class);
						collector.commit();
					}
				});
				
				newButtonCombination.clear();
			}
		}

		@Override
		public void dragged(Point point) { }
	}
	
	private static ToolButton createToolButton(final LivePanel livePanel, final ModelTranscriber modelTranscriber, ButtonGroup group, List<InputButton> buttons, final int tool, final String text) {
		return new ToolButton(tool, buttons, text, livePanel, modelTranscriber);
	}
	
	private static void updateToolButton(JComponent toolButton, List<InputButton> buttons) {
		((ToolButton)toolButton).setButtons(buttons);
	}

	public static class ProductionPanel extends JPanel implements InputListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final Color TARGET_OVER_COLOR = new Color(35, 89, 184);
		public static final Color BIND_COLOR = new Color(25, 209, 89);
		public static final Color UNBIND_COLOR = new Color(240, 34, 54);
		public static final Color SELECTION_COLOR = Color.GRAY;
		
		public static class EditPanelInputAdapter extends MouseAdapter implements KeyListener, InputListener {
			public ProductionPanel productionPanel;

			public int buttonPressed;
			
			public static final int HORIZONTAL_REGION_WEST = 0;
			public static final int HORIZONTAL_REGION_CENTER = 1;
			public static final int HORIZONTAL_REGION_EAST = 2;
			public static final int VERTICAL_REGION_NORTH = 0;
			public static final int VERTICAL_REGION_CENTER = 1;
			public static final int VERTICAL_REGION_SOUTH = 2;
			
			public EditPanelInputAdapter(ProductionPanel productionPanel) {
				this.productionPanel = productionPanel;
				toolConnection = productionPanel.livePanel.getModelTranscriber().createConnection();
			}
			
			private Tool createToolForApplication(List<InputButton> buttons) {
				int toolForButton = productionPanel.livePanel.model.getToolForButtons(buttons);
				if(toolForButton != -1) {
					return productionPanel.livePanel.viewManager.getToolFactories()[toolForButton].createTool();
				} else {
					return new Tool() {
						@Override
						public void mouseReleased(
								ProductionPanel productionPanel,
								ModelComponent modelOver,
								Connection<Model> connection,
								Collector<Model> collector,
								JComponent sourceComponent, Point mousePoint) {

						}

						@Override
						public void mousePressed(
								ProductionPanel productionPanel,
								ModelComponent modelOver,
								Connection<Model> connection,
								Collector<Model> collector,
								JComponent sourceComponent, Point mousePoint) {

						}

						@Override
						public void mouseDragged(
								ProductionPanel productionPanel,
								ModelComponent modelOver,
								Collector<Model> collector,
								Connection<Model> connection,
								JComponent sourceComponent, Point mousePoint) {

						}

						@Override
						public void rollback(ProductionPanel productionPanel,
								Collector<Model> collector) {
						}
					};
				}
			}
			
			private ToolButton getToolButton(List<InputButton> buttons) {
				int toolForButton = productionPanel.livePanel.model.getToolForButtons(buttons);
				if(toolForButton != -1) {
					return productionPanel.livePanel.buttonTools[toolForButton];
				} else {
					return null;
				}
			}
			
			public ModelComponent getModelOver(MouseEvent e) {
				return getModelOver((JComponent)e.getSource(), e.getPoint());
			}
			
			public ModelComponent getModelOver(JComponent source, Point point) {
				Point pointInContentView = SwingUtilities.convertPoint(source, point, (JComponent)productionPanel.contentView.getBindingTarget());
				JComponent componentOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
				
				if(componentOver == null)
					return productionPanel.contentView.getBindingTarget();

				return ModelComponent.Util.closestModelComponent(componentOver);
			}
			
			private Tool toolBeingApplied;
			private int buttonsDown;
			public ArrayList<InputButton> buttonsPressed = new ArrayList<InputButton>();
			
			private Connection<Model> toolConnection;
			
			private void pressedButton(final InputButton button, final JComponent source, final Point point) {
				// Check whether there is an active tool which must be rolled back
				// because a new combination of button is recognized
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
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
						
						if(!buttonsPressed.contains(button)) {
							buttonsPressed.add(button);
							Collections.sort(buttonsPressed);
							toolBeingApplied = createToolForApplication(buttonsPressed);
							
							final ModelComponent modelOver = getModelOver(source, point);
							final Tool toolToApply = toolBeingApplied;
							
							toolToApply.mousePressed(productionPanel, modelOver, toolConnection, collector, source, point);
							
							ToolButton toolButton = getToolButton(buttonsPressed);
							if(toolButton != null)
								toolButton.showAsActive();
						}
					}
				});
			}
			
			public void mousePressed(final MouseEvent e) {
				InputButton inputButton;
				if(pendingPressedKeyboardButton != null) {
					inputButton = pendingPressedKeyboardButton;
					pendingPressedKeyboardButton = null;
				} else {
					inputButton = new MouseButton(e.getButton());
				}
				pressedButton(inputButton, (JComponent)e.getSource(), e.getPoint());
				
//				// Check whether there is an active tool which must be rolled back
//				// because a new combination of button is recognized
//				toolConnection.trigger(new Trigger<Model>() {
//					@Override
//					public void run(Collector<Model> collector) {
//						int button = e.getButton();
//						
//						if(!buttonsPressed.contains(button) && buttonsPressed.size() > 0) {
//							final Tool toolToRollback = toolBeingApplied;
//							
//							toolToRollback.rollback(productionPanel, collector);
//							collector.reject();
//							
//							ToolButton toolButton = getToolButton(buttonsPressed);
//							if(toolButton != null)
//								toolButton.showAsPassive();
//						}
//					}
//				});
//				
//				toolConnection.trigger(new Trigger<Model>() {
//					@Override
//					public void run(Collector<Model> collector) {
//						buttonsDown++;
//						
//						int button = e.getButton();
//						
//						if(!buttonsPressed.contains(new MouseButton(button))) {
//							buttonsPressed.add(new MouseButton(button));
//							Collections.sort(buttonsPressed);
//							toolBeingApplied = createToolForApplication(buttonsPressed);
//							
//							final ModelComponent modelOver = getModelOver(e);
//							final Tool toolToApply = toolBeingApplied;
//							
//							toolToApply.mousePressed(productionPanel, modelOver, toolConnection, collector, (JComponent)e.getSource(), e.getPoint());
//							
//							ToolButton toolButton = getToolButton(buttonsPressed);
//							if(toolButton != null)
//								toolButton.showAsActive();
//						}
//					}
//				});
			}

			public void mouseDragged(final MouseEvent e) {
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						final ModelComponent modelOver = getModelOver(e);

						if(modelOver != null) {
							final Tool toolToApply = toolBeingApplied;
							toolToApply.mouseDragged(productionPanel, modelOver, collector, toolConnection, (JComponent)e.getSource(), e.getPoint());
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
								toolToApply.mouseReleased(productionPanel, modelOver, toolConnection, collector, (JComponent)e.getSource(), e.getPoint());
								
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
//				System.out.println("Moved");
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						if(buttonsDown > 0) {
							final ModelComponent modelOver = getModelOver(e);

							if(modelOver != null) {
								final Tool toolToApply = toolBeingApplied;
								toolToApply.mouseDragged(productionPanel, modelOver, collector, toolConnection, (JComponent)e.getSource(), e.getPoint());
							}
						}
					}
				});
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
//				productionPanel.requestFocusInWindow();
			}
			
			private KeyboardButton pendingPressedKeyboardButton;
			
			@Override
			public void keyPressed(KeyEvent e) {
				if(!productionPanel.livePanel.keysDown.contains(e.getKeyCode())) {
					productionPanel.livePanel.keysDown.add(e.getKeyCode());
					
					try {
						pendingPressedKeyboardButton = new KeyboardButton(e.getKeyCode());
						
//						final Point mousePoint = MouseInfo.getPointerInfo().getLocation();
//						SwingUtilities.convertPointFromScreen(mousePoint, productionPanel);
//						MouseEvent mouseEvent = new MouseEvent((Component)e.getSource(), MouseEvent.MOUSE_PRESSED, e.getWhen(), 0, mousePoint.x, mousePoint.y, 1, false);
//						Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(mouseEvent);
						
						Robot robot = new Robot();
						robot.mousePress(InputEvent.BUTTON1_MASK);
					} catch (AWTException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
//					
//					Point mousePoint = MouseInfo.getPointerInfo().getLocation();
//					SwingUtilities.convertPointFromScreen(mousePoint, productionPanel);
//					pressedButton(new KeyboardButton(e.getKeyCode()), (JComponent)e.getSource(), mousePoint);
				}
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
//				productionPanel.livePanel.keysDown.remove(e.getKeyCode());
				
				try {
//					final Point mousePoint = MouseInfo.getPointerInfo().getLocation();
//					SwingUtilities.convertPointFromScreen(mousePoint, productionPanel);
//					MouseEvent mouseEvent = new MouseEvent((Component)e.getSource(), MouseEvent.MOUSE_RELEASED, e.getWhen(), 0, mousePoint.x, mousePoint.y, 1, false);
//					Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(mouseEvent);
					
					Robot robot = new Robot();
					robot.mouseRelease(InputEvent.BUTTON1_MASK);
				} catch (AWTException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
//				final Point mousePoint = MouseInfo.getPointerInfo().getLocation();
//				SwingUtilities.convertPointFromScreen(mousePoint, productionPanel);
//				
//				toolConnection.trigger(new Trigger<Model>() {
//					@Override
//					public void run(Collector<Model> collector) {
//						buttonsDown--;
//
//						final ModelComponent modelOver = getModelOver((JComponent)e.getSource(), mousePoint);
//
//						if(modelOver != null) {
//							if(buttonsDown == 0) {
//								final Tool toolToApply = toolBeingApplied;
//								toolToApply.mouseReleased(productionPanel, modelOver, toolConnection, collector, (JComponent)e.getSource(), mousePoint);
//								
//								ToolButton toolButton = getToolButton(buttonsPressed);
//								if(toolButton != null)
//									toolButton.showAsPassive();
//								
//								buttonsPressed.clear();
//								toolBeingApplied = null;
//							}
//						}
//					}
//				});
			}
			
			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void pressed(final Point point, final InputButton inputButton) {
				// Check whether there is an active tool which must be rolled back
				// because a new combination of button is recognized
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						if(!buttonsPressed.contains(inputButton) && buttonsPressed.size() > 0) {
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
						
						if(!buttonsPressed.contains(inputButton)) {
							buttonsPressed.add(inputButton);
							Collections.sort(buttonsPressed);
							toolBeingApplied = createToolForApplication(buttonsPressed);
							
							final ModelComponent modelOver = getModelOver(productionPanel, point);
							final Tool toolToApply = toolBeingApplied;
							
							toolToApply.mousePressed(productionPanel, modelOver, toolConnection, collector, productionPanel, point);
							
							ToolButton toolButton = getToolButton(buttonsPressed);
							if(toolButton != null)
								toolButton.showAsActive();
						}
					}
				});
			}

			@Override
			public void released(final Point point, InputButton inputButton) {
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						buttonsDown--;
						
						final ModelComponent modelOver = getModelOver(productionPanel, point);

						if(modelOver != null) {
							if(buttonsDown == 0) {
								final Tool toolToApply = toolBeingApplied;
								toolToApply.mouseReleased(productionPanel, modelOver, toolConnection, collector, productionPanel, point);
								
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
			public void dragged(final Point point) {
				toolConnection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						final ModelComponent modelOver = getModelOver(productionPanel, point);

						if(modelOver != null) {
							final Tool toolToApply = toolBeingApplied;
							toolToApply.mouseDragged(productionPanel, modelOver, collector, toolConnection, productionPanel, point);
						}
					}
				});
			}
		}
		
		public LivePanel livePanel;
		public Binding<ModelComponent> contentView;
		public EditPanelInputAdapter editPanelInputAdapter;
		
		public ProductionPanel(final LivePanel livePanel, final Binding<ModelComponent> contentView) {
			this.setLayout(null);
			this.livePanel = livePanel;
			this.contentView = contentView;
			
			// TODO: Consider the following:
			// For a selected frame, it should be possible to scroll upwards to select its immediate parent
			// - and scroll downwards to select its root parents
			editPanelInputAdapter = new EditPanelInputAdapter(this);

//			this.addMouseListener(editPanelInputAdapter);
//			this.addMouseMotionListener(editPanelInputAdapter);
//			this.addKeyListener(editPanelInputAdapter);
			
			this.setOpaque(true);
			this.setBackground(new Color(0, 0, 0, 0));
		}

		@Override
		public void pressed(Point point, InputButton inputButton) {
			editPanelInputAdapter.pressed(point, inputButton);
		}

		@Override
		public void released(Point point, InputButton inputButton) {
			editPanelInputAdapter.released(point, inputButton);
		}

		@Override
		public void dragged(Point point) {
			editPanelInputAdapter.dragged(point);
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
		private InputAdapter inputAdapter;
		
		private HashSet<Integer> keysDown = new HashSet<Integer>();
		
		private static class InputAdapter implements MouseListener, MouseMotionListener, KeyListener {
			private LivePanel livePanel;
			
			public InputAdapter(LivePanel livePanel) {
				this.livePanel = livePanel;
			}
			
			private void sendInputEvent(Point point, Component component, final Action2<Point, InputListener> inputEventSender) {
				Point newPoint = SwingUtilities.convertPoint(livePanel, point, component);
				inputEventSender.run(newPoint, (InputListener)component);
			}
			
//			private void translateMouseEvent(MouseEvent e, Component component) {
//				Point newPoint = SwingUtilities.convertPoint((Component)e.getSource(), e.getPoint(), component);
//				e.translatePoint(newPoint.x - e.getX(), newPoint.y - e.getY());
//			}
			
			private void getComponent(Point point, Action1<Component> componentAction) {
				if(livePanel.topPanel.getBounds().contains(point)) {
					Component toolPanelComponent = livePanel.topPanel.getComponentAt(point);
					if(toolPanelComponent instanceof ToolButton) {
						ToolButton toolButton = (ToolButton)toolPanelComponent;
						
						componentAction.run(toolButton);
					}
				} else if(livePanel.contentPane.getBounds().contains(point)) {
					componentAction.run(livePanel.productionPanel);
				}
			}
			
			private void inputPressed(final Point point, final InputButton inputButton) {
				if(inputButtonsPressedCount == 0) {
					getComponent(point, new Action1<Component>() {
						@Override
						public void run(Component arg0) {
							Point newPoint = SwingUtilities.convertPoint(livePanel, point, arg0);
							
							pressedOnComponent = (InputListener)arg0;
							pressedOnComponent.pressed(newPoint, inputButton);
						}
					});
				} else {
					Point newPoint = SwingUtilities.convertPoint(livePanel, point, (Component)pressedOnComponent);
					
					pressedOnComponent.pressed(newPoint, inputButton);
				}
				
				inputButtonsPressedCount++;
			}
			
			private void inputReleased(final Point point, final InputButton inputButton) {
				Point newPoint = SwingUtilities.convertPoint(livePanel, point, (Component)pressedOnComponent);
				pressedOnComponent.released(newPoint, inputButton);
				
				inputButtonsPressedCount--;
				
				if(inputButtonsPressedCount == 0)
					pressedOnComponent = null;
			}
			
			private InputListener pressedOnComponent;
			private int inputButtonsPressedCount;
			private HashSet<Integer> keysDown = new HashSet<Integer>();

			@Override
			public void mousePressed(final MouseEvent e) {
				inputPressed(e.getPoint(), new MouseButton(e.getButton()));
			}
			
			public void mouseReleased(MouseEvent e) {
				inputReleased(e.getPoint(), new MouseButton(e.getButton()));
			}

			@Override
			public void mouseMoved(MouseEvent e) { 
				if(inputButtonsPressedCount > 0) {
					System.out.println("dragged on live panel");
					Point newPoint = SwingUtilities.convertPoint(livePanel, e.getPoint(), (Component)pressedOnComponent);
					pressedOnComponent.dragged(newPoint);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				System.out.println("dragged on live panel");
				Point newPoint = SwingUtilities.convertPoint(livePanel, e.getPoint(), (Component)pressedOnComponent);
				pressedOnComponent.dragged(newPoint);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) { }
			
			@Override
			public void mouseEntered(MouseEvent e) { }
			
			@Override
			public void mouseExited(MouseEvent e) { }
			
			@Override
			public void keyPressed(KeyEvent e) {
				if(!keysDown.contains(e.getKeyCode())) {
					keysDown.add(e.getKeyCode());
					
					Point point = MouseInfo.getPointerInfo().getLocation();
					SwingUtilities.convertPointFromScreen(point, livePanel);
					inputPressed(point, new KeyboardButton(e.getKeyCode()));
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				keysDown.remove(e.getKeyCode());

				Point point = MouseInfo.getPointerInfo().getLocation();
				SwingUtilities.convertPointFromScreen(point, livePanel);
				inputReleased(point, new KeyboardButton(e.getKeyCode()));
			}
			
			@Override
			public void keyTyped(KeyEvent e) { }
		}
		
		public LivePanel(final ModelComponent rootView, LiveModel model, ModelTranscriber modelTranscriber, final ViewManager viewManager) {
			modelTranscriber.setComponentToRepaint(this);
			this.setLayout(new BorderLayout());
			this.model = model;
			this.viewManager = viewManager;
			this.modelTranscriber = modelTranscriber;
			this.setFocusable(true);

			contentView = model.getContent().createView(rootView, viewManager, modelTranscriber.extend(new ContentLocator()));

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
			
			inputAdapter = new InputAdapter(this);
			
			this.addMouseListener(inputAdapter);
			this.addMouseMotionListener(inputAdapter);
			this.addKeyListener(inputAdapter);
		}
		
		@Override
		public void initialize() {
			ToolFactory[] toolFactories = viewManager.getToolFactories();
			buttonTools = new ToolButton[toolFactories.length];
			ButtonGroup group = new ButtonGroup();

			for(int i = 0; i < toolFactories.length; i++) {
				ToolFactory toolFactory = toolFactories[i];
				List<InputButton> buttons = model.getButtonsForTool(i);
				buttonTools[i] = createToolButton(this, modelTranscriber, group, buttons, i, toolFactory.getName());
			}
			
			for(JComponent buttonTool: buttonTools) {
				topPanel.add(buttonTool);
			}
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

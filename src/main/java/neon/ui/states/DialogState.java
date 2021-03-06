/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
 * 
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.ui.states;

import neon.core.*;
import neon.entities.Creature;
import neon.entities.Player;
import neon.entities.components.HealthComponent;
import neon.magic.MagicUtils;
import neon.resources.CClient;
import neon.resources.RPerson;
import neon.resources.RSpell.SpellType;
import neon.resources.quest.Topic;
import neon.ui.UserInterface;
import neon.ui.dialog.ChargeDialog;
import neon.ui.dialog.CrafterDialog;
import neon.ui.dialog.EnchantDialog;
import neon.ui.dialog.PotionDialog;
import neon.ui.dialog.RepairDialog;
import neon.ui.dialog.SpellMakerDialog;
import neon.ui.dialog.SpellTradeDialog;
import neon.ui.dialog.TattooDialog;
import neon.ui.dialog.TradeDialog;
import neon.ui.dialog.TrainingDialog;
import neon.ui.dialog.TravelDialog;
import neon.util.fsm.State;
import neon.util.fsm.TransitionEvent;
import net.engio.mbassy.bus.MBassador;

import java.util.EventObject;
import java.util.Vector;

import org.jdom2.Element;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import java.awt.event.*;
import java.awt.*;
import java.io.IOException;

/*
 * Klasse die een lijst met onderwerpen toont om te praten. De getoonde lijst hangt
 * af van de precondities die in quests staan. 
 */
public class DialogState extends State implements KeyListener {
	private static UIDefaults defaults = UIManager.getLookAndFeelDefaults();

	private JPanel panel, conversation;
	private Creature target;
	private JTextPane text = new JTextPane();
	private JList<Topic> subjects;
	private JList<String> services;
	private JList<?> list;
	private JScrollPane left;
    private HTMLEditorKit kit = new HTMLEditorKit();
    private HTMLDocument doc;
    private String big, small;
    private MBassador<EventObject> bus;
    private UserInterface ui;
    private Topic topic;
	
	public DialogState(State parent, MBassador<EventObject> bus, UserInterface ui) {
		super(parent);
		this.bus = bus;
		this.ui = ui;
		CClient ini = (CClient)Engine.getResources().getResource("client", "config");
		big = ini.getSmall();
		small = ini.getBig();
		panel = new JPanel(new BorderLayout());

		// instructies
    	JLabel instructions = new JLabel();
    	instructions.setBorder(new CompoundBorder(new TitledBorder("Instructions"), new EmptyBorder(0,5,10,5)));
		instructions.setText("Use arrow keys and press enter to select subject or service. " +
				"Press space to switch between subjects and services, esc to quit.");
		panel.add(instructions, BorderLayout.PAGE_END);
    	
		// textpane
		text.setBackground(defaults.getColor("Label.background"));
		Color foreground = (defaults.getColor("Label.foreground"));
        text.setEditorKit(kit);
        doc = (HTMLDocument)kit.createDefaultDocument();
        text.setDocument(doc);
		text.setFocusable(false);
		text.setBorder(new TitledBorder("Text"));
		StyleSheet styleSheet = kit.getStyleSheet();
		styleSheet.addRule("body {color: rgb(" + foreground.getRed() + "," + 
				foreground.getGreen() + "," + foreground.getBlue() + ")}");
    	
		// topics
		subjects = new JList<Topic>();
		subjects.setFocusable(false);
		subjects.setCellRenderer(new DialogCellRenderer());
        subjects.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	subjects.setBorder(new TitledBorder("Subjects"));
		
    	// panel en scrollpane voor text en topics
    	conversation = new JPanel(new BorderLayout());
    	conversation.add(text, BorderLayout.PAGE_START);
    	conversation.add(subjects, BorderLayout.CENTER);
		left = new JScrollPane(conversation);
    	
    	// services en subjects
		services = new JList<String>();
		services.setFocusable(false);
		services.setPreferredSize(new Dimension(120, 0));
        JScrollPane down = new JScrollPane(services);
        services.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	down.setBorder(new TitledBorder("Services"));

		panel.add(down, BorderLayout.LINE_END);
	}
	
	@Override
	public void enter(TransitionEvent t) {
		text.setText("");	// tekst van vorig gesprek wissen
		panel.add(left, BorderLayout.CENTER);
		panel.addKeyListener(this);
		if(!t.toString().equals("back")) {
			target = (Creature)t.getParameter("speaker");
		}
		if(target != null) {
			left.setBorder(new TitledBorder(target.toString()));
			Engine.getScriptEngine().put("NPC", target);
			initDialog();
			initServices();
			ui.showPanel(panel);
			list = subjects;
			list.setSelectedIndex(0);
		} else {
			bus.publishAsync(new TransitionEvent("return"));
		}
	}
	
	@Override
	public void exit(TransitionEvent t) {
		panel.removeKeyListener(this);
	}
	
	public void keyTyped(KeyEvent ke) {}
	public void keyReleased(KeyEvent ke) {}
	public void keyPressed(KeyEvent ke) {
		switch(ke.getKeyCode()) {
		case KeyEvent.VK_ESCAPE: 
			bus.publishAsync(new TransitionEvent("return")); 
			break;
		case KeyEvent.VK_UP: 
		case KeyEvent.VK_NUMPAD8:
			if(list.getSelectedIndex() > 0) {
				list.setSelectedIndex(list.getSelectedIndex() - 1);
			}
			break;
		case KeyEvent.VK_NUMPAD2:
		case KeyEvent.VK_DOWN: list.setSelectedIndex(list.getSelectedIndex() + 1); break;
		case KeyEvent.VK_SPACE:
			list.clearSelection();
			list = (list.equals(services)? subjects : services); 
			list.setSelectedIndex(0);
			break;
		case KeyEvent.VK_ENTER:
			int index = list.getSelectedIndex();
			if(list.equals(subjects)) {
				topic = (Topic)list.getSelectedValue();
				String answer = "<p><b>" + topic.phrase + "</b><br>" + topic.answer + "</p>";
		        try {
					kit.insertHTML(doc, doc.getLength(), answer, 0, 0, null);
				} catch (BadLocationException | IOException e) {
					e.printStackTrace();
				}

				Engine.getQuestTracker().doAction(topic);
				initDialog();
			} else {
				String value = list.getSelectedValue().toString();
				if(value.equals("travel")) {
					new TravelDialog(ui, bus).show(Engine.getPlayer(), target);
				} else if(value.equals("training")) {
					new TrainingDialog(ui, bus).show(Engine.getPlayer(), target);
				} else if(value.equals("spells")) {
					new SpellTradeDialog(ui, big, small).show(Engine.getPlayer(), target);
				} else if(value.equals("trade")) {
					new TradeDialog(ui, big, small).show(Engine.getPlayer(), target);
				} else if(value.equals("spell maker")) {
					new SpellMakerDialog(ui).show(Engine.getPlayer(), target);
				} else if(value.equals("potion maker")){
					new PotionDialog(ui, small).show(Engine.getPlayer(), target);
				} else if(value.equals("healer")) {
					heal();
				} else if(value.equals("charge")) {
					new ChargeDialog(ui).show(Engine.getPlayer());
				} else if(value.equals("craft")) {
					new CrafterDialog(ui, small, bus).show(Engine.getPlayer(), target);
				} else if(value.equals("enchant")) {
					new EnchantDialog(ui).show(Engine.getPlayer(), target);
				} else if(value.equals("repair")) {
					new RepairDialog(ui).show(Engine.getPlayer(), target);
				} else if(value.equals("tattoos")) {
					new TattooDialog(ui, small).show(Engine.getPlayer(), target);
				} else {
					System.out.println("not implemented");
				}
			}
			list.setSelectedIndex(index);
			SwingUtilities.invokeLater(new AutoScroller(left.getVerticalScrollBar()));
			break;
		}
	}
	
	private void heal() {
		Player player = Engine.getPlayer();
		HealthComponent health = player.getHealthComponent();
		health.heal(health.getHealth() - health.getBaseHealth());
		MagicUtils.cure(player, SpellType.CURSE);
		MagicUtils.cure(player, SpellType.DISEASE);
		MagicUtils.cure(player, SpellType.POISON);
		ui.showMessage("You have been healed!", 2);
	}
	
	private void initDialog() {
		subjects.removeAll();
		services.removeAll();
		
		if(topic != null && !Engine.getQuestTracker().getSubtopics(topic).isEmpty()) {
			subjects.setListData(Engine.getQuestTracker().getSubtopics(topic));
			subjects.setSelectedIndex(0);
		} else {
			subjects.setListData(Engine.getQuestTracker().getDialog(target));
		}
	}
	
	private void initServices() {
		Vector<String> temp = new Vector<String>();
		
		if(hasService(target.getID(), "training")) {
			temp.add("training");
		}
		if(hasService(target.getID(), "spells")) {
			temp.add("spells");
		}
		if(hasService(target.getID(), "trade")) {
			temp.add("trade");
		}
		if(hasService(target.getID(), "travel")) {
			temp.add("travel");
		}
		if(hasService(target.getID(), "spellmaker")) {
			temp.add("spell maker");
		}
		if(hasService(target.getID(), "alchemy")) {
			temp.add("potion maker");
		}
		if(hasService(target.getID(), "healer")) {
			temp.add("healer");
		}
		if(hasService(target.getID(), "charger")) {
			temp.add("charge");
		}
		if(hasService(target.getID(), "enchant")) {
			temp.add("enchant");
		}
		if(hasService(target.getID(), "craft")) {
			temp.add("craft");
		}
		if(hasService(target.getID(), "repair")) {
			temp.add("repair");
		}
		if(hasService(target.getID(), "tattoo")) {
			temp.add("tattoos");
		}
		services.setListData(temp);	
	}
	
	private boolean hasService(String name, String id) {
		try {
			RPerson person = (RPerson)Engine.getResources().getResource(name);
			for(Element e : person.services) {
				if(e.getAttributeValue("id").equals(id)) {
					return true;
				}
			}
		} catch (Exception e) {} 
		return false;
	}
	
	private static class AutoScroller implements Runnable {
		private JScrollBar bar;
		
		public AutoScroller(JScrollBar bar) {
			this.bar = bar;
		}
		
		@Override
		public void run() {
			bar.setValue(bar.getMaximum());
		}
	}
	
	@SuppressWarnings("serial")
	private static class DialogCellRenderer extends JLabel implements ListCellRenderer<Topic> {
		public Component getListCellRendererComponent(JList<? extends Topic> list, Topic topic, int index, boolean isSelected, boolean cellHasFocus) {
			if(isSelected) {
				setBackground(defaults.getColor("List.selectionBackground"));
				setForeground(defaults.getColor("List.selectionForeground"));
			} else {
				setForeground(defaults.getColor("List.foreground"));
			}
			setOpaque(isSelected);
			setText(topic.phrase);
			return this;
		}
	}
}

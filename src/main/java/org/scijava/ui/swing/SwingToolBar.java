/*
 * #%L
 * SciJava UI components for Java Swing.
 * %%
 * Copyright (C) 2010 - 2024 SciJava developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

 package org.scijava.ui.swing;


 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.net.URL;
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.swing.AbstractButton;
 import javax.swing.ButtonGroup;
 import javax.swing.ImageIcon;
 import javax.swing.JToggleButton;
 import javax.swing.JToolBar;
 
 import org.scijava.Context;
 import org.scijava.InstantiableException;
 import org.scijava.app.StatusService;
 import org.scijava.event.EventHandler;
 import org.scijava.log.LogService;
 import org.scijava.plugin.Parameter;
 import org.scijava.plugin.PluginInfo;
 import org.scijava.tool.Tool;
 import org.scijava.tool.ToolService;
 import org.scijava.tool.event.ToolActivatedEvent;
 import org.scijava.tool.event.ToolDeactivatedEvent;
 import org.scijava.ui.ToolBar;
 import org.scijava.ui.UIService;
 
 // imports needed to change UI - RUKSHIK
 import java.awt.Color;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.RenderingHints;
 import javax.swing.GrayFilter;
 import javax.swing.UIManager;
 import javax.swing.border.EmptyBorder;
 import javax.swing.event.ChangeListener;
 import javax.swing.border.CompoundBorder;
 import javax.swing.border.MatteBorder;
 import javax.swing.ToolTipManager;

 import ij.IJ;

 /**
  * Button bar with selectable tools, styled Material‑UI–like.
  * 
  * @author Curtis Rueden
  */
 public class SwingToolBar extends JToolBar implements ToolBar {
 
	/* update colors and background of toolbar buttons look material ui RUKSHIK */
	 static {
		 UIManager.put("ToolTip.background", UIManager.getColor("Button.disabledBackground"));
		 UIManager.put("ToolTip.foreground", new Color(64, 64, 64)); // darker text
		 UIManager.put("ToolTip.border", new MatteBorder(1, 1, 1, 1, new Color(128, 128, 128)));
	 }
 
	 private final Map<String, AbstractButton> toolButtons;

	 private final ButtonGroup buttonGroup = new ButtonGroup();
 
	 @Parameter
	 private SwingIconService iconService;
 
	 @Parameter
	 private StatusService statusService;
 
	 @Parameter
	 private ToolService toolService;
 
	 @Parameter
	 private UIService uiService;
	 
	 @Parameter
	 private LogService log;
 
	 public SwingToolBar(final Context context) {
		 context.inject(this);
		 
		 toolButtons = new HashMap<>();
		 populateToolBar();
	 }
 
	 // -- Helper methods --
 
	 private void populateToolBar() {
		 final Tool activeTool = toolService.getActiveTool();
		 Tool lastTool = null;
		for (final Tool tool : toolService.getTools()) {
			String name = tool.getInfo().getName();

			if (name.equals("Foreground") || name.equals("Background")) {
			continue;
			}
			try {
				final AbstractButton button = createButton(tool, tool == activeTool);
				toolButtons.put(tool.getInfo().getName(), button);
				iconService.registerButton(tool, button);
				if (toolService.isSeparatorNeeded(tool, lastTool)) addSeparator();
				lastTool = tool;
				add(button);
			}
			catch (final InstantiableException e) {
				log.warn("Invalid tool: " + tool.getInfo(), e);
			}
		 }
	 }
 
	 private AbstractButton createButton(final Tool tool, boolean active)
		 throws InstantiableException
	 {
		 // Style swing button to have Material look RUKSHIK
		 class MaterialToggleButton extends JToggleButton {
			 MaterialToggleButton() { super(); }
 
			 @Override
			 protected void paintComponent(Graphics g) {
				 Graphics2D g2 = (Graphics2D) g.create();
				 g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				 int w = getWidth(), h = getHeight();
				 Color bg = getBackground();
				 g2.setColor(bg);
				 g2.fillRoundRect(0, 0, w, h, 8, 8);
				 g2.dispose();
				 super.paintComponent(g);
			 }
		 }
 
		 final PluginInfo<?> info = tool.getInfo();
		 final String name = info.getName();
		 final String label = info.getLabel();
		 final URL iconURL = info.getIconURL();
		 final boolean enabled = info.isEnabled();
		 final boolean visible = info.isVisible();
 
		 // Use material button instead of stanard swing button RUKSHIK
		 final MaterialToggleButton button = new MaterialToggleButton();
 
		 // set icon
		 if (iconURL == null) {
			 button.setText(name);
			 log.warn("Invalid icon for tool: " + tool);
		 } else {
			 log.debug("Loading icon from " + iconURL);
			 ImageIcon icon = new ImageIcon(iconURL, label);
			 button.setIcon(icon);
			 // Create a darker disabled icon
			 Image grayImg = GrayFilter.createDisabledImage(icon.getImage());
			 button.setDisabledIcon(new ImageIcon(grayImg, label));
			 button.setDisabledSelectedIcon(button.getDisabledIcon());
		 }
 
		 // Tooltip
		 button.setToolTipText(label != null && !label.isEmpty() ? label : name);
		 // Ensure tooltip shows even when disabled
		 ToolTipManager.sharedInstance().registerComponent(button);
		 buttonGroup.add(button);
 
		 // Material Styling RUKSHIK
		 button.setFocusPainted(false);
		 button.setContentAreaFilled(false);
		 button.setOpaque(true);
		 // vertical separator bewtween buttons RUKSHIK
		 MatteBorder sep = new MatteBorder(0, 0, 0, 1, new Color(0, 0, 0, 30)); 
		 CompoundBorder cb = new CompoundBorder(sep, new EmptyBorder(8, 16, 8, 15));
		 button.setBorder(cb);
		 button.setFont(UIManager.getFont("ToggleButton.font"));
		 // light gray-blue for background RUKSHIK
		 Color customBg = new Color(0xC6CDD1);
		 Color menuSelBg = UIManager.getColor("Menu.selectionBackground").brighter();
		 button.setBackground(customBg);
		 button.setForeground(UIManager.getColor("Menu.foreground"));
		 button.addChangeListener((ChangeListener) e -> {
			 button.setBackground(button.isSelected() ? menuSelBg : customBg);
		 });
 
		 // Description on hover RUKSHIK
		 button.addMouseListener(new MouseAdapter() {
			 @Override public void mouseEntered(MouseEvent e) {
				 statusService.showStatus(tool.getDescription());
			 }
			 @Override public void mouseExited(MouseEvent e) {
				 statusService.clearStatus();
			 }
		 });
 
		//  // Hover background RUKSHIK
		//  button.addMouseListener(new MouseAdapter() {
		// 	 @Override public void mouseEntered(MouseEvent e) {
		// 	 }
		// 	 @Override public void mouseExited(MouseEvent e) {
		// 	 }
		//  });
 
		 // Activate tool on selection change
		//  button.addChangeListener(e -> {
		// 	 if (button.isSelected()) {
		// 		 toolService.setActiveTool(tool);
		// 	 }
		//  });

		 button.addActionListener(e -> {
			toolService.setActiveTool(tool);
		});
 
		 // Initial state
		 button.setSelected(active);
		 button.setEnabled(enabled);
		 button.setVisible(visible);
 
		 return button;
	 }
 
	 // -- Event handlers --
 
	 @EventHandler
	 protected void onEvent(final ToolActivatedEvent event) {
		 String name = event.getTool().getInfo().getName();
		 if (name.equals("Paintbrush")) { 
		 	IJ.setTool(16);
		 }
		 else {
			IJ.setTool(name);
		 }
		 AbstractButton btn = toolButtons.get(name);
		 if (btn != null) btn.setSelected(true);
	 }
 
	 @EventHandler
	 protected void onEvent(final ToolDeactivatedEvent event) {
		 String name = event.getTool().getInfo().getName();
		 AbstractButton btn = toolButtons.get(name);
		 if (btn != null) btn.setSelected(false);
	 }
}
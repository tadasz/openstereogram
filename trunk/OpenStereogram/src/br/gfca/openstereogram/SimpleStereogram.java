package br.gfca.openstereogram;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public class SimpleStereogram {

	public void generateSIRD() {
		ImageIcon i = new ImageIcon("./images/depthMaps/Struna.jpg");
		BufferedImage bf = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		bf.getGraphics().drawImage(i.getImage(), 0, 0, null);

		final Image stereogram = StereogramGenerator.generate(
				bf, null,
				Color.BLACK, Color.WHITE, null, 0.5f,
				640, 480,
				12f, 2.5f, 70);

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new StereogramWindow(stereogram).setVisible(true);
			}
		});
	}
}
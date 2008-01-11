package br.gfca.openstereogram;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public class SimpleStereogram {

	public void generateSIRD() {
		BufferedImage depthMap = getImage("./images/depthMaps/Gitara.jpg");
		final Image stereogram = StereogramGenerator.generateSIRD(
				depthMap,
				Color.BLACK, Color.WHITE, Color.RED, 0.5f,
				640, 480,
				14f, 2.5f,
				12f, 0f,				
				72);

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new StereogramWindow(stereogram).setVisible(true);
			}
		});
	}
	
	public void generateTexturedSIRD() {
		BufferedImage depthMap = getImage("./images/depthMaps/Struna.jpg");
		BufferedImage texturePattern = getImage("./images/texturePatterns/RAND7.jpg");
		
		final Image stereogram = StereogramGenerator.generateTexturedSIRD(
				depthMap, texturePattern,
				640, 480,
				14f, 2.5f, 72);

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new StereogramWindow(stereogram).setVisible(true);
			}
		});
	}

	/**
	 * @return
	 */
	private BufferedImage getImage(String file) {
		ImageIcon i = new ImageIcon(file);
		BufferedImage bf = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		bf.getGraphics().drawImage(i.getImage(), 0, 0, null);
		return bf;
	}
}
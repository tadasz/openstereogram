package br.gfca.openstereogram.stereo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ImageManipulator {

	public static BufferedImage resizeDepthMap( BufferedImage original, int width, int height ) {
		BufferedImage newMap = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		Graphics g = newMap.getGraphics();
		g.setColor( new Color(0,0,0) );
		g.fillRect(0, 0, width, height);
		
		int newHeight = (original.getHeight() * width) / original.getWidth();
		if ( newHeight <= height ) {
			int centeredY = (height - newHeight) / 2;
			g.drawImage( original, 0, centeredY, width, newHeight, null);
		}
		else {
			int newWidth = (original.getWidth() * height) / original.getHeight();
			if ( newWidth <= width ) {
				int centeredX = (width - newWidth) / 2;
				g.drawImage( original, centeredX, 0, newWidth, height, null);
			}
			else {
				// Should never get here
				g.drawImage( original, 0, 0, width, height, null );
			}
		}
		return newMap;
	}
	
	public static BufferedImage generateTextDepthMap(String text, int fontSize, int width, int height ) {
		BufferedImage depthMap = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		Graphics g = depthMap.getGraphics();
		g.setColor( new Color(0,0,0) );
		g.fillRect(0, 0, width, height);
		
		Font f = g.getFont().deriveFont( Font.BOLD, fontSize );
		g.setFont( f );
		int textWidth = (int)g.getFontMetrics().getStringBounds( text, g ).getWidth();
		int textHeight = g.getFontMetrics().getAscent();
		
		g.setColor( new Color(127,127,127) );
		g.drawString( text,
				(width - textWidth) / 2,
				((height - textHeight) / 2) + textHeight );
		
		return depthMap;
	}
	
	public static BufferedImage resizeTexturePattern(BufferedImage original, int maxSeparation) {
		if ( original.getWidth() < maxSeparation ) {
			int newHeight = (original.getHeight() * maxSeparation) / original.getWidth();
			BufferedImage resized = new BufferedImage( maxSeparation, newHeight, BufferedImage.TYPE_INT_RGB );
			resized.getGraphics().drawImage( original, 0, 0, resized.getWidth(), resized.getHeight(), null);
			return resized;
		}
		else {
			return original;
		}
	}
}
package br.gfca.openstereogram.stereo;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class DepthMapGenerator {

	public static BufferedImage resizeMap( BufferedImage original, int width, int height ) {

		BufferedImage newMap = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		newMap.getGraphics().setColor( new Color(0,0,0) );
		newMap.getGraphics().fillRect(0, 0, newMap.getWidth(), newMap.getHeight());
		
		int newHeigh = (original.getHeight() * width) / original.getWidth();
		if ( newHeigh <= height ) {
			int centeredY = (height - newHeigh) / 2;
			newMap.getGraphics().drawImage( original, 0, centeredY, width, newHeigh, null);
		}
		else {
			int newWidth = (original.getWidth() * height) / original.getHeight();
			if ( newWidth <= width ) {
				int centeredX = (width - newWidth) / 2;
				newMap.getGraphics().drawImage( original, centeredX, 0, newWidth, height, null);
			}
			else {
				newMap.getGraphics().drawImage( original, 0, 0, null );
			}
		}
		return newMap;
	}
	
	public static BufferedImage generateTextDepthMap(String text, int fontSize, int width, int height) {
		
		return null;
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
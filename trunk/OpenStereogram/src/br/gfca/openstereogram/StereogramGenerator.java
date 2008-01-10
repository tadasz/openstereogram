package br.gfca.openstereogram;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class StereogramGenerator {

	public static Image generate( BufferedImage depthMap, Image texturePattern,
			Color color1, Color color2, Color color3, float color1Intensity,
			int width, int heigh,
			float observationDistanceInches, float eyeSeparationInches, int ppi ) {
		
		if ( width > depthMap.getWidth(null) || heigh > depthMap.getHeight(null) ) {
			throw new IllegalArgumentException( "Depth map smaller than intended stereogram." );
		}
		
		ColorGenerator colors; 
		if ( color3 == null ) {
			colors = new UnbalancedColorGenerator( color1.getRGB(), color2.getRGB(), color1Intensity );
		}
		else {
			colors = new ColorGenerator( color1.getRGB(), color2.getRGB(), color3.getRGB() );
		}
		
		BufferedImage stereogram = new BufferedImage(width, heigh, BufferedImage.TYPE_INT_RGB);
		int[][] links = new int[heigh][width];
		int observationDistance = (int)(observationDistanceInches * ppi);
		int eyeSeparation = (int)(eyeSeparationInches * ppi);
				
		for ( int l = 0; l < heigh; l++ ) {
			for ( int c = 0; c < width; c++ ) {
				links[l][c] = c; 
			}
			
			for ( int c = 0; c < width; c++ ) {
				int depth = obtainDepth( depthMap.getRGB(c, l), observationDistance );
				int separation = (eyeSeparation * depth) / (depth + observationDistance);
				int left = c - (separation / 2);
				int right = left + separation;
				
				if ( left >= 0 && right < width ) {
					links[l][right] = left;
				}
			}
			
			for ( int c = 0; c < width; c++ ) {
				if ( links[l][c] == c ) {
					stereogram.setRGB( c, l, colors.getRandomColor() );
				}
				else {
					stereogram.setRGB( c, l, stereogram.getRGB(links[l][c], l) );
				}
			}
		}
		
		return stereogram;
	}

	private static int obtainDepth(int rgb, int observationDistance) {
//		int depth = 255 - (new Color( rgb )).getRed(); 
//		return (observationDistance * depth) / 255;
		
		// TODO preprocess depth map to discover min and max
		// depth, making de object more centered
		int depth = 255 - (new Color( rgb )).getRed(); 
		return depth + ((observationDistance - 255) / 2);

	}
}

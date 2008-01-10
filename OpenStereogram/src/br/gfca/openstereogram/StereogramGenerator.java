package br.gfca.openstereogram;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class StereogramGenerator {

	public static Image generateSIRD( BufferedImage depthMap,
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
		int[] linksL = new int[width];
		int[] linksR = new int[width];
		int observationDistance = (int)(observationDistanceInches * ppi);
		int eyeSeparation = (int)(eyeSeparationInches * ppi);
				
		for ( int l = 0; l < heigh; l++ ) {
			for ( int c = 0; c < width; c++ ) {
				linksL[c] = c;
				linksR[c] = c;
			}
			
			for ( int c = 0; c < width; c++ ) {
				int depth = obtainDepth( depthMap.getRGB(c, l), observationDistance );
				int separation = (eyeSeparation * depth) / (depth + observationDistance);
				int left = c - (separation / 2);
				int right = left + separation;
				
				if ( left >= 0 && right < width ) {
					boolean visible = true;
					
					if ( linksL[right] != right) {
						if ( linksL[right] < left) {
							linksR[linksL[right]] = linksL[right];
							linksL[right] = right;
						}
						else {
							visible = false;
						}
					}
					if ( linksR[left] != left) {
						if ( linksR[left] > right) {
							linksL[linksR[left]] = linksR[left];
							linksR[left] = left;
						}
						else {
							visible = false;
						}
					}
					
					if ( visible ) {
						linksL[right] = left;
						linksR[left] = right;
					}					
				}
			}
			
			for ( int c = 0; c < width; c++ ) {
				if ( linksL[c] == c ) {
					stereogram.setRGB( c, l, colors.getRandomColor() );
				}
				else {
					stereogram.setRGB( c, l, stereogram.getRGB(linksL[c], l) );
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
		return depth + ((observationDistance - 256) / 2);
	}
	
	public static Image generateTexturedSIRD( BufferedImage depthMap, Image texturePattern,
			int width, int heigh,
			float observationDistanceInches, float eyeSeparationInches, int ppi ) {
		
		if ( width > depthMap.getWidth(null) || heigh > depthMap.getHeight(null) ) {
			throw new IllegalArgumentException( "Depth map smaller than intended stereogram." );
		}
		
		BufferedImage stereogram = new BufferedImage(width, heigh, BufferedImage.TYPE_INT_RGB);
		int[] linksL = new int[width];
		int[] linksR = new int[width];
		int observationDistance = (int)(observationDistanceInches * ppi);
		int eyeSeparation = (int)(eyeSeparationInches * ppi);
				
		for ( int l = 0; l < heigh; l++ ) {
			for ( int c = 0; c < width; c++ ) {
				linksL[c] = c;
				linksR[c] = c;
			}
			
			for ( int c = 0; c < width; c++ ) {
				int depth = obtainDepth( depthMap.getRGB(c, l), observationDistance );
				int separation = (eyeSeparation * depth) / (depth + observationDistance);
				int left = c - (separation / 2);
				int right = left + separation;
				
				if ( left >= 0 && right < width ) {
					boolean visible = true;
					
					if ( linksL[right] != right) {
						if ( linksL[right] < left) {
							linksR[linksL[right]] = linksL[right];
							linksL[right] = right;
						}
						else {
							visible = false;
						}
					}
					if ( linksR[left] != left) {
						if ( linksR[left] > right) {
							linksL[linksR[left]] = linksR[left];
							linksR[left] = left;
						}
						else {
							visible = false;
						}
					}
					
					if ( visible ) {
						linksL[right] = left;
						linksR[left] = right;
					}					
				}
			}
			
			for ( int c = 0; c < width; c++ ) {
				if ( linksL[c] == c ) {
					//stereogram.setRGB( c, l, colors.getRandomColor() );
				}
				else {
					stereogram.setRGB( c, l, stereogram.getRGB(linksL[c], l) );
				}
			}
		}
		
		return stereogram;
	}
}

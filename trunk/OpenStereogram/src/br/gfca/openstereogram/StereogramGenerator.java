package br.gfca.openstereogram;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class StereogramGenerator {

	public static Image generateSIRD( BufferedImage depthMap,
			Color color1, Color color2, Color color3, float color1Intensity,
			int width, int heigh,
			float observationDistanceInches, float eyeSeparationInches,
			float maxDepthInches, float minDepthInches,
			int ppi ) {
		
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
		int observationDistance = convertoToPixels(observationDistanceInches, ppi);
		int eyeSeparation = convertoToPixels(eyeSeparationInches, ppi);
		int maxdepth = getMaxDepth( convertoToPixels(maxDepthInches, ppi), observationDistance );
		int minDepth = getMinDepth( 0.55f, maxdepth, observationDistance, convertoToPixels(minDepthInches, ppi) );

		for ( int l = 0; l < heigh; l++ ) {
			for ( int c = 0; c < width; c++ ) {
				linksL[c] = c;
				linksR[c] = c;
			}
			
			for ( int c = 0; c < width; c++ ) {
				int depth = obtainDepth( depthMap.getRGB(c, l), maxdepth, minDepth );
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

	private static int getMinDepth(float separationFactor, int maxdepth, int observationDistance, int suppliedMinDepth) {
		int computedMinDepth = (int)( (separationFactor * maxdepth * observationDistance) /
			(((1 - separationFactor) * maxdepth) + observationDistance) );
		
		return Math.min( Math.max( computedMinDepth, suppliedMinDepth), maxdepth);
	}

	private static int getMaxDepth(int suppliedMaxDepth, int observationDistance) {
		return Math.max( Math.min( suppliedMaxDepth, observationDistance), 0);
	}
	
	private static int convertoToPixels(float valueInches, int ppi) {
		return (int)(valueInches * ppi);
	}

	private static int obtainDepth(int depth, int maxDepth, int minDepth) {
		return maxDepth - ((new Color( depth )).getRed() * (maxDepth - minDepth) / 255);
	}
	
	public static Image generateTexturedSIRD( BufferedImage depthMap, Image texturePattern,
			int width, int heigh,
			float observationDistanceInches, float eyeSeparationInches, int ppi ) {
		// TODO  :)
		return null;
	}
}

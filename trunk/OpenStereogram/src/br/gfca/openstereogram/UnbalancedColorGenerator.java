package br.gfca.openstereogram;

import java.util.Random;

public class UnbalancedColorGenerator extends ColorGenerator {

	private float color1Intensity;
	
	public UnbalancedColorGenerator(int color1, int color2, float color1Intensity) {
		this.color1Intensity = color1Intensity;
		this.colors = new int[2];
		this.colors[0] = color1;
		this.colors[1] = color2;
		this.randomizer = new Random();
	}

	@Override
	public int getRandomColor() {
		return this.randomizer.nextFloat() < color1Intensity ? colors[0] : colors[1];
	}
}
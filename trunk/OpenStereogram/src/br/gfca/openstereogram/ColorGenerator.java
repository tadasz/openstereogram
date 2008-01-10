package br.gfca.openstereogram;

import java.util.Random;

public class ColorGenerator {
	
	protected Random randomizer;
	protected int[] colors;
	
	public ColorGenerator(int color1, int color2, int color3) {
		this.colors = new int[3];
		this.colors[0] = color1;
		this.colors[1] = color2;
		this.colors[2] = color3;
		this.randomizer = new Random();
	}
	
	protected ColorGenerator() {}
	
	public int getRandomColor() {
		return this.colors[ this.randomizer.nextInt(this.colors.length) ];
	}
}
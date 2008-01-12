/**
 * 
 */
package br.gfca.openstereogram;

/**
 * @author Gustavo
 *
 */
public class OpenStereogram {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleStereogram ss = new SimpleStereogram();
		ss.generateSIRD();
		ss.generateTexturedSIRD();
	}
}

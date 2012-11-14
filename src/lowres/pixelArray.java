package lowres;

import java.util.ArrayList;

import processing.core.PApplet;

class PixelArray {
	/*
	 * The class pixelarray manages the position and size of pixels
	 * in a variable density grid. It maintains the list of pixels
	 * and manages their update, merge and divide
	 */
	
	
	public ArrayList<Pixel> pixels;
	
	PApplet p;
	
	PixelArray(PApplet parent) {
		
		pixels = new ArrayList<Pixel>();
		p=parent;
	}
	
	public void add(float x, float y, float e, String id) {
		
		pixels.add(new Pixel(x,y,e,id, p));
	}
	
	public void merge(int index) {
		/*
		 * merges the current circle with its three predecessors:
		 * reaffects its coordinates, size and id, and removes predecessors
		 * from arraylist
		 */

	Pixel currentPixel = pixels.get(index);
		
		int cpt = 0;
		
		while(cpt<pixels.size()) {
			
		Pixel localPixel=pixels.get(cpt);
			String[] m = p.match(localPixel.id, "^" + currentPixel.id.substring(0, currentPixel.id.length()-1));
			if(m != null && localPixel.id.equals(currentPixel.id)==false) {	// the circle is beginning with the same substring, and is not the current one. remove.
				pixels.remove(cpt);
				p.println("removing pixel id: " + localPixel.id);
			} else
				cpt++;
			
		}
		
		currentPixel.id = currentPixel.id.substring(0, currentPixel.id.length()-1);

		currentPixel.xCenter += (currentPixel.expanse/2)*((p.floor(currentPixel.xCenter/currentPixel.expanse)%2 == 0)?1:-1);
		currentPixel.yCenter += (currentPixel.expanse/2)*((p.floor(currentPixel.yCenter/currentPixel.expanse)%2 == 0)?1:-1);

		currentPixel.expanse *= 2;

	}
	
	public void divide(int index) {
		/*
		 * divides the current circle in 4:
		 * Assigns the new values to its diameter, position and id
		 * and adds three more circles to the arraylist
		 */

		Pixel currentPixel = pixels.get(index);

		currentPixel.xCenter -= currentPixel.expanse/4;
		currentPixel.yCenter -= currentPixel.expanse/4;

		currentPixel.expanse /= 2;

		pixels.add(index+1, new Pixel(currentPixel.xCenter+currentPixel.expanse, currentPixel.yCenter+currentPixel.expanse, currentPixel.expanse, currentPixel.id+"3", p));
		pixels.add(index+1, new Pixel(currentPixel.xCenter, currentPixel.yCenter+currentPixel.expanse, currentPixel.expanse, currentPixel.id+"2", p));
		pixels.add(index+1, new Pixel(currentPixel.xCenter+currentPixel.expanse, currentPixel.yCenter, currentPixel.expanse, currentPixel.id+"1", p));

		currentPixel.id +="0";

	}
	
	public int size() {
		return pixels.size();
	}
	
	public void update() {
		
	}
	
	
}

class Pixel {
	
	float expanse;			// width & height of the pixel
	float xCenter;
	float yCenter;

	int transitionTime;		// time in milliseconds at which a transition started
	boolean transitionUp;	// true if the circle is growing, false otherwise

	String id;		// string that identifies uniquely the circle. It contains info about the circle's parent

	PApplet p;
	
	Pixel(float x, float y, float e, String _id, PApplet parent) {

		expanse = e;
		xCenter = x;
		yCenter = y;

		id = _id;

		p=parent;
		transitionTime= p.millis();		// when created, a transition starts
	}

	public void update(){
		/*
		 * recalculates the correct diameter of the circle, based on 
		 * the time and on the transition direction
		 */

		//diameter = baseDiameter*(1+tanh((millis()-transitionTime-3000)/2));

	}
	

	public boolean isInPixel(int x, int y) {

		if(x < xCenter + expanse / 2 && x>xCenter-expanse/2 && y < yCenter+expanse/2 && y>yCenter-expanse/2)
			return true;
		else
			return false;
	}

	
}

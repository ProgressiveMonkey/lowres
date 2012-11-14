package lowres;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.video.*;
import megamu.mesh.*;
import org.openkinect.processing.Kinect;


public class Lowres extends PApplet {

	Capture myCapture;

	boolean mirror = true;

	int mode = 0;		// operation mode 0: circles, 1: pixels, 2: voronoi, 3: delaunay

	PImage backgroundImage;

	Kinect kinect;
	PImage depth;
	boolean showKinect = false;
	boolean usingKinect = false;

	//ArrayList<Circle> circles; 

	PixelArray pixelArray;
	
	float levelHysteresis = 1f;		// hysteresis for the selection of level, depending on the desired level and current level

	public void setup() {

		size(1080,1080);

		myCapture = new Capture(this,1920, 1080, 30);

	/*	circles = new ArrayList<Circle>();
		circles.add(new Circle(width/2, height/2, width, "0"));
*/
		
		pixelArray = new PixelArray();
		pixelArray.add(width/2, height/2, width, "0");
		
		rectMode(CENTER);
		noStroke();

		if(usingKinect) {
			kinect = new Kinect(this);
			kinect.start();
			kinect.enableDepth(true);
			kinect.enableRGB(false);
			kinect.enableIR(false);
		}

		depth = createImage(640, 480, RGB);

		//backgroundImage = loadImage("/Users/gavrilobozovic/Google Drive 2/Google Drive/Work/Restons Sérieux/04 Communication/02 Web/02 Site final/mock-ups/Screenshots/restons-serieux_template-web-2012-A.jpg");

		//size(backgroundImage.width, backgroundImage.height);

		println("setup completed");

	}

	public void draw() {

		background(255,255,255);

		if(myCapture.available()) {
			myCapture.read();
		}

		if(usingKinect){
			depth = kinect.getDepthImage();
			depth.updatePixels();
		}

		pixelArray.update();
		
		for(int i=0;i<pixelArray.size();i++) {
			
			Pixel currentPixel = pixelArray.pixels.get(i);

			if(usingKinect){
				float desiredLevel = gray(depth.get(round(currentPixel.xCenter*640/1920), round(currentPixel.yCenter*480/height)));
				desiredLevel/=25;
				int currentLevel = currentPixel.id.length();

				println("current level: " + currentLevel + ", desired level: " + desiredLevel);

				if(desiredLevel>currentLevel+levelHysteresis && currentLevel<7)
					pixelArray.divide(i);

				if(desiredLevel<currentLevel-levelHysteresis && currentLevel>1)
					pixelArray.merge(i);
			}


			fill(myCapture.get(width*(mirror==true?1:0)+round(currentPixel.xCenter)*(mirror==true?-1:1), round(currentPixel.yCenter)));

			switch(mode) {
			case 0: ellipse(currentPixel.xCenter, currentPixel.yCenter, currentPixel.expanse, currentPixel.expanse); break;
			case 1: rect(currentPixel.xCenter, currentPixel.yCenter, currentPixel.expanse, currentPixel.expanse); break;
			default: break;
			}

		}	

		text("framerate : " + frameRate+ ", number of circles : " + pixelArray.size(),10,10);


		if(showKinect)
			image(depth,0,0);
	}

	public void mouseClicked() {

		for(int i=0;i<pixelArray.size();i++) {
			Pixel currentPixel = pixelArray.pixels.get(i);

			/*
			if(currentCircle.isInCircle(mouseX, mouseY) && currentCircle.id.length()>1){
				//currentCircle.divide(circles);
				currentCircle.merge(circles);
				break;
			}*/

			if(currentPixel.isInPixel(mouseX, mouseY)){
				pixelArray.merge(i);
				break;
			}

		}

	}

	public void mouseDragged() {

		for(int i=0;i<pixelArray.size();i++) {
		Pixel currentPixel = pixelArray.pixels.get(i);

			if(currentPixel.isInPixel(mouseX, mouseY)){
				pixelArray.divide(i);

				break;
			}

		}

	}


	void captureEvent (Capture myCapture) {
		myCapture.read ();
	}


	public void keyPressed() {
		if (key == CODED) {
			if (keyCode == UP) {
				mode++;
				mode=mode%2;
			} else if (keyCode == DOWN) {
				mode+=2;
				mode=mode%2;
			} 
		} else {
			switch(key) {
			case 'm': mirror = !mirror; break;
			case 'k': showKinect = !showKinect; break;
			default : break;
			}
		}
	}

	public int gray(int value) {
		return max((value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff);
	}
	
	
	class PixelArray {
		/*
		 * The class pixelarray manages the position and size of pixels
		 * in a variable density grid. It maintains the list of pixels
		 * and manages their update, merge and divide
		 */
		
		
		private ArrayList<Pixel> pixels;
		
		
		
		PixelArray() {
			
			pixels = new ArrayList<Pixel>();
		}
		
		public void add(float x, float y, float e, String id) {
			
			pixels.add(new Pixel(x,y,e,id));
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
				String[] m = match(localPixel.id, "^" + currentPixel.id.substring(0, currentPixel.id.length()-1));
				if(m != null && localPixel.id.equals(currentPixel.id)==false) {	// the circle is beginning with the same substring, and is not the current one. remove.
					pixels.remove(cpt);
					println("removing pixel id: " + localPixel.id);
				} else
					cpt++;
				
			}
			
			currentPixel.id = currentPixel.id.substring(0, currentPixel.id.length()-1);

			currentPixel.xCenter += (currentPixel.expanse/2)*((floor(currentPixel.xCenter/currentPixel.expanse)%2 == 0)?1:-1);
			currentPixel.yCenter += (currentPixel.expanse/2)*((floor(currentPixel.yCenter/currentPixel.expanse)%2 == 0)?1:-1);

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

			pixels.add(index+1, new Pixel(currentPixel.xCenter+currentPixel.expanse, currentPixel.yCenter+currentPixel.expanse, currentPixel.expanse, currentPixel.id+"3"));
			pixels.add(index+1, new Pixel(currentPixel.xCenter, currentPixel.yCenter+currentPixel.expanse, currentPixel.expanse, currentPixel.id+"2"));
			pixels.add(index+1, new Pixel(currentPixel.xCenter+currentPixel.expanse, currentPixel.yCenter, currentPixel.expanse, currentPixel.id+"1"));

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

		Pixel(float x, float y, float e, String _id) {

			expanse = e;
			xCenter = x;
			yCenter = y;

			id = _id;

			transitionTime= millis();		// when created, a transition starts
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



}

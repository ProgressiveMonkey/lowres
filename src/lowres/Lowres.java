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

	ArrayList<Circle> circles; 

	float levelHysteresis = 1f;		// hysteresis for the selection of level, depending on the desired level and current level

	public void setup() {

		size(1080,1080);

		myCapture = new Capture(this,1920, 1080, 30);

		circles = new ArrayList<Circle>();
		circles.add(new Circle(width/2, height/2, width, "0"));

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

		for(int i=0;i<circles.size();i++) {
			Circle currentCircle = circles.get(i);

			if(currentCircle.active == false)
				circles.remove(i);

			if(usingKinect){
				float desiredLevel = gray(depth.get(round(currentCircle.xCenter*640/1920), round(currentCircle.yCenter*480/height)));
				desiredLevel/=25;
				int currentLevel = currentCircle.id.length();

				println("current level: " + currentLevel + ", desired level: " + desiredLevel);

				if(desiredLevel>currentLevel+levelHysteresis && currentLevel<7)
					divide(i);

				if(desiredLevel<currentLevel-levelHysteresis && currentLevel>1)
					merge(i);
			}


			if(mirror)
				fill(myCapture.get(width-round(currentCircle.xCenter), round(currentCircle.yCenter)));
			else
				fill(myCapture.get(round(currentCircle.xCenter), round(currentCircle.yCenter)));


			switch(mode) {
			case 0: ellipse(currentCircle.xCenter, currentCircle.yCenter, currentCircle.diameter, currentCircle.diameter); break;
			case 1: rect(currentCircle.xCenter, currentCircle.yCenter, currentCircle.diameter, currentCircle.diameter); break;
			default: break;
			}

		}	

		text("framerate : " + frameRate+ ", number of circles : " + circles.size(),10,10);


		if(showKinect)
			image(depth,0,0);
	}

	public void mouseClicked() {

		for(int i=0;i<circles.size();i++) {
			Circle currentCircle = circles.get(i);

			/*
			if(currentCircle.isInCircle(mouseX, mouseY) && currentCircle.id.length()>1){
				//currentCircle.divide(circles);
				currentCircle.merge(circles);
				break;
			}*/

			if(currentCircle.isInCircle(mouseX, mouseY)){
				merge(i);
				break;
			}

		}

	}

	public void mouseDragged() {

		for(int i=0;i<circles.size();i++) {
			Circle currentCircle = circles.get(i);

			if(currentCircle.isInCircle(mouseX, mouseY)){
				divide(i);

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

	public void divide(int index) {
		/*
		 * divides the current circle in 4:
		 * Assigns the new values to its diameter, position and id
		 * and adds three more circles to the arraylist
		 */

		Circle currentCircle = circles.get(index);

		currentCircle.xCenter -= currentCircle.diameter/4;
		currentCircle.yCenter -= currentCircle.diameter/4;

		currentCircle.diameter /= 2;

		circles.add(index+1, new Circle(currentCircle.xCenter+currentCircle.diameter, currentCircle.yCenter+currentCircle.diameter, currentCircle.diameter, currentCircle.id+"3"));
		circles.add(index+1, new Circle(currentCircle.xCenter, currentCircle.yCenter+currentCircle.diameter, currentCircle.diameter, currentCircle.id+"2"));
		circles.add(index+1, new Circle(currentCircle.xCenter+currentCircle.diameter, currentCircle.yCenter, currentCircle.diameter, currentCircle.id+"1"));

		currentCircle.id +="0";

	}

	public void merge(int index) {
		/*
		 * merges the current circle with its three predecessors:
		 * reaffects its coordinates, size and id, and removes predecessors
		 * from arraylist
		 */

		Circle currentCircle = circles.get(index);
		
		println("merging from circle id: " + 	currentCircle.id + ", new id : " + currentCircle.id.substring(0, currentCircle.id.length()-1));
		int cpt = 0;
		
		while(cpt<circles.size()) {
			
			Circle localCircle=circles.get(cpt);
			String[] m = match(localCircle.id, "^" + currentCircle.id.substring(0, currentCircle.id.length()-1));
			if(m != null && localCircle.id.equals(currentCircle.id)==false) {	// the circle is beginning with the same substring, and is not the current one. remove.
				circles.remove(cpt);
				println("removing circle id: " + localCircle.id);
			} else
				cpt++;
			
		}
		
		currentCircle.id = currentCircle.id.substring(0, currentCircle.id.length()-1);

		currentCircle.xCenter += (currentCircle.diameter/2)*((floor(currentCircle.xCenter/currentCircle.diameter)%2 == 0)?1:-1);
		currentCircle.yCenter += (currentCircle.diameter/2)*((floor(currentCircle.yCenter/currentCircle.diameter)%2 == 0)?1:-1);

		currentCircle.diameter *= 2;

	}



	class Circle {

		float baseDiameter;		// starting diameter, prior to transition
		float diameter;			// current actual diameter
		float xCenter;
		float yCenter;

		boolean active=true;

		int transitionTime;		// time in milliseconds at which a transition started
		boolean transitionUp;	// true if the circle is growing, false otherwise

		String id;		// string that identifies uniquely the circle. It contains info about the circle's parent

		Circle(float x, float y, float d, String _id) {

			diameter = d;
			xCenter = x;
			yCenter = y;

			id = _id;
			active = true;

			transitionTime= millis();		// when created, a transition starts
		}

		/*
		public void divide(ArrayList<Circle> theCircles) {

			xCenter = xCenter - diameter/4;
			yCenter = yCenter - diameter/4;

			diameter = diameter/2;

			theCircles.add(new Circle(xCenter+diameter, yCenter, diameter, id+"1"));
			theCircles.add(new Circle(xCenter, yCenter+diameter, diameter, id+"2"));
			theCircles.add(new Circle(xCenter+diameter, yCenter+diameter, diameter, id+"3"));

			id = id+"0";

		}

		public void merge(ArrayList<Circle> theCircles) {			

			id = id.substring(0, id.length()-1);

			xCenter = xCenter + (diameter/2)*((floor(xCenter/diameter)%2 == 0)?1:-1);
			yCenter = yCenter + (diameter/2)*((floor(yCenter/diameter)%2 == 0)?1:-1);

			diameter = diameter*2;

			for(int i=0; i<theCircles.size();i++) {

				Circle currentCircle=theCircles.get(i);
				String[] m = match(currentCircle.id, id+".");
				if(m != null) {
					currentCircle.active =false;
				} 		


			}

		}*/

		public void update(){
			/*
			 * recalculates the correct diameter of the circle, based on 
			 * the time and on the transition direction
			 */

			//diameter = baseDiameter*(1+tanh((millis()-transitionTime-3000)/2));

		}


		public boolean isInCircle(int x, int y) {

			if(x < xCenter + diameter / 2 && x>xCenter-diameter/2 && y < yCenter+diameter/2 && y>yCenter-diameter/2)
				return true;
			else
				return false;
		}


	}


}

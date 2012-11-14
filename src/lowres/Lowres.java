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

	int captureWidth=320, captureHeight=240;		// size of the capture, does not need to be as large as the display

	Kinect kinect;
	PImage depth;
	boolean showKinect = false;
	boolean usingKinect = false;
	boolean useWebcam = true;

	//ArrayList<Circle> circles; 

	PixelArray pixelArray;

	float levelHysteresis = 1f;		// hysteresis for the selection of level, depending on the desired level and current level

	public void setup() {

		size(1080,1080);

		myCapture = new Capture(this,captureWidth, captureHeight, 30);

		/*	circles = new ArrayList<Circle>();
		circles.add(new Circle(width/2, height/2, width, "0"));
		 */

		pixelArray = new PixelArray(this);
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

		backgroundImage = loadImage("/Users/gavrilobozovic/Downloads/tissot_touch_screen_watch.png");

		//size(backgroundImage.width, backgroundImage.height);

		println("setup completed");

	}

	public void draw() {

		background(255,255,255);

		if(useWebcam) {
			if(myCapture.available()) {
				myCapture.read();
			}

			backgroundImage=myCapture.get();
		}

		if(usingKinect){
			depth = kinect.getDepthImage();
			depth.updatePixels();
		}

		pixelArray.update();

		int fillR=0;
		int fillG=0;
		int fillB=0;
		int nPixels;

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
			} else {

				fillR=0;
				fillG=0;
				fillB=0;
				nPixels =0;

				for(int x=round(currentPixel.xCenter-currentPixel.expanse/2)* captureWidth/width;x<floor(currentPixel.xCenter+currentPixel.expanse/2)* captureWidth/width; x++) {
					for(int y=round(currentPixel.yCenter-currentPixel.expanse/2)* captureHeight/height;y<floor(currentPixel.yCenter+currentPixel.expanse/2)* captureHeight/height; y++) {			

						//int loc = width*(mirror==true?1:0) + x*(mirror==true?-1:1) + y * width;

						int xPosition = (captureWidth*(mirror==true?1:0) + x*(mirror==true?-1:1));// * captureWidth/width;
						int yPosition = y;// * captureHeight/height;

						fillR += red(backgroundImage.get(xPosition,yPosition));
						fillG += green(backgroundImage.get(xPosition,yPosition));
						fillB += blue(backgroundImage.get(xPosition,yPosition));


						nPixels++;						
					}

				}
				if(nPixels!=0) {
					fillR/=nPixels;
					fillG/=nPixels;
					fillB/=nPixels;
				}

			}


			//fill(myCapture.get(width*(mirror==true?1:0)+round(currentPixel.xCenter)*(mirror==true?-1:1), round(currentPixel.yCenter)));

			fill(fillR,fillG,fillB);

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
			case 'w': useWebcam = !useWebcam; break;
			default : break;
			}
		}
	}

	public int gray(int value) {
		return max((value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff);
	}


}

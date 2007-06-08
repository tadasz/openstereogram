package test;

/**
 *
 *  StereoMaker 2.0
 *
 * Clark S. Lindsey
 * Physics Dept.-Frescati
 * Royal Institute of Technology
 * Frescativ. 24
 * 104 05 Stockholm, Sweden
 *
 * Oct-1996
 *
 * Makes stereograms with image backgrounds in addition to RDS.
 * Add options for types of images, oversampling, etc. Also, allow
 * choice of colors for the RDS. 
 *  Based on algorithms of W. A. Steer.
 *  http://www.ucl.ac.uk/~zcapl31/stereo.html
 *
 */
import java.applet.Applet;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

/************************* Class StereoMaker ********************************/
public class StereoMaker extends Applet implements Runnable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4899250516038195406L;

	static boolean debug = false;

	int stereoType = TYPE_RDS;

	static final int TYPE_RDS = 1;

	static final int TYPE_IMAGE = 2;

	int oversample = 1;

	// allow space for up to 6 times oversampling
	static final int MAXWIDTH_OVERSAMPLE = 640 * 6;

	Image depthImage;

	Image backplaneImage;

	String backplaneImageName = "rocket.gif";

	String backplanePrevious = "rocket.gif";

	boolean backplaneLoaded = false;

	int pixels[]; // the stereogram  

	int depthPixels[]; // the depth image  

	int backplanePixels[]; // the backplane image

	int totNumPixels = 0;

	int totNumBackPlanePixels = 0;

	int depthIndex = 0;

	int index = 0;

	int maxWidth, maxHeight, midWidth, midHeight;

	int maxImageWidth, maxImageHeight;

	int imageWidth, imageHeight;

	int backplaneWidth, backplaneHeight;

	static final int frameThickness = 5;

	Graphics offScreenGraphics;

	Color rdsColors[];

	/* Flag for problems in loading any type of file */
	boolean loadError = false;

	// Some stereogram parameters
	int maxDepth = 0;

	int minDepth = 255;

	int maxSceneHeight = 255;

	int minSceneHeight = 0;

	int heightScale = 2; // Optional scale of heights from 0-255 range.

	double sepFactor = 0.55;//= ratio of minimum stereo separation to maximum

	int xdpi = 75; /* x-resolution of typical monitor */

	// Info for the characters to be displayed
	String input;

	int charSize = 180;

	Thread kicker;

	StereoCanvas canvas;

	StereoControlsLo loControls;

	StereoControlsHi hiControls;

	/* Need the MediaTracker to insure that images are loaded before 
	 * the applet continues.
	 */
	MediaTracker tracker;

	/*::::::::::::::::::::::::::::::: init() :::::::::::::::::::::::::::::::::::*/
	public void init() {
		canvas = new StereoCanvas(this);
		input = new String("HI");

		setLayout(new FlowLayout());
		add("Center", canvas);
		add("South", loControls = new StereoControlsLo(this));
		add("North", hiControls = new StereoControlsHi(this));

		/* Get images using a MediaTracker as a monitor */
		tracker = new MediaTracker(this);
		backplaneImageName = new String(backplanePrevious);
		getBackplaneImage();

		rdsColors = new Color[3];
		rdsColors[0] = Color.red;
		rdsColors[1] = Color.green;
		rdsColors[2] = Color.blue;
	}

	/*::::::::::::::::::::::::::::::: start() ::::::::::::::::::::::::::::::::::*/
	public synchronized void start() {
		if (canvas.getImage() == null) {
			kicker = new Thread(this);
			kicker.start();
		}
	}

	/*::::::::::::::::::::::::::::::: stop() :::::::::::::::::::::::::::::::::::*/
	public void stop() {
		try {
			if (kicker != null) {
				kicker.stop();
			}
		} catch (Exception e) {
		}
		kicker = null;
		if (StereoMaker.debug) {
			System.out.println("Applet ended");
		}
	}

	/*::::::::::::::::::::::::::::::: restart() ::::::::::::::::::::::::::::::::*/
	public void restart() {
		stop();
		canvas.setImage(null);
		//Get the current info in the onscreen inputs
		loControls.getSettings();
		hiControls.getSettings();
		start();
	}

	/*::::::::::::::::::::::::::::::: loadError() :::::::::::::::::::::::::::::*/
	/* If there was an error in loading a file, display a message on status line
	 */
	public void loadError(String msg) {
		String errorMsg = "Problems:" + msg;
		showStatus(errorMsg); /* Applet class method to display a message */
		if (StereoMaker.debug)
			System.err.println(errorMsg);
		loadError = true;
	}

	/*::::::::::::::::::::::::::::::: handleEvent() :::::::::::::::::::::::::::::::::*/
	public boolean handleEvent(Event e) {
		if (e.id == Event.WINDOW_DESTROY) {
			System.exit(0);
		}
		return false;
	}

	/*::::::::::::::::::::::::::::::: run() ::::::::::::::::::::::::::::::::::::*/
	public void run() {
		Thread me = Thread.currentThread();
		me.setPriority(Thread.MIN_PRIORITY);
		// Check if a new backplane image has been selected
		if (!backplaneImageName.equals(backplanePrevious)) {
			getBackplaneImage();
			backplanePrevious = new String(backplaneImageName);
		}
		// Check that backplane image OK, - loaded and is big enough.
		if (stereoType == TYPE_IMAGE && !backplaneLoaded) {
			showStatus("Backplane loading failure");
			if (StereoMaker.debug)
				System.out.println("Backplane loading failure");
			return;
		}
		if (stereoType == TYPE_IMAGE) {
			int status = getBackplaneInfo();
			if (status != 0)
				return;
		}

		// This is the max Applet viewing size.
		maxWidth = canvas.size().width;
		maxHeight = canvas.size().height;
		midWidth = maxWidth / 2;
		midHeight = maxHeight / 2;

		// Want to put a nice frame around the image so reduce it's size.
		imageWidth = maxWidth - 2 * frameThickness;
		imageHeight = maxHeight - 2 * frameThickness;

		if (oversample * imageWidth > MAXWIDTH_OVERSAMPLE) {
			showStatus("Oversampling exceeds maximum");
			if (StereoMaker.debug)
				System.out.println("Oversampling exceeds maximum");
			return;
		}

		try {
			// OK so far so go make the depth image
			totNumPixels = imageWidth * imageHeight;
			depthPixels = new int[totNumPixels];
			makeDepthImage();

			// Now use the depth image to make the stereogram.
			pixels = new int[totNumPixels];
			if (stereoType == TYPE_IMAGE)
				makeStereoFromImage();
			else
				makeStereoRds();
			newImage(me);
		} finally {
			showStatus("Stereogram generation stopped");
		}

	}

	/*::::::::::::::::::::::::::::::: newImage() 
	 :::::::::::::::::::::::::::::::::*/
	/* Creates a new image from the pixels array and sends it to the canvas, which
	 * will paint it.
	 */
	synchronized void newImage(Thread me) {
		if (kicker != me) {
			return;
		}
		if (StereoMaker.debug) {
			System.out.println("Creating image");
		}
		Image img;
		img = createImage(new MemoryImageSource(imageWidth, imageHeight,
				ColorModel.getRGBdefault(), pixels, 0, imageWidth));
		canvas.setImage(img);
		kicker = null;
	}

	/*::::::::::::::::::::::::::::::: makeDepthImage() :::::::::::::::::::::::::*/
	/* Make a depth image from the input characters. Use the applet's drawstring
	 * to draw the characters in blue onto an image and then get the resulting
	 * pixel array. This means I don't have to calculated the shape of the 
	 * letters myself.
	 */
	public synchronized void makeDepthImage() {
		char defMask[] = { 'C', 'L', 'A', 'R', 'K' };
		char blank = ' ';
		char Mask[] = new char[5];

		int numChar = input.length();
		if (numChar == 0) {
			for (int i = 0; i < 5; i++) {
				Mask[i] = defMask[i];
			}
		} else {
			for (int i = 0; i < numChar; i++) {
				Mask[i] = input.charAt(i);
			}
		}

		if (StereoMaker.debug) {
			System.out.println("In makeDepthImage");
		}

		//Initialize the pixels to black (R=0,G=0,B=0)
		index = 0;
		int c[] = new int[4];
		c[0] = c[1] = c[2] = 0;
		c[3] = 255;
		int colorPacked = ((c[3] << 24) | (c[0] << 16) | (c[1] << 8) | (c[2] << 0));
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				depthPixels[index++] = colorPacked;
			}
		}

		// Create the depthImage so we can draw on it with
		// the Applets graphics methods

		depthImage = createImage(imageWidth, imageHeight);

		if (StereoMaker.debug) {
			System.out.println("Created Depth Image");
		}
		showStatus("Making the depth image");
		// Don't put the image to the screen. Use only as buffer
		offScreenGraphics = depthImage.getGraphics();
		offScreenGraphics.setFont(getFont());
		offScreenGraphics.setColor(Color.black);
		offScreenGraphics.fillRect(0, 0, imageWidth, imageHeight);
		offScreenGraphics.setColor(Color.blue);

		// Now get the coordinates needed for placing the characters
		// Set the font for the depth letters. Default font size of charSize=180 
		// gives a good appearance. Override extreme values of charSize
		if (charSize < 20)
			charSize = 20;
		if (charSize > (imageHeight - 20))
			charSize = (imageHeight - 20);
		offScreenGraphics.setFont(new Font("Times", Font.BOLD, charSize));
		FontMetrics fontMetric = offScreenGraphics.getFontMetrics();
		int stringWidth = 0;
		for (int j = 0; j < numChar; j++) {
			stringWidth += fontMetric.charWidth(Mask[j]);
		}
		if (stringWidth > (imageWidth - 20)) {
			double frac = ((double) imageWidth) / ((double) (stringWidth + 20));
			int rescale = (int) (charSize * frac);
			offScreenGraphics.setFont(new Font("Times", Font.BOLD, rescale));
			fontMetric = offScreenGraphics.getFontMetrics();
			stringWidth = 0;
			for (int j = 0; j < numChar; j++) {
				stringWidth += fontMetric.charWidth(Mask[j]);
			}
		}
		int fontHeight = fontMetric.getHeight();
		// Now center the text
		int offx = imageWidth / 2;
		int offy = imageHeight / 2;
		int ix = offx - (stringWidth / 2);
		int iy = offy + (fontHeight / 4);

		offScreenGraphics.drawChars(Mask, 0, numChar, ix, iy);
		if (StereoMaker.debug) {
			System.out.println("font wid=" + stringWidth + " ht=" + fontHeight
					+ " numChar=" + numChar);
		}
		if (StereoMaker.debug) {
			System.out.println("Got graphics and drew the character");
		}
		// Need to draw depth image to create pixels
		canvas.setImage(depthImage);
		// Create the PixelGrabber imageConsumer to get pixel array
		PixelGrabber pg = new PixelGrabber(depthImage, 0, 0, imageWidth,
				imageHeight, depthPixels, 0, imageWidth);

		if (StereoMaker.debug) {
			System.out.println("Made PixelGrabber");
		}
		// depthPixel array is still empty at this point. 
		// Now actually put the pixels into the array
		try { // Need to give the image drawing thread time to make the 
			// pixel info. 
			// Need to find a better way to know directly when the data 
			// is ready.
			pg.grabPixels(1000);
		} catch (InterruptedException e) {
			System.err.println("interrupted waiting for pixels!");
			return;
		}
		if (StereoMaker.debug) {
			System.out.println("grabPixels called");
		}

		if ((pg.status() & ImageObserver.ABORT) != 0) {
			System.err.println("image fetch aborted or errored");
			return;
		}
		// By this point the depthPixel data has magically appeared in the array 
		// from the call to pg.grabPixel(1000) above.
		// Now remove the alpha values and keep only the blue pixel values for 
		// use as the depth values.
		index = 0;
		int totPrints = 0;
		int blue, rgb;
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				//Save only the blue component
				rgb = depthPixels[index];
				blue = (rgb) & 0xff;
				depthPixels[index] = blue;
				index++;
			}
		}
	}

	/*::::::::::::::::::::::::::::::: depthMap() :::::::::::::::::::::::::::::::*/
	/* Use Steer's formula for compressing the "height" range (i.e. the distances
	 * above the apparent back plane) to minDepth-maxDepth range of depths BELOW
	 * the screen plane.
	 */
	public synchronized int depthMap(int x, int y) {
		int d;
		d = maxDepth - Height(x, y) * (maxDepth - minDepth) / 256;
		depthIndex++;
		return (d);
	}

	/*::::::::::::::::::::::::::::::: Height() :::::::::::::::::::::::::::::::::*/
	/* This gives the height above the "apparent" image backplane.
	 */
	public synchronized int Height(int x, int y) {
		int h = 0;
		int blue = 0;

		if (depthIndex < totNumPixels)
			blue = depthPixels[depthIndex];
		h = blue / heightScale; // Allow scaling of full 255 height
		if (h < minSceneHeight)
			h = minSceneHeight;
		if (h > maxSceneHeight)
			h = maxSceneHeight;
		return (h);
	}

	/*::::::::::::::::::::::::::::::: makeStereoRds() ::::::::::::::::::::::::::*/
	/* Make the Stereogram here based on W. A. Steer's algorithm and code.
	 * This routing makes the stereogram from random colored. 
	 * In the controls this stereogram option is referred to as the RDS type.
	 */
	public synchronized void makeStereoRds() {
		if (StereoMaker.debug) {
			System.out.println("Starting makeStereo");
		}

		int lookL[] = new int[imageWidth];
		int lookR[] = new int[imageWidth]; //Look ahead for links to hidden points
		boolean vis;

		int numColors = 3;

		int color[] = new int[imageWidth];

		// Convert distances to pixel units
		int obsDist = xdpi * 12;
		int eyeSep = (int) (xdpi * 2.5);

		maxDepth = obsDist;
		minDepth = (int) ((sepFactor * maxDepth * obsDist) / ((1 - sepFactor)
				* maxDepth + obsDist));

		int featureZ, sep;
		int x, y, left, right;
		int rnd;
		if (StereoMaker.debug) {
			System.out.println("maxDepth= " + maxDepth + " minDepth="
					+ minDepth);
		}
		// Get the chosen RDS colors from dropdown lists
		if (rdsColors[2] == null)
			numColors = 2;

		int yold = -1;
		int oldsep = -1;
		int stepDone = imageHeight / 10;
		int percent;
		depthIndex = 0;
		index = 0;
		showStatus("Starting stereogram construction");
		for (y = 0; y < imageHeight; y++) {
			if ((y % stepDone) == 0) {
				float div = (float) (y) / (float) (imageHeight);
				percent = (int) ((100.0 * div) + 0.5);
				if (percent > 0)
					showStatus("Made " + percent + "% of stereogram");
			}

			for (x = 0; x < imageWidth; x++) {
				lookL[x] = x;
				lookR[x] = x;
			}

			for (x = 0; x < imageWidth; x++) {
				featureZ = depthMap(x, y);

				/* the multiplication below is 'long' to prevent overflow errors */

				sep = (int) (((long) eyeSep * featureZ) / (featureZ + obsDist));

				left = x - sep / 2;
				right = left + sep;
				vis = true; // // Default assumes point is visible to both eyes

				if ((left >= 0) && (right < imageWidth)) {
					if (lookL[right] != right) { // Right point already linked.
						if (lookL[right] < left) { // Old link is wider than new one
							// Prefer narrower links, i.e shallower points so
							lookR[lookL[right]] = lookL[right]; // break old links
							lookL[right] = right;
						} else
							vis = false;
					}
					if (lookR[left] != left) { // Left point already linked
						if (lookR[left] > right) { // Old link is wider than new one
							// Prefer narrower links, i.e shallower points so
							lookL[lookR[left]] = lookR[left]; // break old links
							lookR[left] = left;
						} else
							vis = false;
					}
					if (vis == true) {
						lookL[right] = left;
						lookR[left] = right; //Make linked pair
					}
				}
			}
			//Now fill the color array for this line according to the link pairing.
			for (x = 0; x < imageWidth; x++) {
				if (lookL[x] == x) {
					rnd = (int) (numColors * Math.random());
					if (rnd == numColors)
						rnd = numColors - 1;
					color[x] = rdsColors[rnd].getRGB(); /* unconstrained */
				} else
					color[x] = color[lookL[x]]; /* constrained */
			}
			// Finally, transfer one line at a time to the pixel array.
			for (x = 0; x < imageWidth; x++) {
				pixels[index++] = color[x];
			}
		}
		showStatus("Made 100% of the image");
	}

	/*::::::::::::::::::::::::::::::: getBackplaneImage() ::::::::::::::::::::::*/
	/* Load the images using exception code to give time to load and in case 
	 * images don't exist or have problems. Also, find the maximum dimensions 
	 * for resizing the java window.
	 */
	synchronized void getBackplaneImage() {
    int width=0,height=0;
    backplaneLoaded=false;
    /* First load the backplane image */
    showStatus("Fetching backplane image:"+backplaneImageName); 
    backplaneImage = getImage(getCodeBase(), backplaneImageName);
    tracker.addImage(backplaneImage,0);
    try{
        tracker.waitForID(0);
    }catch(InterruptedException e){
      if(debug) System.out.println("Fetch image"+backplaneImageName+e.getMessage());
      loadError("Fetch image error: "+e.getMessage());
      return;
    }
    backplaneLoaded=true;
  }

	/*::::::::::::::::::::::::::::::: getBackplaneInfo() :::::::::::::::::::::::*/
	/* Display the background image and then obtain the resulting 
	 * pixel arrays for use by makeStereoFromImage.
	 */
	public int getBackplaneInfo() {
		int status = 0;
		// Display the image on the canvas for 2 sec
		// Note that the iemage gets stretched and tiled to fit the frame.
		canvas.setImage(backplaneImage);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		backplaneWidth = backplaneImage.getWidth(this);
		backplaneHeight = backplaneImage.getHeight(this);
		totNumBackPlanePixels = backplaneWidth * backplaneHeight;
		backplanePixels = new int[totNumBackPlanePixels];

		// Create the PixelGrabber imageConsumer to get pixel array
		PixelGrabber pg = new PixelGrabber(backplaneImage, 0, 0,
				backplaneWidth, backplaneHeight, backplanePixels, 0,
				backplaneWidth);

		if (StereoMaker.debug) {
			System.out.println("Made PixelGrabber in getBackplaneImage");
		}
		// backgroundPixels array is still empty at this point. 
		// Now actually put the pixels into the array
		try { // Need to give the image drawing thread time to make the pixel 
			// info available. Don't know a way to determine directly when  
			// the data is ready.
			pg.grabPixels(1000);
		} catch (InterruptedException e) {
			System.err.println("interrupted waiting for pixels!");
			return (status = 1);
		}
		return (status);
	}

	/*::::::::::::::::::::::::::::::: getBackplanePixel() :::::::::::::::::::::*/
	public synchronized int getBackplanePixel(int x, int y) {
		int bpindex = y * backplaneWidth + x;
		if (bpindex < totNumBackPlanePixels)
			return (backplanePixels[bpindex]);
		else {
			if (StereoMaker.debug) {
				System.out.println("Error in bpindex for x=" + x + " y=" + y);
			}
			return (0);
		}
	}

	/*::::::::::::::::::::::::::::::: makeStereoFromImage() ::::::::::::::::::::*/
	/* Make the Stereogram here based on W. A. Steer's algorithm and code.
	 * This routine makes the stereogram from an image, called here the 
	 * backplane image. Image strip begins in center of stereogram and linking
	 * moves to the sides to give more balanced look.
	 * 
	 * Oversampling is used to improve resolution.
	 *
	 * In the controls this stereogram option is referred to as the IMAGE type.
	 */
	public synchronized void makeStereoFromImage() {
    if( StereoMaker.debug){
       System.out.println("Starting makeStereo");
    }
    int patHeight=backplaneHeight;

    int ydpi=75;
    int yShift=ydpi/16;

    int lastlinked;
    int vWidth=imageWidth*oversample;

    int lookL[]= new int[vWidth];
    int lookR[]= new int[vWidth];// Look ahead for links to hidden points
    boolean vis;

    int numColors=3;
    loControls.getSettings();
    int color[]=new int[vWidth]; 
    int colorPacked;

    int featureZ=0, sep=0;
    // Convert distances to pixel units
    int obsDist=xdpi*12;
    int eyeSep= (int)(xdpi*2.5);
    int veyeSep=eyeSep*oversample;

    maxDepth= obsDist;
    minDepth= 
      (int)((sepFactor*maxDepth*obsDist)/((1-sepFactor)*maxDepth+obsDist));
    int maxsep=
      (eyeSep*maxDepth)/(maxDepth+obsDist); //Pattern must be at least this wide.
    int vmaxsep=oversample*maxsep;

    // Will make stereogram from middle and work to left and right 
    // s= middle point - half the repeat width
    int s=vWidth/2-vmaxsep/2; 
    int poffset=vmaxsep-(s % vmaxsep);

    if( backplaneWidth < maxsep || backplaneHeight < imageHeight/2){
      showStatus("Backplane image too small");
      return; 
    }

    int x,y, xv,yv, left,right;
    int rnd;

    int yold=-1;
    int oldsep=-1;
    int stepDone=imageHeight/10;
    int percent;

    depthIndex=0;
    index=0;
    sep=0;
    showStatus("Starting stereogram construction"); 
    for(y=0; y < imageHeight; y++)
    {
      if( ( y % stepDone) == 0 ){
        float div = (float)(y)/(float)(imageHeight);
        percent = (int)((100.0*div)+0.5);
        if(percent>0)showStatus("Made "+percent+"% of stereogram"); 
      }
 
      for(x=0; x < vWidth; x++){
        lookL[x]=x; lookR[x]=x; 
      }

      for(x=0; x < vWidth; x++)
      {
        if ((x % oversample)==0) // True values only available 
        {                        // every oversample*x steps.
          featureZ=depthMap(x/oversample,y); 
          sep=(int)(((long)veyeSep*featureZ)/(featureZ+obsDist));
        }

        left=x-sep/2; right=left+sep;
        vis=true; // Default assumes point is visible to both eyes

        if((left>=0) && (right<vWidth)) 
        {  
          if(lookL[right]!=right){   // Right point already linked.
            if(lookL[right] < left){ // Old link is wider than new one
           // Prefer narrower links, i.e shallower points, so
               lookR[lookL[right]]=lookL[right]; // break old link.
               lookL[right]=right;
            }else 
              vis=false;
          }
          if(lookR[left]!=left){     // Left point already linked.
            if(lookR[left]>right){   // Old link is wider than new one
           // Prefer narrower links, i.e shallower points,so
              lookL[lookR[left]]=lookR[left]; // break old link
              lookR[left]=left;
            }else 
              vis=false;
          }
          if(vis==true) { 
            lookL[right]=left; lookR[left]=right; // Make linked pair
          }
        }
      }
      lastlinked=-1;
      // First move from center strip to the right.
      for(x=s; x < vWidth; x++)
      {
        if((lookL[x]==x) || (lookL[x]<s))// If this pixel not linked to a 
        {                                // left pixel or if left link is 
                                         // to left of S
           // Reduce "twinkling" effect of an isolated pixel with an out of
           // place color by making the neighbor have same color.
          if(lastlinked==(x-1)) 
             color[x]=color[x-1];
          else
          {  // poffset insures that xv begins at 0.
             xv=((x+poffset) % vmaxsep)/oversample;           
             // After n*vmaxsep distance, use the section of image n*yShift 
             // down in y. Using this new pattern avoids artificial artifacts
             // in the scene. 
             yv=(y+((x-s)/vmaxsep)*yShift) % patHeight;
             color[x]=getBackplanePixel( xv,yv);
          }
        } 
        else
        {
           color[x]=color[lookL[x]];// Make colors same for this pixel 
                                    // & its linked left pixel
           lastlinked=x; // Keep track of the last pixel to be constrained
        }
      }

      // Then move from center strip to the left.
      lastlinked=-1;
      for(x=s-1; x >= 0; x--)
      {
        if(lookR[x]==x) // If this pixel not linked to a right pixel
        {
          if(lastlinked==(x+1)) 
            color[x]=color[x+1];
          else
          { 
            xv=((x+poffset) % vmaxsep)/oversample;
            // After n*vmaxsep distance, use the section of image n*yShift 
            // down in y. Using this new pattern avoids artificial artifacts
            // in the scene. 
            yv=(y+((s-x)/vmaxsep+1)*yShift) % patHeight;
            color[x]=getBackplanePixel(xv,yv);
          }
        }
        else
        {
           color[x]=color[lookR[x]];// Make colors same for this pixel 
                                      // and its linked right pixel.
                                      // (This differs from Steer's paper 
                                      // where lookL[x] is used.)
           lastlinked=x; // keep track of the last pixel to be constrained
        }
      }
      // Now average the colors to reduce from virtual screen down to
      // the actual image size.
      int red, green, blue, rgb;
      int colorpacked=0;
      int alpha=255;
      for(x=0; x<vWidth; x+=oversample){
        red=0; green=0; blue=0;
        // Use average color of virtual pixels for screen pixel
        for(int i=x; i < (x+oversample); i++){
          rgb = color[i];
          red+=(rgb >> 16) & 0xff;
          green+=(rgb >>  8) & 0xff;
          blue+=rgb & 0xff;
        }
        colorPacked = (    ((alpha) << 24) |
                   ((red/oversample) << 16) |
                   ((green/oversample) << 8) |
                   ((blue/oversample) << 0)  );
        index= y*imageWidth + (x/oversample);
        pixels[index] = colorPacked;
      }
    } /* Main vertical loop */

    showStatus("Made 100% of the image");     
  }
}

/* End of StereoMaker class */

/************************* Class StereoCanvas *******************************/
class StereoCanvas extends Canvas {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6235286648960131487L;

	Applet app;

	Image img;

	static String calcString = "Calculating...";

	/*::::::::::::::::::::::::::::::: StereoCanvas() :::::::::::::::::::::::::::*/
	public StereoCanvas(StereoMaker a) {
		app = a;
	}

	/*::::::::::::::::::::::::::::::: paint() ::::::::::::::::::::::::::::::::::*/
	public synchronized void paint(Graphics g) {
		int w = size().width;
		int h = size().height;

		// First Draw frame around image

		int xframe = StereoMaker.frameThickness;
		int yframe = StereoMaker.frameThickness;

		int rectWidth = w - 2 * xframe;
		int rectHeight = h - 2 * yframe;
		Color bg = getBackground();
		Color fg = getForeground();
		g.setColor(bg);
		g.draw3DRect(0, 0, w - 1, h - 1, true);
		g.draw3DRect(3, 3, w - 7, h - 7, false);
		g.setColor(fg);

		if (img == null) {
			super.paint(g);
			g.setColor(Color.black);
			FontMetrics fm = g.getFontMetrics();
			int x = (w - fm.stringWidth(calcString)) / 2;
			int y = h / 2;
			g.drawString(calcString, x, y);// Message in screen
		} else {
			if (StereoMaker.debug) {
				System.out.println("Drawing image in canvas");
			}
			g.drawImage(img, xframe, yframe, rectWidth, rectHeight, this);
		}
	}

	/*::::::::::::::::::::::::::::::: minimumSize() ::::::::::::::::::::::::::::*/
	public Dimension minimumSize() {
		return new Dimension(600, 300);
	}

	/*::::::::::::::::::::::::::::::: preferredSize() ::::::::::::::::::::::::::*/
	public Dimension preferredSize() {
		return new Dimension(600, 300);
	}

	/*::::::::::::::::::::::::::::::: getImage() :::::::::::::::::::::::::::::::*/
	public Image getImage() {
		return img;
	}

	/*::::::::::::::::::::::::::::::: setImage() :::::::::::::::::::::::::::::::*/
	public void setImage(Image imag) {
		this.img = imag;
		if (StereoMaker.debug) {
			System.out.println("Repainting image");
		}
		repaint();
	}
}

/************************* Class StereoControlsHi ***************************/
class StereoControlsHi extends Panel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6595949601544053920L;

	TextField imageSet;

	int stereoType = StereoMaker.TYPE_IMAGE;

	StereoMaker app;

	Choice type;

	Choice imageChoices;

	Choice rdsCol0, rdsCol1, rdsCol2;

	/*::::::::::::::::::::::::::::::: StereoControlsHi() :::::::::::::::::::::::*/
	public StereoControlsHi(StereoMaker apple) {
		this.app = apple;
		type = new Choice();
		type.addItem("RDS");
		type.addItem("Image");
		add(type);

		rdsCol0 = new Choice();
		rdsCol0.addItem("red");
		rdsCol0.addItem("white");
		rdsCol0.addItem("gray");
		rdsCol0.addItem("darkGray");
		rdsCol0.addItem("black");
		rdsCol0.addItem("pink");
		rdsCol0.addItem("orange");
		rdsCol0.addItem("yellow");
		rdsCol0.addItem("green");
		rdsCol0.addItem("magenta");
		rdsCol0.addItem("cyan");
		rdsCol0.addItem("blue");
		add(rdsCol0);

		rdsCol1 = new Choice();
		rdsCol1.addItem("green");
		rdsCol1.addItem("white");
		rdsCol1.addItem("gray");
		rdsCol1.addItem("darkGray");
		rdsCol1.addItem("black");
		rdsCol1.addItem("red");
		rdsCol1.addItem("pink");
		rdsCol1.addItem("orange");
		rdsCol1.addItem("yellow");
		rdsCol1.addItem("magenta");
		rdsCol1.addItem("cyan");
		rdsCol1.addItem("blue");
		add(rdsCol1);

		rdsCol2 = new Choice();
		rdsCol2.addItem("blue");
		rdsCol2.addItem("white");
		rdsCol2.addItem("gray");
		rdsCol2.addItem("darkGray");
		rdsCol2.addItem("black");
		rdsCol2.addItem("red");
		rdsCol2.addItem("pink");
		rdsCol2.addItem("orange");
		rdsCol2.addItem("yellow");
		rdsCol2.addItem("green");
		rdsCol2.addItem("magenta");
		rdsCol2.addItem("cyan");
		rdsCol2.addItem("none");// Choosing none allows for 2 color RDS's
		add(rdsCol2);

		add(new Label("Image"));

		imageChoices = new Choice();
		imageChoices.addItem(app.backplaneImageName);
		imageChoices.addItem("dog.gif");
		imageChoices.addItem("redTexture.gif");
		imageChoices.addItem("basket.gif");
		add(imageChoices);

		add(new Button("Redraw"));
	}

	/*::::::::::::::::::::::::::::::: action() :::::::::::::::::::::::::::::::::*/
	public boolean action(Event ev, Object arg) {
		if (ev.target instanceof Choice) {
			String choice = (String) arg;
			if (choice.equals("Image"))
				app.stereoType = StereoMaker.TYPE_IMAGE;
			else if (choice.equals("RDS"))
				app.stereoType = StereoMaker.TYPE_RDS;
		} else if (ev.target instanceof Button) {
			app.restart();
		}
		return true;
	}

	/*::::::::::::::::::::::::::::::: getsettings() ::::::::::::::::::::::::::::*/
	/* Get the values from the top controls and pass to the StereoMaker object
	 */
	public void getSettings() {
		Color colorPicks[] = { Color.blue, Color.white, Color.gray,
				Color.darkGray, Color.black, Color.red, Color.pink,
				Color.orange, Color.yellow, Color.green, Color.magenta,
				Color.cyan };
		String colorNames[] = { "blue", "white", "gray", "darkGray", "black",
				"red", "pink", "orange", "yellow", "green", "magenta", "cyan" };

		String str0 = rdsCol0.getSelectedItem();
		for (int i = 0; i < 12; i++) {
			if (colorNames[i].equals(str0)) {
				app.rdsColors[0] = colorPicks[i];
				break;
			}
		}
		String str1 = rdsCol1.getSelectedItem();
		for (int i = 0; i < 12; i++) {
			if (colorNames[i].equals(str1)) {
				app.rdsColors[1] = colorPicks[i];
				break;
			}
		}
		String str2 = new String(rdsCol2.getSelectedItem());
		if (!str2.equals("none")) {
			for (int i = 0; i < 12; i++) {
				if (colorNames[i].equals(str2)) {
					app.rdsColors[2] = colorPicks[i];
					break;
				}
			}
		} else
			app.rdsColors[2] = null;
		app.backplaneImageName = new String(imageChoices.getSelectedItem());

		if (type.getSelectedItem().equals("Image"))
			app.stereoType = StereoMaker.TYPE_IMAGE;
		else
			app.stereoType = StereoMaker.TYPE_RDS;
	}
}

/************************* Class StereoControlsLo ***************************/
class StereoControlsLo extends Panel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6823547350998079901L;

	TextField sepFactorSet;

	TextField dpiSet;

	TextField inputSet;

	TextField charSizeSet;

	TextField oversampleSet;

	StereoMaker app;

	/*::::::::::::::::::::::::::::::: StereoControlsLo() ::::::::::::::::::::::::*/
	public StereoControlsLo(StereoMaker apple) {

		this.app = apple;
		add(new Label("Input:"));
		add(inputSet = new TextField(app.input, 6));
		add(new Label("Sep Factor:"));
		add(sepFactorSet = new TextField(Double.toString(app.sepFactor), 4));
		add(new Label("dpi:"));
		add(dpiSet = new TextField(Integer.toString(app.xdpi), 4));
		add(new Label("Char Size:"));
		add(charSizeSet = new TextField(Integer.toString(app.charSize), 5));
		add(new Label("Oversample:"));
		add(oversampleSet = new TextField(Integer.toString(app.oversample), 2));
	}

	/*::::::::::::::::::::::::::::::: getsettings() 
	 ::::::::::::::::::::::::::::::*/
	/* Get the values from the bottom controls and pass to the StereoMaker applet.
	 */
	public void getSettings() {
		app.input = inputSet.getText().trim();
		app.sepFactor = Double.valueOf(sepFactorSet.getText().trim())
				.doubleValue();
		app.xdpi = Integer.parseInt(dpiSet.getText().trim());
		app.charSize = Integer.parseInt(charSizeSet.getText().trim());
		app.oversample = Integer.parseInt(oversampleSet.getText().trim());
	}
}
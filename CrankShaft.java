//
//  CrankShaft.java
//  
//
//  Created by Mark Williamsen on Thu Oct 24 2002.
//  Added JSlider controls April 10, 2012 MSW.
//

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;

public class CrankShaft extends Applet implements Runnable
{
	// applet instance variables
	CrankCanvas theCanvas;
	CrankPanel thePanel;
	Thread timer;  // animation timer
	volatile boolean running;  // flag to enable timer thread	
	int adder;  // animation increment
	int scale;  // scale factor

	// one time initialization
	public void init()
	{
        // set up user interface
		setLayout(new FlowLayout(FlowLayout.LEFT));
		theCanvas = new CrankCanvas(this);
		add(theCanvas);
		thePanel = new CrankPanel(this);
		add(thePanel);
	}
	
	// start animation when applet is loaded
	public void start()
	{
		// create new timer
		if (timer == null){timer = new Thread(this);}
		
		// start timer running
		running = true;
		timer.start();
	}
	
	// stop animation when applet is unloaded
	public void stop()
	{
		// allow thread to exit
		running = false;
	}
	
	// implement timer thread, 100 msec intervals
	public void run()
	{
		while(running) if(thePanel != null)
		{
			adder = thePanel.theSpeed.getValue();
			scale = thePanel.theScale.getValue();
			theCanvas.animate();
			theCanvas.paint();
			try {Thread.sleep(100);}  // let other apps run
			catch (InterruptedException ie) {return;}
		}
		timer  = null;
	}
    
	public String getAppletInfo()
	{
		return "CrankShaft Applet, ver. 1.1, M. Williamsen, April 12, 2012";
	}
}

// class to implement 2D affine transform, needed for Java 1.0 browsers
class Affine
{
	double m11, m12, m13;
	double m21, m22, m23;
	double m31, m32, m33;
	
	// default constructor gives identity matrix
	Affine()
	{
		m11 = 1.0; m12 = 0.0; m13 = 0.0;
		m21 = 0.0; m22 = 1.0; m23 = 0.0;
		m31 = 0.0; m32 = 0.0; m33 = 1.0;
	}
	
	// return a translation transform matrix
	// input x and y in double float pixels
	public static Affine createTranslate(double x, double y)
	{
		Affine t = new Affine();
		t.m13 = x;
		t.m23 = y;
		return t;
	}
	
	// return a rotation transform matrix
	// input rotation in floating point radians
	public static Affine createRotate(double w)
	{
		Affine r = new Affine();
		r.m11 = Math.cos(w);
		r.m21 = Math.sin(w);
		r.m12 = -r.m21;
		r.m22 =  r.m11;
		return r;
	}
	
	// return a scale transform matrix
	// input scale factor as a double
	public static Affine createScale(double f)
	{
		Affine s = new Affine();
		s.m11 = f;
		s.m22 = f;
		return s;
	}
	
	// concatenate two transform matrices
	// returning a new transform matrix
	public Affine Concat(Affine a)
	{
		Affine b = new Affine();
		
		// perform matrix multiplication
		b.m11 = m11*a.m11 + m12*a.m21 + m13*a.m31;
		b.m12 = m11*a.m12 + m12*a.m22 + m13*a.m32;
		b.m13 = m11*a.m13 + m12*a.m23 + m13*a.m33;

		b.m21 = m21*a.m11 + m22*a.m21 + m23*a.m31;
		b.m22 = m21*a.m12 + m22*a.m22 + m23*a.m32;
		b.m23 = m21*a.m13 + m22*a.m23 + m23*a.m33;
		
		b.m31 = m31*a.m11 + m32*a.m21 + m33*a.m31;
		b.m32 = m31*a.m12 + m32*a.m22 + m33*a.m32;
		b.m33 = m31*a.m13 + m32*a.m23 + m33*a.m33;

		return b;
	}
	
	// returns a new polygon, transformed from input polygon
	public Polygon Transform(Polygon p)
	{
		Polygon q = new Polygon();
		int index;
		double x1, y1, x2, y2;
		for (index = 0; index < p.npoints; index++)
		{
			// perform affine transformation
			x1 = p.xpoints[index];
			y1 = p.ypoints[index];
			x2 = m11*x1 + m12*y1 + m13*1.0;
			y2 = m21*x1 + m22*y1 + m23*1.0;
			q.addPoint((int)x2, (int)y2);
		}
		return q;
	}
}

class CrankCanvas extends Canvas
{
	Image offscr;  // offscreen bitmap
	int index;
	Polygon crank;
	Polygon rod;
	Polygon piston;
	CrankShaft parent;
	final int WIDTH = 360;
	final int HEIGHT = 200;
	final int RADIUS = 40;
	
	// constructor with one param, a reference to parent applet
	CrankCanvas(CrankShaft c)
	{
		// allocate offscreen image bitmap
		parent = c;
		index = 0;
		offscr = parent.createImage(WIDTH, HEIGHT);
		
		// define outline of crank (a triangle)
		crank = new Polygon();
		crank.addPoint(-30, -40);
		crank.addPoint( 40,   0);
		crank.addPoint(-30,  40);
		crank.addPoint(-30, -40);
		
		crank.addPoint(  0,   0);
		crank.addPoint(-30,  40);

		// define outline of rod (a quadrilateral)
		rod = new Polygon();
		rod.addPoint(-10, -10);
		rod.addPoint(130,  -3);
		rod.addPoint(130,   3);
		rod.addPoint(-10,  10);
		rod.addPoint(-10, -10);
		
		rod.addPoint(  0,   0);
		rod.addPoint(-10,  10);
		
		// define outline of piston
		piston = new Polygon();
		piston.addPoint( 30, -20);
		piston.addPoint( 30,  20);
		piston.addPoint(-20,  20);
		piston.addPoint(-20, -20);
		piston.addPoint( 30, -20);

		piston.addPoint( 20, -20);
		piston.addPoint( 20,  20);
		piston.addPoint( 30,  20);
	}

	// tell layout manager what size to render canvas
	public Dimension getPreferredSize()
		{return new Dimension(WIDTH, HEIGHT);}
	
	// perform animation
	void animate()
	{		
		Affine r, s, t, u;
		Polygon a, b, c;
		
		// fill white to clear bitmap
		Graphics g = offscr.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		
		// draw x and y axes
		g.setColor(Color.lightGray);
		g.drawLine(0, 100, WIDTH, 100);
		g.drawLine(100, 0, 100, HEIGHT);

		//-----------------------------------
		// obtain scale factor for animation
		// arrange to scale about the point (100,100)
		u = Affine.createTranslate(-100,-100);
		r = Affine.createScale(parent.scale / 100.0);
		t = Affine.createTranslate(100, 100);
		s = t.Concat(r.Concat(u));
		
		// set up crank transform
		double w = index / 180.0 * Math.PI;
		r = Affine.createRotate(w);
		t = Affine.createTranslate(100, 100);
		u = s.Concat(t.Concat(r));
		
		// apply transform to crank
		a = u.Transform(crank);
		
		// set up rod transform
		double x1 = 100.0 + RADIUS * Math.cos(w);
		double y1 = 100.0 + RADIUS * Math.sin(w);
		t = Affine.createTranslate(x1, y1);
		double v = -Math.asin(RADIUS / 120.0 * Math.sin(w));
		r = Affine.createRotate(v);
		u = s.Concat(t.Concat(r));
		
		// apply transform to rod
		b = u.Transform(rod);
		
		// set up piston transform
		double x2 = 100.0 + RADIUS * Math.cos(w) + 120.0 * Math.cos(v);
		double y2 = 100.0;
		t = Affine.createTranslate(x2, y2);
		u = s.Concat(t);
		
		// apply transform to piston
		c = u.Transform(piston);
		
		//-----------------------------------
		// set up translation for drop shadow
		t = Affine.createTranslate(5, 5);
		g.setColor(Color.gray);
		g.fillPolygon(t.Transform(a));
		g.fillPolygon(t.Transform(b));
		g.fillPolygon(t.Transform(c));
		
		//-----------------------------------
		// draw crank
		g.setColor(Color.green);
		g.fillPolygon(a);
		g.setColor(Color.black);
		g.drawPolygon(a);

		// draw rod
		g.setColor(Color.red);
		g.fillPolygon(b);
		g.setColor(Color.black);
		g.drawPolygon(b);
		
		// draw piston
		g.setColor(Color.blue);
		g.fillPolygon(c);
		g.setColor(Color.black);
		g.drawPolygon(c);
		
		// draw enclosing rectangle border
		g.setColor(Color.black);
		g.drawRect(0, 0, WIDTH-1, HEIGHT-1);
		
		// bump index
		int adder = parent.adder;
		index += adder;
		if (index >= 360){index -= 360;}
	}

	// copy offscreen content to onscreen canvas
	public void paint()
		{paint(getGraphics());}
		
	public void paint(Graphics g)
	{
		// copy offscreen image to applet window
		g.drawImage(offscr, 0, 0, this);
	}
}

// control panel, to hold sliders and text labels
class CrankPanel extends Panel
{
	JSlider theSpeed;
	JSlider theScale;
	Label speedLabel;
	Label scaleLabel;
	CrankShaft parent;
	final int WIDTH = 200;
	final int HEIGHT = 200;
	
	// tell layout manager what size to render control panel
	public Dimension getPreferredSize()
		{return new Dimension(WIDTH, HEIGHT);}
	
	// constructor with one param, a reference to parent applet
	CrankPanel(CrankShaft c)
	{
        // setup up user interface
		parent = c;
		setBackground(Color.lightGray);
		setFont(new Font("Helvetica", Font.PLAIN, 11));
		speedLabel = new Label("Angular Velocity", Label.LEFT);
		scaleLabel = new Label("Scale Factor", Label.LEFT);
		theScale = new JSlider(20, 200, 100);
		theSpeed = new JSlider(-50, 50, 5);
		add(speedLabel);
		add(theSpeed);
		add(scaleLabel);
		add(theScale);
        
        // set up tick marks
        theScale.setMajorTickSpacing(60);
        theScale.setMinorTickSpacing(10);
        theScale.setPaintTicks(true);
        theSpeed.setMajorTickSpacing(25);
        theSpeed.setMinorTickSpacing(5);
        theSpeed.setPaintTicks(true);
	}
}

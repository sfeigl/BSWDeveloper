package de.brettspielwelt.game.cabanga;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Vector;

import de.Data;
import de.Vect;
import de.brettspielwelt.develop.HTMLWrapper;
import de.brettspielwelt.tools.IntVector;


public class Board extends HTMLWrapper
{
	private Font fontDefault,threeFont,fourFont,fontLarge;
	private String[] localStrings;
	static int[] wertung= {0,1,1,1,2,2,2,3,3,3,3,3,3,2,2,2,1,1,1};
	private Vector history=new Vector();
	boolean rep=false;
	int animation=0;
	
	int mouseMoveX=0, mouseMoveY=0;
	int mouseDownX=0, mouseDownY=0;

	int[] cols= {0xf080f0,0xf0f080,0xe0e0a0,0xf0a0a0,0xa0f0a0,0xa0a0f0};
	
	private String[] spielerName={"","","","","",""};
	boolean iAmPlaying = false;
	int anzSpieler = 0;
	int iAmId = -1;
	
	int[][] feld=new int[4][2];
	IntVector hand=new IntVector();
	IntVector cabanga=new IntVector();
	IntVector cabSel=new IntVector();
	Rectangle[] handRect=new Rectangle[32];
	Rectangle[] zuwerfRect=new Rectangle[6];
	int[] handSize=new int[6];
	int[] punkte=new int[6];
	int[] platz=new int[6];
	
	int handSel=-1;
	
	int currentPlayer=-1,startSpieler=-1;
	int phase=0,round=0,lastCol=-1,endPhase2=0;
	int cabangaC=-1;
	
	Image[] baseImg;
	
	public void initer() {
		zuwerfRect=new Rectangle[6];
	}
	
	// ------------------------- Init Stuff - Loading Strings for localization / Fonts / Images ---------------------
	@Override
	public void init(int lev, Runnable run) {
		if(lev==0){
			localStrings = getStringsLocal("t", 1);
			
			registerFont("BodegaSans-Medium.ttf");
			
			fourFont = getFontLocal("fourFont", 1);
			threeFont = getFontLocal("threeFont", 1);
			fontDefault = getFontLocal("defaultFont", 1);
			fontLarge = getFontLocal("largeFont", 1);
			
			initPics();
		}
		if(lev==1){
			getSoundPack(null, new String[] { "cabanga.wav", "takecard.wav","dropcard.wav"});
		}
	}
	
	static final int  MASKS=7, NUMBERS=MASKS+4, SCHRIFT=NUMBERS+11, PLAYER=SCHRIFT+8;
	public void initPics(){
		String[] imageNames = {
				"bg.png", 
				"b0.png","b1.png","b2.png","b3.png","b7.png","rs.png",
				"m0.png","m1.png","m2.png","m3.png",
				"0.png","1.png","2.png","3.png","4.png","5.png","6.png","7.png","8.png","9.png",
				"wert.png",
				"c.png","a1.png","b.png","a2.png","n.png","g.png","a3.png","ruf.png",
				"1card.png","2cards.png","3cards.png","4cards.png","federn.png","stapel.png"
		};
		
		baseImg = new Image[imageNames.length];
		for(int i=0; i<imageNames.length; i++){
			baseImg[i] = getImageLocal(imageNames[i]);
		}
	}	
	
	// ---------------- Get the Data from the Informer into local variables ---------------
	public void getBoard(Vect v){
		if(v.size()>3) {
			int c=0;
			
			anzSpieler=((Integer)v.elementAt(c++)).intValue();
			iAmId=((Integer)v.elementAt(c++)).intValue();
			phase=((Integer)v.elementAt(c++)).intValue();
			currentPlayer=((Integer)v.elementAt(c++)).intValue();
			startSpieler=((Integer)v.elementAt(c++)).intValue();
			lastCol=((Integer)v.elementAt(c++)).intValue();
			endPhase2=((Integer)v.elementAt(c++)).intValue();
			feld=((int[][])v.elementAt(c++));
			hand=((IntVector)v.elementAt(c++));
			cabanga=((IntVector)v.elementAt(c++));
			handSize=((int[])v.elementAt(c++));
			punkte=((int[])v.elementAt(c++));
			platz=((int[])v.elementAt(c++));
			
			if(cabanga.size()!=cabSel.size()) {
				cabSel=new IntVector(cabanga.getArray(),1);
			}
			int[] anim=((int[])v.elementAt(c++));
			handleAnim(anim);
			
			iAmPlaying=(currentPlayer==iAmId);
			repaint();
		}else {
			if(v.size()==1){
				int[] anim=((int[])v.elementAt(0));
				handleAnim(anim);
			}
		}
	}
	

	private void handleAnim(int[] anim) {
		if(anim==null || anim.length==0) return;
		if((anim[0]>>24&7)-1==iAmId) return;
		if((anim[0]>>24&7)==7) {
			if(anim[1]==0) {
				playSound(0);
				cabangaC=0;
			}
			if(anim[1]==1)
				lastColC=0;
			return;
		}
		animationArr=anim;
		animation=1;

	}
	
	int lastColC=-1;
	
	// --------------- Update Loop Triggers every 40ms to have 25 frames/sec ----------------
	public void run() {
		if(cabangaC>-1) {
			cabangaC++;
			if(cabangaC>100) cabangaC=-1;
			rep=true;
		}
		if(lastColC>-1) {
			lastColC++;
			if(lastColC>64) lastColC=-1;
			rep=true;
		}
		if(history!=null && history.size()>0 && animation==0 && cabangaC==-1) { 	// Check if we got new Data from the Informer
			getBoard((Vect)history.elementAt(0));	// And work with it.
			history.removeElementAt(0);
			rep=true;
			
			if(animation==1 && animationArr!=null && animationArr.length>0) {
				flying=new int[animationArr.length/5];
				fsP1=new int[animationArr.length/5];
				fsP2=new int[animationArr.length/5];
				fsPT1=new int[animationArr.length/5];
				fsPT2=new int[animationArr.length/5];
				fp=new Point[flying.length];
				fpt=new Point[flying.length];
				
				for(int i=0; i<flying.length; i++) {
					flying[i]=animationArr[i*5];
					fsP1[i]=animationArr[i*5+1];
					fsP2[i]=animationArr[i*5+2];
					fsPT1[i]=animationArr[i*5+3];
					fsPT2[i]=animationArr[i*5+4];
					fp[i]=getPosition(animationArr[i*5+1],animationArr[i*5+2]);
					fpt[i]=getPosition(animationArr[i*5+3],animationArr[i*5+4]);
					//System.out.println(flying[i]+" from "+fp[i]+" to "+fpt[i]);
					int wtp=(flying[i]>>8)&255; // What starts flying
					if(wtp==0){
						playSound(2); }
				}
				jpc=0;
			}else { animation=0; rep=false; }
		}
		if(animation==1) {
			jpc++;
			if(jpc>=animLength){
				int w=(jpc-animLength);
				if(w%animDelta==0) {
					int wtp=(flying[w/animDelta]>>8)&255; // What starts flying
//					if(wtp==0){
//						playSound(2); }
				}
			}
			if(jpc==animLength+(flying.length-1)*animDelta) {
				animation=0; rep=false;
				int wtp=(flying[0]>>8)&255;  // What ends flying
				if(wtp==0) playSound(1);
			} 
		}
		
		if(rep || animation>0) {
			rep=false;
			repaint();
		}
		
	}
	
	// -------------- Receiving and Sending Data to and from the Server -----------
	
	public void getNioData(int typ, Data dat){
		int c=0;
		
		switch(typ){	// Put received data from the Server in a queue (history)
		case 700:
			history.addElement(dat.v);
			break;
		case 701:
			history.addElement(dat.v);
			break;
			
		case 702:
			for(int i=0; i<4; i++)
				spielerName[i]=(String)dat.v.elementAt(c++);
			repaint();
			break;
			
		case 703:
			history.addElement(dat.v);
			break;
			
		}
	}
	
	public synchronized void sendAction(int power, int act){
		Data dat=makeData(700);
		dat.v.addElement(new Integer((power<<28)|act));
		sendDataObject(dat);
	}
	public synchronized void sendAction(int power, int act, IntVector marks){
		Data dat=makeData(700);
		dat.v.addElement(new Integer((power<<28)|act));
		dat.v.addElement(marks);
		sendDataObject(dat);
	}
	
	// --------------- Mouse / Touch Actions ------------------
	
	int rco(int v) {   // Dummy Helper for BSWDeveloper to get real coordinates of mouse actions.
		return v*1220/getWidth();
	}
	@Override
	public void mouseMoved(MouseEvent ev) {
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		mouseMoveX=x; mouseMoveY=y;
		
		ev.consume();
	}
	
	public void mouseReleased(MouseEvent ev) {
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		
		if(handSel>-1) {
			int ca=hand.elementAt(handSel);
			int toR=0;
			int col=x/270;
			if(y<613) toR=1;
			if(y<260) toR=0;
			if((ca>>5&7)==col&& y<600) {
				sendAction(1,handSel|toR<<8);
				playSound(1);
			}
		}
		
		
		handSel=-1;
		repaint();
	}
	
	public void mousePressed(MouseEvent ev) {
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		mouseDownX=x; mouseDownY=y;
		
		
		for(int i=0; i<handRect.length; i++) {
			if(handRect[i]!=null &&handRect[i].contains(x,y)) {
				if(phase==2) {
					if(cabSel.contains(i)) cabSel.removeElement(i);
					else if(cabanga.contains(i)) cabSel.addElement(i);
				}else {
					handSel=i;
					playSound(2);
				}
			}
		}
		for(int i=0; i<anzSpieler; i++)
			if(zuwerfRect[i]!=null && zuwerfRect[i].contains(x,y)) {
				sendAction(2,0,cabSel);
				endPhase2|=1<<iAmId;
			}
		System.err.println(x+"+"+y);
		repaint();
		ev.consume();
	}
	
	int lastY=0;
	
	@Override
	public void mouseDragged(MouseEvent ev){
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		mouseMoveX=x; mouseMoveY=y;

		System.err.println("lastY-y:"+(lastY-y));
		if(y>700 && lastY-y<2) {
			for(int i=handRect.length-1; i>=0; i--) {
				if(handRect[i]!=null &&handRect[i].contains(x,y)) {
					handSel=i; break;
				}
			}
		}
		lastY=y;
		repaint();
	}
	
	public boolean immediateDrag(){
		return true;
	}

	// ----------------- Drawing the Board ------------------
	
	public void drawNumber(Graphics2D g, int nwert, int x, int y, double sc) {
		int wert=nwert;
		if(nwert<0) wert=-nwert;
		save(g);
		g.translate(x,y);
		g.scale(sc,sc);
		if(wert>9) {
			g.drawImage(baseImg[(wert/10)+NUMBERS], -150,-140,null);
			g.drawImage(baseImg[(wert%10)+NUMBERS], 0,-140,null);
			if(nwert<0) {
				g.setColor(Color.black);
				g.fillRect(-198-8, 0-6, 55+16, 25+12);
				g.setColor(Color.white);
				g.fillRect(-198, 0, 55, 25);
			}
		}else {
			g.drawImage(baseImg[(wert)+NUMBERS], -75+(nwert<0?12:0),-140,null);
			if(nwert<0) {
				g.setColor(Color.black);
				g.fillRect(-198-8+75, 0-6, 55+16, 25+12);
				g.setColor(Color.white);
				g.fillRect(-198+75, 0, 55, 25);
			}
		}
		restore(g);
	}
	public Rectangle drawCard(Graphics2D g, int ca, int x, int y, int w, boolean hand) {
		int h=w*389/250;

		double sc=(double)w/250.0;
		int c=ca>>5&7;
		if(ca==0) {
			drawImage(g,baseImg[6], x-w/2,y-h/2,w);
			return new Rectangle(x-w/2,y-h/2,w,h);
		}
		if(c==7) c=4;
		save(g);
		g.translate(x, y);
		g.scale(sc, sc);
		int va=ca&31;
		g.drawImage(baseImg[c+1], -125,-195, null);
		drawNumber(g,va,0,0, 0.7);
		if(hand) {
			drawNumber(g,va,-90+(va>=10?10:0),-148, 0.2);
			int anz=wertung[ca&31];
			for(int i=0; i<anz; i++)
				g.drawImage(baseImg[NUMBERS+10], -anz*20+i*40,+133, null);
		}
		restore(g);
		return new Rectangle(x-w/2,y-h/2,w,h);
	}
	public void drawMask(Graphics2D g, int m, int x, int y, double scale, int rot) {
		save(g);
		g.translate(x,y);
		g.scale(scale, scale);
		g.rotate(rot*Math.PI/180.0);
		g.drawImage(baseImg[m+MASKS], -125,-200,null);
		restore(g);
	}
	public void paintp(Graphics g) {
		
		initer();
		
		int iId=iAmId;
		if(iId==-1) iId=0;
		
		try {
			Graphics2D backG=(Graphics2D)getOffScreenGraphics();
			backG.setColor(new Color(0x010039));			
			backG.fillRect(0, 0, 1220, 784);
			backG.setColor(Color.white);
			backG.setFont(fontDefault);

			backG.drawImage(baseImg[0], 0,0, null);
			if((iAmPlaying && phase==1) || (phase==2 && (endPhase2>>iAmId&1)==0)  ) {
				backG.drawImage(baseImg[PLAYER+4], 444, 200,null);
			}
			backG.drawImage(baseImg[PLAYER+5], 444, 200,null);
			
			save(backG);
			backG.translate(444+36+60,0);
			backG.rotate(Math.PI);
			drawImage(backG,baseImg[PLAYER+4], -60, -80, 120);
			restore(backG);
			drawNumber(backG,punkte[iId], 546, 32, 0.19);
			
			for(int i=0; i<anzSpieler-1; i++) {
				int sp=((iAmId+1)+i)%anzSpieler;
				save(backG);
				backG.translate(1080, 4+i*156);
				
				backG.setColor(new Color(cols[sp]>>16&255,cols[sp]>>8&255,cols[sp]>>0&255,sp==currentPlayer?200:120));
				backG.fillRoundRect(2, 6, 300, 144, 32, 32);
				backG.setColor(Color.white);
				if(handSize[sp]>0) {
					backG.drawImage(baseImg[PLAYER+(handSize[sp]>3?3:(handSize[sp]-1))], 15,-4, null);
					drawNumber(backG,handSize[sp], 62-4, 30-4, 0.15);
				}
				drawNumber(backG,punkte[sp], 53, 92, 0.22);
				if(sp==currentPlayer) {
					backG.setColor(Color.black);
					backG.fillRect(12-1, 130-1, 84+2, 6+2);
					backG.setColor(Color.white);
					backG.fillRect(12, 130, 84, 6);
				}
				if(phase==2 && (endPhase2>>sp&1)==0) {
					backG.setColor(Color.black);
					backG.fillOval(53-9, 126-1, 16+2, 16+2);
					backG.setColor(Color.white);
					backG.fillOval(53-8, 126, 16, 16);
				}
				if(phase==2 && iAmId>-1 && (endPhase2>>iAmId&1)==0) {
					if(cabanga.size()>0 && sp==currentPlayer && animation==0) {
						for(int j=0;j<cabSel.size(); j++)
							drawCard(backG,hand.elementAt(cabSel.elementAt(j)),0+16*j,80,60,false);
						zuwerfRect[sp]=new Rectangle(1080, 40+i*156,160,156);
					}
				}
				backG.rotate(-Math.PI/2);
				drawSizedBorderdString(backG,Color.white,Color.black,spielerName[sp], -78,130, 156, true);
				restore(backG);
			}

			backG.setFont(fontDefault);
//			if(phase==0)
//				for(int i=0; i<anzSpieler; i++)
//					backG.drawString(spielerName[i], 10, 40+i*40);
			
			if(phase>0) {
				for(int i=0; i<4; i++) {
					Point p=getPosition(1,i);
					drawCard(backG, feld[i][0], p.x,p.y,140, false);
					p=getPosition(2,i);
					drawCard(backG, feld[i][1], p.x,p.y,140, false);
					int lc=lastColC;
					if(lc<0 || lastCol!=i) lc=0;
					
					drawMask(backG, i, i*270+135,290, lastCol==i?0.8:0.5, (int)(Math.sin((double)lc*Math.PI/8.0)*(64-lc)));
				}
				handRect=new Rectangle[hand.size()];
				int wi=132;
				if(hand.size()>8) wi=(1080-132)/(hand.size()-1);
				int totalW=wi*hand.size();
				int hx=540-totalW/2;
				if(hand.size()>8) hx=0;
				for(int i=0; i<hand.size(); i++) {
					boolean inCabanga=cabanga.contains(i);
					boolean inCabSel=cabSel.contains(i);
					if((endPhase2>>iAmId&1)==1 || animation==1) inCabanga=inCabSel=false;
					if(i!=handSel) {
						handRect[i]=drawCard(backG, hand.elementAt(i), hx+i*wi+ 72, 690-(inCabanga?20:0)-(inCabSel?40:0), 130, true);
					}
					else handRect[i]=new Rectangle(hx+i*wi+ 72-75, 690-(inCabanga?20:0)-(389/2), 130,400);
				}
				if(handSel>-1)
					drawCard(backG, hand.elementAt(handSel), mouseMoveX, mouseMoveY-84, 130, true);
				
//				if(iAmPlaying && (phase==1 || phase==2)) {
//					backG.fillOval(540, 300, 50, 50);
//				}
//				backG.drawString("Running game!", 10, 40);
//				backG.drawString("CurrentPlayer:"+currentPlayer, 10, 80);
//				backG.drawString("My Id:"+iAmId+" - so I am playing: "+iAmPlaying, 10, 120);
			}
			
			if(cabangaC>-1) {
				int[] cbx= {0,160,293,426,561,683,825,958,1060};
				for(int i=0; i<8; i++) {
					save(backG);
					int wi=cbx[i+1]-cbx[i];
					backG.translate(cbx[i]+10+wi/2,(int)ease2(cabangaC-i*2,-300,200,40));
					backG.rotate(Math.sin((cabangaC+i*40)*Math.PI/32.0)*0.3);
					backG.drawImage(baseImg[SCHRIFT+i], -wi/2,-200, null);
					restore(backG);
				}
			}
			
			if(animation==1) {
				for(int i=0; i<flying.length; i++) {
					int vx=fpt[i].x-fp[i].x;
					int vy=fpt[i].y-fp[i].y;
					int rjpc=jpc-i*animDelta;
					if(rjpc>animLength) rjpc=animLength;
					if(rjpc>=0 && rjpc<=animLength)
						drawObj(backG,flying[i],fp[i].x+vx*rjpc/animLength,fp[i].y+vy*rjpc/animLength,jiy[rjpc*16/animLength],rjpc,
								fsP1[i],fsP2[i],fsPT1[i],fsPT2[i]);
				}
			}

		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	// Animation part ---------------- Copy here ----------------------

	boolean noResponse=false;
	int animSleep=50, animLength=16, animDelta=animLength/4;
	int jpx,jpy,jpvx,jpvy,jpc;
	int[] jiy= {0,4,6,8,10,12,13,14,14,13,12,10,8,6,4,0,0,0,0};
	int[] jishrink= {255,240,220,200,180,160,140,120,100,80,60,40,40,40,40,40,40,40,40};
	//int[] jiy= {0,2,4,5,6,7,8,9,10,11,12,13,13,14,14,14,14,13,13,12,11,10,9,8,7,6,5,4,2,0,0,0,0,0};
	int[] animationArr;
	int[] flying,fsP1,fsP2,fsPT1,fsPT2;
	Point[] fp,fpt;
	boolean flyingToMe=false;
	
	public Point getPosition(int a, int b) {
		if(a==0) { // spieler
			int sp=b&15;
			if(sp==iAmId || anzSpieler==0) return new Point(540,784);
			int spp=(sp+anzSpieler-iAmId-1)%anzSpieler;
			return new Point(1140,30+spp*156);
		}
		if(a==1) { // feld top
			return new Point(b*270+135,120);
		}
		if(a==2) { // feld bottom
			return new Point(b*270+135,460);
		}
		if(a==10) {
			return new Point(538,284);
		}
		return new Point(0,0);
	}
	
	int[] posesStart={30,-10, -10,0, 0,30, 30,30};
	
	public void drawObj(Graphics2D g, int obj, int x, int y, int h, int ti) {
		drawObj(g, obj, x, y, h, ti, 0,0,0,0);
	}
	public void drawObj(Graphics2D g, int obj, int x, int y, int h, int ti, int p1,int p2, int pt1, int pt2) {
		int objKind=(obj>>8)&255;
		int objTyp=obj&255;
		if(objKind==0) {  // Karte (col 0-3) (1-5)
			int startW=50;
			int endW=50;
			
			if(p1==0 && (p2&15)==iAmId) startW=140;
			if(pt1==0 && (pt2&15)==iAmId) endW=140;
			if(pt1==10 || pt1==1 || pt1==2) endW=140;
			int wi=(int)(ease(ti,startW,endW,16));
			
			drawCard(g,objTyp,x,y,wi,false);
		}
	}
	int[] jiyD= {12,14,16,18,14,10,7,4,0,3,4,2,0,2,3,2,0,0,0};

	
	
	public void drawLocObject(Graphics2D g, int posA, int posB, int obj){
		Point p=getPosition(posA,posB);
		drawObj(g, obj, p.x, p.y, 0,0);
	}
	// -------------------------------------------------


	// -------------  Some Helper Stuff  --------------------------
	
	public void ghost(Graphics2D g, int level){
		save(g);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,level/255.0f));
	}
	public void noGhost(Graphics2D g){
		restore(g);
	}

	public int drawImage(Graphics2D g, Image img, int x, int y, int w) {
		int hi=w*img.getHeight(null)/img.getWidth(null);
		double sc=(double)w/(double)img.getWidth(null);
		save(g);
		g.translate(x, y);
		g.scale(sc,sc);
		g.drawImage(img, 0, 0,null);
		restore(g);
		return hi;
	}

	//Standard-Version
	public double ease(double t, double b, double c, double d) {
		c-=b;
		double ts = (t /= d) * t;
		double tc = ts * t;
		return b + c * (tc + -3 * ts + 3 * t);
	}
	
	public double ease2(double t, double b, double c, double d) {
		c-=b;
		double ts = (t /= d) * t;
		double tc = ts * t;
		return b + c * (4 * tc + -9 * ts + 6 * t);
	}
	
	public double ease3(double t, double b, double c, double d) {
		c-=b;
		double ts = (t /= d) * t;
		double tc = ts * t;
		return b + c * (tc * ts);
	}
	
	
}

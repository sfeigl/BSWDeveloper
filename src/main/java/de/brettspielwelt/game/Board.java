package de.brettspielwelt.game;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import de.Data;
import de.Vect;
import de.brettspielwelt.develop.HTMLWrapper;
import de.brettspielwelt.tools.IntVector;


public class Board extends HTMLWrapper
{
	
	private int oldSize;	//,spacing,pix2;
	private Font fontDefault,threeFont,fourFont;
	private Font fontLarge;	//,bigFont,fontSmaller;
	private boolean scaling;
	private String[] localStrings,kartenTexte;
	
	private Vector history=new Vector();
	boolean rep=false;
	int animation=0;
	
	private String[] spielerName={"","","","","",""};
	boolean iAmPlaying = false;
	int anzSpieler = 0;
	int iAmId = -1;
	
	int currentPlayer=-1,startSpieler=-1,subRound=0;
	int phase=0,round=0,cont=0,stapelSize=0;
	
	Image[] baseImg,baseOrgImg;
	private Image[] wOrgImg=new Image[WIMG_ANZ];
	private Image[] wImg=new Image[WIMG_ANZ];
	static final int WIMG_ANZ=31;
	
	IntVector hand=new IntVector();
	int[][] auslage=new int[6][5];
	int stapel=0;
	int handSel=-1;
	int auslageSel=-1,auslageSelCard=-1;
	int wunsch=-1;
	int nextRound=0;
	int[] punkte=new int[6];
	
	Rectangle[] handRect=new Rectangle[32];
	Rectangle[] auslageRect=new Rectangle[5];
	int[] auslageCard=new int[5];
	@Override
	public void init(int lev, Runnable run) {
		if(lev==0){
			localInitH();
			initPics();
		}
		if(lev==1){
			float scale=1;
			baseImg=new Image[baseOrgImg.length];
			for(int i=0; i<baseOrgImg.length; i++){
				baseImg[i]=getScaledInstance(baseOrgImg[i], 1);
			}
			for(int i=0; i<WIMG_ANZ; i++){
				wImg[i]=wOrgImg[i]; //getScaledInstance(wOrgImg[i][j], scale);
			}
			
			fourFont = getFontLocal("fourFont", scale);
			threeFont = getFontLocal("threeFont", scale);
			fontDefault = getFontLocal("defaultFont", scale);
			fontLarge = getFontLocal("largeFont", scale);
		}
	}
	
	static final int BONI=11,LEVEL=31;
	public void initPics(){
		String[] imageNames = {
				"bg.png","choose.png","chooseshadow.png",
				"s0.png","s1.png","s2.png","s3.png","s4.png","s5.png",
				"sc0.png","sc1.png","sc2.png","sc3.png","sc4.png",
				"w0.png","w1.png","w2.png","w3.png","w4.png","w5.png","w6.png","w7.png","w8.png","w9.png","w10.png",
				"back.png","stand.png","empty.png","score.png" // 25
		};
		
		baseOrgImg = new Image[imageNames.length];
		for(int i=0; i<imageNames.length; i++){
			baseOrgImg[i] = getImageLocal(imageNames[i]);
		}
		
		registerFont("BradBunR.ttf");
		
		
		for(int co=0; co<WIMG_ANZ; co++){
			wOrgImg[co]=getImageLocal((co)+".png");
		}
		
		oldSize = 0;
	}
	
	
	public synchronized void scalePics(){
		
		if(true || oldSize==getWidth())return;
		System.err.println("Scaling");
		double scale = ((double)getWidth()) / ((double)1220);
		
		
		baseImg=new Image[baseOrgImg.length];
		for(int i=0; i<baseOrgImg.length; i++){
			baseImg[i]=getScaledInstance(baseOrgImg[i], scale);
		}
		for(int i=0; i<WIMG_ANZ; i++){
			wImg[i]=wOrgImg[i]; //getScaledInstance(wOrgImg[i][j], scale);
		}
		
		
		// Scaled fonts...
		fourFont = getFontLocal("fourFont", scale);
		threeFont = getFontLocal("threeFont", scale);
		fontDefault = getFontLocal("defaultFont", scale);
		fontLarge = getFontLocal("largeFont", scale);
		
		oldSize=getWidth();
		scaling=false;
	}
	
	public int co(int co) {
		return co; //*getWidth()/1220;
	}
	public int rco(int co) {
		return co*1220/getWidth();
	}
	
	
	protected boolean localInitH(){
		localStrings = getStringsLocal("t", 1);
		
		// Scaled fonts...
		double scale = ((double)getWidth()) / ((double)1220);
		
		fourFont = getFontLocal("fourFont", scale);
		threeFont = getFontLocal("threeFont", scale);
		fontDefault = getFontLocal("defaultFont", scale);
		fontLarge = getFontLocal("largeFont", scale);
		
		initPics();
		
		return getSoundPack(null, new String[] {"passt.wav","nicht.wav","click.wav","takeCard.wav","turn.wav",
				"well.wav"
		}); // 14
	}
	
	
	public void getBoard(Vect v){
		if(v.size()>3) {
			int c=0;
			int aPhase=phase;
			int aPl=currentPlayer;
			
			anzSpieler=((Integer)v.elementAt(c++)).intValue();
			iAmId=((Integer)v.elementAt(c++)).intValue();
			phase=((Integer)v.elementAt(c++)).intValue();
			round=((Integer)v.elementAt(c++)).intValue();
			currentPlayer=((Integer)v.elementAt(c++)).intValue();
			startSpieler=((Integer)v.elementAt(c++)).intValue();
			hand=(IntVector)v.elementAt(c++);
			auslage=(int[][]) v.elementAt(c++);
			stapel=((Integer)v.elementAt(c++)).intValue();
			wunsch=((Integer)v.elementAt(c++)).intValue();
			punkte=(int[])v.elementAt(c++);
			nextRound=((Integer)v.elementAt(c++)).intValue();
			
			int[] anim=((int[])v.elementAt(c++));
			handleAnim(anim);
			
			iAmPlaying=(currentPlayer==iAmId);
			if(currentPlayer!=aPl && iAmPlaying)
				playSound(4);
			if(phase==1) chosC=-1;
			if(phase==2 && iAmPlaying && chosC==-1)
				chosC=0;
			
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
		if(((anim[0]>>16)&15)==1){ // aus hand auf ablage
			if(anim[1]!=iAmId) {
				playSound(0);
				noDraw=-1;
				flyC=0;
				flyCa=anim[2];
				int pl=pIdx(anim[1]);
				flyFx=posses[anzSpieler][pl]+posses[anzSpieler][pl+2]*2;
				flyFy=posses[anzSpieler][pl+1]+posses[anzSpieler][pl+2];
				flyAuslage=-1;
				flyPl=anim[1];
				flyW=130;
			}
		}
		if(((anim[0]>>16)&15)==3){ // aus auslage auf ablage
			if(anim[1]!=iAmId) {
				playSound(0);
				flyC=0;
				flyCa=anim[2];
				int pl=pIdx(anim[1]);
				int wi=posses[anzSpieler][pl+2];
				flyFx=posses[anzSpieler][pl]-4*wi/2+wi*anim[3];
				flyFy=posses[anzSpieler][pl+1];
				flyAuslage=anim[3];
				flyPl=anim[1];
				flyW=130;
			}
		}
		if(((anim[0]>>16)&15)==6){ // von nazistapel zu spieler
			if(anim[1]!=iAmId || true) {
				playSound(1);
				drawC=0;
				drawCa=anim[2];
				flyPl=anim[1];
				int pl=pIdx(anim[1]);
				drawTx=posses[anzSpieler][pl]+posses[anzSpieler][pl+2]*2;
				drawTy=posses[anzSpieler][pl+1]+posses[anzSpieler][pl+2];
				drawW=anim[1]==iAmId?130:50;
			}
		}
		if(((anim[0]>>16)&15)==8){ // ScoreBoard
			if(anim[1]==0) {
				playSound(5);
				dropC=0;
			}
			else wegC=0;
		}
	}
	
	int chosC=-1;
	
	public void run() {

		if(flyC>-1) {
			flyC++;
			if(flyC>=32) flyC=-1;
			rep=true;
		}
		if(drawC>-1) {
			drawC++;
			if(drawC==16)
				playSound(3);

			if(drawC>=32) drawC=-1;
			rep=true;
		}
		if(dropC>-1) {
			dropC++;
			if(dropC>=32) dropC=-1;
			rep=true;
		}
		if(wegC>-1) {
			wegC++;
			if(wegC>=32) wegC=-1;
			rep=true;
		}
		if(chosC>-1) {
			chosC++;
			if(chosC>=32) chosC=32;
			rep=true;
		}
		if(history.size()==0) noResponse=false;
		if(animation==0 && drawC==-1 && flyC==-1 && history!=null && history.size()>0) {
			getBoard((Vect)history.elementAt(0));
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
					//start sounds
					int obj=flying[i];
					int targB=(fsPT2[i]>>10)&255;
//						if(((obj>>16)&15)==4 && (obj&255)==2 && targB==iAmId) playSound(0); // rocket
//						if(((obj>>16)&15)==4 && (obj&255)==1 && targB==iAmId) playSound(12); // boom
//
//						if(((obj>>16)&15)==5) playSound(8); // Nothing possible
//						if(((obj>>16)&15)==3) playSound(10); // Wuerfel disappear
//						if(((obj>>16)&15)==2) playSound(9); // Wuerfel appear
					
					
				}
				jpc=0;
				
				//if(flying.length==1 && flying[0]<16) playSound(0); // von spieler
//					if(flying.length==1 && flying[0]>>8==1) playSound(1); // von spieler
//					if(flying.length==1 && flying[0]==16) playSound(2); // von spieler
				//if(flying.length==1 && flying[0]>=512) playSound(3+rnd(3)); // wuerfel
				//if(flying.length>=3 && flying[0]<512) playSound(2); // La Ola
//					if(flying.length>=1 && ((flying[0]>>8)&15)==1) playSound(12); // von spieler
//					if(flying.length>=1 && ((flying[0]>>8)&15)==5) playSound(12); // von spieler
				
				//if(flying.length>=1 && fsP1[0]==4) playSound(2); // zu spieler
				//if(flying.length>=1 && fsP1[0]==5) playSound(3); // zu spieler
				
			}else { animation=0;  rep=false; }
		}
		if(animation==1) {
			if(jpc%animDelta==0 && jpc/animDelta<flying.length) {
				int targ=fsPT1[jpc/animDelta];
				int targB=(fsPT2[jpc/animDelta]>>10)&255;
				int obj=flying[jpc/animDelta];
				//System.err.println("OBJ-"+(obj>>16)+","+(obj&255)+" - "+targ);
//					if(targ==1 && obj<=6) playSound(5+rnd(2));// Wuerfel legen
//					if(targ==2 && obj<=6) playSound(7); // Wuerfel weglegen
//					if(((obj)&15)==7) playSound(1+rnd(4)); // Cross
//					if(((obj>>16)&15)==3) playSound(10); // Wuerfel disappear
//					if(((obj>>16)&15)==2) playSound(9); // Wuerfel appear
//					if(((obj>>16)&15)==4 && (obj&255)==2 && targB==iAmId) playSound(0); // rocket
//					if(((obj>>16)&15)==4 && (obj&255)==1 && targB==iAmId) playSound(12); // boom
				
			}
			jpc++;
			if(jpc>=animLength){
				int w=(jpc-animLength);
				if(w%animDelta==0) {
					int targ=fsPT1[w/animDelta];
					int targB=(fsPT2[w/animDelta]>>10)&255;
					int obj=flying[w/animDelta];
					//System.err.println("OBJ-"+(obj>>16)+","+(obj&255)+" - "+targ);
//						if(targ==10 && obj<=6) playSound(5+rnd(2));// Wuerfel legen
//						if(targ==2 && obj<=6) playSound(7); // Wuerfel weglegen
//						if(((obj)&15)==7) playSound(1+rnd(4)); // Cross
//						if(((obj>>16)&15)==3) playSound(10); // Wuerfel disappear
//						if(((obj>>16)&15)==2) playSound(9); // Wuerfel appear
					//if(((obj>>16)&15)==4 && (obj&255)==2 && targB==iAmId) playSound(0); // rocket
//						if(((obj>>16)&15)==4 && (obj&255)==1 && targB==iAmId) playSound(12); // boom
					
				}
			}
			if(jpc==animLength+(flying.length-1)*animDelta) {
				animation=0; rep=true;
				//if(flying.length==1 && fsP1[0]==3) playSound(0);
			} 
		}
		
		if(rep || animation>0) {
			rep=false;
			repaint();
		}
		
	}
	
	public void getNioData(int typ, Data dat){
		int c=0;
		
		switch(typ){
		case 700:
			history.addElement(dat.v);
			break;
		case 701:
			history.addElement(dat.v);
			break;
			
		case 702:
//			score=(int[][])dat.v.elementAt(c++);
//			platz=(int[])dat.v.elementAt(c++);
			for(int i=0; i<4; i++)
				spielerName[i]=(String)dat.v.elementAt(c++);
			repaint();
			break;
			
		case 703:
			history.addElement(dat.v);
			break;
			
		case 704:
//			playSound(7);
			errorText=(String)dat.v.elementAt(0);
			errorAge=0; errorX=mouseMoveX; errorY=mouseMoveY;
			repaint();
			break;
		}
	}
	
	public synchronized void sendAction(int power, int act){
		//System.err.println("Sending Action: "+act);
		Data dat=makeData(700);
		dat.v.addElement(new Integer((power<<28)|act));
		sendDataObject(dat);
	}
	public synchronized void sendAction(int power, int act, IntVector marks){
		//System.err.println("Sending Action: "+act);
		Data dat=makeData(700);
		dat.v.addElement(new Integer((power<<27)|act));
		dat.v.addElement(marks);
		sendDataObject(dat);
	}
	
	int movePos=-1, movePop=-1;
	int overFeld=-1, overScore=-1;
	
	int mouseMoveX=0, mouseMoveY=0;
	int mouseDownX=0, mouseDownY=0;
	
	@Override
	public void mouseMoved(MouseEvent ev) {
		int x = ev.getX();
		int y = ev.getY();
		mouseMoveX=x; mouseMoveY=y;
		ev.consume();
	}
	
	int flyC=-1, flyCa=-1, flyFx=0, flyFy=0, flyW=130, flyAuslage=-1,flyPl=-1,noDraw=-1,noAuslage=-1;
	int drawC=-1, drawCa=-1, drawTx=0, drawTy=0, drawW=130;
	int dropC=-1,wegC=-1;
	
	public void mouseReleased(MouseEvent ev) {
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		
//		System.err.println("");
//		for(int i=0; i<anzSpieler; i++) {
//			System.err.print((i==0?"{610,780,110":" ,"+posses[anzSpieler][i*3]+","+posses[anzSpieler][i*3+1]+","+posses[anzSpieler][i*3+2]));
//		}
//		System.err.println("}");
		
		if(handSel>-1 && overAuslage) {
			noDraw=handSel;
			flyPl=iAmId;
			sendAction(2,handSel);
		}
		if(handSel>-1 && overAblage) {
			int ca=hand.elementAt(handSel);
			if(possible(ca)) {
				playSound(0);
				noDraw=handSel;
				noAuslage=-1;
				flyC=0;
				flyCa=ca;
				flyFx=x;
				flyFy=y-80;
				flyW=130;
				flyPl=iAmId;
				sendAction(1,handSel);
			}
		}
		
		if(auslageSel>-1 && overAblage) {
			int ca=auslageSelCard; //auslageCard[auslageSel];
			if(possible(ca)) {
				playSound(0);
				noDraw=-1;
				noAuslage=auslageSel;
				flyC=0;
				flyCa=ca;
				flyFx=x;
				flyFy=y-80;
				flyW=130;
				flyPl=iAmId;
				sendAction(3,auslageSel);
			}
		}
		
		int joki=getOverJoker(x, y);
		if(chosC!=-1 &&  phase==2 && joki!=-1) {
			sendAction(4,joki);
		}
		
		overJoker=-1;
		handSel=-1;
		auslageSel=-1;
		repaint();
	}
	
	int[] xPos= new int[] {441,160, 515,167, 583,164, 650,166, 722,164, 787,164, 406,248, 515,247, 610,250, 716,249, 811,248, };
	int overJoker=-1;
	int movePoss=-1;
	
	public void mousePressed(MouseEvent ev) {
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		mouseDownX=x; mouseDownY=y;
		mouseMoveX=x; mouseMoveY=y;
		
	//	dropC=0;
//		int closest=4800;
//		auslage[0][0]=3<<16|20;
//		auslage[0][4]=3<<16|20;
//		for(int i=1; i<anzSpieler; i++) {
//			auslage[i][0]=3<<16|20;
//			auslage[i][4]=3<<16|20;
//			if(getDistance(new Point(x,y), posses[anzSpieler][i*3], posses[anzSpieler][i*3+1])<closest) {
//				closest=getDistance(new Point(x,y), posses[anzSpieler][i*3], posses[anzSpieler][i*3+1]);
//				movePoss=i;
//			}
//		}
//		if(ev.getButton()==1)
//			posses[anzSpieler][movePoss*3+2]+=10;
//		if(ev.getButton()==3)
//			posses[anzSpieler][movePoss*3+2]-=10;

		if(phase==3 && iAmId>-1) {
			sendAction(7,0);
		}
		if(!iAmPlaying) {
			super.mousePressed(ev);
			return;
		}
		//sendAction(9,9);
		overJoker=-1;
		int smalI = getOverJoker(x, y);
		
		System.err.println("Small: "+smalI);
		//chosC=0;
		for(int i=0; i<auslageRect.length; i++) {
			if(auslageRect[i]!=null &&auslageRect[i].contains(x,y)) {
				if(possible(auslageCard[i])) {
					overAblage=overAuslage=false;
					playSound(2);
					auslageSel=i;
					auslageSelCard=auslageCard[i];
				}
				System.err.println("Hit Auslage: "+i);
			}
		}
		for(int i=0; i<handRect.length; i++) {
			if(handRect[i]!=null &&handRect[i].contains(x,y)) {
				if(phase==2) {
				}else {
					overAblage=overAuslage=false;
					boolean onlyPossible=false;
					for(int j=0; j<5; j++) // passt eine aus Auslage?
						if(auslageCard[j]>0 && possible(auslageCard[j]))
							onlyPossible=true;
					if(!onlyPossible || possible(hand.elementAt(i))) {
						playSound(2);
						handSel=i;
					}
//					playSound(2);
				}
			}
		}
		for(int i=0; i<anzSpieler; i++)
		System.err.print(x+","+y+", ");
		repaint();
		ev.consume();
	}


	private int getOverJoker(int x, int y) {
		int smal=3000,smalI=-1;
		for(int i=0; i<xPos.length; i+=2) {
			int dist=getDistance(new Point(x,y), xPos[i], xPos[i+1]);
			if(dist<smal) {
				smal=dist;
				smalI=i/2;
			}
		}
		if(smalI>-1) overJoker=smalI;
		return smalI;
	}
	
	int lastY=0;
	boolean overAuslage=false;
	boolean overAblage=false;
	
	@Override
	public void mouseDragged(MouseEvent ev){
		int x = rco(ev.getX());
		int y = rco(ev.getY());
		mouseMoveX=x; mouseMoveY=y;

//		posses[anzSpieler][movePoss*3+0]=x;
//		posses[anzSpieler][movePoss*3+1]=y;

		if(getOverJoker(x, y)==-1) overJoker=-1;
		
		if(iAmPlaying) {
			if(auslageSel==-1 && y>700 && lastY-y<2) {
				for(int i=handRect.length-1; i>=0; i--) {
					if(handRect[i]!=null &&handRect[i].contains(x,y)) {
						boolean onlyPossible=false;
						for(int j=0; j<5; j++) // passt eine aus Auslage?
							if(auslageCard[j]>0 && possible(auslageCard[j]))
								onlyPossible=true;
						if(!onlyPossible || possible(hand.elementAt(i)))
							handSel=i;
						break;
					}
				}
			}
		}
		
		overAuslage=false;
		if(handSel>-1 && mouseMoveY<608 && mouseMoveY>480) {
			if(!possible(hand.elementAt(handSel)))
				overAuslage=true;
		}
		overAblage=false;
		if(handSel>-1 && mouseMoveY<450) {
			if(possible(hand.elementAt(handSel)))
				overAblage=true;
		}
		if(auslageSel>-1 && mouseMoveY<450) {
			if(possible(auslageCard[auslageSel]))
				overAblage=true;
		}
		
		lastY=y;
		repaint();
	}
	
	public boolean immediateDrag(){
		return true;
	}

	
	public int getDistance(Point p, int x, int y){
		int vx=x-co(p.x), vy=y-co(p.y);
		return vx*vx+vy*vy;
	}
	

	
	
	// Error part -----------------------------------------------------
	String errorText = "";
	int errorX=200,errorY=200,errorW,errorH;
	int errorAge=-1;
	
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
	
	int[] auslagePos= {820,167,
			1100,167,
			820,332,
			1100,332,
			1100,488};
	
	int[][] posses= new int[][] {{0,0,10},{0,0,10},   
		{610,780,110 ,252,79,80}, // 2 spieler
		{610,780,110 ,232,235,80 ,1011,65,70}, // 3 spieler
		{610,780,110 ,231,242,80 ,407,61,70 ,1005,71,70}, // 4 Spieler
		{610,780,110 ,245,240,80 ,385,54,60 ,956,56,60 ,1016,284,70}, // 5 Spieler
		{610,780,110 ,200,287,70 ,234,96,60 ,561,44,50 ,964,64,60 ,1014,294,70} // 6 Spieler
	};

	public Point getPosition(int a, int b) {
		if(a==0) { // Hand
			return new Point(255+b*300,700);
		}
		return new Point(0,0);
	}
	
	int[] posesStart={30,-10, -10,0, 0,30, 30,30};
	
	public Rectangle drawObj(Graphics2D g, int obj, int x, int y, int h, int ti) {
		int objKind=(obj>>16)&255;
		int objTyp=obj&255;
		if(objKind==0) {  // Tau
			g.drawImage(baseImg[objTyp],x-35,y-35,null);
			return new Rectangle(x-35,y-35,70,70);
		}
		return null;
	}
	int[] jiyD= {12,14,16,18,14,10,7,4,0,3,4,2,0,2,3,2,0,0,0};
	
	int[] stackRnd=new int[12];
	
	public Rectangle drawLocObject(Graphics2D g, int posA, int posB, int obj){
		if(animation==1) {
			for(int i=0; i<flying.length; i++) {
				if((flying[i]>>16&255)==2 && fsPT1[i]==posA && fsPT2[i]==posB) return null;
				if(jpc-i*4>=0){
					if(fsP1[i]==posA && fsP2[i]==posB) return null;		
				}
			}
		}
		Point p=getPosition(posA,posB);
		return drawObj(g, obj, p.x, p.y, 0,0);
	}
	// -------------------------------------------------
	Graphics2D backG;
	private boolean touched;
	
	private int setX, setY;
	
	public int coC(int w, int size) {
		return w*size/350;
	}
	
	public int drawImage(Graphics g, Image img, int x, int y, int w) {
		int hi=w*img.getHeight(null)/img.getWidth(null);
		g.drawImage(img, x, y, w, hi,null);
		return w*100/img.getWidth(null);
		//return hi*100;
	}
	
	public void drawCardTo(Graphics2D g, int ca, int x, int y, int w, int ti) {
		save(backG);
		backG.translate(ease(ti,x,610,32),ease(ti,y,265,32)-Math.sin(ti*Math.PI/32)*100);
		backG.rotate(ease(ti,0,-0.98,32));
		backG.shear(0, ease(ti,0,0.5,32));
		drawCard(backG,ca,0,0,(int)ease(ti,w,110,32),0);
		restore(backG);
		
	}
	public void drawCardFrom(Graphics2D g, int ca, int x, int y, int w, int ti) {
		save(backG);
		backG.translate(ease(ti,x,725,32),ease(ti,y,200,32)-Math.sin(ti*Math.PI/32)*100);
		backG.rotate(ease(ti,0,-0.98,32));
		backG.shear(0, ease(ti,0,0.5,32));
		drawCard(backG,-1,0,0,(int)ease(ti,w,110,32),0);
		restore(backG);
		
	}
	
	public int pIdx(int sp) {
		return ((sp-iAmId+anzSpieler)%anzSpieler)*3;
	}
	
	public void paintp(Graphics g) {
		try {
			if(scaling || oldSize!=getWidth()){
				Graphics gl=getOffScreenGraphics();
				gl.setColor(Color.black);
				gl.fillRect(0,0,getWidth(),getHeight());
				gl.setColor(Color.white);
				FontMetrics fm=gl.getFontMetrics(fontDefault);
				gl.drawString(localStrings[0],getWidth()/2-fm.stringWidth(localStrings[0]),getHeight()/2);	// "Standby..." ("Scaling Pics")
				if(oldSize==getWidth()) return;
			}
			scaling=false;
			
			int iId=iAmId>-1?iAmId:0;
			
			backG=(Graphics2D)getOffScreenGraphics();
			backG.setColor(new Color(0x010039));
			backG.setColor(new Color(0xa30101));
			
			backG.drawImage(baseImg[0], 0, 0, null);
			
			
			save(backG);
			backG.translate(610,265);
			backG.rotate(-0.98);
			backG.shear(0, 0.5);
			drawCard(backG,stapel,0,0,110,0);
			restore(backG);

			save(backG);
			backG.translate(725,200);
			backG.rotate(-0.98);
			backG.shear(0, 0.5);
			drawCard(backG,-1,0,0,110,0);
			restore(backG);
			
			save(backG);
			backG.translate(120,740);
			backG.drawImage(baseImg[28], -100,-100,null);
			backG.rotate(-0.25);
			backG.setColor(Color.white);
			backG.setFont(new Font("Brady Bunch Remastered",0,110));
			drawSizedString(backG,""+""+((punkte[iId]&0xffff)-1000),5,-2,400,true);
			restore(backG);

			
			if(wunsch!=-1) {
				if(wunsch>>4>0)
					backG.drawImage(baseImg[13+(wunsch>>4)], 546,108,null);
				else
					backG.drawImage(baseImg[19+(wunsch&15)], 546,108,null);
			}

			int ci=0;
			for(int sp=0; sp<anzSpieler; sp++) {
				if(sp!=iId) {
					int x=posses[anzSpieler][pIdx(sp)];
					int y=posses[anzSpieler][pIdx(sp)+1];
					int wi=posses[anzSpieler][pIdx(sp)+2];
					int hig=wi*314/200;
					int ax=x-(4)*wi/2;
					backG.setColor(new Color(50,50,50));
					backG.fillRoundRect(x-5*wi/2-wi/4, y+wi/3+wi/10, 5*wi+wi/2, wi*7/6, 16, 16);
					drawImage(backG,baseImg[26], x-5*wi/2, y+wi/10, wi*5);
					
					for(int i=0; i<5; i++) {
						int caC=auslage[sp][i]>>16&255;
						if(flyAuslage==i && flyPl==sp && flyC>-1)
						caC--;
						backG.setStroke(new BasicStroke(1));
						for(int j=1; j<caC; j++) {
							backG.setColor(new Color(0+j*20,0,0));
							backG.fillRoundRect(ax+i*wi-0*(caC-j)-wi/2,y-5*(caC-j)-hig/2,wi, hig, 12, 12);
							backG.setColor(new Color(200,200,200));
							backG.drawRoundRect(ax+i*wi-0*(caC-j-1)-wi/2+1,y-5*(caC-j-1)-hig/2-2,wi-2, hig, 12, 12);
							//drawCard(backG,(i+1)<<4,ax+i*wi-1*(caC-j),y-5*(caC-j),wi,0);
						}
					}
					for(int i=0; i<5; i++) {
						int ca=auslage[sp][i]&255;
						if(flyAuslage==i && flyPl==sp && flyC>-1) 
							ca=auslage[sp][i]>>8&255;
						if(ca>0) 
							drawCard(backG,ca,ax+i*wi,y,wi,possible(ca)?1:0);
					}
					ci++;
					int han=auslage[sp][5];
					int wh=10,xh=x+2*wi;
					for(int i=han-1; i>=0; i--)
						drawImage(backG,baseImg[27], xh-i*wh, y+wi, wi/2);
					backG.setColor(Color.white);
					backG.setFont(new Font("Brady Bunch Remastered",0,wi*3/4));
					drawSizedString(backG,(sp==currentPlayer?"* ":"")+spielerName[sp],x-5*wi/2,y+wi*11/8,4*wi,false);
					drawSizedString(backG,""+auslage[sp][5],xh+wi*7/26,y+wi*25/16,5*wi,true);
					
					save(backG);
					backG.translate(x+wi*4/2+wi/2,y+wi-wi/4);
					backG.scale(wi/200.0, wi/200.0);
					backG.drawImage(baseImg[28], -100,-100,null);
					backG.rotate(-0.25);
					backG.setColor(Color.white);
					backG.setFont(new Font("Brady Bunch Remastered",0,110));
					drawSizedString(backG,""+((punkte[sp]&0xffff)-1000),5,-2,400,true);
					restore(backG);

				}
			}
			// ---------- Draw Auslage eigen ------------
			if(iId>-1) {
				ghost(backG,128);
				backG.drawImage(baseImg[26], 610-5*50, 516, null);
				noGhost(backG);
				int c=0;
				auslageCard=new int[5];
				auslageRect=new Rectangle[5];
				int ax=610-4*50;
				for(int i=0; i<5; i++) {
					int caC=auslage[iId][i]>>16&255;
					if(flyAuslage==i && flyC>-1 && flyPl==iId)
						caC--;
					int wi=100, hig=314/2, y=500;
					backG.setStroke(new BasicStroke(1));
					for(int j=1; j<caC; j++) {
						backG.setColor(new Color(0+j*20,0,0));
						backG.fillRoundRect(ax+i*wi-0*(caC-j)-wi/2+1,y-5*(caC-j)-hig/2,wi-2, hig, 12, 12);
						backG.setColor(new Color(200,200,200));
						backG.drawRoundRect(ax+i*wi-0*(caC-j-1)-wi/2+1,y-5*(caC-j-1)-hig/2-2,wi-2, hig, 12, 12);
						//drawCard(backG,(i+1)<<4,ax+i*wi-1*(caC-j),y-5*(caC-j),wi,0);
					}
				}

				for(int i=0; i<5; i++) {
					int ca=auslage[iId][i]&255;
					auslageCard[i]=ca;
					if(i==noAuslage && flyC>-1 && flyPl==iId) ca=auslage[iId][i]>>8&255;
					if(ca>0 && auslageSel!=i) {
						auslageRect[i]=drawCard(backG,ca,ax+i*100,500,100,iAmPlaying && possible(ca)?1:0);
					}else {
						if(auslageSel==i) {
							int ca2=auslage[iId][i]>>8&255;
							if(ca2>0)
								auslageRect[i]=drawCard(backG,ca2,ax+i*100,500,100,0);
						}
					}
					
				}
			}
			// ---------- Draw Hand ------------
			handRect=new Rectangle[hand.size()];
			int wi=130;
			if(hand.size()>8) wi=(1080-132)/(hand.size()-1);
			int totalW=wi*hand.size();
			int hx=610-totalW/2;
			if(hand.size()>8) hx=0;

			boolean onlyPossible=false;
			for(int j=0; j<5; j++) // passt eine aus Auslage?
				if(auslageCard[j]>0 && possible(auslageCard[j]))
					onlyPossible=true;

			for(int i=0; i<hand.size(); i++) {
				if(drawC>-1 && i==noDraw && flyPl==iId) continue;
				if(i==handSel) continue;
				if(i==noDraw && flyC>-1 && flyPl==iId) continue;
				if(i==noDraw && drawC>-1 && flyPl==iId) continue;

				int possW=possible(hand.elementAt(i))?1:0;
				if(!onlyPossible && possW==0) possW=2;
				handRect[i]=drawCard(backG, hand.elementAt(i), hx+i*wi+ 72, 690+50, 130, iAmPlaying?possW:0); // && (!onlyPossible || possible(hand.elementAt(i))) );
			}
			if(iAmId>-1 && iAmPlaying) {
				if(handSel>-1) { 
					if(overAblage) {
						save(backG);
						backG.translate(mouseMoveX, mouseMoveY-84);
						backG.rotate(-0.98);
						backG.shear(0, 0.5);
						drawCard(backG, hand.elementAt(handSel), 0,0, 110, 0);
						restore(backG);
					}else
						drawCard(backG, hand.elementAt(handSel), mouseMoveX, mouseMoveY-84, overAuslage?90:130, 0);
				}
				if(auslageSel>-1) {
					if(overAblage) {
						save(backG);
						backG.translate(mouseMoveX, mouseMoveY-84);
						backG.rotate(-0.98);
						backG.shear(0, 0.5);
						drawCard(backG, auslageSelCard, 0,0, 110, 0);
						restore(backG);
					}else
						drawCard(backG, auslageSelCard, mouseMoveX, mouseMoveY-84, 130, 0);
				}
			}
			if(phase==2 && iAmPlaying) {
				int[] ppp= {83,1,212,1,289,1,396,1,491,0,590,0,    11,117, 161,118,315,118,456,118,585,115 };
				save(backG);
				backG.translate(610, 220);
				backG.scale(ease2(chosC,0.01,0.7,32), ease(chosC,0.01,0.7,32));
				int cwi=(int)ease(chosC,30,600,32);
				backG.drawImage( baseImg[2], -383,-(int)ease(chosC,130,80,32), null);
				backG.drawImage( baseImg[1], -383,-130, null);
				for(int i=0; i<ppp.length; i+=2) {
					if(i/2==overJoker)
						backG.drawImage(baseImg[3+i/2], ppp[i]-383,ppp[i+1]-130, null);
				}
				restore(backG);
			}
			
			if(flyC>-1)
				drawCardTo(backG, flyCa, flyFx, flyFy, flyW, flyC);
			if(drawC>-1)
				drawCardFrom(backG, drawCa, drawTx, drawTy, drawW, 32-drawC);
			
			if(phase==3||phase==15 || wegC>-1) {
				int sx=610-320,sw=640, sh=anzSpieler*50+100;
				int sy=dropC>-1?(int)ease2(dropC,-sh,150,32):150;
				sx=wegC>-1?(int)ease(wegC,610-320,1300,32):610-320;
				backG.setColor(new Color(0,0,30,10));
				for(int i=0; i<16; i++)
					backG.fillRoundRect(sx+0-i*2, sy-i*2+10, sw+i*4, sh+i*4, 62-i, 62-i);
				backG.setColor(new Color(80,80,80));
				backG.fillRoundRect(sx, sy, sw, sh, 32, 32);
				backG.setColor(Color.black);
				backG.setStroke(new BasicStroke(10));
				backG.drawRoundRect(sx, sy, sw, sh, 32, 32);
				backG.setColor(Color.white);
				backG.setFont(new Font("Brady Bunch Remastered",0,80));
				drawSizedString(backG,localStrings[0],sx+sw/2,sy+70,sw-30,true);
				backG.setColor(Color.white);
				if(phase==3) {
					for(int i=0; i<anzSpieler; i++) {
						backG.setStroke(new BasicStroke(2));
						backG.drawOval(sx+sw-anzSpieler*10+i*10-20, sy+sh-5, 8,8);
						if((nextRound>>i&1)==1)
							backG.fillOval(sx+sw-anzSpieler*10+i*10-20, sy+sh-5, 8,8);
					}
				}
				int rw=0,max=5000;
				for(int i=0; i<anzSpieler; i++) {
					int hi=-1;
					for(int j=0; j<anzSpieler; j++) {
						int pu=punkte[j]&255;
						if(pu>hi && pu<max)
							hi=pu;
					}
					if(hi>-1) {
						max=hi;
						int rank=rw+1;
						for(int j=0; j<anzSpieler; j++) {
							int pu=punkte[j]&255;
							int ry=sy+120+rw*50;
							if(pu==hi) {
								backG.setFont(new Font("Brady Bunch Remastered",0,40));
								drawSizedString(backG,rank+". "+spielerName[j],sx+30,ry,240,false);
								drawImage(backG,baseImg[26],sx+330-50,ry-8,100);
								drawSizedString(backG,"+"+(punkte[j]>>16&255),sx+330,ry,sw,true);
								for(int k=0; k<auslage[j][5]; k++)
									drawImage(backG,baseImg[27],sx+450-50+k*5,ry-26,26);
								drawSizedString(backG,""+(auslage[j][5]),sx+450-50+13-5+auslage[j][5]*5,ry+4,sw,true);
								drawSizedString(backG,"-"+(punkte[j]>>24&255),sx+480,ry,sw,true);

								save(backG);
								backG.translate(sx+580,ry);
								backG.scale(0.4, 0.4);
								backG.drawImage(baseImg[28], -100,-100,null);
								backG.rotate(-0.25);
								backG.setColor(Color.white);
								backG.setFont(new Font("Brady Bunch Remastered",0,110));
								drawSizedString(backG,""+((punkte[j]&0xffff)-1000),5,-2,400,true);
								restore(backG);

								rw++;
							}
						}
					}					
				}
			}

			//backG.drawString("Phase: "+(phase&0xff)+" in:"+diceIn.length+" out:"+diceOut.length, 10, 190);
		}catch(Exception ex) {ex.printStackTrace();}
	}
	
	public boolean possible(int ca) {
		if(!iAmPlaying) return false;
		if(ca==0) return true;
		if(wunsch!=-1) {
			int wunschCol=wunsch>>4;
			if(wunschCol>0) {
				if(ca>>4==wunschCol) return true;
			}
			else if((ca&15)==(wunsch&15)) return true;
			return false;
		}
		if(stapel>>4==ca>>4 || (stapel&15)==(ca&15))
			return true;
		return false;
	}
	public Rectangle drawCard(Graphics2D g, int card, int x, int y, int w, int ww) {
		int h=w*314/200;

		int c=((card>>4)-1)*6+(card&15);
		if(card==0) c=30;
		if(card==-1) c=31;
		if(c<0) return null;
		
		double sc=(double)w/200.0;
		save(g);
		g.translate(x, y);
		g.scale(sc, sc);
		int va=card&31;
		g.drawImage(c==31?baseImg[25]:wImg[c], -100,-157, null);
		if(ww>0) {
			if(ww==1) backG.setColor(new Color(200,200,205,128));
			else backG.setColor(new Color(205,200,255,64));
			backG.setStroke(new BasicStroke(12));
			backG.drawRoundRect(-100+4, -157+4, 200-10, 314-10, 18, 18);
		}
		restore(g);
		return new Rectangle(x-w/2,y-h/2,w,h);
	}
	
	public void ghost(Graphics2D g, int level){
		save(g);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,level/255.0f));
	}
	public void noGhost(Graphics2D g){
		restore(g);
	}
	
	public void drawError(Graphics g) {
		String str=errorText;
		g.setFont(threeFont);
		int strW=g.getFontMetrics().stringWidth(str);
		int ti=errorAge;
		int h1=(100-ti)<0?0:(100-ti);
		int h2=(160-ti)<0?0:(160-ti);
		int h3=(255-ti)<0?0:(255-ti);
		g.setColor(new Color(255,255,255,h1));
		errorW=strW+co(8); errorH=co(24);
		g.fillRect(errorX-errorW/2,errorY,errorW,errorH);
		g.setColor(new Color(0,0,0,h2));
		g.drawString(errorText,errorX+co(6)-errorW/2,errorY+co(18));
		g.setColor(new Color(255,255,255,h3));
		g.drawString(errorText,errorX+co(4)-errorW/2,errorY+co(18));
	}
	
	int colSel=-1;
	
	public long getMemoryUsage() {
		return 25L*1024L*1024L;
	}
	
	// t == aktuelle zeit / d = steps / rückgabe zwischen b und c
	//Standard-Version
	public double ease(double t, double b, double c, double d) {
		c-=b;
		double ts = (t /= d) * t;
		double tc = ts * t;
		return b + c * (tc + -3 * ts + 3 * t);
	}
	
	// KingdomBuilder-Karten
	public double ease2(double t, double b, double c, double d) {
		c-=b;
		double ts = (t /= d) * t;
		double tc = ts * t;
		return b + c * (4 * tc + -9 * ts + 6 * t);
	}
	
	// Hex fadeout bei Spielende
	public double ease3(double t, double b, double c, double d) {
		c-=b;
		double ts = (t /= d) * t;
		double tc = ts * t;
		return b + c * (tc * ts);
	}
	
	
}

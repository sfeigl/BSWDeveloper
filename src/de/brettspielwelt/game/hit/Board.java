package de.brettspielwelt.game.hit;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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
	static final int[] colors= {0xf38187,0xed1943,0x7bb8e5,0x1f8fce,0xfbdc41,0xfebd11,0xb6cb6d,0x63b146};
	
	private int oldSize;	//,spacing,pix2;
	private Font fontDefault,threeFont,fourFont;
	private Font fontLarge;	//,bigFont,fontSmaller;
	private boolean scaling;
	private String[] localStrings,kartenTexte;
	
	private Vector history=new Vector();
	boolean rep=false;
	int animation=0;
	
	private String[] spielerName={"","","",""};
	boolean iAmPlaying = false;
	int anzSpieler = 0;
	int iAmId = -1;
	
	int currentPlayer=-1,startSpieler=-1,subRound=0;
	int phase=0,round=0,cont=0,stapelSize=0;
	
	Image[] baseImg,baseOrgImg;
	private Image[] wOrgImg=new Image[WIMG_ANZ];
	private Image[] wImg=new Image[WIMG_ANZ];
	static final int WIMG_ANZ=40;
	
	
	int[][] poeppel=new int[4][4]; // [-4,43]
	int[] cash=new int[4];
	int[] hand=new int[3];
	int[] auslage=new int[5];
	int[] zehner=new int[4];
	int geschuetzt=0;
	boolean[] canPlay=new boolean[3];
	
	Rectangle[] poepRect=new Rectangle[4];
	Rectangle[] cardRect=new Rectangle[8];
	Rectangle[] posRect=new Rectangle[18];
	int nachziehSize=0, ablageSize=0;
	IntVector ablage=new IntVector();

	IntVector possible=new IntVector();
	int sCard=-1, sPoep=-1;
	int amountPoeppel=3;
	
	@Override
	public void init(int lev, Runnable run) {
		poeppel=new int[4][4]; // [-4,43]
		cash=new int[4];
		hand=new int[3];
		auslage=new int[5];
		poepRect=new Rectangle[4];
		cardRect=new Rectangle[8];
		posRect=new Rectangle[18];
		
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
			
			fourFont = getFontLocal("fourFont", scale);
			threeFont = getFontLocal("threeFont", scale);
			fontDefault = getFontLocal("defaultFont", scale);
			fontLarge = getFontLocal("largeFont", scale);
		}
	}
	
	static final int STUFF=5,LEVEL=17;
	public void initPics(){
		String[] imageNames = {
				"bg.png","p0.png","p1.png","p2.png","p3.png",
				"empty.png","backsm.png","schild.png","muenze.png","two.png","twoinv.png","keep.png","shuffle.png",
				"impossible.png","nexter.png","hide.png","hit.png",
				"bg4.png","bg3.png"
		};
		
		baseOrgImg = new Image[imageNames.length];
		for(int i=0; i<imageNames.length; i++){
			baseOrgImg[i] = getImageLocal(imageNames[i]);
		}
		
		registerFont("Product Sans Bold.ttf");
		
		
		for(int co=0; co<WIMG_ANZ; co++){
			wImg[co]=getImageLocal("k"+(co)+".png");
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
		kartenTexte = getStringsLocal("h", 12);
		// Scaled fonts...
		double scale = ((double)getWidth()) / ((double)1220);
		
		fourFont = getFontLocal("fourFont", scale);
		threeFont = getFontLocal("threeFont", scale);
		fontDefault = getFontLocal("defaultFont", scale);
		fontLarge = getFontLocal("largeFont", scale);
		
		initPics();
		
		return getSoundPack(null, new String[] {
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
			amountPoeppel=((Integer)v.elementAt(c++)).intValue();
			currentPlayer=((Integer)v.elementAt(c++)).intValue();
			startSpieler=((Integer)v.elementAt(c++)).intValue();
			auslage=(int[])v.elementAt(c++);
			cash=(int[])v.elementAt(c++);
			hand=(int[])v.elementAt(c++);
			ablageSize=((Integer)v.elementAt(c++)).intValue();
			nachziehSize=((Integer)v.elementAt(c++)).intValue();
			ablage=((IntVector)v.elementAt(c++));
			
			poeppel=(int[][])v.elementAt(c++);
			sCard=((Integer)v.elementAt(c++)).intValue();
			sPoep=((Integer)v.elementAt(c++)).intValue();
			possible=((IntVector)v.elementAt(c++));
			zehner=(int[])v.elementAt(c++);
			geschuetzt=((Integer)v.elementAt(c++)).intValue();
			canPlay=(boolean[])v.elementAt(c++);
			
			int[] anim=((int[])v.elementAt(c++));
			handleAnim(anim);
			
			if(phase!=15) endC=-1;
			if(phase!=aPhase && phase==15) {
				endC=0; endCol=currentPlayer;
			}
			iAmPlaying=(currentPlayer==iAmId);
			repaint();
		}else {
			if(v.size()==1){
				int[] anim=((int[])v.elementAt(0));
				handleAnim(anim);
			}
		}
	}
	
	int playCardC=-1, playCardPl=0, playCard=0;
	int buyCardC=-1, buyCardPl=0, buyCard=0, buyCardPos=0;
	private void handleAnim(int[] anim) {
		if(anim==null || anim.length==0) return;
		if(((anim[0]>>16)&15)==1){ // PlayCard
			if(anim[1]!=iAmId) {
				playCardC=0;
				playCard=anim[2];
				playCardPl=anim[1];
			}
			return;
		}
		if(((anim[0]>>16)&15)==2){ // BuyCard
			if(anim[1]!=iAmId) {
				buyCardC=0;
				buyCard=anim[2];
				buyCardPos=anim[3];
				buyCardPl=anim[1];
			}
			return;
		}
		animation=1;
		animationArr=anim;
	}
	
	public void run() {
		if(endC>-1) {
			endC++;
			if(endC<200) {
				endC++;
				rep=true;
			}
		}
		if(playCardC>-1) {
			playCardC++;
			if(playCardC>32) {
				playCardC=-1;
			}
			rep=true;
		}
		if(buyCardC>-1) {
			buyCardC++;
			if(buyCardC>32) {
				buyCardC=-1;
			}
			rep=true;
		}
		if(history.size()==0) noResponse=false;
		if(animation==0 && history!=null && history.size()>0) {
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
			playSound(7);
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
	
	public boolean immediateDrag(){
		return true;
	}
	
	public boolean isAutoCard(int pos) {
		int ca=hand[pos];
		if(ca==4||ca==10||ca==16||ca==22||ca==38||ca==39) {
			return true;
		}
		if(!canPlay[pos])
			return true;
		return false;
	}
	boolean down;
	public void mouseReleased(MouseEvent ev) {
		int x = ev.getX();
		int y = ev.getY();
		x=rco(x); y=rco(y);
		down=false;
		
		//for(int z=0; z<3; z++) selCards[z]=0;
		if(playGrab==-1) {
			for(int i=0; i<cardRect.length; i++) {
				if(cardRect[i]!=null && cardRect[i].contains(x,y)) {
					System.err.println("Hit Card: "+i);
					if(i<3) {
						if(!doppelSel) {
							int co=countSel();
							if(co>=1)
								for(int z=0; z<3; z++) selCards[z]=0;
							selCards[i]=1-selCards[i];
							if(!isAutoCard(i))
								sendSelCards();
						}else {
							int co=countSel();
							if(co==2 && selCards[i]!=0)
								selCards[i]=0;
							if(co<2) {
								selCards[i]=1-selCards[i];
								if(countSel()==2)
									sendSelCards();
								else
									sendAction(1,0);
								//for(int z=0; z<3; z++) selCards[z]=0;
							}
							if(countSel()!=2) {
								sendAction(1,0);
							}
						}
						//sendAction(1,i);
						//playGrab=x;
					}
				}
			}
		}
		if((x>210||countSel()==1) && playGrab>-1) {
			int selC = sendSelCards();
			//for(int z=0; z<3; z++) selCards[z]=0;
			if(selC==1) {
				for(int i=0; i<poepRect.length; i++) {
					if(poepRect[i]!=null && poepRect[i].contains(x,y)) {
						System.err.println("Hit: "+i);
						sendAction(2,i);
					}
				}
			}
		}
		playGrab=-1;
		
		if(grabCard>-1) {
			if(mouseMoveY>200) sendAction(4,grabCard);
			grabCard=-1;
		}
		repaint();
	}


	private int sendSelCards() {
		int we=0,selC=0;
		for(int i=0; i<3; i++) {
			if(selCards[i]==1) {
				we|=1<<i;
				selC++;
			}
		}
		sendAction(1,we);
		return selC;
	}
	
	public int getMostFront(Rectangle[] posRect, int x, int y) {
		IntVector hit=new IntVector();
		for(int i=0; i<posRect.length; i++) {
			if(posRect[i]!=null && posRect[i].contains(x,y)) {
				hit.addElement(i);
			}
		}
		int hi=-1, hiSel=-1;
		for(int i=0; i<hit.size(); i++)
			if(posRect[hit.elementAt(i)].y>hi) {
				hi=posRect[hit.elementAt(i)].y;
				hiSel=hit.elementAt(i);
			}
		return hiSel;
	}
	
	int grabCard=-1, grabY, playGrab=0, startGrab=-1;
	boolean doppelSel;
	int[] selCards=new int[3];
	
	public void mousePressed(MouseEvent ev) {
		int x = ev.getX();
		int y = ev.getY();
		x=rco(x); y=rco(y);
		mouseMoveX=x; mouseMoveY=y;
		mouseDownX=x; mouseDownY=y;
		
//		buyCardC=0;
		
		
		System.err.print(x+","+y+", ");
		down=true;
		
		if(phase==3 && weiterRect!=null && weiterRect.contains(x,y)) {
			sendAction(7,1);
		}
		if(phase==2) {
			if(keepRect!=null && keepRect.contains(x,y))
				sendAction(5,1);
			if(shuffleRect!=null && shuffleRect.contains(x,y))
				sendAction(5,0);
			return;
		}
		if(doppelRect!=null && doppelRect.contains(x,y)) {
			doppelSel=!doppelSel;
		}
	
		int posSel=getMostFront(posRect, x, y);
		if(posSel>-1)
			sendAction(3,posSel);

		startGrab=-1;
		for(int i=0; i<cardRect.length; i++) {
			if(cardRect[i]!=null && cardRect[i].contains(x,y)) {
				System.err.println("Hit Card: "+i);
				if(i<3) {
					startGrab=i;
				}
				else{
					grabCard=i-3;
					grabY=y;
				}
			}
		}
		
		int poepSel=getMostFront(poepRect, x, y);
		if(poepSel>-1) {
			System.err.println("Hit: "+poepSel);
			sendAction(2,poepSel);
		}

		repaint();
		ev.consume();
	}


	private int countSel() {
		int sum=0;
		for(int z=0; z<3; z++) {
			sum+=selCards[z];
		}
		return sum;
	}
	private boolean hasHome(int who) {
		for(int z=0; z<4; z++) {
			if(who>-1 && poeppel[who][z]<0)
				return true;
		}
		return false;
	}
	
	public int getDistance(Point p, int x, int y){
		int vx=x-co(p.x), vy=y-co(p.y);
		return vx*vx+vy*vy;
	}
	
	@Override
	public void mouseDragged(MouseEvent ev){
		int x = ev.getX();
		int y = ev.getY();
		x=rco(x); y=rco(y);
		mouseMoveX=x; mouseMoveY=y;
		if(x-mouseDownX>10 && startGrab>-1) {
			for(int i=0; i<3; i++) {
				if(playGrab==-1 && cardRect[i]!=null && cardRect[i].contains(x,y)) {
					System.err.println("Hit Card: "+i);
					int co=countSel();
					if(doppelSel) {
						if(co==1)
							for(int z=0; z<3; z++) selCards[z]=0;
						if(co==2 && selCards[i]==0)
							selCards[i]=1-selCards[i];
						if(co<2 && selCards[i]==0 && doppelSel) {
							selCards[i]=1-selCards[i];
						}
					}else {
						for(int z=0; z<3; z++) selCards[z]=0;
						selCards[i]=1-selCards[i];
					}
					
				}
			}
			playGrab=x;	
		}
		repaint();
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
	
	public Point getPosition(int a, int b) {
		if(a==4) { // Hand
			int[] pos= {475,335, 532,361, 589,386, 645,410, 706,434, 765,409, 820,386, 877,363, 934,338, 998,363, 1059,388, 999,411, 942,436, 888,460, 830,483, 886,508, 943,531, 999,553, 1059,577, 996,602, 936,627, 877,604, 820,581, 765,556, 705,534, 648,557, 591,579, 535,603, 474,628, 413,602, 352,577, 411,553, 470,531, 524,505, 585,482, 526,459, 467,435, 412,411, 354,389, 413,364};
			return new Point(pos[b*2],pos[b*2+1]);
		}
		int[] mpos= {475,335, 532,361, 589,386, 645,410, 706,434, 765,409, 820,386, 877,363, 934,338, 998,363, 1059,388, 999,411, 942,436, 888,460, 830,483, 886,508, 943,531, 999,553, 1059,577, 996,602, 936,627, 877,604, 820,581, 765,556, 705,534, 648,557, 591,579, 535,603, 474,628, 413,602, 352,577, 411,553, 470,531, 524,505, 585,482, 526,459, 467,435, 412,411, 354,389, 413,364};
		int[] mpos3= {344,404, 426,411, 504,421, 582,429, 675,445, 738,418, 797,394, 855,370, 914,344, 974,369, 1032,397, 978,419, 921,443, 862,467, 793,494, 830,532, 852,563, 871,597, 891,630, 811,637, 722,648, 704,615, 683,582, 662,549, 635,510, 537,499, 460,489, 380,483, 302,472, 324,437};

		if(a>=0 && a<4) {
			int[][] pos= {
					{704,303, 704,334, 745,319, 663,318,    468,386, 521,407, 573,429, 626,451},
					{1144,483, 1066,484, 1105,499, 1104,466,    944,386, 891,406, 839,429, 785,450},
					{706,665, 746,646, 664,647, 705,631,     942,582, 891,561, 840,539, 785,517},
					{266,482, 304,465, 304,499, 345,481,    469,580, 521,558, 573,538, 627,516}
			};
			int[][] pos3= {
					{525,354, 597,338, 579,362, 539,332,     398,446, 471,454, 539,462, 613,471},
					{1043,517, 1086,498, 1003,501, 1044,482, 918,392, 867,415, 816,435, 763,457},
					{465,592, 522,585, 482,614, 540,606, 792,608, 772,578, 754,549, 732,519},
					{-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,}
			};
			if(anzSpieler==3) { pos=pos3; mpos=mpos3; }
			int len=mpos.length/2;
			if(b<4) return new Point(pos[a][b*2],pos[a][b*2+1]);
			if(b>=len+4)
				return new Point(pos[a][(b-len)*2],pos[a][(b-len)*2+1]);
			return new Point(mpos[((a*10+(b-4))%len)*2],mpos[((a*10+(b-4))%len)*2+1]);
		}
//		if(a==1) {
//			int[] pos= {1144,483, 1066,484, 1105,499, 1104,466, 
//					1059,388, 999,411, 942,436, 888,460, 830,483, 886,508, 943,531, 999,553, 1059,577, 996,602, 936,627, 877,604, 820,581, 765,556, 705,534, 648,557, 591,579, 535,603, 474,628, 413,602, 352,577, 411,553, 470,531, 524,505, 585,482, 526,459, 467,435, 412,411, 354,389, 413,364,  475,335, 532,361, 589,386, 645,410, 706,434, 765,409, 820,386, 877,363, 934,338, 998,363,
//					944,386, 891,406, 839,429, 785,450};
//			return new Point(pos[b*2],pos[b*2+1]);
//		}
		if(a==10) {
			return new Point(90,135+258*b);
		}
		if(a==11) {
			return new Point(440+b*130,100);
		}
		return new Point(0,0);
	}
	
	int[] posesStart={30,-10, -10,0, 0,30, 30,30};
	
	public Rectangle drawObj(Graphics2D g, int obj, int x, int y, int h, int ti) {
		int objKind=(obj>>16)&255;
		int objTyp=obj&255;
		if(objKind==0) {  // poeppel
			int zehn=obj>>8&15;
			if(ti>-1) drawImage(backG,baseImg[(objTyp)+1], x-28, y-75-4*h, 58);
			if(zehn>0) {
				backG.setFont(threeFont);
				backG.setColor(Color.white);
				backG.fillOval(x-15, y-100, 30, 30);
				backG.setColor(Color.black);
				drawSizedString(backG,""+zehn, x, y-76,200,true);
			}

			return new Rectangle(x-28, y-75,58,90);
		}
		if(objKind==1) {  // card normal size
			int wi=h>300?60:156;
			return drawCard(backG,objTyp,x,y,wi);
		}
		return null;
	}
	
	public void drawCardShadow(Graphics2D g, int x, int y, int w) {
		int wi=w;
		int r=wi/20;
		int hi=(wi-2*r)*(230)/(145)+2*r;
		g.setColor(new Color(0,0,60,20));
		for(int u=0; u<10; u++)
			g.fillRoundRect(x-wi/2+u,y-hi/2+u, wi-2*u,hi-2*u,  2*r,2*r);
	}
	
	public Rectangle drawCard(Graphics2D g, int ca, int x, int y, int w) {
		if(ca<0) return null;
		int wi=w;
		int r=wi/20;
		int hi=(wi-2*r)*(230)/(145)+2*r;
		
		g.setColor(Color.white);
		g.fillRoundRect(x-wi/2,y-hi/2, wi,hi,  2*r,2*r);
		drawImage(g,wImg[ca],x-wi/2+r,y-hi/2+r, wi-2*r);
		return new Rectangle(x-wi/2,y-hi/2, wi,hi);
	}
	
	int[] jiyD= {12,14,16,18,14,10,7,4,0,3,4,2,0,2,3,2,0,0,0};
	
	int[] stackRnd=new int[12];
	Vector<int[]> draw=new Vector<int[]>();
	
	public Rectangle drawLocObject(Graphics2D g, int posA, int posB, int obj, int ghost){
		if(animation==1) {
			for(int i=0; i<flying.length; i++) {
				if((flying[i]>>16&255)==0 && fsPT1[i]==posA && fsPT2[i]==posB) return null;
				if(jpc-i*4>=0){
					if(fsP1[i]==posA && fsP2[i]==posB) return null;		
				}
			}
		}
		Point p=getPosition(posA,posB);
		draw.addElement(new int[] {obj,p.x,p.y,ghost});
		return drawObj(g, obj, p.x, p.y, 0,-1);
	}
	// -------------------------------------------------
	Graphics2D backG;
	private boolean touched;
	
	private int setX, setY;
	
	private Rectangle passenRect,rerollRect,boardRect,losRect,doppelRect;
	private Rectangle[] handRect=new Rectangle[3];
	private Rectangle[] kaufRect=new Rectangle[5];
	Rectangle[][] stonePos=new Rectangle[20][0];

	private Rectangle shuffleRect;
	private Rectangle keepRect,weiterRect;
	
	public int coC(int w, int size) {
		return w*size/350;
	}
	
	public int drawImage(Graphics g, Image img, int x, int y, int w) {
		int hi=w*img.getHeight(null)/img.getWidth(null);
		g.drawImage(img, x, y, w, hi,null);
		return w*100/img.getWidth(null);
		//return hi*100;
	}
	
	
	public void paintp(Graphics g) {
		try {
			draw=new Vector<int[]>();
			
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
			
			backG=(Graphics2D)getOffScreenGraphics();
			backG.setColor(new Color(0x010039));
			backG.setColor(new Color(0xa30101));
			
//			poeppel=new int[4][4];
//			poeppel[0]= new int[] {-1, 1,39,43};
//			poeppel[1]= new int[] {-1,4,6,12};
//			hand= new int[] {3,8,10};
			int myId=iAmId;
			if(myId==-1) myId=0;
			int curPl=anzSpieler==2&&currentPlayer==1?2:currentPlayer;

			if(anzSpieler==2 && myId==1) myId=2;
			
			backG.drawImage(baseImg[0], 0, 0, null);
			
			if(endC>-1) 
				backG.setColor(new Color(colors[(anzSpieler==2&&endCol==1?2:endCol)*2]));			
			else backG.setColor(new Color(colors[(curPl>-1?curPl:2)*2]));			
			backG.fillRect(0, 0, 1220, 440);
			backG.setColor(new Color(0xb3b6d9));
			backG.fillRect(0, 440, 1220, 440);
			backG.drawImage(baseImg[LEVEL+(anzSpieler==3?1:0)], 187, 197, null);
			backG.setColor(new Color(colors[myId*2+1]));
			backG.fillRect(0,0,184,784);
			backG.setColor(Color.black);			
			backG.fillRect(180,0,8,784);
			if(anzSpieler!=3 && amountPoeppel==3)
				backG.drawImage(baseImg[STUFF+10], 398+187,431, null);

			if(phase==3 && iAmPlaying) {
				backG.setColor(Color.black);
				backG.drawImage(baseImg[STUFF+9],1038,600,null);
				backG.setFont(fourFont);
				drawSizedString(backG,spielerName[(currentPlayer+1)%anzSpieler],1108,678,130,true);
				weiterRect=new Rectangle(1038,600,180,784);
			}else weiterRect=null;
			
			if(anzSpieler>0) {
				int[][] namPosO={
						{ 590,314,-23,100},
						{ 590,314,-23,100,  726,704, -23, 100},
						{462,367, -61, 60,   1070,556, -23, 100,  462,656, -5, 60},
						{ 590,314,-23,100,    1130,538, -23, 100, 726,704, -23, 100,  180,496, 23, -100},
				};
				int[] namPos=namPosO[anzSpieler-1];
				for(int i=0; i<namPos.length/4; i++) {
					save(backG);
					backG.setFont(fourFont);
					backG.translate(namPos[i*4],namPos[i*4+1]);
					if(i==0) backG.scale(1, 0.8);
					backG.rotate(namPos[i*4+2]*Math.PI/180);
					backG.shear(namPos[i*4+3]/100.0, 0);
					backG.drawString(spielerName[i], 0, 0);
					restore(backG);
				}
			}
			for(int sp=0; sp<4; sp++) {
				for(int i=0; i<amountPoeppel; i++) {
					if(poeppel[sp][i]<50) { // TODO Max Strecke auch bei 3 oder 4 poeppel spiel
						Rectangle por=drawLocObject(backG, sp, poeppel[sp][i]+4, (sp==myId&&zehner[i]>-1?zehner[i]:0)<<8|sp, 0);
						if(sp==myId) {
							poepRect[i]=por;
						}
					}
					//drawImage(backG,baseImg[1], 474-28, 338-75, 58);
				}
			}
			for(int i=0; i<3; i++) {
				if(hand[i]!=-1) {
					if(playGrab==-1) {
						if(selCards[i]==1)
							drawCardShadow(backG, 100,135+258*i+10, 160);
						cardRect[i]=drawCard(backG, hand[i], 100-8,135+258*i, 160);
					}
					if(selCards[i]==0) 
						cardRect[i]=drawCard(backG, hand[i], 100-8,135+258*i, 160);
					if(iAmPlaying)
						if(!canPlay[i])
							drawImage(backG, baseImg[STUFF+8],125,135+258*i+30, 80);
				}else cardRect[i]=null;
			}
			for(int i=0; i<5; i++) {
				if(i!=grabCard && endC==-1)
					cardRect[i+3]=drawCard(backG, auslage[i], 440+i*130,100, 120);
			}
			
			for(int i=0; i<posRect.length; i++) posRect[i]=null;
			if(currentPlayer>-1 && sPoep>-1 && iAmPlaying && animation==0) {
				for(int i=0; i<possible.size(); i++) {
					int zfDelta=possible.elementAt(i)>>8&255;
					if(zfDelta>200) zfDelta=-1;
					posRect[i]=drawLocObject(backG, myId, (poeppel[myId][sPoep]+zfDelta)+4, myId, 128);
				}
			}
			
			for(int i=0; i<draw.size(); i++)
				for(int j=0; j<draw.size(); j++)
					if(draw.elementAt(i)[2]<draw.elementAt(j)[2]) {
						int[] tmp=draw.elementAt(j);
						draw.setElementAt(draw.elementAt(i), j);
						draw.setElementAt(tmp, i);
					}
						
			for(int i=0; i<draw.size(); i++) {
				int[] el=draw.elementAt(i);
				if(el[3]!=0) ghost(backG,el[3]);
				drawObj(backG,el[0],el[1],el[2], 0,0);
				if(el[3]!=0) noGhost(backG);
			}
			
			for(int i=0; i<anzSpieler; i++) {
				if((geschuetzt>>i&1)==1)
					backG.drawImage(baseImg[STUFF+2],getShieldPos(i).x-25,getShieldPos(i).y-56, 50,60, null );
			}
			
			if(playCardC>-1) {
				int pcx=getShieldPos(playCardPl).x, pcy=(int)(ease(playCardC,getShieldPos(playCardPl).y-50,getShieldPos(playCardPl).y-150,32));
				int pcw=50+playCardC*2;
				ghost(backG,(int)(ease(playCardC,255,0,32)));
				drawCard(backG,playCard,pcx,pcy,pcw);
				noGhost(backG);
			}
			if(buyCardC>-1) {
				int fx=440+buyCardPos*130, fy=100, fw=120;
				int tx=getShieldPos(buyCardPl).x, ty=getShieldPos(buyCardPl).y-50, tw=50;
				ghost(backG,(int)(ease(buyCardC,255,0,32)));
				drawCard(backG,buyCard,(int)(ease(buyCardC,fx,tx,32)),(int)(ease(buyCardC,fy,ty,32)),(int)(ease(buyCardC,fw,tw,32)));
				noGhost(backG);
			}
			if(hasHome(myId) && iAmPlaying) {
				backG.drawImage(baseImg[STUFF+(doppelSel?5:4)],208,594, null);
				doppelRect=new Rectangle(208,594,75,75);
			}else {
				doppelSel=false;
				doppelRect=null;
			}
			
			int monX=430;
			backG.setFont(fontLarge);
			drawImage(backG,baseImg[STUFF+3],monX,690,80);
			drawSizedBorderdString(backG,Color.black, Color.black, ""+cash[iAmId], monX+40, 740, 200, true);
			backG.setFont(fontDefault);
			
			if(phase==2 && iAmPlaying) {
				shuffleRect=new Rectangle(346,625,75,75);
				keepRect=new Rectangle(346,705,75,75);
				backG.drawImage(baseImg[STUFF+7], 346,625, null);
				backG.drawImage(baseImg[STUFF+6], 346,705, null);
			}
			if(animation==1) {
				for(int i=0; i<flying.length; i++) {
					int vx=fpt[i].x-fp[i].x;
					int vy=fpt[i].y-fp[i].y;
					int rjpc=jpc-i*animDelta;
					if(rjpc>animLength) rjpc=animLength;
					if(rjpc>=0 && rjpc<=animLength)
						drawObj(backG,flying[i],fp[i].x+vx*rjpc/animLength,fp[i].y+vy*rjpc/animLength,jiy[rjpc*16/animLength],rjpc);
				}
			}

			
			int ox=210,oy=680, ow=66;
			
			drawImage(backG,baseImg[nachziehSize>0?STUFF+1:STUFF],ox,oy,60 );
			if(ablage.size()>0)
				drawObj(backG,1<<16|ablage.lastElement(), ox+ow+ow/2,oy+44, 400,0 ); // TODO - last card
			else
				drawImage(backG,baseImg[STUFF],ox+ow,oy,60 );

			
			drawSizedBorderdString(backG,Color.white,new Color(0),""+nachziehSize,ox+48,770,200,true);
			drawSizedBorderdString(backG,Color.white,new Color(0),""+ablageSize,ox+ow+48,770,200,true);

			if(grabCard>-1) {
				int delta=(mouseMoveY-grabY);
				int hi=784-grabY;
				if(delta<0) delta=0;
				if(delta>hi) delta=hi;
				int sh=(int)(Math.sin((double)delta*Math.PI/(double)hi)*40);
				int px=(int)(ease(delta,440+grabCard*130,210+33,hi));
				int py=(int)(ease(delta,120,680+40,hi))-sh;
				int pw=(int)(ease(delta,160,66,hi));
				
				drawCardShadow(backG, px+sh, py+10, pw);
				drawCard(backG, auslage[grabCard], px, py, pw);
			}

			if(playGrab>0) {
				int c=0;
				for(int i=0; i<3; i++) {
					if(selCards[i]==1) {
						drawCard(backG, hand[i], mouseMoveX, mouseMoveY+c++*50, mouseMoveX>210?30:160);
					}
				}
			}
			
			if(endC>-1) { 
				
				int[] rc= {0,2,3,1};
				int[] xPoints= {187,187,316,286,713,1122,1069,1217,1217};
				int[] yP= {0,108,188,108,219,123,191,137,0};
				int[] yPoints=new int[yP.length];
				for(int i=0; i<9; i++)
					yPoints[i]=endC<=32?(int)ease2(endC,0,yP[i],32):yP[i];
				backG.setColor(new Color(colors[(anzSpieler==2&&endCol==1?2:endCol)*2+1]));
				backG.fillPolygon(xPoints, yPoints, 9);
				int[] targets= {-150,-160, -240,250, -200,593, 1411,-194,  1459,170, 1491,538};
				for(int i=0; i<6; i++) {
					save(backG);
					double cx=ease(endC,714,targets[i*2],255), cy=ease(endC,268,targets[i*2+1],255);
					backG.translate(cx, cy);
					backG.rotate((endC+(i-3)*40)*Math.PI/255);
					backG.scale((endC+200)/255.0, (endC+200)/255.0);
					backG.shear(endC/255.0, 0);
					
					drawCard(backG,i+(anzSpieler==2&&endCol==1?3:rc[endCol])*6,0,0,100);
					restore(backG);
				}
				backG.drawImage(baseImg[16], 510,endC<=32?(int)(ease2(endC,-600,0,32)):0, null);
			}
				//backG.drawString("Phase: "+(phase&0xff)+" in:"+diceIn.length+" out:"+diceOut.length, 10, 190);
		}catch(Exception ex) {ex.printStackTrace();}
	}
	
	int endC=-1,endCol=1;
	public Point getShieldPos(int sp) {
		if(anzSpieler==3) {
			int[] shieldPos= {656,335, 1151,504, 411,580};
			return new Point(shieldPos[sp*2],shieldPos[sp*2+1]);
		}
		int[] shieldPos= {706,357, 1105,523, 708,686, 304,523};
		int pi=(anzSpieler==2 && sp==1)?2:sp;
		return new Point(shieldPos[pi*2],shieldPos[pi*2+1]);
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

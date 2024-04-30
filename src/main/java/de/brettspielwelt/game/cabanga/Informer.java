package de.brettspielwelt.game.cabanga;

import java.io.Serializable;

import de.Data;
import de.brettspielwelt.client.boards.games.GameReceiver;
import de.brettspielwelt.client.boards.games.PlainInformer;
import de.brettspielwelt.tools.IntVector;

public class Informer extends PlainInformer implements Serializable{

	int phase=0; 
	int endPhase2=0;
	int lastCol=0;
	
	static int[] wertung= {0,1,1,1,2,2,2,3,3,3,3,3,3,2,2,2,1,1,1};
	
	int[] score=new int[0];
	int[] platz=new int[0];
	int[] punkte=new int[0];
	
	int[][] feld=new int[4][2];
	IntVector[] hand=new IntVector[6];
	IntVector[] cabanga=new IntVector[6];
	IntVector stapel=new IntVector();
	IntVector ablage=new IntVector();
	
	public Informer(){
		baseInit();
	}
	
	// ----------------------- Init and Starting of Game: reset() / spielStart()  
	public void spielStart() {
		baseInit();

		punkte=new int[anzMitSpieler];
		platz=new int[anzMitSpieler];

		setupRound();
		
		phase=1;
		currentPlayer=startSpieler; //(currentPlayer+anzMitSpieler-1)%anzMitSpieler;
		sendBoard();

		super.spielStart();
	}

	private void setupRound() {
		cabanga=new IntVector[anzMitSpieler];
		hand=new IntVector[anzMitSpieler];
		lastCol=-1;
		int[] startis= {1,3,5,7,12,14,16,18};
		
		
		IntVector startCards=new IntVector(startis,1);
		startCards.mix();
		
		feld=new int[4][2];
		for(int i=0; i<4; i++) {
			feld[i][0]=7<<5|startCards.removeFirst();
			feld[i][1]=7<<5|startCards.removeFirst();
		}
		
		stapel=new IntVector();
		stapel.removeAllElements();
		ablage.removeAllElements();
		for(int c=0; c<4; c++)
			for(int i=1; i<19; i++)
				stapel.addElement(c<<5|i);
		stapel.mix();
		
		for(int i=0; i<anzMitSpieler; i++) {
			hand[i]=new IntVector();
			cabanga[i]=new IntVector();
			for(int j=0; j<8; j++)
				hand[i].addElement(stapel.removeFirst());
			hand[i].sort();
		}
	}
	
	@Override
	public void reset() {
		baseInit();
		super.reset();
	}

	public void baseInit(){
		punkte=new int[6];
		platz=new int[0];
		score=new int[0];
		
		cabanga=new IntVector[6];
		hand=new IntVector[6];
		for(int i=0; i<6; i++) {
			hand[i]=new IntVector();
			cabanga[i]=new IntVector();
		}
		phase=0;
	}

	// ------------- Game End ---------------------------
	
	
	@Override
	public void spielEnde() {
		phase=15;

		calcScorePlatz();

		sendGameStatus();
		sendBoard();

		super.spielEnde();
	}

	private void calcScorePlatz() {
		int[] tie=new int[anzMitSpieler];
		score=new int[anzMitSpieler];
		platz=new int[anzMitSpieler];

		for (int u=0; u<anzMitSpieler; u++) {
			platz[u]=1;
			score[u]=1;
			tie[u]=1;  // Tiebreaker 
		}
		
		for (int u=0; u<anzMitSpieler-1; u++) {
			for (int v=u+1; v<anzMitSpieler; v++) {
				if (score[u] < score[v]) {
					platz[u]++;
				} else if (score[u] < score[v]) {
					platz[v]++;
				}
			}
		}

		for(int u=0; u<anzMitSpieler-1; u++) {
			for(int v=u+1; v<anzMitSpieler; v++) {
				if(score[u]==score[v]) {
					if(tie[v]<tie[u]){
						platz[v]++;
					}else{ if(tie[u]<tie[v])
						platz[u]++;
					}
				}
			}
		}
	}
	
	// --------------- Input received from the Boards -----------------

	@Override
	public void doAnswer(int command,int pl,Data dat){
		switch(command){
		case 700:
			execAction(pl,((Integer)dat.v.elementAt(0)).intValue(),((IntVector)dat.v.elementAt(1)));
			break;
		}
	}

	public IntVector inBetween(int col) {
		IntVector ret=new IntVector();
		int lo=(feld[col][0]&31)<=(feld[col][1]&31)?0:1;
		for(int w=(feld[col][lo]&31)+1; w<(feld[col][1-lo]&31); w++) {
			ret.addElement(col<<5|w);
		}
		return ret;
	}
	
	private void execAction(int curPl, int action, IntVector v) {
		int act=action>>28&7;
		if(!isRunningGame()) return;

		if(act==2 && phase==2 && (endPhase2>>curPl&1)==0) { // zuwerfen
			IntVector betw=inBetween(lastCol);
			boolean cab=true;
			for(int i=0; i<v.size(); i++) {
				if(!betw.contains(hand[curPl].elementAt(v.elementAt(i)))) {
					cab=false;
				}
			}
			if(cab) {
				int[] anim=null;
				int[] anim2=null;
				for(int i=hand[curPl].size()-1; i>=0; i--) {
					if(v.contains(i)) {
						int ca=dropCard(curPl,i);
						anim=appendAnim(anim, ca, 0, curPl|(i<<4), 10, 0);
					}
				}
				if(anim!=null) 
					sendBoard(anim);
				for(int i=0; i<v.size(); i++) {
					drawStapel(currentPlayer);
					anim2=appendAnim(anim2, 0, 10, 0, 0, currentPlayer);
				}
				if(anim2!=null) {
					sendAnim(anim2);
				}
				
				endPhase2|=(1<<curPl);
				if(endPhase2==(1<<anzMitSpieler)-1) {
					for(int i=0; i<anzMitSpieler; i++) cabanga[i].removeAllElements();
					weiter();
				}
				sendBoard();
			}
			System.err.println("V: "+v+" "+cab);
		}
		if(currentPlayer==curPl) {
			if(phase==1 && act==1) {
				int ca=hand[curPl].elementAt(action&255);
				int loc=action>>8&1;
				int col=ca>>5&7;
				
				boolean strafe=false;
				feld[col][loc]=ca;
				dropCard(curPl,action&255);
				if(col==lastCol) strafe=true;
				lastCol=col;
				
				sendBoard(appendAnim(null, (curPl+1)<<24|ca, 0, curPl, 1+loc, col));
				sendAnim(appendAnim(null,7<<24,1,0,0,0));
				
				if(strafe) { // Strafkarte
					drawStapel(curPl);
					sendAnim(appendAnim(null, 0, 10,0, 0,curPl));
				}
				
				IntVector betw=inBetween(col);
				boolean cab=false;
				endPhase2=0;
				for(int sp=0; sp<anzMitSpieler; sp++) {
					cabanga[sp]=new IntVector();
					if(sp!=curPl) {
						for(int i=0; i<hand[sp].size(); i++) {
							if(betw.contains(hand[sp].elementAt(i))) {
								cabanga[sp].addElement(i);
								cab=true;
							}
						}
					}
					if(cabanga[sp].size()==0) // nix da zum werfen also erledigt
						endPhase2|=(1<<sp);
				}
				if(cab) {
					sendAnim(appendAnim(null,7<<24,0,0,0,0));
					phase=2;
				}else weiter();
				//System.err.println("Cabanga:"+cabanga[1]+" Betw: "+betw);
			}
			sendBoard();
		}
	}
	public void weiter() {
		phase=1;
		boolean end=false;
		for(int i=0; i<anzMitSpieler; i++) {
			if(hand[i].size()==0)
				end=true;
		}
		if(end) {
			boolean realEnd=false;
			for(int i=0; i<anzMitSpieler; i++) {
				int minus=0;
				for(int j=0; j<hand[i].size(); j++) {
					minus+=wertung[hand[i].elementAt(j)&31];
				}
				punkte[i]-=minus;
				if(punkte[i]<=-18)
					realEnd=true;
			}
			if(realEnd)
				spielEnde();
			else {
				setupRound();
				
				phase=1;
				currentPlayer=(currentPlayer+1)%anzMitSpieler;
			}
		}else
			currentPlayer=(currentPlayer+1)%anzMitSpieler;
	}
	
	public int dropCard(int sp, int pos) {
		int ret=hand[sp].removeElementAt(pos);
		ablage.addElement(ret);
		return ret;
	}
	public int drawStapel(int sp) {
		if(stapel.size()==0) { stapel.append(ablage); stapel.mix(); }
		int ret=stapel.removeFirst();
		hand[sp].addElement(ret);
		hand[sp].sort();
		return ret;
	}
	// --------------  Sending Stuff  --------------------- 
	public void sendAnim(int sp, int[] anim) {
		if(spieler[sp]!=null)
			sendAnim(spieler[sp],anim);
	}
	
	public int[] appendAnim(int[] arr, int wh, int fr1, int fr2, int to1, int to2) {
		int[] ret;
		int le=arr==null?0:arr.length;
		if(arr!=null) {
			ret=new int[arr.length+5];
			System.arraycopy(arr,0,ret,0,arr.length);
		} else ret=new int[5];
		ret[le]=wh;
		ret[le+1]=fr1;
		ret[le+2]=fr2;
		ret[le+3]=to1;
		ret[le+4]=to2;
		return ret;
	}

	public void sendBoard(){
		for (GameReceiver playerInfo : getReceiverArray()) {
			sendBoard(playerInfo);
		}
	}
	public void sendBoard(int[] anim){
		for (GameReceiver playerInfo : getReceiverArray()) {
			sendBoard(playerInfo,anim);
		}
	}
	
	public void sendBoard(GameReceiver st){
		sendBoard(st,null);
	}
	
	// ------------------ The informations for all the Boards connected ---------------
	
	IntVector empty=new IntVector();
	
	public void sendBoard(GameReceiver st, int[] anim){
		int id=st.getPlaying();
		
		int[] handSize=new int[anzMitSpieler];
		for(int i=0; i<anzMitSpieler; i++)
			handSize[i]=hand[i].size();
		
		Data dat=st.makeData(700,getSpielClient());
		dat.v.addElement(new Integer(anzMitSpieler));
		dat.v.addElement(new Integer(st.getPlaying()));
		dat.v.addElement(new Integer(phase));
		dat.v.addElement(new Integer(currentPlayer));
		dat.v.addElement(new Integer(startSpieler));
		dat.v.addElement(new Integer(lastCol));
		dat.v.addElement(new Integer(endPhase2));
		dat.v.addElement(feld);
		dat.v.addElement(id>-1?hand[id]:empty);
		dat.v.addElement(id>-1?cabanga[id]:empty);
		dat.v.addElement(handSize);
		dat.v.addElement(punkte);
		dat.v.addElement(platz);
		
		if(anim!=null)
			dat.v.addElement(anim);
		else
			dat.v.addElement(new int[0]);
		st.sendDataObject(dat);
		sendGameStatus(st);
	}

	public void sendGameStatus(GameReceiver st) {
		Data dat=st.makeData(702,getSpielClient());

		for(int i=0; i<4; i++) {
			if(spieler[i]!=null) {
				dat.v.addElement(spieler[i].getPName());
			} else {
				dat.v.addElement("");
			}
		}
		st.sendDataObject(dat);
	}

}

package de.brettspielwelt.game;

import de.Data;
import de.brettspielwelt.client.boards.games.GameReceiver;
import de.brettspielwelt.client.boards.games.PlainInformer;
import de.brettspielwelt.tools.IntVector;

public class Informer extends PlainInformer{

	int phase=0; 
	int round=0,nextRound=0;	
	
	int[] score=new int[0];
	int[] platz=new int[0];
	int[] punkte=new int[6];
	
	IntVector stapel=new IntVector();
	IntVector ablage=new IntVector();
	
	IntVector[] hand=new IntVector[6];
	IntVector[][] auslage=new IntVector[6][5];
	int wunsch=-1;
	
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
	
	public Informer(){
		baseInit();
	}
	
	public void construct(int nr) {
//		super.construct(nr);
		initGame();
		baseInit();
	}

	public int getMinMaxPlayer(){
		return (2<<16)|6;
	}

	public int getSpielID() {
		return 169;
	}
	public String getSpielClient() {
		return "PasstNichtBoard";
	}

	public void sendComplete() {
		sendGameStatus();
		sendBoard();
	}

	public void sendComplete(GameReceiver pl) {
		sendGameStatus(pl);
		sendBoard(pl);
	}
	@Override
	public void spielStart() {
		initGame();
		super.spielStart();
		
		los();
	}
	
	@Override
	public void reset() {
		initGame();
		super.reset();
	}

	// Boilerplate ends ---------------------------
	
	public void initGame() {
		baseInit();
	}

	@Override
	public void spielEnde() {
		for(int i=0; i<4; i++)
			phase=15;

		calcScorePlatz();

		sendGameStatus();
		sendBoard();

	//	insertGame(getSpielID(), score, platz);
		super.spielEnde();
	}

	private void calcScorePlatz() {
		int[] hi=new int[anzMitSpieler];
		score=new int[anzMitSpieler];
		platz=new int[anzMitSpieler];

		for (int u=0; u<anzMitSpieler; u++) {
			hi[u]=0;  // Tiebreaker countCross(u);
		}
		
		for (int u=0; u<anzMitSpieler-1; u++) {
			for (int v=u+1; v<anzMitSpieler; v++) {
				if (score[u] > score[v]) {
					platz[u]++;
				} else if (score[u] < score[v]) {
					platz[v]++;
				}
			}
		}

		for(int u=0; u<anzMitSpieler-1; u++) {
			for(int v=u+1; v<anzMitSpieler; v++) {
				if(score[u]==score[v]) {
					if(hi[v]>hi[u]){
						platz[v]++;
					}else{ if(hi[u]>hi[v])
						platz[u]++;
					}
				}
			}
		}
	}

	
	public void los() {
		baseInit();
		startRound();
		currentPlayer=startSpieler;
		
		sendBoard();
	}

	public int drawCard(int sp) {
		if(stapel.size()==0) {
			int top=ablage.removeLast();
			stapel.append(ablage);
			stapel.mix();
			ablage.removeAllElements();
			ablage.addElement(top);
		}
		return stapel.removeLast();
	}
	
	public void startRound() {
		round++;
		nextRound=0;

		for(int i=0; i<6; i++) {
			hand[i]=new IntVector();
			for(int j=0; j<5; j++)
				auslage[i][j]=new IntVector();
		}
		stapel=new IntVector();
		for(int c=0; c<2; c++)
			for(int i=0; i<5; i++) {
				for(int j=0; j<6; j++) {
					stapel.addElement(((i+1)<<4|j));
				}
			}
		for(int i=0; i<4; i++)
			stapel.addElement(0);
		
		stapel.mix();
		ablage=new IntVector();
		for(int i=0; i<stapel.size(); i++)
			if(stapel.elementAt(i)!=0) {
				ablage.addElement(stapel.removeElementAt(i));
				break;
			}
		
		for(int i=0; i<anzMitSpieler; i++) {
			for (int j=0; j<5; j++) {
				hand[i].addElement(drawCard(i));
			}
		}
		wunsch=-1;
		phase=1;
		int min=5000, minSp=-1;
		for(int i=0; i<anzMitSpieler; i++) {
			if((punkte[i]&255)<min) {
				min=punkte[i]&255;
				minSp=i;
			}
		}
		currentPlayer=minSp;
	}
	

	public void baseInit(){
		round=0;
		punkte=new int[6];
		for(int i=0; i<6; i++)
			punkte[i]=1000;
		platz=new int[0];
		score=new int[0];
		nextRound=0;
		startRound();
		currentPlayer=0;
	}
	
	
	@Override
	public void doAnswer(int command,int pl,Data dat){
		switch(command){
		case 700:
//		    if(!pl.kiebitz){
				execAction(pl,((Integer)dat.v.elementAt(0)).intValue());
				break;
//			}
		}
	}

	static final int JOKER=0;
	
	private void execAction(int curPl, int action) {
		int act=action>>28&7;
		if(!isRunningGame()) return;

		if(phase==3 && act==7) {
			nextRound|=1<<curPl;
			if(nextRound==(1<<anzMitSpieler)-1) {
				startRound();
				sendBoard();
				sendAnim(appendAnim(null,8<<16,1, 0,0,0)); //wegc
			}
			sendBoard();
		}
		if(phase==2 && act==4) {
			int wu=action&15;
			if(wu<6) wunsch=wu;
			else if(wu<11) wunsch=(wu-5)<<4;
			phase=1;
			weiter();
			sendBoard();
		}
		if(phase==1 && act==1 && currentPlayer==curPl) { // (Passt) Spiele Karte aus Hand auf ablage
			 // geht immer
			int hIndx=action&31;
			if(hIndx<hand[curPl].size()) {
				int hCol=hand[curPl].elementAt(hIndx)>>4;
				int hZahl=hand[curPl].elementAt(hIndx)&15;
				int col=ablage.lastElement()>>4;
				int zahl=ablage.lastElement()&15;
				if(wunsch!=-1 && col==0 && zahl==0) { // joker liegt und wunsch ist
					col=wunsch>>4;
					zahl=col==0?wunsch&15:-1;
				}
				if(hand[curPl].elementAt(hIndx)==JOKER) {
					int ca=hand[curPl].removeElementAt(hIndx);
					sendAnim(appendAnim(null,1<<16,curPl,ca,hIndx,0));
					ablage.addElement(ca);
					phase=2;
					sendBoard();
				}else
				if(col==hCol || zahl==hZahl) { // wenn Farbe oder Zahl passt
					int ca=hand[curPl].removeElementAt(hIndx);
					sendAnim(appendAnim(null,1<<16,curPl,ca,hIndx,0));
					ablage.addElement(ca);
					wunsch=-1;
					weiter();
					sendBoard();
				}
			}
		}
		if(phase==1 && act==2 && currentPlayer==curPl) { // (Passt nicht) Spiele Karte aus Hand in Auslage
			int pa=auslagePasst(curPl);
			if(pa==0) { // geht nur wenn keine Auslage passt und die karte nicht passt
				int hIndx=action&31;
				if(hIndx<hand[curPl].size() && hand[curPl].elementAt(hIndx)!=0) { // Kein joker
					int hCol=hand[curPl].elementAt(hIndx)>>4;
					int hZahl=hand[curPl].elementAt(hIndx)&15;
					int col=ablage.lastElement()>>4;
					int zahl=ablage.lastElement()&15;
					if(wunsch!=-1 && col==0 && zahl==0) { // joker liegt und wunsch ist
						col=wunsch>>4;
						zahl=col==0?wunsch&15:-1;
					}

					if(col!=hCol && zahl!=hZahl) { // wenn Farbe oder Zahl nicht passt
						auslage[curPl][hCol-1].addElement(hand[curPl].removeElementAt(hIndx));
						int ca=drawCard(curPl);
						hand[curPl].addElement(ca);
						sendBoard(appendAnim(null,6<<16,curPl,ca,hIndx,0));
						weiter();
						sendBoard();
					}
				}				
			}
		}
		if(phase==1 && act==3 && currentPlayer==curPl) { // Spiele Karte aus Auslage auf ablage
			int pa=auslagePasst(curPl);
			if(pa!=0) { // geht nur wenn eine Auslage auch passt
				int aslIndx=action&7;
				if((pa>>aslIndx&1)==1) { // Auslage passt
					int ca=auslage[curPl][aslIndx].removeLast();
					sendAnim(appendAnim(null,3<<16,curPl,ca,aslIndx,0));
					ablage.addElement(ca);
					wunsch=-1;
					weiter();
					sendBoard();
				}
			}
		}
	}

	public int auslagePasst(int sp) { // returns bitfeld der auslagestapel die passen
		int ret=0;
		int col=ablage.lastElement()>>4;
		int zahl=ablage.lastElement()&15;
		if(wunsch!=-1 && col==0 && zahl==0) { // joker liegt und wunsch ist
			col=wunsch>>4;
			zahl=col==0?wunsch&15:-1;
		}
		if(col>0 && auslage[sp][col-1].size()>0) // farbe passt
			ret|=1<<(col-1);
		for(int i=0; i<5; i++)
			if(auslage[sp][i].size()>0 && (auslage[sp][i].lastElement()&15)==zahl) // zahl passt
				ret|=1<<i;
		return ret;
	}
	
	public void weiter() {
		if(hand[currentPlayer].size()==0)
			rundenEnde();
		else currentPlayer=(currentPlayer+1)%anzMitSpieler;
	}
	
	public void rundenEnde() {
		phase=3;
		for(int sp=0; sp<anzMitSpieler; sp++) {
			int minus=0, plus=0;
			int before=(punkte[sp]&0xffff)-1000;
			for(int i=0; i<hand[sp].size(); i++) {
				int ca=hand[sp].elementAt(i);
				if(ca==0) minus+=10;
				else minus+=ca&15;
			}
			for(int j=0; j<5; j++) {
				int st=auslage[sp][j].size();
				for(int i=0; i<st; i++) {
					int ca=auslage[sp][j].elementAt(i);
					plus+=ca&15;
				}
			}
			punkte[sp]=plus<<16|minus<<24|(1000+before+(plus-minus));
		}
		sendBoard();
		sendAnim(appendAnim(null,8<<16,0, 0,0,0));
		for(int sp=0; sp<anzMitSpieler; sp++)
			if(((punkte[sp]&0xffff)-1000)>=50)
				spielEnde();
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
	
	public int[][] getAuslagen() {
		int[][] ret=new int[anzMitSpieler][6];
		for(int i=0; i<anzMitSpieler; i++) {
			//ret[i]=new IntVector();
			for(int j=0; j<5; j++) {
				if(auslage[i][j].size()>0) {
					ret[i][j]=auslage[i][j].lastElement();
					if(auslage[i][j].size()>1) {
						ret[i][j]|=auslage[i][j].elementAt(auslage[i][j].size()-2)<<8;
					}
					ret[i][j]|=auslage[i][j].size()<<16;
				}
			}
			ret[i][5]=hand[i].size();
		}
		return ret;
	}
	IntVector empty=new IntVector();
	public void sendBoard(GameReceiver st, int[] anim){
		int id=st.getPlaying();
		int[][] ausls=getAuslagen();
		
		Data dat=st.makeData(700,getSpielClient());
		dat.v.addElement(new Integer(anzMitSpieler));
		dat.v.addElement(new Integer(st.getPlaying()));
		dat.v.addElement(new Integer(phase));
		dat.v.addElement(new Integer(round));
		dat.v.addElement(new Integer(currentPlayer));
		dat.v.addElement(new Integer(startSpieler));
		dat.v.addElement(id>-1?hand[id]:empty);
		dat.v.addElement(ausls);
		dat.v.addElement(ablage.lastElement());
		dat.v.addElement(wunsch);
		dat.v.addElement(punkte);
		dat.v.addElement(nextRound);

		if(anim!=null)
			dat.v.addElement(anim);
		else
			dat.v.addElement(new int[0]);
		st.sendDataObject(dat);
		sendGameStatus(st);
	}



	public void sendGameStatus(GameReceiver st) {
		Data dat=st.makeData(702,getSpielClient());

		for(int i=0; i<5; i++) {
			if(spieler[i]!=null) {
				dat.v.addElement(spieler[i].getPName());
			} else {
				dat.v.addElement("");
			}
		}
		st.sendDataObject(dat);
	}

	public void sendGameStatus() {
		for (GameReceiver playerInfo : getReceiverArray()) {
			sendGameStatus(playerInfo);
		}
	}
}

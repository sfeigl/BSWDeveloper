package de.brettspielwelt.game.hit;

import java.util.Vector;

import de.Data;
import de.brettspielwelt.client.boards.games.GameReceiver;
import de.brettspielwelt.client.boards.games.PlainInformer;
import de.brettspielwelt.tools.IntVector;

public class Informer extends PlainInformer{

	int phase=0; 
	int round=0, amountPoeppel=4;	
	
	int[] score=new int[0];
	int[] platz=new int[0];
	int[] punkte=new int[6];
	
	int[][] poeppel=new int[4][4]; // [-4,43]
	int[] cash=new int[4];
	int[][] hand=new int[4][3];
	int[] auslage=new int[5];
	IntVector stapel=new IntVector();
	IntVector ablageStapel=new IntVector();
	IntVector[] ablage=new IntVector[4];
	IntVector[] nachzieh=new IntVector[4];
	
	int sCard, sPoep;
	IntVector possible=new IntVector();
	int geschuetzt;
	int feldLen=40;
	boolean[] canPlay=new boolean[3];
	
	// summe an bewegung == 10
	int[] zehner=new int[4];
	int[] logIn=new int[4];
	
	// entfernt eigene figuren schlagen und ueber zielfeld hinaus
	public IntVector filterPossible(int sp, int pp, int card, IntVector inp) {
		for(int i=inp.size()-1; i>=0; i--) {
			int target=inp.elementAt(i);
			for(int j=0; j<4; j++) {
				if(poeppel[sp][j]==target) // eigene Figur schlagen geht nicht
					inp.removeElementAt(i);
			}
			if(target>43) // nicht übers ziel hinaus
				inp.removeElementAt(i);
		}
		return inp;
	}
	
	public IntVector selectPP(int sp, int card, int pp) {
		return null; // all possible for pp and card
	}
	
	public IntVector toPossiblePerm(int sp, int pp, Vector<int[]> res) {
		IntVector ret=new IntVector();
		for(int i=0; i<res.size(); i++) {
			int[] rp=res.elementAt(i);
			if(rp[i]>0 && !ret.contains(rp[i]))
				ret.addElement(rp[i]);
		}
		return ret;
	}
	public Vector<int[]> permutationen(int sp, IntVector[] v) {
		Vector<int[]> ret=new Vector<int[]>();
		for(int a1=0;a1<v[0].size(); a1++) {
			for(int a2=0;a2<v[1].size(); a2++) {
				for(int a3=0;a3<v[2].size(); a3++) {
					for(int a4=0;a4<v[3].size(); a4++) {
						int sum=v[0].elementAt(a1)+v[1].elementAt(a2)+v[2].elementAt(a3)+v[3].elementAt(a4);
						if(sum==10) {
							int[] res=new int[] {a1,a2,a3,a4};
							ret.add(res);
						}
					}
				}
			}
		}
		return ret;
	}
	
	public void schlagen(int sp, int f) {
		if(f>=feldLen) return;
		for(int s=0; s<4; s++) {
			for(int p=0; p<4; p++) {
				if(poeppel[s][p]>-1 && poeppel[s][p]<feldLen) {
					int baseP=(poeppel[s][p]+10*s)%feldLen;
					if(sp!=s && (sp*10+f)%(feldLen)==baseP) {
						for(int w=-1; w>-5; w--) {
							boolean belegt=false;
							for(int j=0; j<4; j++)
								if(poeppel[s][j]==w) belegt=true;
							if(!belegt) {
								int op=poeppel[s][p];
								poeppel[s][p]=w;
								sendBoard(appendAnim(null, s, s, op+4, s, w+4));
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public boolean choosePossible(int sp, int pp, int card, int x) {
		if(card!=33) {
			int op=poeppel[sp][pp];
			int zf=possible.elementAt(x)&255;
			poeppel[sp][pp]=possible.elementAt(x)&255;
			possible.removeAllElements();
			sendBoard(appendAnim(null, sp, sp, op+4, sp, (zf)+4));
			schlagen(sp,zf);
			
		}
		if(card==33) {
			logIn[pp]=possible.elementAt(x)>>8;
			poeppel[sp][pp]=possible.elementAt(x)&255;
			possible.removeAllElements();
			int sum=0;
			for(int i=0; i<4; i++)
				sum+=logIn[i]>0?logIn[i]:0;
			if(sum!=10) return false;
			// move poeppel nextfree
		}
		possible=new IntVector();
		return true;
	}
	
	public boolean eigen(int sp, int zf) {
		for(int i=0; i<4; i++)
			if(poeppel[sp][i]==zf) return true;
		return false;
	}
	public boolean frei(int sp, int i) {
		for(int s=0; s<4; s++) {
			for(int p=0; p<4; p++) {
				if(poeppel[s][p]>-1 && poeppel[s][p]<feldLen) {
					int baseP=(poeppel[s][p]+10*s)%feldLen;
					if((sp*10+i)%feldLen==baseP)
						return false;
				}
			}
		}
		return true;
	}
	public IntVector selectPoeppel(int sp, int pp, int card) { // waehle eine figur und gib die mögl. felder zurueck
		card=card<24?card%6:card;
		IntVector ret=new IntVector();
		int[] zahlenK= {0,1, 1,3, 3,6, 24,6, 25,7, 26,8, 28,11, 29,4, 30,5 };
		boolean darfSchlag=card!=28;
		boolean brecher=card==24;
		for(int i=0; i<zahlenK.length; i+=2) {
			if(card==zahlenK[i]) {
				int zielfeld=-1;
				if(poeppel[sp][pp]>=0)
					zielfeld=poeppel[sp][pp]+zahlenK[i+1];
				else if(card==0 || card==3)
					zielfeld=1;
				int nf=nextFree(sp,zielfeld,darfSchlag,brecher,true);
				if(nf>-1)
					ret.addElement((zielfeld-poeppel[sp][pp])<<8|nf);
			}
		}
		if(card<24) card=card%6;
		if(card==2) {
			for(int j=1; j<6; j++) {
				int zielfeld=-1;
				if(poeppel[sp][pp]>=0)
					zielfeld=poeppel[sp][pp]+j;
				int nf=nextFree(sp,zielfeld,false,false,true);
				if(nf>-1)
					ret.addElement((zielfeld-poeppel[sp][pp])<<8|nf);
			}
		}
		if(card==5) {
			int[] ecken= {4,8,10,14,18,20,24,28,30,34,38};
			for(int j=0; j<(anzMitSpieler==3?8:ecken.length); j++) {
				int zielfeld=-1;
				if(poeppel[sp][pp]>-1 && poeppel[sp][pp]<ecken[j] && frei(sp,ecken[j]))
					zielfeld=ecken[j];
				if(zielfeld>-1) {
					ret.addElement((ecken[j]-poeppel[sp][pp])<<8|ecken[j]);
					return ret;
				}
			}
		}
		if(card==27) {
			if(poeppel[sp][pp]>0 && !eigen(sp, poeppel[sp][pp]-1))
				ret.addElement((-1)<<8|(poeppel[sp][pp]-1));
			if(poeppel[sp][pp]>0 && !eigen(sp, poeppel[sp][pp]+3))
				ret.addElement(3<<8|poeppel[sp][pp]+3);
		}
		if(card==31) {
			int[] ecken= {4,8,10,14,18,20,24,28,30,34,38};
			int ec=0;
			for(int j=0; j<(anzMitSpieler==3?8:ecken.length); j++) {
				int zielfeld=-1;
				if(poeppel[sp][pp]>-1 && poeppel[sp][pp]<ecken[j]) {
					ec++;
					if(!eigen(sp,ecken[j]))
						zielfeld=ecken[j];
				}
				int nf=nextFree(sp,zielfeld,true,false,true);
				if(nf>-1) {
					ret.addElement((zielfeld-poeppel[sp][pp])<<8|nf);
				}
				if(ec==3)
					return ret;
			}
		}
		if(card==32) {
			int[] ecken= {1,11,21,31};
			for(int j=0; j<(anzMitSpieler==3?3:ecken.length); j++) {
				int zielfeld=-1;
				if(poeppel[sp][pp]>-1 && poeppel[sp][pp]<ecken[j] && !eigen(sp,ecken[j]))
					zielfeld=ecken[j];
				int nf=nextFree(sp,zielfeld,true,false,true);
				if(nf>-1) {
					ret.addElement((zielfeld-poeppel[sp][pp])<<8|nf);
					return ret;
				}
			}
		}
		if(card==34) {
			boolean flag=false;
			for(int i=1; i<feldLen; i++) {
				if(poeppel[sp][pp]+i<feldLen && poeppel[sp][pp]>-1 && frei(sp,poeppel[sp][pp]+i) && flag) {
					ret.addElement(i<<8|poeppel[sp][pp]+i);
					if(ret.size()==2)
						return ret;
					flag=false;
				}
				if(!flag && !frei(sp,poeppel[sp][pp]+i))
					flag=true;
			}
		}
		if(card==35) {
			for(int i=1; i<feldLen; i++) {
				int zielfeld=poeppel[sp][pp]+i;
				if(zielfeld<feldLen && poeppel[sp][pp]>-1 && !frei(sp,zielfeld) && !eigen(sp,zielfeld)) {
					int nf=nextFree(sp,zielfeld,true,false,true);
					if(nf>-1) {
						ret.addElement((zielfeld-poeppel[sp][pp])<<8|nf);
						return ret;
					}
				}
			}
		}
		if(card==36) {
			for(int i=1; i<feldLen; i++) {
				if(poeppel[sp][pp]+i<feldLen && poeppel[sp][pp]>-1 && !frei(sp,poeppel[sp][pp]+i)) {
					if(i>1) ret.addElement((i-1)<<8|poeppel[sp][pp]+i-1);
					return ret;
				}
			}
		}

		if(card==37) {
			int[] ecken= {9,19,29,39};
			for(int j=0; j<(anzMitSpieler==3?3:ecken.length); j++) {
				int zielfeld=-1;
				if(poeppel[sp][pp]>-1 && poeppel[sp][pp]<ecken[j] && !eigen(sp,ecken[j]))
					zielfeld=ecken[j];
				int nf=nextFree(sp,zielfeld,true,false,true);
				if(nf>-1) {
					ret.addElement((zielfeld-poeppel[sp][pp])<<8|nf);
					return ret;
				}
			}
		}
		if(card==33) {
			if(logIn[pp]>-1) {
//				logIn[pp]=-1; // auflösen wenn neu angewählt alle 4 figuren zurück
//				poeppel[sp][pp]=zehner[pp];
				for(int i=0; i<4; i++) {
					logIn[i]=-1; // auflösen wenn neu angewählt alle 4 figuren zurück
					poeppel[sp][i]=zehner[i];
				}
			}
			int bereitsGenutzt=0;
			for(int i=0; i<4; i++) {
				if(logIn[i]>-1) bereitsGenutzt+=logIn[i];
			}
			int rest=10-bereitsGenutzt;
			for(int i=1; i<=rest; i++) {
				if(poeppel[sp][pp]>-1)
					if(!eigen(sp,poeppel[sp][pp]+i)) {
						int nf=nextFree(sp,poeppel[sp][pp]+i, false,false, true);
						if(nf>-1)
							ret.addElement(i<<8|nf);
					}
			}
			//filterPossible(sp, pp, card, ret);
			
//			
//			IntVector[] v=new IntVector[4];
//			for(int p=0; p<4; p++) {
//				if(logIn[p]==-1) {
//					for(int i=1; i<=rest; i++) {
//						if(poeppel[sp][pp]>-1)
//							ret.addElement(i<<8|nextFree(sp,poeppel[sp][pp]+i, false));
//					}
//					v[p]=playCard(sp,p,card);
//					filterPossible(sp, p, card, v[p]);
//				}else
//					v[p]=new IntVector(); // hat sich schon bewegt keine möglichkeit mehr
//				
//				Vector<int[]> res=permutationen(sp, v);
//				return toPossiblePerm(sp, pp, res);
//			}
		}
		return ret;
	}
	
	public int nextFree(int sp, int zielF, boolean darfSchlag, boolean brecher, boolean darfEinlauf) {
		if(zielF<0) return -1;
		return nextFree(sp, zielF, darfSchlag, brecher, darfEinlauf, true);
	}
	public int nextFree(int sp, int zielF, boolean darfSchlag, boolean brecher, boolean darfEinlauf, boolean first) {
		// test
//		currentPlayer=2;
////		geschuetzt=0;
////		hand[0][0]=1;
//		hand[1][0]=21;
//		poeppel[2][3]=-2;
//		poeppel[2][2]=9;
//		poeppel[0][0]=1;
//		poeppel[0][3]=-1;
//		poeppel[0][1]=4;
//		poeppel[0][2]=9;
		
		if(zielF>=(darfEinlauf?feldLen+4:feldLen)) return -1; // ohne einlauferlaubnis sowieso alles üpber ziel ungültig
		for(int s=0; s<4; s++) {
			for(int p=0; p<4; p++) {
				if(poeppel[s][p]>-1) {
					if(s!=sp && (poeppel[s][p]>=feldLen || poeppel[s][p]<0)) continue;// schon im Ziel ignoriere
					int baseP=(poeppel[s][p]+10*s)%feldLen;
					int outerZf=(sp*10+zielF)%feldLen;
					if(s==sp) { baseP=poeppel[s][p]; outerZf=zielF; } // die eigenen immer ohne modulo
					if(zielF>=feldLen && darfEinlauf && sp==s) { // nur eigene können im einlauf sein
						if(poeppel[s][p]==zielF) { // da ist einer von mir
							if(first) return -1;
							return nextFree(sp,zielF+1,darfSchlag,brecher,darfEinlauf, false);
						}
					}
					if(zielF<feldLen && outerZf==baseP) { // Da steht was im umlauf
						if(first) {
							if(sp==s) return -1; //eigene figur geht eigentlich gar nicht
							if(sp!=s && darfSchlag && (brecher || (geschuetzt>>getPlayerFromCol(s)&1)==0))
								return zielF;
							//else return -1;
						}
						return nextFree(sp,zielF+1,darfSchlag,brecher,darfEinlauf, false);
					}
					
				}
			}
		}
		return zielF;
	}
	public int getPlayerFromCol(int col) {
		if(anzMitSpieler==2)
			return col==2?1:0;
		return col;
	}
	public boolean canPlayCard(int sp, int card) {
		if(card==4 || card==16 || card==10 || card==22||card==38||card==39) // Geld geht immer
			return true;
		if(card==33) {
			IntVector[] v=new IntVector[4];
			int sum=0;
			for(int p=0; p<4; p++) {
				v[p]=selectPoeppel(sp,p,card);
				sum+=v[p].size();
//				filterPossible(sp, p, card, v[p]);
			}
			if(sum>=10) return true;
			Vector<int[]> res=permutationen(sp, v);
			if(res.size()>0) return true;
			return false;
		}

		for(int pp=0; pp<4; pp++)
			if(selectPoeppel(sp, pp, card).size()>0) return true;

		return false;
	}
	
	
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
		initGame();
		baseInit();
	}

	public int getMinMaxPlayer(){
		return (2<<16)|4;
	}

	public int getSpielID() {
		return 176;
	}
	
	@Override
	public String getSpielClient() {
		return "HITBoard";
	}

	@Override
	public void sendComplete() {
		sendGameStatus();
		sendBoard();
	}

	@Override
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

	public void checkEnd() {
		for(int i=0; i<anzMitSpieler; i++) {
			punkte[i]=0;
			for(int j=0; j<amountPoeppel; j++) {
				if(poeppel[anzMitSpieler==2&&i==1?2:i][j]>=feldLen) punkte[i]++;
			}
			if(punkte[i]>=1) //=amountPoeppel)
				spielEnde();
		}
	}
	@Override
	public void spielEnde() {
		for(int i=0; i<4; i++)
			phase=15;

		calcScorePlatz();

		sendGameStatus();
		sendBoard();

		//insertGame(getSpielID(), score, platz);
		super.spielEnde();
	}

	private void calcScorePlatz() {
		int[] hi=new int[anzMitSpieler];
		score=new int[anzMitSpieler];
		platz=new int[anzMitSpieler];

		for (int u=0; u<anzMitSpieler; u++) {
			score[u]=punkte[u];
			platz[u]=1;
			for(int i=0; i<amountPoeppel;i++)
				hi[u]+=poeppel[u][i];  // Tiebreaker countCross(u);
		}
		
		for (int u=0; u<anzMitSpieler-1; u++) {
			for (int v=u+1; v<anzMitSpieler; v++) {
				if (score[u] < score[v]) {
					platz[u]++;
				} else if (score[u] > score[v]) {
					platz[v]++;
				}
			}
		}

		for(int u=0; u<anzMitSpieler-1; u++) {
			for(int v=u+1; v<anzMitSpieler; v++) {
				if(score[u]==score[v]) {
					if(hi[v]<hi[u]){
						platz[v]++;
					}else{ if(hi[u]<hi[v])
						platz[u]++;
					}
				}
			}
		}
	}

	
	public void los() {
		baseInit();

		amountPoeppel=4;
		if(anzMitSpieler==4) amountPoeppel=3;
		stapel.removeAllElements();
		for(int j=24; j<40; j++) {
			stapel.addElement(j);
			stapel.addElement(j);
		}
		stapel.mix();
		
		for(int i=0; i<5; i++)
			auslage[i]=stapel.removeFirst();

		for(int i=0; i<anzMitSpieler; i++) {
			int[] rc= {0,2,3,1};
			nachzieh[i]=new IntVector();
			for(int j=0; j<6; j++)
				nachzieh[i].addElement(j+(anzMitSpieler==2&&i==1?3:rc[i])*6);
			nachzieh[i].mix();
			takeCards(i,0);
		}

		geschuetzt=0;
		phase=1;
		for(int i=0; i<4; i++) {
			poeppel[i][0]=-3;
			poeppel[i][1]=-2;
			poeppel[i][2]=-1;
			poeppel[i][3]=-4;
		}
		
		for(int si=0; si<anzMitSpieler; si++) {
			int i=anzMitSpieler==2&&si==1?2:si;
			poeppel[i][0]=1;
			poeppel[i][1]=-2;
			poeppel[i][2]=-1;
			poeppel[i][3]=amountPoeppel==3?43:-3;
//			poeppel[i][0]=43;
//			poeppel[i][1]=40;
//			poeppel[i][2]=38;
//			poeppel[i][3]=1; //33;
		}
		//poeppel[0][3]=43;
//		poeppel[2][3]=19;
		
//		hand[0][0]=3;
//		hand[0][2]=2;
//		hand[0][1]=1;
		sCard=-1;
		sPoep=-1;
		
		feldLen=40;
		if(anzMitSpieler==3)
			feldLen=30;
		canPlay=canPlay(currentPlayer);
		
		sendBoard();
		
	}
	
	public void baseInit(){
		round=0;
		punkte=new int[4];
		platz=new int[0];
		score=new int[0];
		auslage=new int[5];
		
		nachzieh=new IntVector[4];
		ablage=new IntVector[4];
		ablageStapel=new IntVector();
		stapel=new IntVector();
		
		stapel.removeAllElements();
		for(int j=5; j<40; j++) {
			stapel.addElement(j);
			stapel.addElement(j);
		}
		stapel.mix();
		ablageStapel.removeAllElements();
		
		for(int i=0; i<5; i++)
			auslage[i]=-1;

		for(int i=0; i<4; i++) {
			hand[i]=new int[] {-1,-1,-1};
			nachzieh[i]=new IntVector();
			ablage[i]=new IntVector();
			cash[i]=0;
		}

		phase=0;
		currentPlayer=0;
	}
	
	int[] idGes= {1,7,13,19,29,38};
	public void setGeschuetzt() {
		geschuetzt=0;
		idGes= new int[] {1,7,13,19,29,38};
		for(int i=0; i<4; i++) {
			if(ablage[i].size()>0) {
				for(int j=0; j<idGes.length; j++)
					if(ablage[i].lastElement()==idGes[j])
						geschuetzt|=1<<i;
			}
		}
	}

	int takeRest=0;
	public boolean takeCards(int sp, int start) {
		for(int i=start; i<3; i++) {
			if(nachzieh[sp].size()==0) {
				for(int j=0; j<idGes.length; j++)
					if(ablage[sp].lastElement()==idGes[j]) {
						takeRest=i;
						return true;
					}

				nachzieh[sp]=new IntVector(ablage[sp].getArray(),5);
				ablage[sp]=new IntVector();
				nachzieh[sp].mix();
			}
			if(nachzieh[sp].size()>0)
				hand[sp][i]=nachzieh[sp].removeLast();
			else
				hand[sp][i]=-1;
		}
		return false;
	}
	public boolean buyCard(int sp, int pl) {
		int bc=auslage[pl];
		nachzieh[sp].addElement(bc);
		auslage[pl]=-1;
		sendBoard(appendAnim(null,2<<16,sp,bc,pl,0));
		return true;
	}

	
	@Override
	public void doAnswer(int command,int pl,Data dat){
		switch(command){
		case 700:
			execAction(pl,((Integer)dat.v.elementAt(0)).intValue());
			break;
		}
	}

	public int getColFromPos(int pos) {
		if(anzMitSpieler==2)
			return pos==1?2:0;
		return pos;
	}
	private void execAction(int curPl, int action) {
		int act=action>>28&7;
		if(!isRunningGame()) return;

		cash[curPl]=20;
		//canPlay=canPlay(curPl);
		if((phase==1 || phase==2 || phase==3) && act==4) { // karte kaufen
			int slot=action&7;
			if(slot>-1 && slot<5 && auslage[slot]>-1) {
				int card=auslage[slot];
				int[] cost= {6,4,5,5,7,5,3,10,6,8,12,10,8,8,6,6};
				if(cash[curPl]>=cost[card-24]) {
					buyCard(curPl,slot);
					cash[curPl]-=cost[card-24];
				}
			}
			sendBoard();
		}
		
		if(phase==3 && act==7) {
			weiter(0);
			sendBoard();
		}else
		if(phase==2 && act==5 && curPl==currentPlayer) {
			boolean keep=(action&1)==1;
			int weg=-1;
			if(keep)
				weg=ablage[curPl].removeLast();
			nachzieh[curPl]=new IntVector(ablage[curPl].getArray(),5);
			ablage[curPl]=new IntVector();
			if(keep)
				ablage[curPl].addElement(weg);
			nachzieh[curPl].mix();
			weiter(takeRest);
			sendBoard();
		}else
		if(phase==1 && curPl==currentPlayer) {
			
			int poepCol=curPl;
			poepCol=getColFromPos(curPl);
			

			if(act==1) {
				int sumC=0;
				int selC=-1;
				for(int i=0; i<3; i++)
					if((action>>i&1)==1) { sumC++; selC=i; }
				if(sumC==0) {
					possible.removeAllElements();
				}else
				if(sumC==2) {
					sCard=1<<8|(action&7);
					for(int i=0; i<4; i++)
						if(poeppel[poepCol][i]<0) {
							sPoep=i;
							possible=selectPoeppel(poepCol, i, 0);
						}
					
				}else
				if(sumC==1 && hand[curPl][selC]!=-1){
					
					if(sCard!=-1) {
						for(int i=0; i<4; i++)
							poeppel[poepCol][i]=zehner[i];
					}
					sCard=selC;
					
					for(int i=0; i<4; i++)
						zehner[i]=poeppel[poepCol][i];
					
//					boolean canPlay=false;
//					for(int i=0; i<anzPoeppel; i++)
//						if(selectPoeppel(poepCol, i, hand[curPl][sCard]).size()>0)
//							canPlay=true;
					
					//if(!canPlay)
					int card=hand[curPl][selC];
					if(!canPlay[sCard] || card==4 || card==16 || card==10 || card==22||card==38||card==39) // Geld geht immer
						cardDone(curPl);
					
					possible=new IntVector();
				}
			}
			if(act==2 && sCard>-1 && sCard<1<<8) {
				sPoep=action&7;
				possible=selectPoeppel(poepCol, sPoep, hand[curPl][sCard]);
			}
			if(act==3 && sCard>-1 && sPoep>-1) {
				boolean done=false;
				if((sCard>>8&1)==1)
					done=choosePossible(poepCol, sPoep, 0, action&31);
				else
					done=choosePossible(poepCol, sPoep, hand[curPl][sCard], action&31);
				if(done) {
					cardDone(curPl);
					checkEnd();
				}
			}
			sendBoard();
		}
	}

	private void cardDone(int curPl) {
		int[] earn= {0,2,0,0,3,1, 0,2,0,0,3,1, 0,2,0,0,3,1, 0,2,0,0,3,1, 1,2,2,0,0,0,2,0,0,0,0,0,0,0,3,5};
		
		if((sCard>>8&1)==1) {
			for(int i=0; i<3; i++) {
				if((sCard>>i&1)==1) {
					int cardDone=hand[curPl][i];
					boolean saveCard=false;
					for(int j=0; j<idGes.length; j++)
						if(cardDone>-1 && cardDone==idGes[j]) saveCard=true;
					if(!saveCard) {
						sendAnim(appendAnim(null,1<<16,curPl,cardDone,0,0));
						ablage[curPl].addElement(cardDone);
						hand[curPl][i]=-1;
						break;
					}
				}
			}
			for(int i=0; i<3; i++) {
				if((sCard>>i&1)==1) {
					int cardDone=hand[curPl][i];
					if(cardDone>-1) {
						sendAnim(appendAnim(null,1<<16,curPl,cardDone,0,0));
						ablage[curPl].addElement(cardDone);
						hand[curPl][i]=-1;
					}
				}
			}
		}else {
			int cardDone=hand[curPl][sCard];
			cash[curPl]+=earn[cardDone];
			sendAnim(appendAnim(null,1<<16,curPl,cardDone,0,0));
			ablage[curPl].addElement(cardDone);
			hand[curPl][sCard]=-1;
		}
		setGeschuetzt();
		canPlay=canPlay(currentPlayer);

		//Karte weg usw
		sCard=-1;
		for(int i=0; i<4; i++) {
			zehner[i]=0;
			logIn[i]=-1;
		}
		boolean next=true;
		for(int i=0; i<3; i++)
			if(hand[curPl][i]!=-1) next=false;
		if(next)
			nexter();
	}

	
	public void weiter(int sta) {
		if(takeCards(currentPlayer,sta)) {
			phase=2;
			return;
		};
		for(int i=0; i<5; i++) {
			if(stapel.size()>0 && auslage[i]==-1)
				auslage[i]=stapel.removeFirst();
		}

		currentPlayer=(currentPlayer+1)%anzMitSpieler;
		phase=1;
		canPlay=canPlay(currentPlayer);
	}

	public void nexter() {
		phase=3;
	}
	
	public boolean[] canPlay(int sp) {
		boolean[] ret=new boolean[3];
		for(int i=0; i<3; i++) {
			if(hand[sp][i]>-1)
				ret[i]=canPlayCard(getColFromPos(currentPlayer), hand[currentPlayer][i]);
		}		
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
	
	public void sendBoard(GameReceiver st, int[] anim){
		int id=st.getPlaying();
		int[] spHand=new int[0];
		int ablageSize=0, nachziehSize=0;
		if (id>-1) {
			spHand=hand[id];
			ablageSize=ablage[id].size();
			nachziehSize=nachzieh[id].size();
		}
		
		Data dat=st.makeData(700,getSpielClient());
		dat.v.addElement(new Integer(anzMitSpieler));
		dat.v.addElement(new Integer(st.getPlaying()));
		dat.v.addElement(new Integer(phase));
		dat.v.addElement(new Integer(round));
		dat.v.addElement(new Integer(amountPoeppel));
		dat.v.addElement(new Integer(currentPlayer));
		dat.v.addElement(startSpieler);
		dat.v.addElement(auslage);
		dat.v.addElement(cash);
		dat.v.addElement(spHand);
		dat.v.addElement(ablageSize);
		dat.v.addElement(nachziehSize);
		dat.v.addElement(id>-1?ablage[id]:new IntVector());
		dat.v.addElement(poeppel);
		dat.v.addElement(sCard);
		dat.v.addElement(sPoep);
		dat.v.addElement(possible);
		dat.v.addElement(logIn);
		dat.v.addElement(geschuetzt);
		dat.v.addElement(canPlay);
		
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

	public void sendGameStatus() {
		for (GameReceiver playerInfo : getReceiverArray()) {
			sendGameStatus(playerInfo);
		}
	}
}

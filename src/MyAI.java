// ======================================================================
// FILE:        MyAI.java
//
// AUTHOR:      Thomas Huang
//
// DESCRIPTION: This file contains your agent class, which you will
//              implement. You are responsible for implementing the
//              'getAction' function and any helper methods you feel you
//              need.
//
// NOTES:       - If you are having trouble understanding how the shell
//                works, look at the other parts of the code, as well as
//                the documentation.
//
//              - You are only allowed to make changes to this portion of
//                the code. Any changes to other portions of the code will
//                be lost when the tournament runs your code.
// ======================================================================

public class MyAI extends Agent
{
	/*=======================================================================================================================*/
	/*=======================================Custom Classes Start============================================================*/
	/*=======================================================================================================================*/

	private class Pair {
		private int x;
		private int y;

		public Pair(){
			this.x = -1;
			this.y = -1;
		}

		public Pair(int x, int y){
			this.x = x;
			this.y = y;
		}

		public int getX(){
			return this.x;
		}

		public int getY(){
			return this.y;
		}
	}

	private class PairQueue{
		private Pair[] queue;
		private int size;

		public PairQueue(){
			this.queue = new Pair[100];
			this.size = 0;
		}

		public PairQueue(PairQueue queue){
			this.queue = new Pair[100];
			for(int i = 0; i < queue.size(); i++){
				this.queue[i] = queue.get(i);
			}
			this.size = queue.size();
		}

		public void addToQueue(Pair p){
			this.queue[this.size] = p;
			this.size++;
		}

		public int size(){
			return this.size;
		}

		public Pair get(int pos){
			return queue[pos];
		}

		public boolean isEmpty(){
			return this.size == 0;
		}
	}

	private class State {
		private int x;
		private int y;
		private int goalX;
		private int goalY;
		private int direction;

		private int est_total_cost; //fn cost of whole path
		private int est_cost; //hn est_cost to goal from this node
		private int current_cost; //gn path cost to this node

		private PairQueue path;
		private boolean[][] vboard;

		public int directionCost(int x, int y, int goalX, int goalY, int dir){
			int NEITHER = -1;

			int horizontal;
			int vertical;

			if(x < goalX){
				horizontal = RIGHT;
			}else if(x > goalX){
				horizontal = LEFT;
			}else{
				horizontal = NEITHER;
			}

			if(y < goalY){
				vertical = UP;
			}else if(y > goalY){
				vertical = DOWN;
			}else{
				vertical = NEITHER;
			}

			if(horizontal == NEITHER){
				if(dir == vertical){
					return 0;
				}else if((dir+2)%4 == vertical){
					return 2;
				}else{
					return 1;
				}
			}else if(vertical == NEITHER){
				if(dir == horizontal){
					return 0;
				}else if((dir+2)%4 == horizontal){
					return 2;
				}else{
					return 1;
				}
			}else{
				if(dir == horizontal || dir == vertical){
					return 1;
				}else{
					return 2;
				}
			}
		}

		public State(int x, int y, int goalX, int goalY, int direction, int current_cost, PairQueue path, boolean[][] vboard){
			this.x = x;
			this.y = y;
			this.goalX = goalX;
			this.goalY = goalY;
			this.direction = direction;
			this.current_cost = current_cost;
			this.est_cost = Math.abs(goalY-y) + Math.abs(goalX-x) + directionCost(x, y, goalX, goalY, direction);
			this.est_total_cost = this.current_cost + this.est_cost;
			this.path = path;
			this.vboard = vboard;
		}

		public int getX(){
			return this.x;
		}

		public int getY(){
			return this.y;
		}

		public int getGoalX(){
			return this.goalX;
		}

		public int getGoalY(){
			return this.goalY;
		}

		public int getDirection(){
			return this.direction;
		}

		public int getCurrentCost(){
			return this.current_cost;
		}

		public int getEstCost(){
			return this.est_cost;
		}

		public int getEstTotalCost(){
			return this.est_total_cost;
		}

		public PairQueue getPath(){
			return this.path;
		}

		public boolean[][] getVBoard(){
			return this.vboard;
		}

		public boolean isGoal(){
			return this.x == this.goalX && this.y == this.goalY;
		}

		//compare est_total_cost of two states.  1 = current state is larger, 0 = tied, -1 = current state is smaller
		public int compareState(State s){
			return this.est_total_cost >= s.getEstTotalCost() ? (this.est_total_cost > s.getEstTotalCost() ? 1 : 0) : -1;
		}
	}

	private class StateNode{
		private State state;
		private StateNode next;

		public StateNode(State state){
			this.state = state;
			this.next = null;
		}

		public void setState(State state){
			this.state = state;
		}

		public void setNext(StateNode next){
			this.next = next;
		}

		public State getState(){
			return this.state;
		}

		public StateNode getNext(){
			return this.next;
		}
	}

	private class MoveCommand{
		private int command;
		private MoveCommand next;

		public MoveCommand(int command){
			this.command = command;
			this.next = null;
		}

		public int getCommand(){
			return this.command;
		}

		public MoveCommand getNext(){
			return this.next;
		}

		public void setNext(MoveCommand mc){
			this.next = mc;
		}
	}

	private class MoveQueue{
		private MoveCommand first;
		private MoveCommand last;

		public MoveQueue(){
			this.first = null;
			this.last = null;
		}

		public void addToQueue(MoveCommand mc){
			if(this.first == null){
				this.first = mc;
				this.last = mc;
			}else{
				this.last.setNext(mc);
				this.last = mc;
			}
		}

		public int push(){
			if(this.first != null){
				int command = this.first.getCommand();
				this.first = this.first.getNext();
				return command;
			}
			return -100;
		}

		public boolean empty(){
			return this.first == null;
		}

		public void addArrow(){
			MoveCommand mc = this.first;
			while(mc != null){
				if(mc.getNext() == null){
					MoveCommand newMC = new MoveCommand(SHOOT);
					newMC.setNext(mc);
					this.first = newMC;
					this.last = mc.getNext();
					break;
				}else if(mc.getNext() == this.last){
					MoveCommand saveLast = this.last;
					mc.setNext(new MoveCommand(SHOOT));
					mc.getNext().setNext(saveLast);
					break;
				}else{
					mc = mc.getNext();
				}
			}
		}

		public MoveCommand getFirst(){
			return this.first;
		}

	}

	/*=======================================================================================================================*/
	/*=======================================Custom Classes End==============================================================*/
	/*=======================================================================================================================*/

	/*
		INDEX:
			B = BREEZE; S = STENCH; G = GLITTER; W = WUMPUS; P = PIT; 

		ACTIONS:
			TURN_LEFT, TURN_RIGHT, FORWARD, SHOOT, GRAB, CLIMB
	*/

	private static final int SQ_UNKNOWN = -1;
	private static final int SQ_EMPTY = 0;
	private static final int SQ_B = 1;
	private static final int SQ_S = 2;
	private static final int SQ_BS = 3;
	private static final int SQ_W = 4;

	private static final int UP = 0;
	private static final int RIGHT = 1;
	private static final int DOWN = 2;
	private static final int LEFT = 3;
	private static final int FORWARD = 10;
	private static final int SHOOT = 100;


	// board related variables
	private int maxX;
	private int maxY;
	private int[][] board;

	// wumpus related variables
	private boolean arrow;
	private boolean wumpusAlive;
	private boolean wumpusFound;
	private int wumpusX;
	private int wumpusY;
	private int numStench;
	private Pair[] stenchCoords;

	// gold related variables
	private boolean goldFound;

	// game related variables
	private boolean canExplore;
	private boolean hitWallX;
	private boolean hitWallY;

	// current state variables
	private int currX;
	private int currY;
	private int currDir;

	private MoveQueue moveQueue;

	public MyAI ( )
	{
		// ======================================================================
		// YOUR CODE BEGINS
		// ======================================================================

		// initialize board variables
		maxX = 6; // maxX is always max x position +1
		maxY = 6; // maxY is always max y position +1
		board = new int[maxX][maxY];
		for (int i = 0; i < maxX; i++){
			for (int j = 0; j < maxY; j++){
				board[i][j] = SQ_UNKNOWN;
			}
		}

		// initialize wumpus variables
		arrow = true;
		wumpusAlive = true;
		wumpusFound = false;
		wumpusX = -1;
		wumpusY = -1;
		numStench = 0;
		stenchCoords = new Pair[5];

		// initialize gold variables
		goldFound = false;

		// initialize game variables
		canExplore = true;
		hitWallX = false;
		hitWallY = false;

		// initialize current state variables
		currX = 0;
		currY = 0;
		currDir = RIGHT;

		moveQueue = new MoveQueue();

		// ======================================================================
		// YOUR CODE ENDS
		// ======================================================================
	}
	public Action getAction
	(
		boolean stench,
		boolean breeze,
		boolean glitter,
		boolean bump,
		boolean scream
	)
	{
		// ======================================================================
		// YOUR CODE BEGINS
		// ======================================================================
		if(scream){ //wumpus died
			wumpusAlive = false;
			removeWumpus();
		}

		//check parameters
		if(bump){ //hit a wall
			if(currDir == RIGHT){
				maxX = currX;
				currX--;
			}else if (currDir == UP){
				maxY = currY;
				currY--;
			}
		}

		if(board[currX][currY] == SQ_UNKNOWN){ // if current tile has not been visited
			board[currX][currY] = SQ_EMPTY;
			if(breeze){
				board[currX][currY] += SQ_B;
			}
			if(stench){
				board[currX][currY] += SQ_S;
				stenchCoords[numStench] = new Pair(currX, currY);
				numStench++;
			}
			if(glitter){
				goldFound = true;
				canExplore = false;
				predictSafeBoard();
				return Action.GRAB;
			}
		}

		if(wumpusAlive){
			predictWumpus();
		}

		if(!moveQueue.empty()){
			int mc = moveQueue.push();
			switch(mc){
				case LEFT:
					changeAgentState(LEFT);
					return Action.TURN_LEFT;
				case RIGHT:
					changeAgentState(RIGHT);
					return Action.TURN_RIGHT;
				case FORWARD:
					changeAgentState(FORWARD);
					return Action.FORWARD;
				case SHOOT:
					arrow = false;
					canExplore = true;
					return Action.SHOOT;
			}
		}

		if(goldFound){
			if(currX == 0 && currY == 0){
				return Action.CLIMB;
			}
			State s = startAStarSearch(0,0);
			convertPathToCommands(currX, currY, currDir, s.getPath());
		}else{
			if(!findNearestEmpty()){
				canExplore = false;
			}
			if(!canExplore){
				// if(checkBreezeDiagonal4()){
				// 	State s = startAStarSearch(2,2);
				// 	convertPathToCommands(currX, currY, currDir, s.getPath());
				// 	// int stenchX = stenchCoords[0].getX();
				// 	// int stenchY = stenchCoords[0].getY();
				// 	if(board[1][2] == SQ_BS && board[2][1] == SQ_BS){
				// 		moveQueue.addArrow();
				// 	}
				// }else if(checkBreezeDiagonal5()){
				// 	int bestx;
				// 	int besty;
				// 	State s1 = startAStarSearch(3,2);
				// 	State s2 = startAStarSearch(2,3);
				// 	State best;
				// 	if(s1.getEstTotalCost() < s2.getEstTotalCost()){
				// 		bestx = 3;
				// 		besty = 2;
				// 		best = s1;
				// 	}else{
				// 		bestx = 2;
				// 		besty = 3;
				// 		best = s2;
				// 	}
				// 	// 3, 2 => 3, 1
				// 	// 2, 3 => 1, 3
				// 	if((board[bestx][besty] == SQ_BS && board[2][2] == SQ_BS) || 
				// 		(numStench == 1 && ((bestx == 3 && board[bestx][besty-1] == SQ_BS) || (besty == 3 && board[bestx-1][besty] == SQ_BS)))){
				// 		moveQueue.addArrow();
				// 	}
				// 	convertPathToCommands(currX, currY, currDir, best.getPath());
				// }
				// else if(arrow){
				if(arrow){
					//see if we should shoot or not
					State shootState;
					if(wumpusFound){
						shootState = shootArrow();
					}else{
						shootState = shootRandomArrow();
					}

					if(shootState == null){
						if(currX == 0 && currY == 0){
							return Action.CLIMB;
						}
						predictSafeBoard();
						State s = startAStarSearch(0,0);
						convertPathToCommands(currX, currY, currDir, s.getPath());
					}else{
						convertPathToCommands(currX, currY, currDir, shootState.getPath());
						moveQueue.addArrow();
					}
				}else{
					if(currX == 0 && currY == 0){
						return Action.CLIMB;
					}
					predictSafeBoard();
					State s = startAStarSearch(0,0);
					convertPathToCommands(currX, currY, currDir, s.getPath());
				}
			}
		}

		if(!moveQueue.empty()){
			//perform moves instead
			int mc = moveQueue.push();
			switch(mc){
				case LEFT:
					changeAgentState(LEFT);
					return Action.TURN_LEFT;
				case RIGHT:
					changeAgentState(RIGHT);
					return Action.TURN_RIGHT;
				case FORWARD:
					changeAgentState(FORWARD);
					return Action.FORWARD;
				case SHOOT:
					arrow = false;
					canExplore = true;
					return Action.SHOOT;
			}
		}
		
		// // No change direction, straight if can, fill squares once reach gold.
		return Action.CLIMB;
		// // ======================================================================
		// // YOUR CODE ENDS
		// // ======================================================================

	}
	
	// ======================================================================
	// YOUR CODE BEGINS
	// ======================================================================

	/*=======================================================================================================================*/
	/*=======================================Wumpus Prediction Functions Start===============================================*/
	/*=======================================================================================================================*/

	//direct predict, call when acquire new stench
	public boolean predictWumpus(){
		int x = -1;
		int y = -1;

		if(numStench == 1){
			int stenchX = stenchCoords[0].getX();
			int stenchY = stenchCoords[0].getY();
			boolean sUp 	= inBounds(stenchX, stenchY+1) && !tileSafe(stenchX, stenchY+1) && !sideStenchSafe(stenchX-1, stenchY+1, stenchX+1, stenchY+1, stenchX, stenchY+2);
			boolean sDown 	= inBounds(stenchX, stenchY-1) && !tileSafe(stenchX, stenchY-1) && !sideStenchSafe(stenchX+1, stenchY-1, stenchX-1, stenchY-1, stenchX, stenchY-2);
			boolean sLeft 	= inBounds(stenchX-1, stenchY) && !tileSafe(stenchX-1, stenchY) && !sideStenchSafe(stenchX-1, stenchY-1, stenchX-1, stenchY+1, stenchX-2, stenchY);
			boolean sRight 	= inBounds(stenchX+1, stenchY) && !tileSafe(stenchX+1, stenchY) && !sideStenchSafe(stenchX+1, stenchY+1, stenchX+1, stenchY-1, stenchX+2, stenchY);
			boolean uniqueBool = (sUp ? 1 : 0) + (sDown ? 1 : 0) + (sLeft ? 1 : 0) + (sRight ? 1 : 0) == 1;

			if(uniqueBool){
				if(sUp){
					x = stenchX;
					y = stenchY + 1;
				}else if(sDown){
					x = stenchX;
					y = stenchY - 1;
				}else if(sLeft){
					x = stenchX - 1;
					y = stenchY;
				}else if(sRight){
					x = stenchX + 1;
					y = stenchY;
				}
			}
		}
		if(numStench == 2){
			if(stenchCoords[0].getX() == stenchCoords[1].getX()){ //same col
				x = stenchCoords[0].getX();
				y = (stenchCoords[0].getY() + stenchCoords[1].getY())/2;
			}else if(stenchCoords[0].getY() == stenchCoords[1].getY()){ //same row
				x = (stenchCoords[0].getX() + stenchCoords[1].getX())/2;
				y = stenchCoords[0].getY();
			}else{ // diagonal stenches
				int potential_wumpus1 = board[stenchCoords[0].getX()][stenchCoords[1].getY()];
				int potential_wumpus2 = board[stenchCoords[1].getX()][stenchCoords[0].getY()];
				if(potential_wumpus1 == SQ_EMPTY || potential_wumpus1 == SQ_B){
					x = stenchCoords[1].getX();
					y = stenchCoords[0].getY();
				}else if(potential_wumpus2 == SQ_EMPTY || potential_wumpus2 == SQ_B){
					x = stenchCoords[0].getX();
					y = stenchCoords[1].getY();
				}
			}
		}else if(numStench == 3){//check same row
			if(stenchCoords[0].getX() == stenchCoords[1].getX()){ 		//same col
				x = stenchCoords[0].getX();
				y = (stenchCoords[0].getY() + stenchCoords[1].getY())/2;
			}else if(stenchCoords[0].getX() == stenchCoords[2].getX()){ //same col
				x = stenchCoords[0].getX();
				y = (stenchCoords[0].getY() + stenchCoords[2].getY())/2;
			}else if(stenchCoords[1].getX() == stenchCoords[2].getX()){ //same col
				x = stenchCoords[1].getX();
				y = (stenchCoords[1].getY() + stenchCoords[2].getY())/2;
			}else if(stenchCoords[0].getY() == stenchCoords[1].getY()){ //same row
				x = (stenchCoords[0].getX() + stenchCoords[1].getX())/2;
				y = stenchCoords[0].getY();
			}else if(stenchCoords[0].getY() == stenchCoords[2].getY()){ //same row
				x = (stenchCoords[0].getX() + stenchCoords[2].getX())/2;
				y = stenchCoords[0].getY();
			}else if(stenchCoords[1].getY() == stenchCoords[2].getY()){ //same row
				x = (stenchCoords[1].getX() + stenchCoords[2].getX())/2;
				y = stenchCoords[1].getY();
			}
		}else if(numStench == 4){
			x = (stenchCoords[0].getX() + stenchCoords[1].getX() + stenchCoords[2].getX() + stenchCoords[3].getX())/4;
			y = (stenchCoords[0].getY() + stenchCoords[1].getY() + stenchCoords[2].getY() + stenchCoords[3].getY())/4;
		}
		
		if(x != -1 && y != -1){
			wumpusFound = true;
			wumpusX = x;
			wumpusY = y;
			return true;
		}
		return false;
	}

	public Pair[] makeWumpusRandomPrediction(){
		Pair[] possWumpusTiles = new Pair[4];
		int count = 0;
		if(numStench == 1){
			int x = stenchCoords[0].getX();
			int y = stenchCoords[0].getY();
			boolean sUp 	= inBounds(x, y+1) && !tileSafe(x, y+1) && !sideStenchSafe(x-1, y+1, x+1, y+1, x, y+2);
			boolean sDown 	= inBounds(x, y-1) && !tileSafe(x, y-1) && !sideStenchSafe(x+1, y-1, x-1, y-1, x, y-2);
			boolean sLeft 	= inBounds(x-1, y) && !tileSafe(x-1, y) && !sideStenchSafe(x-1, y-1, x-1, y+1, x-2, y);
			boolean sRight 	= inBounds(x+1, y) && !tileSafe(x+1, y) && !sideStenchSafe(x+1, y+1, x+1, y-1, x+2, y);
			if(sUp){
				possWumpusTiles[count] = new Pair(x, y+1);
				count++;
			}
			if(sDown){
				possWumpusTiles[count] = new Pair(x, y-1);
				count++;
			}
			if(sLeft){
				possWumpusTiles[count] = new Pair(x-1, y);
				count++;
			}
			if(sRight){
				possWumpusTiles[count] = new Pair(x+1, y);
				count++;
			}
		}else if(numStench == 2){
			int potential_wumpus1 = board[stenchCoords[0].getX()][stenchCoords[1].getY()];
			int potential_wumpus2 = board[stenchCoords[1].getX()][stenchCoords[0].getY()];
			possWumpusTiles[0] = new Pair(stenchCoords[1].getX(), stenchCoords[0].getY());
			possWumpusTiles[1] = new Pair(stenchCoords[0].getX(), stenchCoords[1].getY());
		}
		return possWumpusTiles;
	}

	//predicts movement if AI on stench & Wumpus unknown
	public boolean sideStenchSafe(int x1, int y1, int x2, int y2, int x3, int y3){
		if((x1 < 0 || x1 >= maxX || y1 < 0 || y1 >= maxY) || board[x1][y1] == SQ_UNKNOWN || board[x1][y1] == SQ_S || board[x1][y1] == SQ_BS){
			if((x2 < 0 || x2 >= maxX || y2 < 0 || y2 >= maxY) || board[x2][y2] == SQ_UNKNOWN || board[x2][y2] == SQ_S || board[x2][y2] == SQ_BS){
				if((x3 < 0 || x3 >= maxX || y3 < 0 || y3 >= maxY) || board[x3][y3] == SQ_UNKNOWN || board[x3][y3] == SQ_S || board[x3][y3] == SQ_BS){
					return false;
				}
			}
		}
		return true;
	}
	
	//predicts movement if AI on breeze
	public boolean sideBreezeSafe(int x1, int y1, int x2, int y2, int x3, int y3){
		if((x1 < 0 || x1 >= maxX || y1 < 0 || y1 >= maxY) || board[x1][y1] == SQ_UNKNOWN || board[x1][y1] == SQ_B || board[x1][y1] == SQ_BS){
			if((x2 < 0 || x2 >= maxX || y2 < 0 || y2 >= maxY) || board[x2][y2] == SQ_UNKNOWN || board[x2][y2] == SQ_B || board[x2][y2] == SQ_BS){
				if((x3 < 0 || x3 >= maxX || y3 < 0 || y3 >= maxY) || board[x3][y3] == SQ_UNKNOWN || board[x3][y3] == SQ_B || board[x3][y3] == SQ_BS){
					return false;
				}
			}
		}
		return true;
	}

	public boolean sidesSafe(int x, int y){
		boolean bUp 	= y == maxY-1 || board[x][y+1] == SQ_B || board[x][y+1] == SQ_BS || board[x][y+1] == SQ_UNKNOWN;
		boolean bDown 	= y == 0 || board[x][y-1] == SQ_B || board[x][y-1] == SQ_BS || board[x][y-1] == SQ_UNKNOWN;
		boolean bLeft 	= x == 0 || board[x-1][y] == SQ_B || board[x-1][y] == SQ_BS || board[x-1][y] == SQ_UNKNOWN;
		boolean bRight	= x == maxX-1 || board[x+1][y] == SQ_B || board[x+1][y] == SQ_BS || board[x+1][y] == SQ_UNKNOWN;

		boolean sUp 	= y == maxY-1 || board[x][y+1] == SQ_S || board[x][y+1] == SQ_BS || board[x][y+1] == SQ_UNKNOWN;
		boolean sDown 	= y == 0 || board[x][y-1] == SQ_S || board[x][y-1] == SQ_BS || board[x][y-1] == SQ_UNKNOWN;
		boolean sLeft 	= x == 0 || board[x-1][y] == SQ_S || board[x-1][y] == SQ_BS || board[x-1][y] == SQ_UNKNOWN;
		boolean sRight 	= x == maxX-1 || board[x+1][y] == SQ_S || board[x+1][y] == SQ_BS || board[x+1][y] == SQ_UNKNOWN;

		boolean pit = bUp && bDown && bLeft && bRight;
		boolean wumpus = (!wumpusFound && sUp && sDown && sLeft && sRight) || (x == wumpusX && y == wumpusY);

		return !pit && !wumpus;
	}

	//Removes wumpus and stench from game once killed
	public void removeWumpus(){
		if(wumpusFound){
			board[wumpusX][wumpusY] = SQ_UNKNOWN; // might be glitched due to random kill
		}
	}

	public boolean wumpusDeadPitCheck(int x, int y){
		return !wumpusAlive && board[x][y] != SQ_B && board[x][y] != SQ_BS && board[x][y] != SQ_UNKNOWN;
	}

	/*=======================================================================================================================*/
	/*=======================================Wumpus Prediction Functions End=================================================*/
	/*=======================================================================================================================*/


	/*=======================================================================================================================*/
	/*=======================================Shooting Arrow Functions Start==================================================*/
	/*=======================================================================================================================*/
	//determine if arrow should be shot if necessary.  must be same row and col and facing direction of wumpus to shoot.  
	//*****************might need modifications

	public State shootArrow(){
		if(shootTile(wumpusX, wumpusY)){
			return startAStarSearch(wumpusX, wumpusY);
		}
		return null;
	}

	//shoot when wumpus is not found and cannot advance any square. *** or when shooting is more favorable than moving.
	public State shootRandomArrow(){
		Pair[] possWumpusTiles = makeWumpusRandomPrediction();
		State best_state = null;
		for(int i = 0; i < 4; i++){
			if(possWumpusTiles[i] != null){
				int x = possWumpusTiles[i].getX();
				int y = possWumpusTiles[i].getY();
				if(shootTile(x, y)){
					State state = startAStarSearch(x, y);
					if(best_state == null){
						best_state = state;
					}else if(state.getEstTotalCost() < best_state.getEstTotalCost()){
						best_state = state;
					}
				}
			}
		}
		return best_state;
	}

	// Determines if tile is safe after wumpus is killed.
	public boolean shootTile(int x, int y){
		boolean shoot = false;
		if(y+1 < maxY && board[x][y+1] == SQ_S){
			shoot = true;
		}
		if(y-1 >= 0 && board[x][y-1] == SQ_S){
			shoot = true;
		}
		if(x-1 >= 0 && board[x-1][y] == SQ_S){
			shoot = true;
		}
		if(x+1 < maxX && board[x+1][y] == SQ_S){
			shoot = true;
		}
		return shoot;
	}

	/*=======================================================================================================================*/
	/*=======================================Shooting Arrow Functions End====================================================*/
	/*=======================================================================================================================*/

	/*=======================================================================================================================*/
	/*=======================================A* Search Functions Start=======================================================*/
	/*=======================================================================================================================*/

	public boolean findNearestEmpty(){
		//divide board into quadrants dependent on where AI is currently at and determine the direction it wants to move in.  prefer farther from 0,0 or corners
		State best_state = null;
		for(int i = maxX-1; i >= 0; i--){
			for(int j = maxY-1; j >= 0; j--){
				if(!(i == currX && j == currY) && board[i][j] == SQ_UNKNOWN){
					boolean left  = i != 0 && (board[i-1][j] == SQ_EMPTY || sidesSafe(i, j) || wumpusDeadPitCheck(i-1, j));
					boolean right = i != maxX-1 && (board[i+1][j] == SQ_EMPTY || sidesSafe(i, j) || wumpusDeadPitCheck(i+1, j));
					boolean down = j != 0 && (board[i][j-1] == SQ_EMPTY || sidesSafe(i, j) || wumpusDeadPitCheck(i, j-1));
					boolean up = j != maxY-1 && (board[i][j+1] == SQ_EMPTY || sidesSafe(i, j) || wumpusDeadPitCheck(i, j+1));
					if (left || right || down || up){
						State state = startAStarSearch(i, j);
						if(best_state == null){
							best_state = state;
						}else if(state.getEstTotalCost() < best_state.getEstTotalCost()){
							best_state = state;
						}
					}
				}
			}
		}
		if(best_state != null){
			convertPathToCommands(currX, currY, currDir, best_state.getPath());
		}
		
		return best_state != null;
	}

	public State startAStarSearch(int goalX, int goalY){

		boolean[][] vBoard = new boolean[maxX][maxY];

		PairQueue path = new PairQueue();
		for(int i = 0; i < maxX; i++){
			for(int j = 0; j < maxY; j++){
				vBoard[i][j] = false;
			}
		}

		vBoard[currX][currY] = true;

		State init = new State(currX, currY, goalX, goalY, currDir, 0, path, vBoard);

		StateNode frontier = createChildren(init, true);
		
		boolean goal = false;

		while(!goal && frontier != null){
			if(frontier.getState().isGoal()){
				goal = true;
			}else{
				//expand node, add children to queue in order
				StateNode children_queue = createChildren(frontier.getState(), false);

				frontier = frontier.getNext();
				if(frontier == null){
					frontier = children_queue;
				}else if(children_queue != null){
					if(frontier.getState().compareState(children_queue.getState()) == 1){
						StateNode child = children_queue;
						children_queue = children_queue.getNext();
						child.setNext(frontier);
						frontier = child;
					}
					StateNode node = frontier;
					while(children_queue != null){
						if(node.getNext() == null){
							node.setNext(children_queue);
							break;
						}
						else if(node.getNext().getState().compareState(children_queue.getState()) == 1){
							StateNode child = children_queue;
							children_queue = children_queue.getNext();
							child.setNext(node.getNext());
							node.setNext(child);
						}
						node = node.getNext();
					}
				}
			}
		}

		State solution = frontier.getState();

		return solution;
	}

	public boolean[][] newChildVBoard(int x, int y, boolean[][] b){
		if(x-1 >= 0){
			b[x-1][y] = true;
		}
		if(x+1 < maxX){
			b[x+1][y] = true;
		}
		if(y-1 >= 0){
			b[x][y-1] = true;
		}
		if(y+1 < maxY){
			b[x][y+1] = true;
		}
		return b;
	}

	public boolean inBounds(int x, int y){
		return (x >= 0 && x < maxX) && (y >=0 && y < maxY);
	}

	public boolean unvisitedTile(int x, int y, boolean[][] b){
		return b[x][y];
	}

	public boolean tileSafe(int x, int y){
		return board[x][y] == SQ_EMPTY || board[x][y] == SQ_B || board[x][y] == SQ_BS || board[x][y] == SQ_S;
	}

	public boolean safeUnknownTile(int x, int y, int goalX, int goalY){
		return x == goalX && y == goalY;
	}

	public StateNode createChildren(State s, boolean reversible){
		int x = s.getX();
		int y = s.getY();
		int goalX = s.getGoalX();
		int goalY = s.getGoalY();
		int direction = s.getDirection();
		int current_cost = s.getCurrentCost();
		boolean[][] vBoard = s.getVBoard();
		boolean[][] new_vBoard = newChildVBoard(x, y, vBoard);
		
		State[] queue = new State[4];
		int count = 0;

		if(inBounds(x, y+1) && unvisitedTile(x, y+1, vBoard) && (tileSafe(x, y+1) || safeUnknownTile(x, y+1, goalX, goalY)) && (reversible || direction != DOWN)){//up
			PairQueue new_path = new PairQueue(s.getPath());
			new_path.addToQueue(new Pair(x, y+1));
			int new_current_cost = current_cost+turnCost(direction, UP)+1;
			queue[count] = new State(x, y+1, goalX, goalY, UP, new_current_cost, new_path, new_vBoard);
			count++;
		}
		if(inBounds(x, y-1) && unvisitedTile(x, y-1, vBoard) && (tileSafe(x, y-1) || safeUnknownTile(x, y-1, goalX, goalY)) && (reversible || direction != UP)){//down
			PairQueue new_path = new PairQueue(s.getPath());
			new_path.addToQueue(new Pair(x, y-1));
			int new_current_cost = current_cost+turnCost(direction, DOWN)+1;
			queue[count] = new State(x, y-1, goalX, goalY, DOWN, new_current_cost, new_path, new_vBoard);
			count++;
		}
		if(inBounds(x-1, y) && unvisitedTile(x-1, y, vBoard) && (tileSafe(x-1, y) || safeUnknownTile(x-1, y, goalX, goalY)) && (reversible || direction != RIGHT)){//left
			PairQueue new_path = new PairQueue(s.getPath());
			new_path.addToQueue(new Pair(x-1, y));
			int new_current_cost = current_cost+turnCost(direction, LEFT)+1;
			queue[count] = new State(x-1, y, goalX, goalY, LEFT, new_current_cost, new_path, new_vBoard);
			count++;
		}
		if(inBounds(x+1, y) && unvisitedTile(x+1, y, vBoard) && (tileSafe(x+1, y) || safeUnknownTile(x+1, y, goalX, goalY)) && (reversible || direction != LEFT)){//right
			PairQueue new_path = new PairQueue(s.getPath());
			new_path.addToQueue(new Pair(x+1, y));
			int new_current_cost = current_cost+turnCost(direction, RIGHT)+1;
			queue[count] = new State(x+1, y, goalX, goalY, RIGHT, new_current_cost, new_path, new_vBoard);
			count++;
		}

		StateNode root = null;

		//modify adding node to LL (LinkedList)
		for(int i = 0; i < count; i++){
			if(root == null){
				root = new StateNode(queue[i]);
			}else{
				StateNode node = root;
				StateNode new_sn = new StateNode(queue[i]);
				boolean added = false;
				if(root.getState().compareState(queue[i]) == 1){
					new_sn.setNext(root);
					root = new_sn;
				}else{
					while(node.getNext() != null){	
						if(node.getNext().getState().compareState(queue[i]) == 1){
							new_sn.setNext(node.getNext());
							node.setNext(new_sn);
							added = true;
							break;
						}else{
							node = node.getNext();
						}
					}
					if(!added){
						node.setNext(new_sn);
					}
				}
			}
		}
		return root;
	}

	public void convertPathToCommands(int x, int y, int direction, PairQueue path){
		if(!path.isEmpty()){
			for(int i = 0 ; i < path.size(); i++){
				Pair p = path.get(i);
				int next_dir;
				if(x == p.getX()){
					next_dir =  y < p.getY() ? UP : DOWN;
				}else{
					next_dir = x < p.getX() ? RIGHT : LEFT;
				}
				if((direction+2)%4 == next_dir){
					moveQueue.addToQueue(new MoveCommand(LEFT));
					moveQueue.addToQueue(new MoveCommand(LEFT));
				} else if((direction+1)%4 == next_dir){
					moveQueue.addToQueue(new MoveCommand(RIGHT));
				} else if((direction+3)%4 == next_dir){
					moveQueue.addToQueue(new MoveCommand(LEFT));
				}
				moveQueue.addToQueue(new MoveCommand(FORWARD));
				x = p.getX();
				y = p.getY();
				direction = next_dir;
			}
		}
	}

	/*=======================================================================================================================*/
	/*=======================================A* Search Functions End=========================================================*/
	/*=======================================================================================================================*/

	/*=======================================================================================================================*/
	/*=======================================Moving Mechanics Functions Start================================================*/
	/*=======================================================================================================================*/
	public void changeAgentState(int command){
		switch(command){
			case LEFT:
				currDir = (currDir+3)%4;
				break;
			case RIGHT:
				currDir = (currDir+1)%4;
				break;
			case FORWARD:
				if(currDir == UP){
					currY++;
				}else if(currDir == DOWN){
					currY--;
				}else if(currDir == LEFT){
					currX--;
				}else if(currDir == RIGHT){
					currX++;
				}
				break;
			default:
				System.out.printf("ChangeAgentState invalid param: %d\n", command);
				break;
		}
	}

	// Calculate Turning Cost
	public int turnCost(int start_dir, int end_dir){
		if(start_dir == end_dir){
			return 0;
		}else if(Math.abs(start_dir - end_dir) == 2){
			return 2;
		}else{
			return 1;
		}
	}

	// METHOD: How to turn to new direction.
	public int turn(int oldDir, int newDir){
		switch(newDir-oldDir){
			case -1:
			case 3:
				return LEFT;
			case 1:
			case -3:
				return RIGHT;
			case -2:
			case 2:
				return LEFT;
		}
		return -1;
	}

	public int left(int dir){
		return (dir + 3) % 4;
	}

	public int right(int dir){
		return (dir + 1) % 4;
	}

	public int back(int dir){
		return (dir + 2) % 4;
	}

	/*=======================================================================================================================*/
	/*=======================================Moving Mechanics Functions End==================================================*/
	/*=======================================================================================================================*/

	/*=======================================================================================================================*/
	/*=======================================End Game Mechanics Functions Start==============================================*/
	/*=======================================================================================================================*/

	//call once gold is found; only call when ending game
	// create new board so we do not mistakenly propagate safe tiles
	public void predictSafeBoard(){
		int[][] newBoard = new int[maxX][maxY];
		for(int i = 0; i < maxX; i++){
			for(int j = 0; j < maxY; j++){
				if(board[i][j] == SQ_UNKNOWN){
					boolean left  = i != 0 && (board[i-1][j] == SQ_EMPTY || (!wumpusAlive && board[i-1][j] == SQ_S));
					boolean right = i != maxX-1 && (board[i+1][j] == SQ_EMPTY || (!wumpusAlive && board[i+1][j] == SQ_S));
					boolean down = j != 0 && (board[i][j-1] == SQ_EMPTY || (!wumpusAlive && board[i][j-1] == SQ_S));
					boolean up = j != maxY-1 && (board[i][j+1] == SQ_EMPTY || (!wumpusAlive && board[i][j+1] == SQ_S));
					if (left || right || down || up){
						newBoard[i][j] = SQ_EMPTY;
					}else{
						newBoard[i][j] = board[i][j];
					}
				}else if(board[i][j] == SQ_B || board[i][j] == SQ_S || board[i][j] == SQ_BS){
					newBoard[i][j] = SQ_EMPTY;
				}else{
					newBoard[i][j] = board[i][j];
				}
			}
		}
		board = newBoard;
	}

	/*=======================================================================================================================*/
	/*=======================================End Game Mechanics Functions End================================================*/
	/*=======================================================================================================================*/

	/*=======================================================================================================================*/
	/*=======================================Utility Functions Start=========================================================*/
	/*=======================================================================================================================*/

	public boolean checkExplorable(){
		// if tile borders a visited safe tile then its possible to explore
		for(int i = 0; i < maxX; i++){
			for(int j = 0; j < maxY; j++){
				boolean left  = i != 0 && board[i-1][j] == SQ_EMPTY; 
				boolean right = i != maxX-1 && board[i+1][j] == SQ_EMPTY;
				boolean down = j != 0 && board[i][j-1] == SQ_EMPTY;
				boolean up = j != maxY-1 && board[i][j+1] == SQ_EMPTY;
				if (left || right || down || up){
					return true;
				}
			}
		}
		return false;
	}

	/*=======================================================================================================================*/
	/*=======================================Utility Functions End===========================================================*/
	/*=======================================================================================================================*/

	// public boolean checkBreezeDiagonal4(){
	// 	if((board[0][3] == SQ_B || board[0][3] == SQ_BS) && 
	// 		(board[1][2] == SQ_B || board[1][2] == SQ_BS || board[1][2] == SQ_UNKNOWN) && 
	// 		(board[2][1] == SQ_B || board[2][1] == SQ_BS || board[2][1] == SQ_UNKNOWN) && 
	// 		(board[3][0] == SQ_B || board[3][0] == SQ_BS)){
	// 		return true;
	// 	}
	// 	return false;
	// }

	// public boolean checkBreezeDiagonal5(){
	// 	if(maxX >= 5 && maxY >= 5){
	// 		if((board[0][4] == SQ_B || board[0][4] == SQ_BS) && 
	// 			(board[1][3] == SQ_B || board[1][3] == SQ_BS) && 
	// 			(board[2][2] == SQ_B || board[2][2] == SQ_BS || board[2][2] == SQ_UNKNOWN) && 
	// 			(board[3][1] == SQ_B || board[3][1] == SQ_BS) &&
	// 			(board[4][0] == SQ_B || board[4][0] == SQ_BS)){
	// 			return true;
	// 		}
	// 	}
	// 	return false;
	// }

	// ======================================================================
	// YOUR CODE ENDS
	// ======================================================================
}
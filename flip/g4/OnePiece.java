public class Player implements flip.sim.Player
{
	//HashMap<Integer,Point> playerPieces = new HashMap<Integer,Point>();
	//playerPieces.put(0, piece);
	//HashMap<Integer,Point> opponentPieces = new HashMap<Integer,Point>();
	//opponentPieces.put(0, opponentPiece);
	
	private int stepsToGoal(Point piece, boolean isPlayer1)
	{
		// Returns the number of steps to the finish line for a piece, assuming it always go straight forward
		double dist;  // distance on x-direction to finish line
		if (isPlayer1) {
			dist = piece.x + 21.0;  // finish line at x = -21.0, piece moves to the left
		}
		else {
			dist = 21.0 - piece.x;  // finish line at x = 21.0, piece moves to the right
		}
		int steps = (int) Math.floor(dist / diameter_piece) + 1;
		return steps;
	}
	
	private boolean winning(Point piece, Point opponentPiece, boolean isPlayer1, boolean moveFirst)
	{
		// Evaluate if your piece is in a winning or losing position		
		int steps = stepsToGoal(piece, isPlayer1);  // number of steps to goal for your piece
		int opponentSteps = stepsToGoal(opponentPiece, !isPlayer1);  // number of pieces to goal for the opponent piece
		int rounds = steps / 2 + 1;  // number of rounds to goal for your piece (move 2 steps in each round)
		int opponentRounds = opponentSteps / 2 + 1;  // number of rounds to goal for the opponent piece
		if (rounds < opponentRounds) {
			return true;  // you need fewer rounds to reach the goal, so you are winning
		}
		else if (rounds > opponentRounds) {
			return false;  // you need more rounds to reach the goal, so you are losing
		}
		else {
			return moveFirst;  // both players need the same number of rounds to reach the goal, whoever moves first wins
		}
	}
	
	private Pair<Integer,Point> moveForward(HashMap<Integer,Point> playerPieces, HashMap<Integer,Point> opponentPieces, boolean isPlayer1)
	{
		// Move a piece as forward as possible
		Pair<Integer,Point> pair = playerPieces.get(0);  // there is only one piece
		Point oldPosition = pair.getValue();  // the original position of the piece
        	for (int trial_num = 0; trial_num < 200; trial_num++) {
			// Select an angle of turn that changes from 0 deg to 100 deg
            		double theta = ((random.nextDouble() > 0.5)? -1 : 1) * trial_num * 0.5 * Math.PI / 180.0;
            		double dx = Math.cos(theta) * (isPlayer1? -1 : 1) * diameter_piece;  // move on x-direction
			double dy = Math.sin(theta) * diameter_piece;  // move on y-direction
            		Point newPosition = new Point(oldPosition.x + dx, oldPosition.y + dy);
            		Pair<Integer,Point> move = new Pair<Integer,Point>(pair.getKey(), newPosition);
            		if (Utilities.check_validity(move, playerPieces, opponentPieces)) {
				return move;  // return the move if it is valid
			}
        	}
        	return null;
    	}

	public static HashMap<Integer,Point> duplicate(HashMap<Integer,Point> pieces)
	{
		// Duplicate (deep copy) a hashmap of pieces
    		HashMap<Integer,Point> newPieces = new HashMap<Integer,Point>();  // create a new hashmap
    		for (Map.Entry<Integer,Point> piece : pieces.entrySet()) {
        		newPieces.put(piece.getKey(), piece.getValue());  // add elements one by one
		}
    		return newPieces;
	}

	private Pair<Integer,Point> move1Piece(HashMap<Integer,Point> playerPieces, HashMap<Integer,Point> opponentPieces, boolean isPlayer1)
	{
		// Move a piece forward two steps (if not at the beginning)
		// Use moveForward directly for the very first move
		List<Pair<Integer,Point>> moves = new ArrayList<Pair<Integer,Point>>();  // create a list of moves
		HashMap<Integer,Point> sandbox = duplicate(playerPieces);  // create a "sandbox" for evaluating moves
		Pair<Integer,Point> pair = playerPieces.get(0);  // there is only one piece
		Point oldPosition = pair.getValue();  // the original position of the piece
        	Pair<Integer,Point> move1 = moveForward(sandbox, opponentPieces, isPlayer1);  // first move in the sandbox
		sandbox.put(pair.getKey(), move1.getValue());
		Pair<Integer,Point> move2 = moveForward(sandbox, opponentPieces, isPlayer1);  // second move in the sandbox
		sandbox.put(pair.getKey(), move2.getValue());
		//double dy1 = move1.getValue().y - oldPosition.y;  // move on y-direction for the first move
		double dy2 = move2.getValue().y - oldPosition.y;  // total move on y-direction after the second move
		if (Math.abs(dy2) <= 2) {
			// Move on y-direction less than 2, accept the two moves
			moves.add(move1);
			moves.add(move2);
			return moves;
		}
		else {
			// Move on y-direction larger than 2, evaluate the two moves
			boolean isWinning = winning(sandbox.get(0).getValue(), opponentPieces.get(0).getValue(), isPlayer1, false);
			if (isWinning) {
				// You are winning after the two moves, accept them
				moves.add(move1);
				moves.add(move2);
				return moves;
			}
			else {
				// You are losing after the two moves, refuse to move
				return null;
			}
		}
    	}
}

package flip.g4;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Collections;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.*;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
import flip.g4.PieceStore;
import flip.g4.Utilities;

// Abstracted class for wall building
class SuperSmallNStrategy {
	private int debugCount = 0;
	// Store details about the game
	private Player mPlayer;
	public Integer n;
	public Random random;
	public Double diameter_piece;
	public SuperSmallNStrategy(Player mPlayer, HashMap<Integer,Point> pieces) {
		this.n = mPlayer.n;
		this.mPlayer = mPlayer;
		this.random  = mPlayer.random;
		this.diameter_piece = mPlayer.diameter_piece;
    	}
	
    	private int stepsToGoal(Point piece, boolean isPlayer1) {
		// Returns the number of steps to the finish line for a piece, assuming it always go straight forward
		double dist;  // distance on x-direction to finish line
		if (isPlayer1) {
			dist = piece.x + 21.0;  // finish line at x = -21.0, piece moves to the left
		}
		else {
			dist = 21.0 - piece.x;  // finish line at x = 21.0, piece moves to the right
		}
		//System.out.println("Distance to goal: " + dist);
		int steps = (int) Math.floor(dist / diameter_piece) + 1;
		return steps;
	}
	
	private boolean winning(Point piece, Point opponentPiece, boolean isPlayer1, boolean moveFirst) {
		// Evaluate if your piece is in a winning or losing position
		int steps = stepsToGoal(piece, isPlayer1);  // number of steps to goal for your piece
		int opponentSteps = stepsToGoal(opponentPiece, !isPlayer1);  // number of pieces to goal for the opponent piece
		int rounds = (steps + 1) / 2;  // number of rounds to goal for your piece (move 2 steps in each round)
		int opponentRounds = (opponentSteps + 1) / 2;  // number of rounds to goal for the opponent piece
		//System.out.println("Player needs " + rounds + " steps to goal.");
		//System.out.println("Opponent needs " + opponentRounds + " steps to goal.");
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
	
	private Pair<Integer,Point> moveForward(HashMap<Integer,Point> playerPieces, HashMap<Integer,Point> opponentPieces, boolean isPlayer1) {
		// Move a piece as forward as possible
		Point oldPosition = playerPieces.get(0);  // get the original position of the (only) piece
		for (int trial_num = 0; trial_num < 200; trial_num++) {
			// Select an angle of turn that changes from 0 deg to 100 deg
			double theta = ((random.nextDouble() > 0.5)? -1 : 1) * trial_num * 0.5 * Math.PI / 180.0;
			double dx = Math.cos(theta) * (isPlayer1? -1 : 1) * diameter_piece;  // move on x-direction
			double dy = Math.sin(theta) * diameter_piece;  // move on y-direction
			Point newPosition = new Point(oldPosition.x + dx, oldPosition.y + dy);
			Pair<Integer,Point> move = new Pair<Integer,Point>(0, newPosition);
			if (Utilities.check_validity(move, playerPieces, opponentPieces)) {
				return move;  // return the move if it is valid
			}
		}
		return null;
	}
	
	public static HashMap<Integer,Point> duplicate(HashMap<Integer,Point> pieces) {
		// Duplicate (deep copy) a hashmap of pieces
		HashMap<Integer,Point> newPieces = new HashMap<Integer,Point>();  // create a new hashmap
		for (Map.Entry<Integer,Point> piece : pieces.entrySet()) {
			newPieces.put(piece.getKey(), piece.getValue());  // add elements one by one
		}
		return newPieces;
	}
	
	private List<Pair<Integer,Point>> move1Piece(HashMap<Integer,Point> playerPieces, HashMap<Integer,Point> opponentPieces, boolean isPlayer1) {
		// Move a piece forward two steps (if not at the beginning)
		// Use moveForward directly for the very first move
		List<Pair<Integer,Point>> moves = new ArrayList<Pair<Integer,Point>>();  // create a list of moves
		HashMap<Integer,Point> sandbox = duplicate(playerPieces);  // create a "sandbox" for evaluating moves
		Point oldPosition = playerPieces.get(0);  // get the original position of the (only) piece
		Pair<Integer,Point> move1 = moveForward(sandbox, opponentPieces, isPlayer1);  // first move in the sandbox
		sandbox.put(0, move1.getValue());
		Pair<Integer,Point> move2 = moveForward(sandbox, opponentPieces, isPlayer1);  // second move in the sandbox
		sandbox.put(0, move2.getValue());
		//double dy1 = move1.getValue().y - oldPosition.y;  // move on y-direction for the first move
		double dy2 = move2.getValue().y - oldPosition.y;  // total move on y-direction after the second move
		if (Math.abs(dy2) <= 1) {
			// Move on y-direction less than 2, accept the two moves
			//if (Math.abs(dy2) > 0) {
			//	System.out.println("Start at: " + oldPosition.x + ", " + oldPosition.y);
			//	System.out.println("End at: " + move2.getValue().x + ", " + move2.getValue().y);
			//	System.out.println("y-direction move: " + dy2);
			//}
			moves.add(move1);
			moves.add(move2);
			return moves;
		}
		else {
			// Move on y-direction larger than 2, evaluate the two moves
			//System.out.println("Start at: " + oldPosition.x + ", " + oldPosition.y);
			//System.out.println("End at: " + move2.getValue().x + ", " + move2.getValue().y);
			//System.out.println("y-direction move: " + dy2);
			boolean isWinning = winning(move2.getValue(), opponentPieces.get(0), isPlayer1, false);
			if (isWinning) {
				// You are winning after the two moves, accept them
				//System.out.println("Player is winning.");
				moves.add(move1);
				moves.add(move2);
				return moves;
			}
			else {
				// You are losing after the two moves, move to the stop position immediately next to opponent piece
				//System.out.println("Player is losing and moves to the stop position.");
				double stop_x = opponentPieces.get(0).x + (isPlayer1? 1 : -1) * diameter_piece;
				double stop_y = opponentPieces.get(0).y;
				Point stopPosition = new Point(stop_x, stop_y);
				//System.out.println("New position: " + stop_x + ", " + stop_y);
				Pair<Integer, Point> m1 = Utilities.getNextMove(oldPosition, stopPosition, 0, playerPieces, opponentPieces);
				Pair<Integer, Point> m2 = new Pair<Integer,Point>(0, stopPosition);
				moves.add(m1);
				moves.add(m2);
				return moves;
			}
		}
	}
	
	public List<Pair<Integer,Point>> getSuperSmallNMove(List<Pair<Integer,Point>> moves, Integer numMoves) {
		HashMap<Integer,Point> playerPieces = this.mPlayer.playerPieces;
		HashMap<Integer,Point> opponentPieces = this.mPlayer.opponentPieces;
		boolean isPlayer1 = this.mPlayer.isPlayer1;
		List<Pair<Integer,Point>> newMoves = move1Piece(playerPieces, opponentPieces, isPlayer1);
		int movesAdded = 0;
		while (moves.size() < numMoves) {
			moves.add(newMoves.get(movesAdded));
			movesAdded++;
		}
		return moves;
	}
}

package flip.g3;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair;
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player {

  // Constants
  private int seed = 42;
  private int wallSize = 14;
  private Random random;

  // Init Parameters
  private boolean isPlayerOne;
  private Integer numPieces;
  private Double pieceDiameter;

  // ArrayList
	private ArrayList<Point> wallCoordinates = new ArrayList<Point>();
  private HashMap<Point, Integer> coinWallMatches = new HashMap<>();

  // Constructor
  public Player() {
		random = new Random(seed);
	}

  // Init Function
  public void init(HashMap<Integer, Point> pieces, int numPieces, double t, boolean isPlayerOne, double pieceDiameter) {

		System.out.println("====");
		System.out.println("Initializing NTK Player");
    this.numPieces = numPieces;
    this.isPlayerOne = isPlayerOne;
    this.pieceDiameter = pieceDiameter;

    setUpWallCoordinates();
		System.out.println("Wall Cooodinates Setup");
    matchCoins(pieces);
		System.out.println("Initialized NTK Player");
		System.out.println("====");

  }

  // getMoves
  public List<Pair<Integer, Point>> getMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
      double nearestDistance = Double.POSITIVE_INFINITY;
      List<Pair<Integer, Point>> moves = new ArrayList<>();
      Point chosenWallCoordinate = wallCoordinates.get(0);
      int counter = 0;
      int index = 0;
//      for(Point wallCord : wallCoordinates){
//          Integer matchedCoinId = coinWallMatches.get(wallCord);
//          Point matchedCoinPoint = playerPieces.get(matchedCoinId);
//          double distance = Board.getdist(matchedCoinPoint, chosenWallCoordinate);
//
//          if(distance < nearestDistance) {
//            nearestDistance = distance;
//            chosenWallCoordinate = wallCord;
//            index = counter;
//          }
//          counter++;
//      }

      System.out.println(nearestDistance);
      System.out.println(chosenWallCoordinate);
      Integer nearestCoinId = coinWallMatches.get(chosenWallCoordinate);
      Point nearestCoinPoint = playerPieces.get(nearestCoinId);
      System.out.println(nearestCoinPoint);
      boolean isValid = false;
      double numTrial = 0;
      while (!isValid && numTrial < 500){
          double deltaY = chosenWallCoordinate.y - nearestCoinPoint.y;
          double deltaX = chosenWallCoordinate.x - nearestCoinPoint.x;
          double theta = Math.atan(deltaY/deltaX) + (numTrial / 500);

          // first move
          double newX = 0;
          double newY = nearestCoinPoint.y + (2 * Math.sin(theta));
          if(isplayer1){
              newX = nearestCoinPoint.x - (2 * Math.cos(theta));

          }
          else{
              newX = nearestCoinPoint.x + (2 * Math.cos(theta));
          }
          Point newPosition = new Point(newX, newY);
          if(check_validity(new Pair(nearestCoinId, newPosition), playerPieces, opponent_pieces)){
              moves.add(new Pair(nearestCoinId, newPosition));
              if(Board.getdist(newPosition, chosenWallCoordinate) < 2.1 || newX < 19){
                wallCoordinates.remove(index);
              }
              break;
          }

          theta = Math.atan(deltaY/deltaX) - (numTrial / 500);

          // first move
          newX = 0;
          newY = nearestCoinPoint.y + (2 * Math.sin(theta));
          if(isplayer1){
              newX = nearestCoinPoint.x - (2 * Math.cos(theta));

          }
          else{
              newX = nearestCoinPoint.x + (2 * Math.cos(theta));
          }
          newPosition = new Point(newX, newY);
          if(check_validity(new Pair(nearestCoinId, newPosition), playerPieces, opponent_pieces)){
              moves.add(new Pair(nearestCoinId, newPosition));
              if(Board.getdist(newPosition, chosenWallCoordinate) < 2.1 || newX < 19){
                  wallCoordinates.remove(index);
              }
              break;
          }
          numTrial++;
      }

//        if(check_validity(new Pair(nearestCoinId, newPosition), playerPieces, opponent_pieces)){
//
//            moves.add(new Pair(nearestCoinId, newPosition));
//
//            // second move
//            newY +=  (2 * Math.sin(theta));
//            if(isplayer1){
//                newX -= 2 * Math.cos(theta);
//            }
//            else{
//                newX += 2 * Math.cos(theta);
//            }
//            newPosition = new Point(newX, newY);
//            moves.add(new Pair(nearestCoinId, newPosition));
//
//            if(Board.getdist(newPosition, chosenWallCoordinate) < 1){
//                wallCoordinates.remove(index);
//            }
//            return moves;
//        }
//    }
    return moves;
  }


  // Our Functions

  public void setUpWallCoordinates(){
    int wallSize = this.wallSize;
    double height = 40;
    double y_top = height/2;
    double diameter = this.pieceDiameter;
    double totalOccupiedSpace = diameter*wallSize;
    double totalGap = height - totalOccupiedSpace;
    double gap = totalGap/(wallSize+1);
    double[] yCoordinates = new double[wallSize];

    // based on what side of board we're on
    double xCoordinate = this.isPlayerOne ? 19 : -19;

    yCoordinates[0] = y_top - gap - diameter/2;
    for(int i = 1; i < wallSize; i++){
      yCoordinates[i] = yCoordinates[i-1]-diameter-gap;
    }

    for(double yCoordinate: yCoordinates){
      double roundedY = Math.round(yCoordinate * 100.0) / 100.0;
      System.out.println(xCoordinate + " and " + roundedY);
      wallCoordinates.add(new Point(xCoordinate, roundedY));
    }
  }

  public void matchCoins(HashMap<Integer, Point> coins){
      List<Integer> coinKeys = new ArrayList<Integer>(coins.keySet());
    for(Point wallCord : wallCoordinates){
      double minDist = Double.POSITIVE_INFINITY;
      int minCoin = -1;
      for(Integer coinId: coinKeys){
        Point coin = coins.get(coinId);
        double dist = Board.getdist(coin, wallCord);
        if(dist < minDist){
          minDist = dist;
          minCoin = coinId;
        }
      }
      try{
      coinKeys.remove(new Integer(minCoin));
      System.out.println(wallCord);
      System.out.println(coins.get(minCoin));
      System.out.println(minDist);
      coinWallMatches.put(wallCord, minCoin);
		} catch(Exception e){
			System.out.println(e);
		}
    }
		return;
	}

  public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
  {
      boolean valid = true;

      // check if move is adjacent to previous position.
     // System.out.println(Board.getdist(player_pieces.get(move.getKey()), move.getValue()));
      if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter))
      {
          return false;
      }
      // check for collisions
      valid = valid && !Board.check_collision(player_pieces, move);
      valid = valid && !Board.check_collision(opponent_pieces, move);

      // check within bounds
      valid = valid && Board.check_within_bounds(move);
      return valid;

  }

  public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
  {
      boolean valid = true;

      // check if move is adjacent to previous position.
      if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
          {
              return false;
          }
      // check for collisions
      valid = valid && !Board.check_collision(player_pieces, move);
      valid = valid && !Board.check_collision(opponent_pieces, move);

      // check within bounds
      valid = valid && Board.check_within_bounds(move);
      return valid;

  }

}

package flip.g3Wall;
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
  private int wallSize = 11;
  private Random random;
  private int wallVertical = 20;

  // Init Parameters
  private boolean isPlayerOne;
  private Integer numPieces;
  private Double pieceDiameter;

  // ArrayList
  private ArrayList<Integer> allCoins = new ArrayList<>();
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
    this.wallVertical = this.isPlayerOne ? 20 : -1*20;

    for(Integer i: pieces.keySet()){
      allCoins.add(i);
    }

    setUpWallCoordinates();
    System.out.println("Wall Cooodinates Setup");
    matchCoins(pieces);
    System.out.println("Initialized NTK Player");
    System.out.println("====");

  }

  public Pair<Integer, Point> nextStepTowardssX(Point current, Integer coinId, Point destination, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces){
    // finding the next move
    double deltaY = destination.y - current.y;
    double deltaX = destination.x - current.x;
    double theta = Math.atan(deltaY/deltaX);

    double newY;
    if(!isPlayerOne){
      newY =  current.y + 2*(Math.sin(theta));
    } else {
      newY =  current.y - 2*(Math.sin(theta));
    }

    double newX;
    if(!isPlayerOne){
      newX = current.x + 2*(Math.cos(theta));
    } else {
      newX = current.x - 2*(Math.cos(theta));
    }

    Point nextPoint = new Point(newX, newY);
    Pair nextMove = new Pair(coinId, nextPoint);
    System.out.println("--\n Move validity");
    if(!check_validity(nextMove, playerPieces, opponent_pieces)){
      System.out.println("=====");
      System.out.print("Chosen wall coordinate ");
      System.out.println(destination);
      System.out.print("Old position ");
      System.out.println(current);
      System.out.print("New Position ");
      System.out.println(nextPoint);
      System.out.print("Theta ");
      System.out.println(theta);
      System.out.println("Invalid move");
      System.out.println("=====");
    }
    return nextMove;
  }

  public Pair<Integer, Point> nextStepTowardsX(Point current, Integer coinId, Point destination, double noise){
    double deltaY = destination.y - current.y;
    double deltaX = destination.x - current.x;
    double theta = Math.atan(deltaY/deltaX) + noise;

    double newY = current.y + 2*(Math.sin(theta));

    int orientationFactor = +1;


    double newX;

    if(current.x > wallVertical && isPlayerOne){
      orientationFactor = -1;
    }
    else if(current.x < wallVertical && isPlayerOne ){
      orientationFactor = 1;
    }
    else if(current.x > wallVertical && !isPlayerOne) {
      orientationFactor = -1;
    }
    else {
      orientationFactor = 1;
    }

    newX = current.x + orientationFactor*2*(Math.cos(theta));

    Point nextPoint = new Point(newX, newY);
    Pair nextMove = new Pair(coinId, nextPoint);

    return nextMove;
  }

  public Pair<Integer, Point> nextValidStepTowardsX(Point current, Integer coinId, Point destination, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces){
    int granularity = 15;
    for(double noise = 0; noise < Math.PI; noise+=Math.PI/granularity ){
      Pair<Integer, Point> nextMove = nextStepTowardsX(current, coinId, destination, noise);
      if(check_validity(nextMove, playerPieces, opponent_pieces)){
        return nextMove;
      }
      nextMove = nextStepTowardsX(current, coinId, destination, -1*noise);
      if(check_validity(nextMove, playerPieces, opponent_pieces)){
        return nextMove;
      }
    }
    return null;
  }

  public List<Pair<Integer, Point>> getMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
    List<Pair<Integer, Point>> moves = new ArrayList<>();

    // filtering out parts of the wall that are built
    ArrayList<Point> newWallCoordinates = new ArrayList<Point>();

    for(Point wallCoordinate : wallCoordinates){
      Integer coin = coinWallMatches.get(wallCoordinate);
      Point coinpos = playerPieces.get(coin);
      if(Board.getdist(coinpos, wallCoordinate) > 0.1){
        newWallCoordinates.add(wallCoordinate);
      }
    }
    wallCoordinates = newWallCoordinates;

    if(wallCoordinates.size() != 0) {
      Point chosenWallCoordinate = wallCoordinates.get(0);
      Integer chosenCoinId = coinWallMatches.get(chosenWallCoordinate);
      Point chosenCoinPosition = playerPieces.get(chosenCoinId);
      System.out.println(chosenWallCoordinate);
      System.out.println(chosenCoinPosition);
      Pair<Integer, Point> nextMove = nextValidStepTowardsX(chosenCoinPosition, chosenCoinId, chosenWallCoordinate, playerPieces, opponent_pieces);
      if(check_validity(nextMove, playerPieces, opponent_pieces)){
        moves.add(nextMove);
      }
      moves.add(null);
      return moves;
    }
    return moves;
    // // march the wall
    //
    // for(Integer i : coinWallMatches.values()){
    //
    // }
  }

  public Point chooseWallCoordinate(HashMap<Integer, Point> playerPieces) {
    double minDistance = Double.POSITIVE_INFINITY;
    Point minCoordinate = wallCoordinates.get(0);

    for(Point coordinate: wallCoordinates){

      Integer coinId = coinWallMatches.get(coordinate);
      Point coinPoint = playerPieces.get(coinId);
      double distance = Board.getdist(coinPoint, coordinate);

      if( distance < minDistance ){
        minDistance = distance;
        minCoordinate = coordinate;
      }
    }

    return minCoordinate;
  }

  public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
  {
      boolean valid = true;

      // check if move is adjacent to previous position.
      if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter))
          {
              System.out.println("move is adjacent to prev position");
              return false;
          }
      // check for collisions
      valid = valid && !Board.check_collision(player_pieces, move);
      System.out.println("self collision " + Board.check_collision(player_pieces, move));
      valid = valid && !Board.check_collision(opponent_pieces, move);
      System.out.println("other collision " + Board.check_collision(opponent_pieces, move));
      // check within bounds
      valid = valid && Board.check_within_bounds(move);
      System.out.println("within bounds " + Board.check_within_bounds(move));
      return valid;

  }

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
    double xCoordinate = wallVertical;

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


}

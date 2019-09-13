package flip.ntkplayer;
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
    Point chosenWallCoordinate = wallCoordinates.get(0);

    for(Point wallCord : wallCoordinates){
        Integer matchedCoinId = coinWallMatches.get(wallCord);
        Point matchedCoinPoint = playerPieces.get(matchedCoinId);
        double distance = calculateEuclideanDistance(wallCord.x, wallCord.y, matchedCoinPoint.x, matchedCoinPoint.y);

        if(distance < nearestDistance) {
          nearestDistance = distance;
          chosenWallCoordinate = wallCord;
        }
    }

    Integer nearestCoinId = coinWallMatches.get(chosenWallCoordinate);
    Point nearestCoinPoint = playerPieces.get(nearestCoinId);
    double deltaY = chosenWallCoordinate.y - nearestCoinPoint.y;
    double deltaX = chosenWallCoordinate.x - nearestCoinPoint.x;
    double theta = Math.atan(deltaY/deltaX);

    List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

    // first move
    double newX = nearestCoinPoint.x + (2*Math.cos(theta));
    double newY = nearestCoinPoint.y + (2*Math.sin(theta));
		System.out.println(newX);
		System.out.println(newY);
    Point newPosition = new Point(newX, newY);
    moves.add(new Pair(nearestCoinId, newPosition));

    // second move
    newX +=  (2/Math.cos(theta));
    newY +=  (2/Math.sin(theta));
    newPosition = new Point(newX, newY);
    moves.add(new Pair(nearestCoinId, newPosition));

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
    double xCoordinate = this.isPlayerOne ? -19 : 19;

    yCoordinates[0] = y_top - gap - diameter/2;
    for(int i = 1; i < wallSize; i++){
      yCoordinates[i] = yCoordinates[i-1]-diameter-gap;
    }

    for(double yCoordinate: yCoordinates){
      double roundedY = Math.round(yCoordinate * 100.0) / 100.0;
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
        double dist = calculateEuclideanDistance(coin.x, coin.y, wallCord.x, wallCord.y);
        if(dist < minDist){
          minDist = dist;
          minCoin = coinId;
        }
      }
			try{
      coinKeys.remove(new Integer(minCoin));
      coinWallMatches.put(wallCord, minCoin);
		} catch(Exception e){
			System.out.println(e);
		}
    }
		return;
	}

  public double calculateEuclideanDistance(double x1, double y1, double x2, double y2){
    return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
  }
}

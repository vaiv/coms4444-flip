package flip.g3;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
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
  private int wallVertical;
  private boolean isWallBuilt = false;
  private boolean firstWall = false;
  public boolean marchedToMiddle = false;
  public int opponentEndzone = 23;

  // Init Parameters
  private boolean isPlayerOne;
  private Integer numPieces;
  private Double pieceDiameter;

  // ArrayList
  private ArrayList<Integer> allCoins = new ArrayList<>();
  private ArrayList<Point> wallPositions = new ArrayList<Point>();
	private ArrayList<Point> wallCoordinatesEmpty = new ArrayList<Point>();
  private ArrayList<Integer> behindWall = new ArrayList<>();
  public ArrayList<Integer> inFormation = new ArrayList<>();
  private HashMap<Point, Integer> coinWallMatches = new HashMap<>();
  Integer runnerCoin = null;
  Point runnerCoinDestination = null;
  Point runnerCoinLastLocation = null;

  // wall marching
  Queue<Integer> runners = new LinkedList<>();

  // alternate between marching and building wall


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
      this.wallVertical = this.isPlayerOne ? -27 : 27;
      this.opponentEndzone = this.isPlayerOne ? -23 : 23;
      for(Integer i: pieces.keySet()){
        allCoins.add(i);
        behindWall.add(i);
      }
      setUpwallCoordinatesEmpty();
      System.out.println("Wall Cooodinates Setup");
      matchCoins(pieces);
      System.out.println("Initialized NTK Player");
      System.out.println("====");
  }

  public List<Pair<Integer, Point>> getMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
    ArrayList<Pair<Integer, Point>> moves = new ArrayList<>();
    runnerCoinLastLocation = runnerCoinLastLocation== null ? new Point(0,0) : runnerCoinLastLocation ;
    //flip this to disable
    if(inFormation.size() > numPieces || inFormation.size() > wallSize) {
      HashMap<Integer, Point> forwardPieces = new HashMap<Integer, Point>();
      ArrayList newInFormation = new ArrayList<Integer>();
      for(Integer i: inFormation){
        Point currentPosition = playerPieces.get(i);
        if(!isCoinInEndzone(currentPosition)){
          newInFormation.add(i);
        }
        forwardPieces.put(i, playerPieces.get(i));
      }
      inFormation = newInFormation;
      System.out.println(forwardPieces);
      for(int i = 0; i<100; i++){
        if(moves.size() >= 2){
          break;
        }
        Integer piece_id = inFormation.get(random.nextInt(inFormation.size()));
        Point curr_position = playerPieces.get(piece_id);
        Point new_position = new Point(curr_position);
        double theta = -Math.PI/2 + Math.PI * random.nextDouble();
        double delta_x = pieceDiameter * Math.cos(theta);
        double delta_y = pieceDiameter * Math.sin(theta);
        new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
        new_position.y += delta_y;
        Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
        if(check_validity(move, playerPieces, opponent_pieces))
          moves.add(move);
      }
      return moves;

      // System.out.println(runnerCoin);
      // System.out.println(inFormation);
      // try{
      //   System.out.println("HEELLOOsss");
      //   if(runnerCoinDestination != null) {
      //     if(Board.getdist(playerPieces.get(runnerCoin), runnerCoinDestination) < 2){
      //       runnerCoin = null;
      //       runnerCoinDestination = null;
      //     }
      //   }
      //   if(runnerCoin != null) {
      //     if(Board.getdist(playerPieces.get(runnerCoin), runnerCoinLastLocation) < 2){
      //       runnerCoin = null;
      //     }
      //   }
      //
      //     System.out.println("HEELLOOsss");
      //
      //   if(runnerCoin == null){
      //     System.out.println("HEELLOO");
      //     runnerCoin = inFormation.get(0);
      //     inFormation.remove(0);
      //     runnerCoinDestination = new Point(opponentEndzone, playerPieces.get(runnerCoin).y);
      //   }
      //   runnerCoinLastLocation = playerPieces.get(runnerCoin);
      //   Pair<Integer, Point> stepOne = nextValidStepTowardsX(playerPieces.get(runnerCoin), runnerCoin, runnerCoinDestination, playerPieces, opponent_pieces);
      //   Pair<Integer, Point> stepTwo = nextValidStepTowardsX(stepOne.getValue(), runnerCoin, runnerCoinDestination, playerPieces, opponent_pieces);
      //   moves.add(stepOne);
      //   if(check_validity(stepOne, opponent_pieces, playerPieces))
      //   {
      //     runnerCoin = null;
      //   }
      //   // moves.add(stepTwo);
      //   return moves;
      // } catch (Exception e) {
      //   System.out.println(e);
      //   return moves;
      // }
    } else {
      if(!isWallBuilt) {
        List<Pair<Integer, Point>> wallMoves = getWallMoves(numMoves, playerPieces, opponent_pieces, isplayer1);
        return wallMoves;
      }
      else {
        System.out.println("Old vertical "+this.wallVertical);
        this.wallVertical = this.isPlayerOne ? this.wallVertical + 4 : this.wallVertical - 4;
        System.out.println("nu vertical "+this.wallVertical);
        setUpwallCoordinatesEmpty();
        HashMap<Integer, Point> behindWallPieces = new HashMap<>();
        for(Integer i: behindWall) {
          behindWallPieces.put(i, playerPieces.get(i));
        }
        matchCoins(behindWallPieces);
        isWallBuilt = false;
        runnerCoin = runners.remove();
      }
      return moves;
    }
  }

// Random

public List<Pair<Integer, Point>> getRandomMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
{
  List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
   int num_trials = 30;
   int i = 0;
   while(moves.size()!= num_moves && i<num_trials)
   {

    Integer piece_id = random.nextInt(player_pieces.keySet().size());
    Point curr_position = player_pieces.get(piece_id);
    Point new_position = new Point(curr_position);
    double theta = -Math.PI/2 + Math.PI * random.nextDouble();
    double delta_x = pieceDiameter * Math.cos(theta);
    double delta_y = pieceDiameter * Math.sin(theta);

    Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
    // System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
    // Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

    new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
    new_position.y += delta_y;
    Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);

    Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
    // System.out.println("distance from previous position is " + dist.toString());
    // Log.record("distance from previous position is " + dist.toString());

    if(check_validity(move, player_pieces, opponent_pieces))
      moves.add(move);
    i++;
   }

   return moves;
}



  /* ========================================= */
  /* Wall Builder */
  /* ========================================= */

  public List<Pair<Integer, Point>> getWallMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
    List<Pair<Integer, Point>> moves = new ArrayList<>();
    ArrayList<Point> newwallCoordinatesEmpty = new ArrayList<Point>();
    for(Point wallCoordinate : wallCoordinatesEmpty){
      Integer coin = coinWallMatches.get(wallCoordinate);
      Point coinpos = playerPieces.get(coin);
      if(Board.getdist(coinpos, wallCoordinate) > 2){
        newwallCoordinatesEmpty.add(wallCoordinate);
      } else {
        System.out.println("Wall coordinate");
        System.out.println(wallCoordinate);
        Integer chosenCoinId = coinWallMatches.get(wallCoordinate);
        System.out.println("ID: " + chosenCoinId);
        inFormation.add(chosenCoinId);
        Point chosenCoinPosition = playerPieces.get(chosenCoinId);
        System.out.println(chosenCoinPosition);
        runners.add(chosenCoinId);
        moves = doFinalCorrection(chosenCoinId, chosenCoinPosition, wallCoordinate, playerPieces, opponent_pieces);
      }
    }
    wallCoordinatesEmpty = newwallCoordinatesEmpty;
    if(moves.size() > 0) {
      return moves;
    }

    if(wallCoordinatesEmpty.size() != 0) {
      Point chosenWallCoordinate = wallCoordinatesEmpty.get(0);
      Integer chosenCoinId = coinWallMatches.get(chosenWallCoordinate);
      Point chosenCoinPosition = playerPieces.get(chosenCoinId);
      Pair<Integer, Point> nextMove = nextValidStepTowardsX(chosenCoinPosition, chosenCoinId, chosenWallCoordinate, playerPieces, opponent_pieces);
      if(check_validity(nextMove, playerPieces, opponent_pieces)){
        moves.add(nextMove);
      }
      moves.add(null);
      return moves;
    } else {
      isWallBuilt = true;
      firstWall = true;
    }
    return moves;
  }
  public Point chooseWallCoordinate(HashMap<Integer, Point> playerPieces) {
    double minDistance = Double.POSITIVE_INFINITY;
    Point minCoordinate = wallCoordinatesEmpty.get(0);

    for(Point coordinate: wallCoordinatesEmpty){

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
  public void setUpwallCoordinatesEmpty(){
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
      wallCoordinatesEmpty.add(new Point(xCoordinate, roundedY));
      wallPositions.add(new Point(xCoordinate, roundedY));
    }
  }
  public void matchCoins(HashMap<Integer, Point> coins){
      List<Integer> coinKeys = new ArrayList<Integer>(coins.keySet());
      for(Point wallCord : wallCoordinatesEmpty){
          Integer minCoin = pickClosestCoin(wallCord, coins, coinKeys);
        try{
          coinKeys.remove(new Integer(minCoin));
          behindWall.remove(new Integer(minCoin));
          coinWallMatches.put(wallCord, minCoin);
      } catch(Exception e){
          System.out.println(e);
      }
    }
    return;
  }
  public Integer pickClosestCoin(Point position, HashMap<Integer, Point> coinPositions, List<Integer> coinsToConsider){
    List<Integer> coinKeys = coinsToConsider;
    double minDist = Double.POSITIVE_INFINITY;
    int minCoin = -1;
    for(Integer coinId: coinKeys){
      Point coin = coinPositions.get(coinId);
      double dist = Board.getdist(coin, position);
      if(dist < minDist){
        minDist = dist;
        minCoin = coinId;
      }
    }
    return minCoin;
  }
  public Boolean isWallBuilt(){
    return wallCoordinatesEmpty.size() == 0;
  }
  /* ========================================= */
  /* HELPER FUNCTIONS */
  /* ========================================= */

  public boolean isCoinInEndzone(Point position){
      if(isPlayerOne){
        if(position.x < -22){
          return true;
        }
      } else if ( !isPlayerOne ) {
        if(position.x > 22) {
          return true;
      }
    }
    return false;
  }
  public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces){
      boolean valid = true;

      if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter))
          {
              return false;
          }
      valid = valid && !Board.check_collision(player_pieces, move);
      valid = valid && !Board.check_collision(opponent_pieces, move);
      valid = valid && Board.check_within_bounds(move);
      return valid;

  }
  public List<Pair<Integer, Point>> doFinalCorrection(Integer coinId, Point position, Point target, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces) {
    List<Pair<Integer, Point>> moves = new ArrayList<>();
    double distance = Board.getdist(position, target);
    double theta = Math.acos(distance/(2.0*pieceDiameter));
    for(double i = 0.001; i <= 1; i += 0.001){
      double newY = position.y + 2.0*Math.sin(theta);
      double newX = position.x + 2.0*Math.cos(theta);
      moves.add(new Pair(coinId, new Point(newX, newY)));
      newY = position.y - 2.0 *Math.sin(Math.PI/2.0 - theta);
      newX = position.x + 2.0*Math.cos(Math.PI/2.0 - theta);
      moves.add(new Pair(coinId, target));
      return moves;
    }
    return moves;
  }
  public Pair<Integer, Point> nextValidStepTowardsX(Point current, Integer coinId, Point destination, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponent_pieces){
    int granularity = 400;
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
}

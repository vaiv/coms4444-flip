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
import java.util.Map;


import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player {
  // Init Parameters
  private boolean isPlayerOne;
  private int numPieces;
  private double pieceDiameter;
  public double goal_X;
  private int playerMultiplier;
  private HashMap<Integer, Point> endzonePieces;

  public void init(HashMap<Integer, Point> pieces, int numPieces, double t, boolean isPlayerOne, double pieceDiameter) {
    this.numPieces = numPieces;
    this.isPlayerOne = isPlayerOne;
    this.pieceDiameter = pieceDiameter;
    this.goal_X = isPlayerOne? -23:23;
    playerMultiplier = isPlayerOne? -1:1;
    endzonePieces = new HashMap<>();
  }

  public List<Pair<Integer, Point>> getMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne) {
    //playerPieces = removeEndzonePieces(playerPieces);
    //Integer closest_piece = findPreferredPiece(playerPieces);
    //Point current = playerPieces.get(closest_piece);
    //return moveToTarget(closest_piece, new Point(goal_X,current.y), playerPieces, opponentPieces);

    ArrayList<Pair<Integer, Point>> moves = new ArrayList<>();

    for(int i=0; i<numMoves; i++)
    {

      Pair<Integer, Point> move = getOneMove(playerPieces,opponentPieces, isPlayerOne);
      moves.add(move);
      //System.out.println("Move: "+move);
      playerPieces.put(move.getKey(), move.getValue()); //update board
    }
    //add provision to fill up moves with random moves if size is less than num_Moves.
    System.out.println("Moves:"+moves);
    return moves;

  }


  public Pair<Integer, Point> getOneMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
  {
      Integer piece = findIdealPiece(playerPieces,opponentPieces, isPlayerOne);
      Point idealPoint = new Point(playerPieces.get(piece).x + playerMultiplier*pieceDiameter,playerPieces.get(piece).y);
      Pair<Integer,Point> idealMove = new Pair(piece, idealPoint);
      if(checkValidity(idealMove,playerPieces,opponentPieces))
        return idealMove;
      else //in case we can't move straight ahead, we look for smallest deviation
      {
        //check if an endzone piece is blocking
        for(Map.Entry<Integer, Point> e : endzonePieces.entrySet()){
          Integer endzonePiece = e.getKey();
          Point endzonePoint = e.getValue();
          if(Board.getdist(idealPoint, endzonePoint) < 2){
            idealPoint = getValidPosition(endzonePiece, new Point(goal_X+1,playerPieces.get(endzonePiece).y),playerPieces,opponentPieces);
            Pair<Integer,Point> next_move = new Pair(endzonePiece, idealPoint);
            System.out.println("validity of endzone move :"+checkValidity(next_move,playerPieces,opponentPieces));
            return next_move;
          }
        }
        Point validPoint = getValidPosition(piece, new Point(goal_X,playerPieces.get(piece).y),playerPieces,opponentPieces);
        Pair<Integer,Point> next_move = new Pair(piece, validPoint);
        System.out.println("validity of non-ideal move :"+checkValidity(next_move,playerPieces,opponentPieces));
        return next_move;
      }

  }


  public Integer findIdealPiece( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
  {
    HashMap<Integer, Point> eligiblePieces = removeEndzonePieces(playerPieces);
    //maybe remove all pieces behind a wall if detected

    HashMap<Integer, Point> nearestObstacles = new HashMap<>();
    double initial_ob = isPlayerOne? Double.NEGATIVE_INFINITY: Double.POSITIVE_INFINITY;

    for(Map.Entry<Integer, Point> e : eligiblePieces.entrySet())
    {

      Integer piece = e.getKey();
      Point current = e.getValue();

      nearestObstacles.put(piece,new Point(initial_ob,current.y));

      for(Map.Entry<Integer, Point> f : opponentPieces.entrySet())
      {
        if(Math.abs(f.getValue().y-current.y)<pieceDiameter) //IF IN MY LANE.  [--maybe account for epsilon?]
        {
           if(isPlayerOne && f.getValue().x<=current.x) //if its actually in your path to success
              {
                  if(f.getValue().x > nearestObstacles.get(piece).x) //if it's closer than current closest obstacle
                    nearestObstacles.put(piece, f.getValue());
              }
            else if(!isPlayerOne && f.getValue().x>=current.x)
            {
                {
                  if(f.getValue().x < nearestObstacles.get(piece).x) //if it's closer than current closest obstacle
                    nearestObstacles.put(piece, f.getValue());
              }

            }
        }


      }

      for(Map.Entry<Integer, Point> r : playerPieces.entrySet())
      {
        if(r.getKey()==e.getKey())
        {
          //System.out.println("Current Piece being checked");
        }
        else
        {
          //System.out.println("Not Current Piece being checked");

        if(Math.abs(r.getValue().y-current.y)<pieceDiameter) //IF IN MY LANE.  [--maybe account for epsilon?]
        {
           if(isPlayerOne && r.getValue().x<current.x) //if its actually in your path to success
              {
                  if(r.getValue().x > nearestObstacles.get(piece).x) //if it's closer than current closest obstacle
                    nearestObstacles.put(piece, r.getValue());
              }
            else if(!isPlayerOne && r.getValue().x>current.x)
            {
                {
                  if(r.getValue().x < nearestObstacles.get(piece).x) //if it's closer than current closest obstacle
                    nearestObstacles.put(piece, r.getValue());
              }

            }
        }
      }


      }




      }

      System.out.println(nearestObstacles);

      double maxDist = Double.NEGATIVE_INFINITY;
      Integer idealPiece = 0;

      for (Map.Entry<Integer, Point> g : nearestObstacles.entrySet() )
      {
          Point curpos = playerPieces.get(g.getKey());
          Point obstaclepos = g.getValue();
          double newdist = Board.getdist(curpos,obstaclepos);
          if(newdist>maxDist)
          {
              idealPiece = g.getKey();
              maxDist = newdist;

          }


      }

      //System.out.println("idealPiece: "+idealPiece);

      return idealPiece;


  }


  public Integer findPreferredPiece(HashMap<Integer, Point> playerPieces){
    if(numPieces > 8){
      return findClosestPiece(playerPieces);
    }
    return findFurthestPiece(playerPieces);
  }

  public HashMap<Integer, Point> removeEndzonePieces(HashMap<Integer, Point> playerPieces){
    double endzone = isPlayerOne? -goal_X:goal_X;
    HashMap<Integer, Point> pieces = new HashMap<Integer, Point>();
    for(Map.Entry<Integer, Point> e : playerPieces.entrySet()){
      Point pos = e.getValue();
      if(!((isPlayerOne && pos.x < -21) || (!isPlayerOne && pos.x > 21))){
        pieces.put(e.getKey(), e.getValue());
      }
      else{
        endzonePieces.put(e.getKey(), e.getValue());
      }
    }
    System.out.println("removed pieces.");
    return pieces;
  }

  public Integer findClosestPiece(HashMap<Integer, Point> playerPieces){
    Integer minPoint = 0;
    Double minDiff = Double.POSITIVE_INFINITY;
    for(Map.Entry<Integer, Point> e : playerPieces.entrySet()){
      Point pos = e.getValue();
      if(Board.getdist(pos, new Point(goal_X, pos.y)) < minDiff){
        minDiff = Board.getdist(pos, new Point(goal_X, pos.y));
        minPoint = e.getKey();
      }
    }
    return minPoint;
  }

  public Integer findFurthestPiece(HashMap<Integer, Point> playerPieces){
    Integer maxPoint = 0;
    Double maxDiff = Double.NEGATIVE_INFINITY;
    for(Map.Entry<Integer, Point> e : playerPieces.entrySet()){
      Point pos = e.getValue();
      if(Board.getdist(pos, new Point(goal_X, pos.y)) > maxDiff){
        maxDiff = Board.getdist(pos, new Point(goal_X, pos.y));
        maxPoint = e.getKey();
      }
    }
    return maxPoint;
  }

  public List<Pair<Integer, Point>> moveToTarget(Integer id, Point target, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces ){
    ArrayList<Pair<Integer, Point>> moves = new ArrayList<>();
    Point pos1, pos2;

    pos1 = getValidPosition(id, target, playerPieces, opponentPieces);
    playerPieces.put(id, pos1);
    pos2 = getValidPosition(id, target, playerPieces, opponentPieces);

    moves.add(new Pair(id, pos1));
    moves.add(new Pair(id, pos2));
    return moves;
  }

  public Point getValidPosition(Integer id, Point target, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces ){
    Point current = playerPieces.get(id);
    double targetDiffX = target.x - current.x;
    double targetDiffY = target.y - current.y;

    // trig
    double hypotenuse = Math.sqrt(Math.pow(targetDiffX,2)+Math.pow(targetDiffY,2));
    double theta = Math.atan(targetDiffY/targetDiffX);
    System.out.println(theta*180/Math.PI);

    // move directly towards
    Point possiblePosition = getNextPositionInTheta(current, theta);
    if(checkValidity(new Pair(id, possiblePosition), playerPieces, opponentPieces)){
      System.out.println("Forward move possible");
      return possiblePosition;
    }
    else{
      System.out.println("Forward move not possible");
    }

    // 90 degree fan
    double difference = 0.02;
    for(double noise=0; noise < Math.PI; noise+=difference){
      possiblePosition = getNextPositionInTheta(current, theta+noise);
      if(checkValidity(new Pair(id, possiblePosition), playerPieces, opponentPieces)){
        System.out.println("return pos");
        return possiblePosition;
      }
      possiblePosition = getNextPositionInTheta(current, theta-noise);
      if(checkValidity(new Pair(id, possiblePosition), playerPieces, opponentPieces)){
        System.out.println("return pos");
        return possiblePosition;
      }
    }
    return null;
  }

  public Point getNextPositionInTheta(Point current, double theta){
    double delta_x = pieceDiameter * Math.cos(theta);
    double delta_y = pieceDiameter * Math.sin(theta);

    double new_x = current.x;
    new_x += this.isPlayerOne? -delta_x:delta_x;

    double new_y = current.y + delta_y;

    return new Point(new_x, new_y);
  }


    public boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces){
        boolean valid = true;
        if(!Board.almostEqual(Board.getdist(playerPieces.get(move.getKey()), move.getValue()), pieceDiameter))
            {
                System.out.println("adjacent");
                return false;
            }
        valid = valid && !Board.check_collision(playerPieces, move);
        valid = valid && !Board.check_collision(opponentPieces, move);
        valid = valid && Board.check_within_bounds(move);
        return valid;
    }



}

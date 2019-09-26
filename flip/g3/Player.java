package flip.g3;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;
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
  private boolean allAcross;
  private int numPieces;
  private double pieceDiameter;
  public double goal_X;
  private int playerMultiplier;
  private HashMap<Integer, Point> endzonePieces;
  private HashMap<Integer, Point> actionPieces;
  ArrayList<Pair<Integer, Point>> allMoves;


  //for Vaibhav's Random code
  private int seed = 42;
  private Random random;

  //Wall stuff
  private double wallX;
  private ArrayList<Point> idealWallLocations;
  private int wallMover;
  private HashSet<Integer> wall = new HashSet<>();
  private boolean wallBuilt = false;
  private boolean wallMode;




  public void init(HashMap<Integer, Point> pieces, int numPieces, double t, boolean isPlayerOne, double pieceDiameter) {
    this.numPieces = numPieces;
    this.isPlayerOne = isPlayerOne;
    this.pieceDiameter = pieceDiameter;
    this.goal_X = isPlayerOne? -28:28;
    playerMultiplier = isPlayerOne? -1:1;
    endzonePieces = new HashMap<>();
    actionPieces = new HashMap<>();
    allAcross = false;
    allMoves = new ArrayList<>(); //use to track if moves are being repeated - generate random if they are.

    random = new Random(seed);

    //Wall stuff

    wallBuilt = false;
    wallMode = numPieces > 12;
    wallMover = -1;

    wallX = isPlayerOne ? 20.0 : -20.0;
    idealWallLocations = new ArrayList<>();
    for(int i=0; i < 11; i++){
        this.idealWallLocations.add(new Point(wallX, 2*i*Math.sqrt(3) + Math.sqrt(3) - 19));
      }


  }

  public List<Pair<Integer, Point>> getMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne) {
    //playerPieces = removeEndzonePieces(playerPieces);
    //Integer closest_piece = findPreferredPiece(playerPieces);
    //Point current = playerPieces.get(closest_piece);
    //return moveToTarget(closest_piece, new Point(goal_X,current.y), playerPieces, opponentPieces);

    ArrayList<Pair<Integer, Point>> moves = new ArrayList<>();

    if(numPieces > 12 && idealWallLocations.size() == 0)
      return null;

    for(int i=0; i<numMoves; i++)
    {

      Pair<Integer, Point> move = getOneMove(playerPieces, opponentPieces, isPlayerOne);
      //write code to check if piece in move is stuck, and generate alternate moves if it is.

      if(move==null) //check for no move returned - don't waste moves
        move = getRandomMove(playerPieces,opponentPieces, isPlayerOne);

      allMoves.add(move);

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
      if(!allAcross && numPieces<=12 && numPieces >5)
        return getPrelimMove(playerPieces,opponentPieces,isPlayerOne);

      else if(numPieces>12)
        return getWallMove(playerPieces,opponentPieces,isPlayerOne);

      else
        return getRegMove(playerPieces,opponentPieces,isPlayerOne);
  }

  public Pair<Integer, Point> getPrelimMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
{

        HashMap<Integer, Point> eligiblePieces = removeActionPieces(removeEndzonePieces(playerPieces));
        if(eligiblePieces.size()==0)
        {
          allAcross = true;
          return getRegMove(playerPieces,opponentPieces,isPlayerOne);
        }
        Integer piece = findIdealPrelimPiece(eligiblePieces, playerPieces, opponentPieces, isPlayerOne);
        Point idealPoint = new Point(playerPieces.get(piece).x + playerMultiplier*pieceDiameter,playerPieces.get(piece).y);
        Pair<Integer,Point> idealMove = new Pair(piece, idealPoint);
        if(checkValidity(idealMove,playerPieces,opponentPieces))
          return idealMove;
        else //in case we can't move straight ahead, we look for smallest deviation
        {
          //check if an endzone piece is blocking
          for(Map.Entry<Integer, Point> e : actionPieces.entrySet()){
            Integer actionPiece = e.getKey();
            Point actionPoint = e.getValue();
            if(Board.getdist(idealPoint, actionPoint) < 2){
//            idealPoint = getValidPosition(endzonePiece, new Point(goal_X+1,playerPieces.get(endzonePiece).y),playerPieces,opponentPieces);
//            Pair<Integer,Point> next_move = new Pair(endzonePiece, idealPoint);
//            System.out.println("validity of endzone move :"+checkValidity(next_move,playerPieces,opponentPieces));
              return getActionMove(actionPiece, playerPieces, opponentPieces);
            }
          }
          Point validPoint = getValidPosition(piece, new Point(goal_X,playerPieces.get(piece).y),playerPieces,opponentPieces);
          Pair<Integer,Point> next_move = new Pair(piece, validPoint);
          System.out.println("validity of non-ideal move :"+checkValidity(next_move,playerPieces,opponentPieces));
          if(checkValidity(next_move,playerPieces,opponentPieces)){
            return next_move;
          }
          else{;
            //eligiblePieces.remove(piece);
          }
        }


      return null;


}

public Point getPreferredWallLocation(HashMap<Integer, Point> opponentPieces){
  Point mostThreatened = idealWallLocations.get(0);
  double mostThreatenedDist = Double.POSITIVE_INFINITY;
  for(Point wallPoint : idealWallLocations){
    for(Map.Entry<Integer, Point> e: opponentPieces.entrySet()){
      if(Board.getdist(wallPoint, e.getValue()) < mostThreatenedDist){
        mostThreatened = wallPoint;
        mostThreatenedDist = Board.getdist(wallPoint, e.getValue());
      }
    }
  }
  return mostThreatened;
}


public Pair<Integer, Point> getWallMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
{
    if(idealWallLocations.size() == 1 && (wallMover != -1)){
      if(Board.getdist(idealWallLocations.get(0), playerPieces.get(wallMover)) < 0.05){
        idealWallLocations.remove(0);
        System.out.println("out");
        return null;
      }
      if(Board.getdist(idealWallLocations.get(0), playerPieces.get(wallMover)) > 2.0 - 0.00007 && Board.getdist(idealWallLocations.get(0), playerPieces.get(wallMover)) < 2.0 + 0.00007 )
      {
        Point validPos = getValidPosition(wallMover, idealWallLocations.get(0), playerPieces, opponentPieces);
        if(checkValidity(new Pair(wallMover, validPos), playerPieces, opponentPieces)){
          System.out.println("YA");
        }
        return new Pair(wallMover, validPos);
      }
    }

    Point wallPoint = getPreferredWallLocation(opponentPieces);//idealWallLocations.get(0);
    if(wallMover < 0){
      double minDist = Double.POSITIVE_INFINITY;
      for(Map.Entry<Integer, Point> e : playerPieces.entrySet()){
        Integer wallPiece = e.getKey();
        Point wallPiecePoint = e.getValue();
        if(Board.getdist(wallPoint, wallPiecePoint) < minDist && !wall.contains(wallPiece)){
            wallMover = wallPiece;
            minDist = Board.getdist(wallPoint, wallPiecePoint);
        }
      }
    }
    Pair<Integer, Point> wallMove = getNextMove(playerPieces.get(wallMover), wallPoint, wallMover, playerPieces, opponentPieces);
    if(wallMove != null){
      if(Board.getdist(wallMove.getValue() , wallPoint) < 0.001){
        idealWallLocations.remove(wallPoint);
        wall.add(wallMover);
        wallMover = -1;
        if(idealWallLocations.size() == 0){
          wallMode = false;
        }
      }
      System.out.println(wallMove);
      return wallMove;
    }
    return null;

  }

  public Pair<Integer, Point> getNextMove(Point a, Point b,
                                          Integer pieceID,
                                          HashMap<Integer, Point> playerPieces,
                                          HashMap<Integer, Point> opponentPieces){

    Pair<Integer, Point> move;
    double dist = Board.getdist(a, b);

    if (Board.almostEqual(dist, 0))
      return null;

    else if (Board.almostEqual(dist, 2)){
      move = new Pair<Integer, Point>(pieceID, b);
      if(checkValidity(move, playerPieces, opponentPieces)) {
        return move;
      }
      else
        return new Pair(pieceID, getValidPosition(pieceID, b, playerPieces, opponentPieces));
    }

    else if (dist < 4) {
      double x1 = 0.5 * (b.x + a.x);
      double y1 = 0.5 * (b.y + a.y);

      double sqrt_const = Math.sqrt(16/(dist*dist)-1) / 2;
      double x2 = sqrt_const * (b.y - a.y);
      double y2 = sqrt_const * (a.x - b.x);

      move = new Pair<Integer, Point>(pieceID, new Point(x1+x2, y1+y2));
      if(checkValidity(move, playerPieces, opponentPieces)){
        return move;
      }

      move = new Pair<Integer, Point>(pieceID, new Point(x1-x2, y1-y2));
      if(checkValidity(move, playerPieces, opponentPieces)){
        return move;
      }
      return new Pair(pieceID, getValidPosition(pieceID, b, playerPieces, opponentPieces));
    }

    else{
      move = new Pair<Integer, Point>(pieceID,  new Point(
              a.x + 2 * (b.x - a.x) / dist,
              a.y + 2 * (b.y - a.y) / dist
      ));

      if(checkValidity(move, playerPieces, opponentPieces))
        return move;

      return new Pair(pieceID, getValidPosition(pieceID, b, playerPieces, opponentPieces));
    }
  }


  public Pair<Integer, Point> getRegMove( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
  {
      //HashMap<Integer, Point> eligiblePieces = removeEndzonePieces(playerPieces);

        Integer piece = findIdealRegPiece(playerPieces, opponentPieces, isPlayerOne);
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
//            idealPoint = getValidPosition(endzonePiece, new Point(goal_X+1,playerPieces.get(endzonePiece).y),playerPieces,opponentPieces);
//            Pair<Integer,Point> next_move = new Pair(endzonePiece, idealPoint);
//            System.out.println("validity of endzone move :"+checkValidity(next_move,playerPieces,opponentPieces));
              return getEndzoneMove(endzonePiece, playerPieces, opponentPieces);
            }
          }
          Point validPoint = getValidPosition(piece, new Point(goal_X,playerPieces.get(piece).y),playerPieces,opponentPieces);
          Pair<Integer,Point> next_move = new Pair(piece, validPoint);
          System.out.println("validity of non-ideal move :"+checkValidity(next_move,playerPieces,opponentPieces));
          if(checkValidity(next_move,playerPieces,opponentPieces)){
            return next_move;
          }
          else{;
            //eligiblePieces.remove(piece);
          }
        }


      return null;
  }


public Pair<Integer,Point> getActionMove(Integer actionPiece, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces){
    Point idealPoint = getValidPosition(actionPiece, new Point(goal_X,playerPieces.get(actionPiece).y),playerPieces,opponentPieces);
    Pair<Integer,Point> next_move = new Pair(actionPiece, idealPoint);
    if(checkValidity(next_move,playerPieces,opponentPieces)){
      return next_move;
    }
    else{
      for(Map.Entry<Integer, Point> e : actionPieces.entrySet()) {
        actionPiece = e.getKey();
        Point actionPoint = e.getValue();
        if (Board.getdist(actionPoint, actionPoint) < 2) {
//            idealPoint = getValidPosition(endzonePiece, new Point(goal_X+1,playerPieces.get(endzonePiece).y),playerPieces,opponentPieces);
//            Pair<Integer,Point> next_move = new Pair(endzonePiece, idealPoint);
//            System.out.println("validity of endzone move :"+checkValidity(next_move,playerPieces,opponentPieces));
          return getActionMove(actionPiece, playerPieces, opponentPieces);
        }
      }
    }
    return null;
  }

  public Pair<Integer,Point> getEndzoneMove(Integer endzonePiece, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces){
    Point idealPoint = getValidPosition(endzonePiece, new Point(goal_X+1,playerPieces.get(endzonePiece).y),playerPieces,opponentPieces);
    Pair<Integer,Point> next_move = new Pair(endzonePiece, idealPoint);
    System.out.println("validity of endzone move :"+checkValidity(next_move,playerPieces,opponentPieces));
    if(checkValidity(next_move,playerPieces,opponentPieces)){
      return next_move;
    }
    else{
      for(Map.Entry<Integer, Point> e : endzonePieces.entrySet()) {
        endzonePiece = e.getKey();
        Point endzonePoint = e.getValue();
        if (Board.getdist(idealPoint, endzonePoint) < 2) {
//            idealPoint = getValidPosition(endzonePiece, new Point(goal_X+1,playerPieces.get(endzonePiece).y),playerPieces,opponentPieces);
//            Pair<Integer,Point> next_move = new Pair(endzonePiece, idealPoint);
//            System.out.println("validity of endzone move :"+checkValidity(next_move,playerPieces,opponentPieces));
          return getEndzoneMove(endzonePiece, playerPieces, opponentPieces);
        }
      }
    }
    return null;
  }

  public Pair<Integer, Point> getRandomMove( HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
  {


    player_pieces = removeEndzonePieces(player_pieces);

     while(true)
     {
      Integer piece_id = findFurthestPiece(player_pieces);
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

      if(checkValidity(move, player_pieces, opponent_pieces))
        return move;
     }

  }


/*public boolean isStuck(Pair<Integer, Point> move)
{
  Pair<Integer, Point> last_move = allMoves.get(allMoves.size()-1);
  Pair<Integer, Point> second_move = allMoves.get(allMoves.size()-2);
  if(move.getKey() == last_move.getKey() && move.getKey()==second_move.getKey())
  {
      if(Board.getdist(move.getValue(),last_move.getValue())<0.3 && Board.getdist(last_move.getValue(),second_move.getValue())<0.3 )
        {
          System.out.println("Piece was stuck");
          return true;
        }
  }
  return false;
}*/

  public Integer findIdealRegPiece( HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
  {
    //HashMap<Integer, Point> eligiblePieces = removeEndzonePieces(playerPieces);
    //maybe remove all pieces behind a wall if detected

    /*if(!allAcross) //find termination clause
    {
      eligiblePieces = removeCrossedPieces(eligiblePieces);
      if(eligiblePieces.size()==0)
      {
        allAcross = true;
        eligiblePieces = removeEndzonePieces(playerPieces);
      }
    }
*/
    HashMap<Integer, Point> eligiblePieces = removeEndzonePieces(playerPieces);
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

  public Integer findIdealPrelimPiece( HashMap<Integer, Point> eligiblePieces,HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayerOne)
  {


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

  public HashMap<Integer, Point> removeActionPieces(HashMap<Integer, Point> playerPieces){
    HashMap<Integer, Point> pieces = new HashMap<Integer, Point>();
    for(Map.Entry<Integer, Point> e : playerPieces.entrySet()){
      Point pos = e.getValue();
      if((e.getValue().x>18 && isPlayerOne)||(e.getValue().x<-18 && !isPlayerOne)){
        pieces.put(e.getKey(), e.getValue());
      }
      else
      {
        actionPieces.put(e.getKey(), e.getValue());
      }
    }
    System.out.println("removed crossed pieces.");
    return pieces;
  }


  public Integer findPreferredPiece(HashMap<Integer, Point> playerPieces){
    if(numPieces > 8){
      return findClosestPiece(playerPieces);
    }
    return findFurthestPiece(playerPieces);
  }

  public HashMap<Integer, Point> removeEndzonePieces(HashMap<Integer, Point> playerPieces){
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


 public Pair<Integer, Point>getNextWallMove(Integer id,Point cur , Point target, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces ){

    /*if(cur.x==target.x && cur.y == target.y)
      return null;*/

    Point pos = getValidPosition(id, target, playerPieces, opponentPieces);
    Pair<Integer, Point> move = new Pair<>(id, pos);
    return move;
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
    for(double noise=0; noise < 4 * Math.PI; noise+=difference){
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

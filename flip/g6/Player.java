package flip.g6;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.util.Arrays;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	

    // Indicator - whether wall is formed
	private boolean dynamicWall_Init = false;
    private boolean dynamicWall_Second = false;

    // Wall Point Positions
	private static final double EPSILON = 0.001;

    private static final List<Double> WALL_POSITION_X = new ArrayList<>(Arrays.asList(-17.30, -13.84, -10.38, -6.92, -3.46, 0.00, 3.46, 6.92, 10.38, 13.84, 17.30));
    private ArrayList<Point> wall_pos_list = new ArrayList<Point>();  
    private ArrayList<Point> wall_pos_list_second = new ArrayList<Point>();  
    private HashMap<Integer, Point> wall_pieces = new HashMap<Integer, Point>();
    private HashMap<Integer, Point> wall_pieces_second = new HashMap<Integer, Point>();

	public Player()
	{
		random = new Random(seed);
	}

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
        CalculateWallPositionsMap(pieces);
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		 List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		 //int num_trials = 30;
		 //int i = 0;

         if(!dynamicWall_Init){
            
            InitWall(player_pieces, opponent_pieces, moves);
         }
         
         if(!dynamicWall_Second){
            
            SecondWall(player_pieces, opponent_pieces, moves);
         }
		 


		 return moves;
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

    // TODO
    public List<Pair<Integer, Point>> InitWall(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, List<Pair<Integer, Point>> moves){
        
        Integer id;
        Point destination;
        Pair<Integer, Point> move;

        for (HashMap.Entry<Integer, Point> entry : wall_pieces.entrySet()) {
            id = entry.getKey();
            destination = entry.getValue(); 
           
            List<Point> points = moveCurrentToTarget(id, player_pieces.get(id), destination, player_pieces, opponent_pieces);

            for (Point point: points) {
                move = new Pair(id, point);
                if (check_validity(move, player_pieces, opponent_pieces)) {
                    moves.add(move);
                    player_pieces.put(id, point);
                } else {
                    break;
                }
            }
        }
 
        return moves;
    }

    public List<Pair<Integer, Point>> SecondWall(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, List<Pair<Integer, Point>> moves){
        
        Integer id;
        Point destination;
        Pair<Integer, Point> move;

        for (HashMap.Entry<Integer, Point> entry : wall_pieces_second.entrySet()) {
            id = entry.getKey();
            destination = entry.getValue(); 
           
            List<Point> points = moveCurrentToTarget(id, player_pieces.get(id), destination, player_pieces, opponent_pieces);

            for (Point point: points) {
                move = new Pair(id, point);
                if (check_validity(move, player_pieces, opponent_pieces)) {
                    moves.add(move);
                    player_pieces.put(id, point);
                } else {
                    break;
                }
            }
        }
 
        return moves;
    }



    public void CalculateWallPositionsMap(HashMap<Integer, Point> player_pieces){

        double x1 = this.isplayer1 ? 19.99 : -19.99;
        double x2 = this.isplayer1 ? 22.01 : -22.01;
       
        wall_pos_list = new ArrayList<Point>();
        wall_pos_list_second = new ArrayList<Point>();  

        for(int i = 0; i < WALL_POSITION_X.size(); i++){
            wall_pos_list.add(new Point(x1, WALL_POSITION_X.get(i)));
            wall_pos_list_second.add(new Point(x2, WALL_POSITION_X.get(i)));
        }

        for(Point p : wall_pos_list){
            Integer id = NearestPoint(p, player_pieces);
            wall_pieces.put(id, p);
        }

        for(Point p : wall_pos_list_second){
            Integer id = NearestPoint(p, player_pieces);
            wall_pieces_second.put(id, p);
        }
    }



     private Integer NearestPoint(Point point, HashMap<Integer, Point> player_pieces){
        Integer id = 0;
        Point p;
        double d;
        double min_dist = Double.MAX_VALUE;
        for(int i = 0; i < n; i++){
            if(wall_pieces.containsKey(i) || wall_pieces_second.containsKey(i))
                continue;
            p = player_pieces.get(i);
            d = Math.sqrt(Math.pow(point.x - p.x, 2) + Math.pow(point.y - p.y, 2));
            if(d < min_dist){
                min_dist = d;
                id = i;
            }
        }
        return id;
    }

    

    public List<Point> moveCurrentToTarget(Integer id, Point current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        List<Point> moves = new ArrayList<>();
        double tmcx = target.x-current.x;
        double tmcy = target.y-current.y;
        double d = Math.sqrt(tmcx*tmcx + tmcy*tmcy);
        double theta = Math.atan(tmcy/tmcx);
        if (d < EPSILON) {
            ; // do nothing
        }
        if (d >= diameter_piece - EPSILON && d < diameter_piece + EPSILON) {
            moves.add(target);
        } else if (d > EPSILON && d <= 2*diameter_piece) {
            moves.addAll(moveCurrentToTargetClose(new Pair<Integer, Point>(id, current), target, player_pieces, opponent_pieces));
            if (moves.isEmpty()) {
                Point behind_current = new Point(current.x, current.y);
                behind_current.x += isplayer1 ? diameter_piece : -diameter_piece;
                moves.add(behind_current);
            }
        } else if (d > 2*diameter_piece && d <= 3*diameter_piece) {
            Point new_position = getNewPointFromOldPointAndAngle(current, theta);
            moves.add(new_position);
            moves.addAll(moveCurrentToTargetClose(new Pair<Integer, Point>(id, new_position), target, player_pieces, opponent_pieces));
            if (moves.size() == 1) {
                Point behind_current = new Point(current.x, current.y);
                behind_current.x += isplayer1 ? diameter_piece : -diameter_piece;
                moves.add(behind_current);
            }
        } else if (d > 3*diameter_piece) {
            Point m1 = getNewPointFromOldPointAndAngle(current, theta);
            moves.add(m1);
            Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
            moves.add(m2);
        }
        return moves;
    }

    public List<Point> moveCurrentToTargetClose(Pair<Integer, Point> current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        List<Point> moves = new ArrayList<>();
        Integer current_id = current.getKey();
        Point current_point = current.getValue();
        double tmcx = target.x-current_point.x;
        double tmcy = target.y-current_point.y;
        // We need to solve for a 2-move sequence that gets the current point to the target
        double tmcx2 = tmcx/2;
        double tmcy2 = tmcy/2;
        // tpp2 is (theta + phi)/2
        double tpp2 = Math.atan(tmcy/tmcx);
        // tmp2 is (theta - phi)/2
        double tmp2 = Math.acos(Math.sqrt(tmcx2*tmcx2 + tmcy2*tmcy2)/2);
        double theta = tpp2 + tmp2;
        double phi = tpp2 - tmp2;
        // if you are blocked, take the other angle first
        // if that still doesn't work, move to the point directly behind the current spot
        Point m1 = getNewPointFromOldPointAndAngle(current_point, theta);
        Pair<Integer, Point> next = new Pair(current_id, m1);
        if (check_validity(next, player_pieces, opponent_pieces)) {
            moves.add(m1);
            Point m2 = getNewPointFromOldPointAndAngle(m1, phi);
            moves.add(m2);
        } else {
            m1 = getNewPointFromOldPointAndAngle(current_point, phi);
            if (check_validity(next, player_pieces, opponent_pieces)) {
                moves.add(m1);
                Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
                moves.add(m2);
            } else {
                System.out.println("FAILED TO MOVE TO BLOCKADE POINT");
            }
        }
        return moves;
    }

    public Point getNewPointFromOldPointAndAngle(Point current, double theta) {
        Point new_position = new Point(current);
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        new_position.x += this.isplayer1 ? -delta_x : delta_x;
        new_position.y += this.isplayer1 ? -delta_y : delta_y;
        return new_position;
    }

    
   
}

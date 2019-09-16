package flip.john;
import java.util.*;
import java.util.List;

import javafx.util.Pair;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;
	private boolean dispatch_runner_mode = true;
	private boolean defense_mode = false;  // when true, we play all moves to build wall, else all moves are offense
	private Point goal;  // to be set after wall is made
	private Integer runnerID; // dito as above

    private static final double EPSILON = 0.001;
    private static final double B1  = -17.30;
    private static final double B2  = 17.30;
    private static final double B3  = -13.84;
    private static final double B4  = 13.84;
    private static final double B5  = -10.38;
    private static final double B6  = 10.38;
    private static final double B7  = -6.92;
    private static final double B8  = 6.92;
    private static final double B9  = -3.46;
    private static final double B10 = 3.46;
    private static final double B11 = 0.00;

    private static final List<Double> BLOCKADE_YCOORD = new ArrayList<>(Arrays.asList(B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11));
    private List<Point> blockadeList = new ArrayList<>();
    private Map<Point, Integer> blockadeMap = new HashMap<>();


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
        this.initializeBlockadeList(isplayer1);
        computeBlockadeMap(pieces, isplayer1);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Core getMoves function.  While we still have moves to make, add defense or offense moves depending on global mode
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        if (this.dispatch_runner_mode) {  //get runner in front of barrier line
            if (this.runnerID == null) {
                this.runnerID = getPieceNotInBlockade();  // identify runner
            }
            if (isPieceWayPastDefense(this.runnerID, player_pieces, isplayer1)) {  // runner is far enough out, now build the wall starting next turn!
                this.dispatch_runner_mode = false;
                this.defense_mode = true;
            }
            return getRunnerMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
        }

        else if (this.defense_mode) {  // Play defense.  Stop when barrier is formed.
            if (this.blockadeList.isEmpty()) {  // We filled all the spots in the wall
                this.defense_mode = false;
            }
            if (this.defense_mode) {
                return getDefenseMoves(player_pieces, opponent_pieces, isplayer1);
            }
        }

        else {   // play offense if runner is still not in the endzone
            if (!isPointInEndZone(player_pieces.get(runnerID), isplayer1)) {
                return getRunnerMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
            }
        }

		return null;
	}

	// is a point in the end zone?  Mainly used to stop runners when they are far enough.
	private boolean isPointInEndZone(Point point, boolean isplayer1) {
	    return isplayer1 ? point.x < -25 : point.x > 25;
    }

	// returns random move, code from TA
    private Pair<Integer, Point> getRandomMove(Integer piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        Point curr_position = player_pieces.get(piece_id);
        Point new_position = new Point(curr_position);
        double theta = isPiecePastDefense(piece_id, player_pieces, isplayer1) ? 0 : -Math.PI/2 + (Math.PI * random.nextDouble());
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
        new_position.y += delta_y;
        Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
        if(check_validity(move, player_pieces, opponent_pieces))
            return move;
        else return null;

    }

    // Very useful function for determining a single move based on a decided theta and piece to move.
    private Pair<Integer, Point> getSingleMove(int piece_id, HashMap<Integer, Point> player_pieces, double theta) {
        Point curr_position = player_pieces.get(piece_id);
        Point new_position = new Point(curr_position);
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
        int orientation_factor = isplayer1 ? -1 : 1;
        new_position.y += orientation_factor * delta_y;
        Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
        return move;
    }

    //getRunnerMoves tries to go straight, and if not, call getAround function.
    public List<Pair<Integer, Point>> getRunnerMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
    {
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
        List<Pair<Integer, Point>> nearby_oppo = new ArrayList<Pair<Integer, Point>>();
        int num_trails = 30;
        int i = 0;
        while(moves.size()!=num_moves && i< num_trails){
            Integer piece_id = this.runnerID;
            Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, 0);

            if(move != null && check_validity(move, player_pieces, opponent_pieces)) {
                moves.add(move);
            }

            else {
//                && check_validity(move_around, player_pieces, opponent_pieces)
                //can't move straight we do something when there is only one obstacle ahead
                //check for collision, add opponent pieces into nearyby_oppo
                Pair<Integer, Point> move_around = getAround(piece_id, move, player_pieces, opponent_pieces, isplayer1);
                //-10000 means there is no valid move possible
                if (move_around.getValue().x != -10000){
                    moves.add(move_around);
                }
            }
        }
        i++;
        return moves;
    }

	private Integer getPieceNotInBlockade() {
        for (int i = 0; i < this.n; i++) {
            if (!this.blockadeMap.values().contains(i)) {
                return i;
            }
        }
        return 0;
    }

	private boolean isPiecePastDefense(Integer piece_id, HashMap<Integer, Point> player_pieces, boolean isplayer1) {
	    double curr_x = player_pieces.get(piece_id).x;
	    return isplayer1 ? (curr_x < 19) : (curr_x > 19);
    }

    private boolean isPieceWayPastDefense(Integer piece_id, HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        double curr_x = player_pieces.get(piece_id).x;
        return isplayer1 ? (curr_x < 15) : (curr_x > -15);
    }

	private List<Pair<Integer, Point>> getTwoForwardMoves(int piece_id, HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        List<Pair<Integer, Point>> forwardMoves = new ArrayList<>();
        double curr_x = player_pieces.get(piece_id).x;
        double curr_y = player_pieces.get(piece_id).y;
        int orientation_factor = isplayer1 ? -1 : 1;
        forwardMoves.add(new Pair(piece_id, new Point(curr_x + orientation_factor * this.diameter_piece, curr_y)));
        forwardMoves.add(new Pair(piece_id, new Point(curr_x + orientation_factor * 2 * this.diameter_piece, curr_y)));
        return forwardMoves;
    }


	// CORE DEFENSE MOVES FUNCTION.  GET PAIR OF BLOCKADE MOVES
	private List<Pair<Integer, Point>> getDefenseMoves(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        this.removeOccupiedBlockadePoints(player_pieces);
        List<Point> blockadeImportance = this.computeBlockadeImportance(opponent_pieces, isplayer1);
        return getMovesForBlockade(blockadeImportance, player_pieces, opponent_pieces, isplayer1);
    }

	// CORE OFFENSE MOVE FUNCTION - to be improved by Jingya
	// drive already established global runner to already established goal
	private void addOffenseMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Set<Integer> stuck, List<Pair<Integer, Point>> moves) {
	    boolean terminal_condition1 = isplayer1 && player_pieces.get(this.runnerID).x <= this.goal.x;
		boolean terminal_condition2 = !isplayer1 && player_pieces.get(this.runnerID).x >= this.goal.x;
		if (terminal_condition1 || terminal_condition2) {return;} // we've reached goal
		Point runner_location = player_pieces.get(this.runnerID);
		int orientation_factor = isplayer1 ? -1 : 1;
		double theta = orientation_factor * Math.atan((goal.y - runner_location.y) / (goal.x - runner_location.x));
		double delta_x = diameter_piece * Math.cos(theta);
		double delta_y = diameter_piece * Math.sin(theta);
		Point new_position = new Point(0,0);
		new_position.x = isplayer1 ? runner_location.x - delta_x : runner_location.x + delta_x;
		new_position.y = runner_location.y + delta_y;
		Pair<Integer, Point> move = new Pair<Integer, Point>(this.runnerID, new_position);

		if (check_validity(move, player_pieces, opponent_pieces)) {
            moves.add(move);
        }
	}


	/////////////////////////////////////////////////////////////////
	// SEE ALL HELPER / STATE FEATURE FUNCTIONS BELOW ///////////////
	/////////////////////////////////////////////////////////////////


	// calculate whether a particular piece is in some rectangular region
	private boolean inBounds(Point piece, Point upperLeft, Point lowerRight) {
		return piece.x > upperLeft.x && piece.x < lowerRight.x && piece.y > upperLeft.y && piece.y < lowerRight.y;
	}

	// counts the number of pieces in a rectangular region. Pieces could either be opponent's HashMap or your own.
	private int countInBounds(HashMap<Integer, Point> pieces, Point upperLeft, Point lowerRight) {
		int num_in_bounds = 0;
		for (int i=0; i<this.n; i++) {
			if (inBounds(pieces.get(i), upperLeft, lowerRight)) {num_in_bounds += 1;}
		}
		return num_in_bounds;
	}

	// Can get center of mass in x or y dimension.  Will only consider pieces in some rectangular region specified
	// by upperLeft and lowerRight
	private double getCenterOfMass(HashMap<Integer, Point> pieces, String dim, Point upperLeft, Point lowerRight) {
		double sum = 0;
		int num_in_bounds = 0;
		for (int i=0; i<this.n; i++) {
			Point piece = pieces.get(i);
			if (!inBounds(piece, upperLeft, lowerRight)) {}
			else {
				num_in_bounds += 1;
				double coord = (dim == "x") ? piece.x : piece.y;
				sum += coord;
			}
		}
		return sum / num_in_bounds;
	}

	// find the our own furthest back peice that's not currently unable to move directly forward
	private int getFurthestBack(HashMap<Integer, Point> player_pieces, Set<Integer> stuck, boolean isplayer1) {
		double furthest_back = isplayer1 ? -1000 : 1000.0;
		int piece_to_return = 0;
		for (int j = 0; j < n; j++) {
			double x_location = player_pieces.get(j).x;
			boolean condition1 = x_location < furthest_back && !stuck.contains(j) && !isplayer1;
			boolean condition2 = x_location > furthest_back && !stuck.contains(j) && isplayer1;
			if (condition1 || condition2) {
				piece_to_return = j;
				furthest_back = x_location;
			}
		}
		return piece_to_return;
	}

	private int getFurthestForward(HashMap<Integer, Point> player_pieces, Set<Integer> stuck, boolean isplayer1) {
		double furthest_forward = isplayer1 ? 1000 : -1000;
		int piece_to_return = 0;
		for (int j = 0; j < n; j++) {
			double x_location = player_pieces.get(j).x;
            boolean condition1 = x_location > furthest_forward && !stuck.contains(j) && !isplayer1;
            boolean condition2 = x_location < furthest_forward && !stuck.contains(j) && isplayer1;
			if (condition1 || condition2) {
				piece_to_return = j;
				furthest_forward = x_location;
			}
		}
		return piece_to_return;
	}

	// This function breaks the map into k horizontal slices in front of location x.  It returns an array with
	// the number of opponent pieces in each of those slices.  This will be a useful high level feature.
	private int[] getOpponentDensity(int k, double x, boolean opponent_is_player1, HashMap<Integer, Point> opponentPieces) {
		int[] densities = new int[k];
		double x_end = opponent_is_player1 ? 60 : -60;
		double x_left = opponent_is_player1 ? x : x_end;
		double x_right = opponent_is_player1 ? x_end : x;
		for (int i=0; i<k; i++) {
			double boundWidth = 40.0 / k;
			double upperBound = -20 + boundWidth * i;
			double lowerBound = upperBound + boundWidth;
			densities[i] = countInBounds(opponentPieces, new Point(x_left, upperBound), new Point(x_right, lowerBound));
		}
		return densities;
	}

	// get goal point for runner based on opponent density array.
	// might want to modify depending on other factors that come up
	private Point locateGoal(int[] opponentDensity, boolean opponent_isplayer1) {
		int num_bins = opponentDensity.length;
		int min_idx = findMinIdx(opponentDensity);
		double bin_width = 40.0 / num_bins;
		double y = -10 + bin_width * min_idx;
		double x = opponent_isplayer1 ? 21 : -21;
		return new Point(x,y);
	}

	// get idx of maximum number in array.  What's an inbuilt function for this?
	private int findMinIdx(int[] array) {
		int min_num = 1000;  // or positive infinity
		int min_idx = 0;
		for (int i = 0; i<array.length; i++) {
			if (array[i] < min_num) {
				min_num = array[i];
				min_idx = i;
			}
		}
		return min_idx;
	}

	// Get runner ID based on which candidate is closest to some predefined goal point.
	private int getRunnerID(HashMap<Integer, Point> player_pieces, Point goal) {
		double min_distance = 1000; // or positive infinity
		int runnerID = 0;
		for (int i=0; i<this.n; i++) {
			Point piece = player_pieces.get(i);
			double distance = Math.sqrt(Math.pow(piece.x - goal.x, 2) + Math.pow(piece.y - goal.y, 2));
			if (distance < min_distance) {
				min_distance = distance;
				runnerID = i;
			}
		}
		return runnerID;
	}

	////////////////////////////////////////////////////////
	// Below are Ethan's functions
    ///////////////////////////////////////////////////////

    private List<Pair<Integer, Point>> getMovesForBlockade(List<Point> blockade, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        if (this.n < 11) return new ArrayList<>();

        List<Pair<Integer, Point>> moves = new ArrayList<>();

        // Match pieces to blockade points from top to bottom !!! (as an approximation)
        // Compute the minimum weight bipartite matching for a better solution
        for (Point target : blockade) {
            // Find the nearest piece behind the blockade and move it towards the blockade point
            Integer id = this.blockadeMap.get(target);
            Point current = player_pieces.get(id);
            System.out.println("Start: " + " x: " + current.x + " y: " + current.y);
            System.out.println("Target: " + " x: " + target.x + " y: " + target.y);
            List<Point> points = moveCurrentToTarget(id, current, target, player_pieces, opponent_pieces);
            for (Point point: points) {
                Pair<Integer, Point> move = new Pair(id, point);
                System.out.println("Update: " + " x: " + point.x + " y: " + point.y);
                if (check_validity(move, player_pieces, opponent_pieces)) {
                    System.out.println("Valid!");
                    moves.add(move);
                    player_pieces.put(id, point);
                } else {
                    System.out.println("Invalid!");
                    break;
                }
            }
            if (moves.size() >= 2) break;
        }
        return moves;
    }

    private void initializeBlockadeList(boolean isplayer1) {
        for (double blockade_ycoord: BLOCKADE_YCOORD) {
            Point blockade = new Point(isplayer1 ? 19.99 : -19.99, blockade_ycoord);
            this.blockadeList.add(blockade);
        }
    }

    // Construct the blockade points and return a priority list of the blockade points we want to fill
    private List<Point> computeBlockadeImportance(Map<Integer, Point> opponent_pieces, boolean isplayer1) {
        // Can code this to assign importance to which part of the blockade is computed first
        // For now, this just constructs the blockade as the default list ordering
        return this.blockadeList;
    }

    private void removeOccupiedBlockadePoints(Map<Integer, Point> player_pieces) {
        Set<Point> blockade_filled = new HashSet<>();
        for (Point blockade: this.blockadeList) {
            for (Point piece: player_pieces.values()) {
                if (getDistance(blockade, piece) < EPSILON) {
                    blockade_filled.add(blockade);
                }
            }
        }
        for (Point filled: blockade_filled) {
            this.blockadeList.remove(filled);
        }
    }

    private void computeBlockadeMap(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        this.blockadeMap = new HashMap<>();
        // This is just to create a deep copy of available pieces, to prevent re-using the same piece in the blockade
        Set<Integer> unused_pieces = player_pieces.keySet().stream().collect(Collectors.toSet());
        for (Point target: blockadeList) {
            double shortest_distance = -1;
            Integer best_piece = -1;
            for (Integer unused_piece: unused_pieces) {
                Point current = player_pieces.get(unused_piece);
                double distance = getDistance(current, target, 2);
                if (shortest_distance == -1 || distance < shortest_distance) {
                    shortest_distance = distance;
                    best_piece = unused_piece;
                }
            }
            this.blockadeMap.put(target, best_piece);
            unused_pieces.remove(best_piece);
        }
        for (Map.Entry<Point, Integer> entry: this.blockadeMap.entrySet()) {
            System.out.println("Point ID: " + entry.getValue() + " Current: " + player_pieces.get(entry.getValue()).x + " " + player_pieces.get(entry.getValue()).y + " Target: " + entry.getKey().x + " " + entry.getKey().y);
        }
    }

    // Given a current point and a target point, move the current point towards the current point
    // If this cannot be done in a single move, move the current point directly towards the target
    // The calling method will have to store the second move if it can only make one more move

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

    private double getDistance(Point p1, Point p2) {
        return getDistance(p1, p2, 1);
    }

    private double getDistance(Point p1, Point p2, int y_scale) {
        double x_diff = p2.x-p1.x;
        double y_diff = (p2.y-p1.y)/y_scale;
        return Math.sqrt(x_diff*x_diff + y_diff*y_diff);
    }

    private HashMap<Integer, Point> deepCopy(HashMap<Integer, Point> map) {
        HashMap<Integer, Point> copy = new HashMap<>();
        for (Map.Entry<Integer, Point> entry: map.entrySet()) {
            copy.put(new Integer(entry.getKey()), new Point(entry.getValue()));
        }
        return copy;
    }

    private double evaluateSquare(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        double value = 0.0;
        for (Map.Entry<Integer, Point> entry: player_pieces.entrySet()) {
            value += (60+entry.getValue().x) * (60+entry.getValue().x);
        }
        return value;
    }

    private double evaluateV(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Point target) {
        double value = 0.0;
        for (Map.Entry<Integer, Point> entry: player_pieces.entrySet()) {
            double x_diffsq = (entry.getValue().x - target.x)*(entry.getValue().x - target.x);
            double y_diffsq = (entry.getValue().y - target.y)*(entry.getValue().y - target.y);
            value += Math.sqrt(x_diffsq + y_diffsq);
        }
        return value;
    }

    // Utility Methods

    // Given your piece and all other pieces, return the farthest forward point (can get stuck locally)
    public Point getBestNonConflictingLocalMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece) {
        double theta = -Math.PI/2;
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        return new Point(0.0, 0.0);
    }

    // Given your piece and all other pieces, return the next move on the shortest path leading to the end (should not get stuck locally)
    public Point getBestNonConflictingGlobalMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece) {
        return new Point(0.0, 0.0);
    }


    //this function takes in the runner piece current position, obstacle position, and calculate theta
    //if usualway is true, we are minimizing the off-course angle. i.e., if opponent on left, flip to right and vice versa
    //if usualway is false, then flip to the other side
    public double getAroundTheta(Integer runner_id, Point curr_position, Point obstacle, boolean usualway){
        double m1 = Math.abs(curr_position.y - obstacle.y);
        double m2 = Math.abs(curr_position.x - obstacle.x);
        double m3 = Math.sqrt((Math.pow(m1, 2) + Math.pow(m2, 2)));
        double cos_a2 = (Math.pow(m3, 2) + Math.pow(m2, 2) - Math.pow(m1, 2)) / (2 * m2 * m3);
        double cos_a1 = Math.pow(m3, 2) / (diameter_piece * diameter_piece * m3);
        //a1 should be greater than a2
        double theta1;
        //opponent on right
        if (obstacle.y > curr_position.y) {
            theta1 = Math.acos(cos_a2) - Math.acos(cos_a1);
            if (!usualway){
                theta1 = Math.acos(cos_a2) + Math.acos(cos_a1);
            }

        } else {
            theta1 = Math.acos(cos_a1) - Math.acos(cos_a2);
            if (!usualway){
                theta1 = -Math.acos(cos_a2) - Math.acos(cos_a1);
            }
        }

        return theta1;

    }


    //////////////////////////////////////////////
    // Jingya's FUNCTIONS ////////////////////////
    ////////////////////////////////


    public Pair<Integer, Point> getAround(Integer runner_id, Pair<Integer, Point> invalid_move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1){

        int i = 0;
        Point curr_position = player_pieces.get(runner_id);
        List<Pair<Integer, Point>> nearby_oppo = new ArrayList<Pair<Integer, Point>>();
        Point new_position = new Point(curr_position);
        //can't move straight we do something when there is only one obstacle ahead
        //check for collision, add opponent pieces into nearyby_oppo

        for (HashMap.Entry<Integer, Point> entry : opponent_pieces.entrySet()) {
            //this clause takes from the check_collision code written by Professor in Simulator.java
            if (Board.getdist(invalid_move.getValue(), entry.getValue()) + 1E-7 < diameter_piece) {
                // Double dist = getdist(move.getValue(), entry.getValue()) + eps;
                //System.out.println("First time collision detected between pieces " + invalid_move.getKey().toString() + " and " + entry.getKey().toString() + "distance was " + dist.toString());
                Pair<Integer, Point> collide = new Pair<Integer, Point>(entry.getKey(), entry.getValue());
                nearby_oppo.add(collide);

            }
        }
///Consider our own pieces also
        for (HashMap.Entry<Integer, Point> entry : player_pieces.entrySet()){

            if (Board.getdist(invalid_move.getValue(), entry.getValue()) + 1E-7 < diameter_piece && entry.getKey()!= runner_id) {
                // Double dist = getdist(move.getValue(), entry.getValue()) + eps;
                //System.out.println("First time collision detected between pieces " + invalid_move.getKey().toString() + " and " + entry.getKey().toString() + "distance was " + dist.toString());
                Pair<Integer, Point> collide = new Pair<Integer, Point>(entry.getKey(), entry.getValue());
                nearby_oppo.add(collide);

            }

        }


        if (nearby_oppo.size() > 0) {
            //System.out.println(nearby_oppo);
            Point obstacle = new Point(nearby_oppo.get(0).getValue());
            //figure out the nearest oppo
            for (i = 0; i < nearby_oppo.size(); i++) {
                double min_distance = 100000;
                double distance = Math.sqrt(Math.pow(curr_position.x - nearby_oppo.get(i).getValue().x, 2)
                        + Math.pow(curr_position.y - nearby_oppo.get(i).getValue().y, 2));
                if (distance < min_distance) {
                    min_distance = distance;
                    obstacle = nearby_oppo.get(i).getValue();
                }
            }


            //get the nearest point in nearby_oppo and flip to avoid that
            //TODO: sometimes invalid move passed in is NaN, needs to figure out way
            double theta1 = getAroundTheta(runner_id, curr_position, obstacle, true);
            //System.out.println("theta 1 is " + theta1);
            double delta_x1 = diameter_piece * Math.cos(theta1);
            double delta_y1 = diameter_piece * Math.sin(theta1);
            new_position.x = isplayer1 ? curr_position.x - delta_x1 : curr_position.x + delta_x1;
            new_position.y = curr_position.y + delta_y1;
            Pair<Integer, Point> move2 = new Pair<Integer, Point>(runner_id, new_position);
            //System.out.println(move2);
            if (check_validity(move2, player_pieces, opponent_pieces)) {
                System.out.println("move2 is valid");
                return move2;
            } else {
                theta1 = getAroundTheta(runner_id, curr_position, obstacle, false);
                delta_x1 = diameter_piece * Math.cos(theta1);
                delta_y1 = diameter_piece * Math.sin(theta1);
                new_position.x = isplayer1 ? curr_position.x - delta_x1 : curr_position.x + delta_x1;
                new_position.y = curr_position.y + delta_y1;
                Pair<Integer, Point> move3 = new Pair<Integer, Point>(runner_id, new_position);
                if (check_validity(move3, player_pieces, opponent_pieces)) {
                    System.out.println("move3 is valid");
                    return move3;
                } else {

                    //want to return null move here. I don't know to return null directly.
                    Point null_pos = new Point(-10000, -10000);
                    Pair<Integer, Point> move_null = new Pair<Integer, Point>(runner_id, null_pos);
                    return move_null;

                    //version 2: we can give a final shot by go to a randomnized angle, but I don't use it
                    //for now.

                }

            }
        }
        else{
            //there is no obstacle in the way, return going straight
            double theta = 0;
            double delta_x = diameter_piece * Math.cos(theta);
            double delta_y = diameter_piece * Math.sin(theta);

            new_position.x = isplayer1 ? curr_position.x - delta_x : curr_position.x + delta_x;
            new_position.y += curr_position.y+ delta_y;
            Pair<Integer, Point> move = new Pair<Integer, Point>(runner_id, new_position);
            return move;
        }

    }



    /////////////////////////////////////
	// PROGRAMED BY TA //////////////////
	////////////////////////////////////

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

//////////////
/////////////
/////////////
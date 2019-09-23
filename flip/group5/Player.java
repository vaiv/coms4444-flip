package flip.group5;
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
    private boolean make_run_mode = false;
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
    private List<Pair<Integer, Point>> blockadeList = new ArrayList<>();
    private Map<Integer, Integer> blockadeMap = new HashMap<>();
	public Player()
	{
		random = new Random(seed);
	}

	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
        this.initializeBlockadeList(isplayer1);
        this.computeBlockadeMapMWBM(pieces);
	}

	/**
	* Core getMoves function.  While we still have moves to make, add defense or offense moves depending on global mode
	*/
	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        if (this.n <= 11) { // just play forward if we can't play wall strategy
            return getMovesSmallN(num_moves, player_pieces, opponent_pieces, isplayer1);
        }

        // n > 11, play wall strategy, which has 3 modes or game states
	    if (this.dispatch_runner_mode) {  //get runner in front of barrier line
            identifyRunner(player_pieces, isplayer1);
            refreshGameState(player_pieces, isplayer1);
            return getRunnerMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
        }

        else if (this.defense_mode) {  // Play defense.  Stop when barrier is formed.
            refreshGameState(player_pieces, isplayer1);
            if (this.defense_mode) {
                return getDefenseMoves(player_pieces, opponent_pieces, isplayer1);
            }
        }

        else {   // play offense if runner is still not in the endzone
            refreshGameState(player_pieces, isplayer1);
            if (this.make_run_mode) {
                return getRunnerMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
            }
        }
		return null;  // our wall should be up and runner at goal
	}

	private void refreshGameState(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
	    if (isRunnerPastDefense(this.runnerID, player_pieces, isplayer1) && this.dispatch_runner_mode) {  // runner is far enough out, now build the wall starting next turn!
            this.dispatch_runner_mode = false;
            this.defense_mode = true;
        }
	    if (this.defense_mode && isBlockadeComplete(player_pieces)) {
	        this.defense_mode = false;
	        this.make_run_mode = true;}
	    if (isPointInEndZone(player_pieces.get(runnerID), isplayer1)) {this.make_run_mode = false;}
    }

	private void identifyRunner(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        if (this.runnerID == null) {
            this.runnerID = getClosestRunnerNotInBlockade(player_pieces, isplayer1);  // identify runner
        }
    }

    private boolean isBlockadeComplete(HashMap<Integer, Point> player_pieces) {
        boolean blockadeComplete = true;
        int block = 0;
        while (blockadeComplete && block < blockadeList.size()) {
            Pair<Integer, Point> blockade = blockadeList.get(block);
            boolean blockComplete = false;
            for (Map.Entry<Integer, Point> piece: player_pieces.entrySet()) {
                if (getDistance(blockade.getValue(), piece.getValue()) < EPSILON) {
                    blockComplete = true;
                    break;
                }
            }
            blockadeComplete &= blockComplete;
            block++;
        }
        return blockadeComplete;
    }

    // Function to handle game with small n.  Let's look into why this seems poor.
	private List<Pair<Integer, Point>> getMovesSmallN(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
	    Set<Integer> stuck = new HashSet<Integer>();
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
        int i = 0;
        int trials = 30;

        while (moves.size() != num_moves && i < trials) {
            int proposed_peice = getFurthestForward(player_pieces, stuck, isplayer1);
            Pair<Integer, Point> move = getSingleMove(proposed_peice, player_pieces, 0, isplayer1);
            double x = player_pieces.get(proposed_peice).x;
            boolean notFarEnough = isplayer1 ? (x > -23) : (x < 23);
            if (check_validity(move, player_pieces, opponent_pieces) && notFarEnough) {moves.add(move);}  // all moves valid
            else {
                stuck.add(proposed_peice);
            }
        }
        i++;
        if (moves.size() < 2) {   // Can watch print to see if we return less than two moves
            System.out.print("AHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
        }
        return moves;
    }

	// is a point in the end zone?  Mainly used to stop runners when they are far enough.
	private boolean isPointInEndZone(Point point, boolean isplayer1) {
	    return isplayer1 ? point.x < -22 : point.x > 22;
    }

	// returns random move, code from TA
    private Pair<Integer, Point> getRandomMove(Integer piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        Point curr_position = player_pieces.get(piece_id);
        Point new_position = new Point(curr_position);
        double theta = -Math.PI/2 + (Math.PI * random.nextDouble());
        Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, theta, isplayer1);
        return check_validity(move, player_pieces, opponent_pieces) ? move : null;
    }

    // Very useful function for determining a single move based on a decided theta and piece to move.
    private Pair<Integer, Point> getSingleMove(int piece_id, HashMap<Integer, Point> player_pieces, double theta, boolean isplayer1) {
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

    private Pair<Integer, Point> getSingleValidMove(int piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, double theta, boolean isplayer1) {
        Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, theta, isplayer1);
        if (check_validity(move, player_pieces, opponent_pieces)) {
            return move;
        }
        return null;
    }

    //getRunnerMoves tries to go straight, and if not, call getAround function.
    public List<Pair<Integer, Point>> getRunnerMoves(Integer num_moves, HashMap<Integer, Point> player_pieces,
                                                     HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
    {
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
        List<Pair<Integer, Point>> nearby_oppo = new ArrayList<Pair<Integer, Point>>();
        int num_trails = 30;
        int i = 0;
        //we can default this to be 0 for now
        double preferred_angle = 0;

        while(moves.size()!=num_moves && i< num_trails){
            Integer piece_id = this.runnerID;
            Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, preferred_angle, isplayer1);
            if(check_validity(move, player_pieces, opponent_pieces)) {
                moves.add(move);
            }

            else {
                //can't move to the preferred angle we try to getAround
                Pair<Integer, Point> move_around = getAroundWithPreferredAngle(piece_id, move, player_pieces, opponent_pieces, isplayer1, preferred_angle);
                //-10000 means there is no valid move possible
                if (move_around.getValue().x != -10000){
                    System.out.println("Runner Position: " + player_pieces.get(piece_id));
                    System.out.println("Runner Move: " + move_around.getValue());
                    moves.add(move_around);
                }
            }
            i++;
        }
        return moves;
    }

	private Integer getClosestRunnerNotInBlockade(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        int best_runner = 0;
        double best_x = isplayer1 ? 1000 : -1000;
	    for (int i = 0; i < this.n; i++) {
	        double x = player_pieces.get(i).x;
	        boolean condition1 = !this.blockadeMap.values().contains(i);
	        boolean condition2 = isplayer1 ? (x < best_x) : (x > best_x);
            if (condition1 && condition2) {
                best_runner = i;
                best_x = x;
            }
        }
        return best_runner;
    }

	private boolean isRunnerPastDefense(Integer piece_id, HashMap<Integer, Point> player_pieces, boolean isplayer1) {
	    double curr_x = player_pieces.get(piece_id).x;
	    return isplayer1 ? (curr_x < 20 - this.diameter_piece) : (curr_x > -20 + this.diameter_piece);
    }


	// CORE DEFENSE MOVES FUNCTION.  GET PAIR OF BLOCKADE MOVES
	private List<Pair<Integer, Point>> getDefenseMoves(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        this.blockadeList = this.computeBlockadeImportance(opponent_pieces, isplayer1);
        return getMovesForBlockade(player_pieces, opponent_pieces, isplayer1);
    }

	// SEE ALL HELPER / STATE FEATURE FUNCTIONS BELOW

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

	// Below are Ethan's functions

    private List<Pair<Integer, Point>> getMovesForBlockade(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        if (this.n < 11) return new ArrayList<>();

        List<Pair<Integer, Point>> moves = new ArrayList<>();

        for (Pair<Integer, Point> blockade: this.blockadeList) {
            Integer blockade_id = blockade.getKey();
            Point blockade_point = blockade.getValue();
            Integer piece_id = this.blockadeMap.get(blockade_id);
            Point piece_point = player_pieces.get(piece_id);

            System.out.println("Start: " + " x: " + piece_point.x + " y: " + piece_point.y);
            System.out.println("Target: " + " x: " + blockade_point.x + " y: " + blockade_point.y);
            List<Point> points = moveCurrentToTarget(piece_id, piece_point, blockade_point, player_pieces, opponent_pieces);
            for (Point point: points) {
                Pair<Integer, Point> move = new Pair(piece_id, point);
                System.out.println("Update: " + " x: " + point.x + " y: " + point.y);
                if (check_validity(move, player_pieces, opponent_pieces)) {
                    System.out.println("Valid!");
                    moves.add(move);
                    player_pieces.put(piece_id, point);
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
        for (int i = 0; i < BLOCKADE_YCOORD.size(); i++) {
            Point blockade = new Point(isplayer1 ? 19.99 : -19.99, BLOCKADE_YCOORD.get(i));
            this.blockadeList.add(new Pair(i, blockade));
        }
    }

    private void computeBlockadeMapMWBM(HashMap<Integer, Point> player_pieces) {
        Integer offset = 1000; // this is a hack to make MWBM realize blockade points and piece points are distinct
        List<Pair<Integer, Point>> player_pieces_list = player_pieces.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(Collectors.toList());
        Collections.sort(player_pieces_list, (p1, p2) -> new Double(p1.getValue().x).compareTo(new Double(p2.getValue().x)));
        if (!this.isplayer1) Collections.reverse(player_pieces_list);
        Map<String, Map<String, Double>> mwbmMap = new HashMap<>();
        for (int i = offset; i < offset + this.blockadeList.size(); i++) {
            Map<String, Double> map = new HashMap<>();
            Pair<Integer, Point> blockade = this.blockadeList.get(i-offset);
            for (int j = 1; j < this.blockadeList.size() + 1; j++) {
                Pair<Integer, Point> piece = player_pieces_list.get(j);
                Double distance = getDistance(blockade.getValue(), piece.getValue());
                map.put("" + piece.getKey(), distance);
            }
            mwbmMap.put("" + i, map);
        }
        MWBM mwbm = new MWBM();
        try {
            MWBM.Result result = mwbm.apply(mwbmMap);
            Map<String, String> assignment = result.assignment;
            this.blockadeMap = new HashMap<>();
            for (Map.Entry<String, String> entry: assignment.entrySet()) {
                this.blockadeMap.put(Integer.parseInt(entry.getKey()) - offset, Integer.parseInt(entry.getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Construct the blockade points and return a priority list of the blockade points we want to fill
    private List<Pair<Integer, Point>> computeBlockadeImportance(Map<Integer, Point> opponent_pieces, boolean isplayer1) {
        // Can code this to assign importance to which part of the blockade is computed first
        return this.blockadeList;
    }

    // Given a current point and a target point, move the current point towards the target point
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
                Pair<Integer, Point> p = getAroundWithPreferredAngle(current_id, new Pair<Integer, Point>(current_id, m1), player_pieces, opponent_pieces, this.isplayer1, theta);
                if (check_validity(next, player_pieces, opponent_pieces)) {
                    moves.add(p.getValue());
                    System.out.println("only adding one move theta");
                } else {
                    p = getAroundWithPreferredAngle(current_id, new Pair<Integer, Point>(current_id, m1), player_pieces, opponent_pieces, this.isplayer1, phi);
                    moves.add(p.getValue());
                    System.out.println("only adding one move phi");
                }
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
        public double getAroundTheta(Integer runner_id, Point curr_position, Point obstacle, boolean usualway, boolean isplayer1, double preferred_angle){
        double m1 = Math.abs(curr_position.y - obstacle.y);
        double m2 = Math.abs(curr_position.x - obstacle.x);
        double m3 = Math.sqrt(m1*m1 + m2*m2);
        double cos_a2 = (m3*m3 + m2*m2 - m1*m1) / (2 * m2 * m3);
        double cos_a1 = m3*m3 / (diameter_piece * diameter_piece * m3);
        //a1 should be greater than a2
        double theta1;
        //opponent on right
        if (isplayer1 ? obstacle.y < curr_position.y : obstacle.y > curr_position.y) {
            //move to the left theta1 negative
            theta1 = Math.acos(cos_a2) - Math.acos(cos_a1);
            if (!usualway){
                //move to the right theta1 positive
                theta1 = Math.acos(cos_a2) + Math.acos(cos_a1);
            }
            //opponent on left
        } else {
            theta1 = Math.acos(cos_a1) - Math.acos(cos_a2);
            if (!usualway){
                theta1 = -Math.acos(cos_a2) - Math.acos(cos_a1);
            }
        }

        return theta1 + preferred_angle;

    }


    // Jingya's FUNCTIONS

    //This is our version of check collision, it does basically the same thing as check_collision
    //It detects collisions and put them in a list
    public List<Pair<Integer,Point>> ourCheckCollision(HashMap<Integer, Point> m, Pair<Integer, Point> move){
        List<Pair<Integer, Point>> nearby_oppo = new ArrayList<Pair<Integer, Point>>();
        for (HashMap.Entry<Integer, Point> entry : m.entrySet()) {
            if (Board.getdist(move.getValue(), entry.getValue()) + 1E-7 < diameter_piece) {

                // Double dist = getdist(move.getValue(), entry.getValue()) + eps;
                //System.out.println("First time collision detected between pieces " + invalid_move.getKey().toString() + " and " + entry.getKey().toString() + "distance was " + dist.toString());
                Pair<Integer, Point> collide = new Pair<Integer, Point>(entry.getKey(), entry.getValue());
                nearby_oppo.add(collide);

            }
        }
        return nearby_oppo;
    }

    public Pair<Integer, Point> getAroundWithPreferredAngle(Integer runner_id, Pair<Integer, Point> invalid_move, HashMap<Integer, Point> player_pieces,
                                                            HashMap<Integer, Point> opponent_pieces, boolean isplayer1, double preferred_angle) {
        int i = 0;
	    Point curr_position = player_pieces.get(runner_id);
        Point new_position = new Point(curr_position);
        //can't move straight we do something when there is only one obstacle ahead
        //check for collision, add opponent pieces into nearyby_oppo
        List<Pair<Integer, Point>> nearby_oppo = ourCheckCollision(opponent_pieces,invalid_move);
        List<Pair<Integer, Point>> nearby_self = ourCheckCollision(player_pieces, invalid_move);
        //concatenate two lists
        nearby_oppo.addAll(nearby_self);

        if (nearby_oppo.size() == 0) {return getSingleMove(runner_id, player_pieces, preferred_angle, isplayer1);}  // just move freely

        // else we need to find tangent move
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
        double theta1 = getAroundTheta(runner_id, curr_position, obstacle, true, isplayer1, preferred_angle);
        Pair<Integer, Point> move2 = getSingleValidMove(runner_id, player_pieces, opponent_pieces, theta1, isplayer1);
        if (move2 != null) {return move2;}

        double theta2 = getAroundTheta(runner_id, curr_position, obstacle, false, isplayer1, preferred_angle);
        Pair<Integer, Point> move3 = getSingleValidMove(runner_id, player_pieces, opponent_pieces, theta2, isplayer1);
        if (move3 != null) {return move3;}
        else {return getNullMove(runner_id);}  //want to return null move here. I don't know to return null directly.
        //version 2: we can give a final shot by go to a randomnized angle, but I don't use it for now
    }


    private Pair<Integer, Point> getNullMove(int piece_id) {
        Point null_pos = new Point(-10000, -10000);
        Pair<Integer, Point> move_null = new Pair<Integer, Point>(piece_id, null_pos);
        return move_null;
    }

	// PROGRAMED BY TA
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
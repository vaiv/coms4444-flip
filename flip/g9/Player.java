package flip.g9;
import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import javafx.util.Pair;
import java.util.ArrayList;

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
    private int nPieceForWall = 11;
    private Wall wall;
	public Player()
	{
		random = new Random(seed);
	}

	private double player1StoppingPoint = -15;
	private double player2StoppingPoint = 15;

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;

        this.wall = new Wall(pieces, 11, isplayer1 ? 20.0 : -20.0);
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
        // Too few pieces, don't wall
        if (player_pieces.size() < this.nPieceForWall)
            return playGreedy(num_moves, player_pieces, opponent_pieces, isplayer1);

        // TODO: deal with case where buildWall only returns 1 move
        if (!this.wall.isBuilt())
           return buildWall(num_moves, player_pieces, opponent_pieces, isplayer1);

        ArrayList<Integer> wallPieces = this.wall.getWallPieceIds();
        ArrayList<Integer> backPieces = new ArrayList<>();
        ArrayList<Integer> runnerPieces = new ArrayList<>();

        List<Pair<Integer, Point>> output = new ArrayList<Pair<Integer, Point>> ();
        for (int i = 0; i < n; i++) {
            if (!wallPieces.contains(i)) {
                if (isplayer1) {
                    if (player_pieces.get(i).x < this.wall.xPos) {
                        runnerPieces.add(i);
                    } else {
                        backPieces.add(i);
                    }
                } else {
                    if (player_pieces.get(i).x > this.wall.xPos) {
                        runnerPieces.add(i);
                    } else {
                        backPieces.add(i);
                    }
                }
            }
        }

        Pair<Integer, Point> move;
        for (Integer i : runnerPieces) {
            if ((isplayer1 && player_pieces.get(i).x > player1StoppingPoint) || (!isplayer1 && player_pieces.get(i).x < player2StoppingPoint)) {
                move = new Pair(i, greedyMove(i, player_pieces, opponent_pieces, isplayer1));
                if (move != null) {
                    player_pieces.put(i, move.getValue());
                    output.add(move);
                    if (output.size() == num_moves) {
                        return output;
                    }
                }
            }
        }

        int minPiece = -1;
        double minDist = Double.POSITIVE_INFINITY;
        int closestWallPiece = -1;
        if (backPieces.size() > 0) {

            while(output.size() < num_moves) {
                for (Integer i : backPieces) {
                    for (Integer j : wallPieces) {
                        double dist = getDist(player_pieces.get(i), player_pieces.get(j));
                        if (dist < minDist) {
                            minPiece = i;
                            closestWallPiece = j;
                            minDist = dist;
                        }
                    }
                }
                if (minPiece == -1 || closestWallPiece == -1) {
                    return null;
                }
                if (minDist < 2.1) {
                    //Pieces are tangent
                    System.out.println("Min");
                    if(num_moves - output.size() < 2) {
                        System.out.println("AIJOSIEJ0");
                        return null;
                    }
                    Point oldPos = player_pieces.get(closestWallPiece);
                    move = greedyMove(closestWallPiece, player_pieces, opponent_pieces, isplayer1);

                    output.add(move);

                    move = new Pair(minPiece, getMostDirectMove(oldPos, player_pieces.get(minPiece)));
                    output.add(move);
                    wall.changeBrick(closestWallPiece, move);
                    backPieces.remove(closestWallPiece);
                    wallPieces.add(closestWallPiece);
                    runnerPieces.add(minPiece);
                    wallPieces.remove(minPiece);
                }
                else {
                    output.add(new Pair(minPiece, getIdealTangentMove(closestWallPiece, minPiece, player_pieces, opponent_pieces)));
                }
            }
            return output;
        }

        return null;
        //return playGreedy(num_moves, player_pieces, opponent_pieces, isplayer1);
	}

    private Pair<Integer, Point> greedyMove(Integer movingPiece, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        Point curr_position = player_pieces.get(movingPiece);
        Point new_position = new Point(player_pieces.get(movingPiece));
        Pair<Integer, Point> move;
        double theta = 0;
        while (theta < Math.PI) {
            new_position.x = curr_position.x;
            new_position.y = curr_position.y;

            //TODO: could create a table w/ these values to make runtime faster
            double delta_x1 = diameter_piece * Math.cos(theta);
            double delta_y1 = diameter_piece * Math.sin(theta);

            new_position.x = isplayer1 ? new_position.x - delta_x1 : new_position.x + delta_x1;
            new_position.y += delta_y1;
            move = new Pair<Integer, Point>(movingPiece, new_position);

            if(check_validity(move,	player_pieces, opponent_pieces)) {
                return new Pair(movingPiece, new_position);
            }

            //check same move reflected over x axis
            new_position.x = curr_position.x;
            new_position.y = curr_position.y;
            double delta_x2 = diameter_piece * Math.cos(-1 * theta);
            double delta_y2 = diameter_piece * Math.sin(-1 * theta);
            new_position.x = isplayer1 ? new_position.x - delta_x2 : new_position.x + delta_x2;
            new_position.y += delta_y2;
            move = new Pair<Integer, Point>(movingPiece, new_position);
            if(check_validity(move,	player_pieces, opponent_pieces)) {
                return new Pair(movingPiece, new_position);
            }
            theta += .1;
        }
        return null;
    }

    private Point getIdealTangentMove(Integer goalPiece, Integer movingPiece, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        double dist = Math.sqrt(Math.pow(playerPieces.get(goalPiece).x - playerPieces.get(movingPiece).x, 2) +
                Math.pow(playerPieces.get(goalPiece).y - playerPieces.get(movingPiece).y, 2));
        Point curr_position = playerPieces.get(movingPiece);
        Point new_position = new Point(playerPieces.get(movingPiece));
        Pair<Integer, Point> move;
        Point mostDirectMove = getMostDirectMove(playerPieces.get(goalPiece), playerPieces.get(movingPiece));
        if (dist % 2 < .1 && check_validity(new Pair<Integer, Point>(movingPiece,mostDirectMove), playerPieces, opponentPieces)) {
            return mostDirectMove;
        }
        //piece misaligned, move to align properly
        double theta = 0;
        while (theta < Math.PI) {
            new_position.x = curr_position.x;
            new_position.y = curr_position.y;

            //TODO: could create a table w/ these values to make runtime faster
            double delta_x1 = diameter_piece * Math.cos(theta);
            double delta_y1 = diameter_piece * Math.sin(theta);

            new_position.x = isplayer1 ? new_position.x - delta_x1 : new_position.x + delta_x1;
            new_position.y += delta_y1;
            move = new Pair<Integer, Point>(movingPiece, new_position);
            dist = getDist(new_position, playerPieces.get(goalPiece));
            if(dist % 2 < .1) {
                if(check_validity(move,	playerPieces, opponentPieces)) {
                    return new_position;
                }
            }

            //check same move reflected over x axis
            new_position.x = curr_position.x;
            new_position.y = curr_position.y;
            double delta_x2 = diameter_piece * Math.cos(-1 * theta);
            double delta_y2 = diameter_piece * Math.sin(-1 * theta);
            new_position.x = isplayer1 ? new_position.x - delta_x2 : new_position.x + delta_x2;
            new_position.y += delta_y2;
            move = new Pair<Integer, Point>(movingPiece, new_position);
            dist = getDist(new_position, playerPieces.get(goalPiece));
            if(dist % 2 < .1) {
                if(check_validity(move,	playerPieces, opponentPieces)) {
                    return new_position;
                }
            }
            theta += .1;
        }
        return null;
    }

    private Point getMostDirectMove(Point goalPoint, Point currentPoint) {
        double theta = Math.atan((goalPoint.x - currentPoint.x)	/ (goalPoint.y - currentPoint.y));
        Point move = new Point(currentPoint);
        move.x = currentPoint.x + 2*diameter_piece * Math.cos(theta);
        move.y = currentPoint.y + 2 * diameter_piece * Math.sin(theta);
        //whoooo knows if this works
        return move;
    }

    private double getDist(Point piece1, Point piece2) {
        double xdist = piece1.x - piece2.x;
        double ydist = piece1.y - piece2.y;
        return Math.sqrt(Math.pow(xdist, 2) + Math.pow(ydist, 2));
    }

    private List<Pair<Integer, Point>> buildWall(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
    {
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        Pair<Integer, Point> move;

        for (int i = 0; i < num_moves; i++) {
            move = this.wall.buildAndGetMove(player_pieces, opponent_pieces);
            moves.add(move);
            player_pieces.put(move.getKey(), move.getValue());
        }

        return moves;
    }

    private List<Pair<Integer, Point>> playGreedy(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer)
    {
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        double theta = 0.0;
        while (moves.size() != num_moves)
        {
            // Sort pieces according to shortest distance from goal
            List<Pair<Integer, Point>> sorted_pieces = sortPieces(player_pieces, new GoalDistanceComparator());

            while ((moves.size() != num_moves) && (sorted_pieces.size() != 0))
            {
                double delta_x = diameter_piece * Math.cos(theta);
                double delta_y = diameter_piece * Math.sin(theta);

                double pos = sorted_pieces.get(0).getValue().x;

                // No pieces can move with this theta, let's break and try a greater theta
                if (new GoalDistanceComparator().getGoalDistance(sorted_pieces.get(0)) == Double.POSITIVE_INFINITY)
                   break;

                Integer piece_id = sorted_pieces.get(0).getKey();

                Point curr_pos = player_pieces.get(piece_id);

                Point new_pos = new Point(curr_pos);
                new_pos.x += isplayer1 ? -delta_x : delta_x;
                new_pos.y += delta_y;

                Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_pos);

                if (check_validity(move, player_pieces, opponent_pieces)) {
                   moves.add(move);
                   player_pieces.put(move.getKey(), move.getValue());
                   theta = 0.0;
                   break;
                 }

                // Moving up didn't work, we'll try moving down
                new_pos.y -= 2 * delta_y;
                move = new Pair<Integer, Point>(piece_id, new_pos);
                if (check_validity(move, player_pieces, opponent_pieces)) {
                   moves.add(move);
                   player_pieces.put(move.getKey(), move.getValue());
                   theta = 0.0;
                   break;
               }

               sorted_pieces.remove(0);
        }

        theta += Math.PI/300;

        }

		 return moves;
    }

	private boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
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

    private class GoalDistanceComparator implements Comparator<Pair<Integer, Point>>
    {
        public Double getGoalDistance(Pair<Integer, Point> piece)
        {
            if (isplayer1) {
                Double d = piece.getValue().x - (-28.0);
                if (d <= 0) d = Double.POSITIVE_INFINITY;
                return d;
            } else {
                Double d = 29.0 - piece.getValue().x;
                if (d <= 0) d = Double.POSITIVE_INFINITY;

                return d;
            }
        }

        public int compare(Pair<Integer, Point> piece1, Pair<Integer, Point> piece2)
        {
            return getGoalDistance(piece1).compareTo(getGoalDistance(piece2));
        }
    }

    private List<Pair<Integer, Point>> sortPieces(HashMap<Integer, Point> player_pieces, Comparator c)
    {
        List<Pair<Integer, Point>> list = new LinkedList<Pair<Integer, Point>>();

        // Store map entries into list as pairs
        for (Map.Entry<Integer, Point> piece : player_pieces.entrySet())
            list.add(new Pair<Integer, Point>(piece.getKey(), piece.getValue()));

        Collections.sort(list, c);

        return list;
    }

    // Computes euclidean distance b/t 2 points.
    private Double l2norm(Point pos1, Point pos2)
    {
        if (pos1 == null || pos2 == null)
            return Double.POSITIVE_INFINITY;

        Double delta_x = pos2.x - pos1.x;
        Double delta_y = pos2.y - pos1.y;
        return Math.sqrt(delta_x*delta_x + delta_y*delta_y);
    }

    // Computes euclidean distance b/t a piece and a point.
    private Double l2norm(Pair<Integer, Point> piece, Point pos)
    {
        if (piece == null || pos == null)
            return Double.POSITIVE_INFINITY;

        return l2norm(piece.getValue(), pos);
    }

    // Computes euclidean distance b/t a piece as a map entry and a point.
    private Double l2norm(Map.Entry<Integer, Point> piece, Point pos)
    {
        if (piece == null || pos == null)
            return Double.POSITIVE_INFINITY;

        return l2norm(piece.getValue(), pos);
    }

    // Computes euclidean distance b/t 2 pieces.
    private Double l2norm(Pair<Integer, Point> piece1, Pair<Integer, Point> piece2)
    {
        if (piece1 == null || piece2 == null)
            return Double.POSITIVE_INFINITY;

        return l2norm(piece1.getValue(), piece2.getValue());
    }

    // Returns move that moves piece directly towards dst by diameter_piece. Caller responsible for checking validity of move.
    private Pair<Integer, Point> getDirectMove(Pair<Integer, Point> piece, Point dst)
    {
        Point piecePos = piece.getValue();

        double theta = Math.atan2(dst.y-piecePos.y, dst.x-piecePos.x);
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        Point newPos = new Point(piecePos.x + delta_x, piecePos.y + delta_y);
        Pair<Integer, Point> move = new Pair<Integer, Point>(piece.getKey(), newPos);

        return move;
    }

    // Randomly takes a step backwards to try and get unstuck.
    private Pair<Integer, Point> getRandomBackMove(Pair<Integer, Point> piece, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        int nAttempts = 0;
        Point piecePos = piece.getValue();
        double theta, delta_x, delta_y;
        Point newPos;
        Pair<Integer, Point> move = null;

        while (nAttempts < 1000) {

            theta = -Math.PI/2 + Math.PI * random.nextDouble();
            delta_x = diameter_piece * Math.cos(theta);
            delta_y = diameter_piece * Math.sin(theta);

            newPos = new Point(piecePos);
            newPos.x = isplayer1 ? piecePos.x + delta_x : piecePos.x - delta_x;
            newPos.y = piecePos.y + delta_y;

            move = new Pair<Integer, Point>(piece.getKey(), newPos);
            if (check_validity(move, player_pieces, opponent_pieces))
                break;

            nAttempts++;
        }

        System.out.println("Moving back randomly to get unstuck");

        return move;
    }

    /*
     * Returns move that either moves piece exactly to dst, or moves piece towards dst s.t. future calls to getExactMove()
     * will eventually move piece exactly to dst. Caller responsible for checking validity of move.
     */
    private Pair<Integer, Point> getExactMove(Pair<Integer, Point> piece, Point dst, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        Pair<Integer, Point> move;

        // If the dist to dst is >= 2 piece widths or is exactly 1 width just move towards dst in a straight line
        if (l2norm(piece, dst) >= 2 * diameter_piece || Math.abs(l2norm(piece, dst) - diameter_piece) < 0.001) {
            move = getDirectMove(piece, dst);
            if (check_validity(move, player_pieces, opponent_pieces)) {
                System.out.println("Moving directly");
                return move;
            } else {
                move = getRandomBackMove(piece, player_pieces, opponent_pieces);
                return move;
            }
        }

        // We'll move s.t. piece is exactly diameter_piece away from dst so next move will get it there
        System.out.println("Moving at an angle");

        Point piecePos = piece.getValue();
        // Angle between piece and dst
        double alpha = Math.atan2(dst.y-piecePos.y, dst.x-piecePos.x);
        System.out.println("alpha = " + Double.toString(Math.toDegrees(alpha)));
        // If we draw an isoceles triangle with the equal sides = diameter_piece, beta is the value of the base angles
        System.out.println(l2norm(piecePos, dst)/2);
        double beta = Math.acos((l2norm(piecePos, dst)/2) / diameter_piece);
        System.out.println("beta = " + Double.toString(Math.toDegrees(beta)));

        // alpha + beta and alpha - beta corresponds to drawing the lower/upper isoceles triangles for player1/2 respectively
        double theta = alpha + beta;
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        Point newPos = new Point(piecePos.x + delta_x, piecePos.y + delta_y);

        move = new Pair<Integer, Point>(piece.getKey(), newPos);

        // If that's not valid, we'll try the opposite triangle
        if (!check_validity(move, player_pieces, opponent_pieces)) {
            theta = alpha - beta;
            delta_x = diameter_piece * Math.cos(theta);
            delta_y = diameter_piece * Math.sin(theta);
            newPos = new Point(piecePos.x + delta_x, piecePos.y + delta_y);

            // Note if this move is also invalid, we rely on caller to handle it
            move = new Pair<Integer, Point>(piece.getKey(), newPos);
        }

        return move;
    }

    // Same idea as getExactMove(), but tries to take a step back first to get unstuck.
    // TODO: We can combine this with getExactMove()? Depends on if we prioritize moving this piece first or prioritize
    // trying to avoid moving backwards.
    private Pair<Integer, Point> getExactMoveWithBack(Pair<Integer, Point> piece, Point dst, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        Pair<Integer, Point> move;

        // If the dist to dst is >= 2 piece widths or is exactly 1 width just move towards dst in a straight line
        if (l2norm(piece, dst) >= 2 * diameter_piece || Math.abs(l2norm(piece, dst) - diameter_piece) < 0.001) {
            move = getDirectMove(piece, dst);
            if (check_validity(move, player_pieces, opponent_pieces)) {
                System.out.println("Moving directly");
                return move;
            }
        }

        // We'll move s.t. piece is exactly diameter_piece away from dst so next 2 direct moves will get it there
        System.out.println("Moving backwards at an angle");
        Point piecePos = piece.getValue();
        // Angle between piece and dst
        double alpha = Math.atan2(dst.y-piecePos.y, dst.x-piecePos.x);
        System.out.println("alpha = " + Double.toString(Math.toDegrees(alpha)));

        // We're drawing a triangle with sides diameter_piece, 2 * diameter_piece and l2norm(piece, dst)
        // beta is the angle opposite to the side with length 2 * diameter_piece computed w/ law of cosines
        double beta = Math.acos((diameter_piece*diameter_piece + l2norm(piece, dst)*l2norm(piece, dst) -
            (2*diameter_piece)*(2*diameter_piece)) / (2 * diameter_piece * l2norm(piece, dst)));
        System.out.println("beta = " + Double.toString(Math.toDegrees(beta)));

        double theta = alpha + beta;
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        Point newPos = new Point(piecePos.x + delta_x, piecePos.y + delta_y);

        move = new Pair<Integer, Point>(piece.getKey(), newPos);

        // If that's not valid, we'll try the opposite triangle
        if (!check_validity(move, player_pieces, opponent_pieces)) {
            theta = alpha - beta;
            delta_x = diameter_piece * Math.cos(theta);
            delta_y = diameter_piece * Math.sin(theta);
            newPos = new Point(piecePos.x + delta_x, piecePos.y + delta_y);

            // Note if this move is also invalid, we rely on caller to handle it
            move = new Pair<Integer, Point>(piece.getKey(), newPos);
        }

        return move;
    }

    private class Wall
    {
        private class Brick
        {
            private Point idealPos;
            private Pair<Integer, Point> piece;
            private boolean pieceInPlace = false;

            public Brick(Point idealPos) { this.idealPos = idealPos; }
            public Point getIdealPos() { return this.idealPos; }
            public Pair<Integer, Point> getPiece() { return this.piece; }
            public boolean hasPiece() { return (this.piece != null); }
            public boolean isPieceInPlace() { return this.pieceInPlace; }

            // Assign a piece to a brick without a piece
            public void assignPiece(Pair<Integer, Point> piece) { this.piece = piece; }

            // Update the location of the brick's piece and whether it's now in place
            public void updatePiece(Pair<Integer, Point> piece)
            {
                this.assignPiece(piece);

                // If the euclidean distance b/t the piece and the ideal brick position <= 0.001 it's in place
                if (l2norm(this.piece, this.idealPos) <= 0.001) {
                    this.pieceInPlace = true;
                    System.out.println(this.piece + " is now in place");
                }
            }

            public String toString()
            {
                String ideal = "Ideal: (" + Double.toString(this.idealPos.x) + ", " + Double.toString(this.idealPos.y) + ")\t";

                String piece;
                if (this.piece == null)
                    piece = "Piece: (null)";
                else
                    piece = "Piece: " + this.piece.getKey() + " @ (" + Double.toString(this.piece.getValue().x) + ", " +
                        Double.toString(this.piece.getValue().y) + ")";

                return ideal + piece;
            }
        }

        // Slightly closer to border than minimum flippable gap 20.0 - (1 + sqrt(3))
        public final Double yPosTopBrick = 17.27;
        public final Double yPosBotBrick = -17.27;

        private List<Brick> bricks;
        private int nBricks;
        private Double xPos;
        private boolean isBuilt = false;

        public Wall(HashMap<Integer, Point> player_pieces, int nBricks, Double xPos)
        {
            System.out.println("in Wall constructor");

            this.nBricks = nBricks;
            this.xPos = xPos;
            this.bricks = new ArrayList<Brick>();

            // Distance between each brick
            Double brickDist = (yPosTopBrick-yPosBotBrick) / (this.nBricks-1);

            // Compute ideal brick positions
            Point topBrickPos = new Point(this.xPos, yPosTopBrick);

            for (int i = 0; i < nBricks; i++) {
                this.bricks.add(new Brick(new Point(topBrickPos.x, topBrickPos.y - i*brickDist)));
            }

            // Assign pieces to bricks greedily using shortest distance

            // Pieces that aren't assigned to a brick yet
            List<Pair<Integer, Point>> unassignedPieces = new LinkedList<Pair<Integer, Point>>();
            for (Map.Entry<Integer, Point> piece : player_pieces.entrySet())
                unassignedPieces.add(new Pair(piece.getKey(), piece.getValue()));

            // Bricks still needing pieces
            List<Brick> bricksNeedingPieces = new LinkedList<Brick>(this.bricks);

            while (bricksNeedingPieces.size() > 0)
            {
                // Find brick with closest unassigned piece
                Brick brickWithClosestPiece = null;
                Pair<Integer, Point> closestPiece = null;
                Double shortestDist = Double.POSITIVE_INFINITY;

                for (Brick brick : bricksNeedingPieces) {
                    for (Pair<Integer, Point> piece : unassignedPieces) {
                        if (l2norm(piece, brick.getIdealPos()) < shortestDist) {
                            closestPiece = piece;
                            brickWithClosestPiece = brick;
                            shortestDist = l2norm(piece, brick.getIdealPos());
                        }
                    }
                }

                brickWithClosestPiece.assignPiece(closestPiece);
                bricksNeedingPieces.remove(brickWithClosestPiece);
                unassignedPieces.remove(closestPiece);
            }

            System.out.println(this);
            System.out.println("exiting Wall constructor");
        }

        public ArrayList<Integer> getWallPieceIds() {
            ArrayList<Integer> out = new ArrayList<Integer>();
            for(Brick brick : this.bricks) {
                out.add(brick.piece.getKey());
            }
            return out;
        }
        public void changeBrick(Integer currentVal, Pair<Integer, Point> newBrick) {
            for(Brick brick : this.bricks) {
                if(brick.getPiece().getKey() == currentVal) {
                    brick.piece = newBrick;
                    brick.idealPos = newBrick.getValue();
                }
            }
        }
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            for (Brick brick : this.bricks) {
                sb.append(brick.toString());
                sb.append("\n");
            }

            return sb.toString();
        }

        /*
         * Greedy algo to find the best wall building move based on distance of closest opponent piece.
         * Returns the move and updates this.bricks accordingly (under this implementation, if you call this to get a move,
         *  you must make the move lest this.bricks will not match board state)
         */
        public Pair<Integer, Point> buildAndGetMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
        {
            // Find closest opponent piece to any ideal brick pos that needs filling, we build that first
            List<Brick> bricksNeedingFill = new LinkedList<Brick>();
            for (Brick brick : this.bricks)
                if (!brick.isPieceInPlace()) bricksNeedingFill.add(brick);

            Brick toBuild = null;
            Pair<Integer, Point> move;

            do {
                Double minDist = Double.POSITIVE_INFINITY;

                for (Brick brick : bricksNeedingFill) {
                    for (Map.Entry<Integer, Point> piece : opponent_pieces.entrySet()) {
                        if (l2norm(piece, brick.getIdealPos()) < minDist) {
                            toBuild = brick;
                            minDist = l2norm(piece, brick.getIdealPos());
                        }
                    }
                }

                System.out.println(bricksNeedingFill.size());

                bricksNeedingFill.remove(toBuild);

                // Move the piece associated with brick toBuild towards ideal position
                System.out.println("Moving " + toBuild.getPiece() + " to " + toBuild.getIdealPos());
                System.out.println("Distance is: " + l2norm(toBuild.getPiece(), toBuild.getIdealPos()));

                move = getExactMove(toBuild.getPiece(), toBuild.getIdealPos(), player_pieces, opponent_pieces);

            // If this move is invalid, we'll look for the next best move
            } while ((!check_validity(move, player_pieces, opponent_pieces)) && (bricksNeedingFill.size() != 0));

            // TODO: refactor; combine getExactMoveWithBack() with getExactMove()?
            // This is the case where the wall isn't built yet, but all remaining pieces can't move.
            // This typically happens for the last piece where it doesn't have enough room to do the triangle flip from
            // either top or bottom. We try line up with idealPos by taking a step back.
            if (!check_validity(move, player_pieces, opponent_pieces))
                // TODO: This function doesn't seem to be working right now; it seems we're never doing this move option.
                move = getExactMoveWithBack(toBuild.getPiece(), toBuild.getIdealPos(), player_pieces, opponent_pieces);

            // Try to get unstuck
            if (!check_validity(move, player_pieces, opponent_pieces))
                // TODO: what if this returns invalid too, e.g. the piece is surrounded?
                move = getRandomBackMove(toBuild.getPiece(), player_pieces, opponent_pieces);

            toBuild.updatePiece(move);

            // If all pieces in place the wall is built
            boolean allInPlace = true;
            for (Brick brick : this.bricks)
                if (!brick.isPieceInPlace()) allInPlace = false;
            this.isBuilt = allInPlace;

            return move;
        }

        public boolean isBuilt() { return this.isBuilt; }
    }


}

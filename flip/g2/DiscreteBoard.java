package flip.g2;

import flip.sim.Point;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author juand.correa
 */
public class DiscreteBoard {

    private static final short NONE = 0;
    private static final short UP = 1;
    private static final short DOWN = 2;
    private static final short LEFT = 3;
    private static final short RIGHT = 4;

    int width;
    int height;
    int gridResolution;
    Integer[][] board;

    public DiscreteBoard(int width, int height, int gridResolution) {
        this.width = width;
        this.height = height;
        this.gridResolution = gridResolution;
        board = new Integer[height / gridResolution][width / gridResolution];
    }

    public void reset() {
        for (Integer[] board1 : board) {
            for (int j = 0; j < board1.length; j++) {
                board1[j] = null;
            }
        }
    }

    public boolean isFree(int x, int y, boolean[][] visited) {
        boolean inBounds = x >= 0 && x < visited[0].length && y >= 0 && y < visited.length;

        if (!inBounds) {
            return false;
        }

        boolean notVisited = !visited[y][x];
        boolean isFree = this.board[y][x] == null || this.board[y][x] > -1;
        return inBounds && notVisited && isFree;
    }

    public Pair<Integer, List<Point>> findClosestPiece(Point targetPosition) {
        try {
            return findAPath(targetPosition, (n) -> n != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Pair<Integer, List<Point>> findAPath(Point targetPosition, Predicate<Integer> criteria) {
        final Pair<Integer, Integer> target = getDiscreteBoardCoords(targetPosition);
        final Queue<Pair> q = new LinkedList<>();
        q.add(target);
        final short[][] directions = new short[height / gridResolution][width / gridResolution];
        final boolean[][] visited = new boolean[height / gridResolution][width / gridResolution];
        for (boolean[] visited1 : visited) {
            for (int j = 0; j < visited1.length; j++) {
                visited1[j] = false;
            }
        }
        for (short[] directions1 : directions) {
            for (int j = 0; j < directions1.length; j++) {
                directions1[j] = NONE;
            }
        }
        visited[target.getKey()][target.getValue()] = true;

        ArrayList<Point> path = new ArrayList<>();

        while (!q.isEmpty()) {
            Pair<Integer, Integer> p = q.remove();
            int yCoord = p.getKey();
            int xCoord = p.getValue();

            final Integer boardValue = this.board[yCoord][xCoord];
            if (criteria.test(boardValue) && target != p) {
                int yPath = yCoord;
                int xPath = xCoord;

                while (directions[yPath][xPath] != NONE) {
                    short nextDirection = directions[yPath][xPath];
                    if (nextDirection == UP) {
                        yPath--;
                    } else if (nextDirection == RIGHT) {
                        xPath++;
                    } else if (nextDirection == DOWN) {
                        yPath++;
                    } else if (nextDirection == LEFT) {
                        xPath--;
                    }
                    path.add(this.getRealBoardCoords(xPath, yPath));
                }

                return new Pair<>(boardValue, path);
            } else {
                //Pair<Integer, Integer> up = new Pair<>(yCoord - 1, xCoord);
                int y, x;

                y = yCoord;
                x = xCoord - 1;
                if (this.isFree(x, y, visited)) {
                    visited[y][x] = true;
                    q.add(new Pair<>(y, x));
                    directions[y][x] = RIGHT;
                }

                y = yCoord - 1;
                x = xCoord;
                if (this.isFree(x, y, visited)) {
                    visited[y][x] = true;
                    q.add(new Pair<>(y, x));
                    directions[y][x] = DOWN;
                }

                y = yCoord + 1;
                x = xCoord;
                if (this.isFree(x, y, visited)) {
                    visited[y][x] = true;
                    q.add(new Pair<>(y, x));
                    directions[y][x] = UP;
                }

//                y = yCoord;
//                x = xCoord + 1;
//                if (this.isFree(x, y, visited)) {
//                    visited[y][x] = true;
//                    q.add(new Pair<>(y, x));
//                    directions[y][x] = LEFT;
//                }
            }
        }

        return null;
    }

    public Point getRealBoardCoords(Point p) {
        return getRealBoardCoords((int) p.x, (int) p.y);
    }

    public Point getRealBoardCoords(int x, int y) {
        return new Point((double) ((x * gridResolution) - width / 2), (double) ((y * gridResolution) - height / 2));
    }

    public Pair<Integer, Integer> getDiscreteBoardCoords(Point p) {
        return new Pair((int) Math.round((p.y + height / 2) / gridResolution), (int) Math.round((p.x + width / 2) / gridResolution));
    }

    public void recordOpponentPieces(Map<Integer, Point> opponentPieces) {
        opponentPieces.entrySet().forEach((pp) -> {
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(pp.getValue());
            int x1 = (int) Math.floor((pp.getValue().x + width / 2) / gridResolution);
            int x2 = (int) Math.ceil((pp.getValue().x + width / 2) / gridResolution);
            int y1 = (int) Math.floor((pp.getValue().y + height / 2) / gridResolution);
            int y2 = (int) Math.ceil((pp.getValue().y + height / 2) / gridResolution);
            //System.out.println("***" + pp.getValue() + " -> " + discreteCoords);
            if (x1 >= 0 && x1 < board[0].length && y1 >= 0 && y1 < board.length) {
                board[y1][x1] = -1;
            }
            if (x2 >= 0 && x2 < board[0].length && y1 >= 0 && y1 < board.length) {
                board[y1][x2] = -1;
            }
            if (x1 >= 0 && x1 < board[0].length && y2 >= 0 && y2 < board.length) {
                board[y2][x1] = -1;
            }
            if (x2 >= 0 && x2 < board[0].length && y2 >= 0 && y2 < board.length) {
                board[y2][x2] = -1;
            }
        });
    }

    public void recordPlayerPieces(Collection<Point> playerPieces) {
        playerPieces.stream().map((p) -> this.getDiscreteBoardCoords(p)).forEachOrdered((discreteCoords) -> {
            board[discreteCoords.getKey()][discreteCoords.getValue()] = 1;
        });
    }

    public void recordPlayerPiecesIdx(Map<Integer, Point> playerPieces) {
        playerPieces.entrySet().forEach((pp) -> {
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(pp.getValue());
            board[discreteCoords.getKey()][discreteCoords.getValue()] = pp.getKey();
        });
    }

    public Double getCrowdedColumn() {
        return getCrowdedColumn(0, width);
    }

    public Double getCrowdedColumn(double minX, double maxX) {
        final int minXR = (int) Math.round((minX + width / 2) / gridResolution);
        final int maxXR = (int) Math.round((maxX + width / 2) / gridResolution);
        for (int j = minXR; j < maxXR; j++) {
            int count = 0;
            for (int i = 0; i < board.length; i++) {
                if (board[i][j] != null) {
                    count++;
                }
            }
            //System.out.println("***Row " + j + ": " + count);
            if (((double) count) / board.length > 0.25) {
                return (double) j * gridResolution - width / 2;
            }
        }
        return null;
    }

    public boolean isThereAFullColumn(double minX, double maxX) {
        final int minXR = (int) Math.round((minX + width / 2) / gridResolution);
        final int maxXR = (int) Math.round((maxX + width / 2) / gridResolution);
        for (int j = minXR; j < maxXR; j++) {
            int count = 0;
            for (int i = 0; i < board.length; i++) {
                if (board[i][j] != null) {
                    count++;
                }
            }
            if (count >= 0.75 * board.length) {
                return true;
            }
        }
        return false;
    }

    public Double findAHole(double x) {
        final int col = (int) ((x + width / 2) / gridResolution);
        int d = 4;
        int blanks;
        Integer bestRow = null;
        int bestRowBlanks = 0;
        for (int i = 0; i < board.length; i++) {
            blanks = 0;
            for (int j = Math.max(i - d, 0); j < Math.min(i + d, board.length); j++) {
                for (int k = Math.max(col - d, 0); k < Math.min(col + d, board[i].length); k++) {
                    if (board[j][k] == null) {
                        blanks++;
                    }
                }
            }
            if (bestRowBlanks < blanks || Math.abs(i - board.length / 2) < Math.abs(bestRow - board.length / 2)) {
                bestRow = i;
                bestRowBlanks = blanks;
            }
        }
        return (bestRow == null) ? null : bestRow.doubleValue() * gridResolution - height / 2;
    }

    public int findClosestSumDistances(int lowY, int highY, int x) {
        boolean[][] used = new boolean[board.length][board[0].length];

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                used[i][j] = false;
            }
        }

        int totalDistance = 0;
        for (int wallY = lowY; wallY < highY; wallY++) {
            int closestX = -1;
            int closestY = -1;
            int closestDistance = 999;
            for (int row = 0; row < board.length; row++) {
                for (int col = x + 1; col < board[0].length; col++) {
                    if (board[row][col] != null && board[row][col] == -1 && !used[row][col]) {
                        int distance = Math.abs(wallY - row) + Math.abs(x - col);
                        if (distance <= closestDistance) {
                            closestX = col;
                            closestY = row;
                            closestDistance = distance;
                        }
                    }
                }
            }
            used[closestY][closestX] = true;
            totalDistance += closestDistance;
        }

        return totalDistance;
    }

    public double findBestHole(double x, double runnerY, double runnerX) {
        int runnerYDiscrete = (int) Math.round((runnerY + height / 2) / gridResolution);
        int runnerXDiscrete = (int) Math.round((runnerX + width / 2) / gridResolution);
        final int wallCol = (int) Math.round((x + width / 2) / gridResolution);

        double bestY = -1.0;
        int bestDeltaAdvantage = -9999;

        int currTop = -1;
        for (int i = 0; i < board.length; i++) {
            if (board[i][wallCol] != null) {
                if (currTop > -1) {
                    int opponentDistances = findClosestSumDistances(currTop, i, wallCol);
                    int wallMid = (int) ((i - 1 + currTop) / 2.0);
                    int myDistance = Math.abs(runnerXDiscrete - wallCol) + Math.abs(runnerYDiscrete - wallMid);
                    int delta = opponentDistances - myDistance;
                    if (delta >= bestDeltaAdvantage) {
                        bestY = (double) wallMid * gridResolution - 20.;
                        bestDeltaAdvantage = delta;
                    }
                }
                currTop = -1;
            } else {
                currTop = currTop == -1 ? i : currTop;
            }
        }

        if (currTop > -1) {
            int opponentDistances = findClosestSumDistances(currTop, board.length, wallCol);
            int wallMid = (int) ((board.length - 1 + currTop) / 2.0);
            int myDistance = Math.abs(runnerXDiscrete - wallCol) + Math.abs(runnerYDiscrete - wallMid);
            int delta = opponentDistances - myDistance;
            if (delta >= bestDeltaAdvantage) {
                bestY = (double) wallMid * gridResolution - 20.;
            }
        }

        return bestY;
    }

    public List<Point> findPath(Point source, Point target) {
        DiscretePoint s = getDiscreteCoords(source);
        DiscretePoint t = getDiscreteCoords(target);
        AStar<DiscretePoint, SNode, List<DiscretePoint>> as = new AStar<>(
                (n) -> n.getState().equals(t), //goal test
                (n) -> n.tracePath(), //solution creation
                (n) -> { //children/action state creation
                    List<SNode> children = new ArrayList<>();
                    DiscretePoint p = n.getState();
                    DiscretePoint c;

                    c = new DiscretePoint(p.x + 1, p.y);
                    if (canMove(c) || c.equals(t)) {
                        children.add(new SNode(c, n, n.cost + 1));
                    }

                    c = new DiscretePoint(p.x, p.y + 1);
                    if (canMove(c) || c.equals(t)) {
                        children.add(new SNode(c, n, n.cost + 1));
                    }

                    c = new DiscretePoint(p.x, p.y - 1);
                    if (canMove(c) || c.equals(t)) {
                        children.add(new SNode(c, n, n.cost + 1));
                    }

                    c = new DiscretePoint(p.x - 1, p.y);
                    if (canMove(c) || c.equals(t)) {
                        children.add(new SNode(c, n, n.cost + 1));
                    }

                    return children;
                },
                (n) -> Math.hypot(t.x - n.getState().x, t.y - n.getState().y), //heuristic
                (n) -> n.cost //cost function
        );
        List<DiscretePoint> path = as.search(new SNode(s, null, 0.0));
        return path.stream().map((dp) -> this.getRealBoardCoords(dp)).collect(Collectors.toList());
    }

    private boolean canMove(DiscretePoint p) {
        if (p.x < 0 || p.x >= board[0].length || p.y < 0 || p.y >= board.length) {
            return false;
        } else {
            return board[p.y][p.x] == null;
        }
    }

    private DiscretePoint getDiscreteCoords(Point p) {
        return new DiscretePoint(
                (int) Math.round((p.x + width / 2) / gridResolution),
                (int) Math.round((p.y + height / 2) / gridResolution)
        );
    }

    private Point getRealBoardCoords(DiscretePoint dp) {
        return this.getRealBoardCoords(dp.x, dp.y);
    }

}

class SNode implements SearchNode<DiscretePoint> {

    DiscretePoint state;
    SNode previous;
    Double cost;

    public SNode(DiscretePoint state, SNode previous, Double cost) {
        this.state = state;
        this.previous = previous;
        this.cost = cost;
    }

    @Override
    public DiscretePoint getState() {
        return state;
    }

    public List<DiscretePoint> tracePath() {
        List<DiscretePoint> list;
        if (previous == null) {
            list = new ArrayList<>();
        } else {
            list = previous.tracePath();
        }
        list.add(state);
        return list;
    }

}

class DiscretePoint {

    final int x;
    final int y;

    public DiscretePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.x;
        hash = 59 * hash + this.y;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DiscretePoint other = (DiscretePoint) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

}

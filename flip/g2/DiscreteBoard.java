package flip.g2;

import flip.sim.Point;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Collection;
import java.util.List;

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
            if (boardValue != null && target != p) {
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
        return new Pair((int) ((p.y + height / 2) / gridResolution), (int) ((p.x + width / 2) / gridResolution));
    }

    public void recordOpponentPieces(Collection<Point> opponentPieces) {
        for (Point p : opponentPieces) {
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(p);
            board[discreteCoords.getKey()][discreteCoords.getValue()] = -1;
        }
    }

    public void recordPlayerPieces(Collection<Point> playerPieces) {
        for (Point p : playerPieces) {
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(p);
            board[discreteCoords.getKey()][discreteCoords.getValue()] = 1;
        }
    }

    public void recordPlayerPiecesIdx(Collection<Point> playerPieces) {
        for (int i = 0; i < playerPieces.size(); i++) {
            Point p = (Point) playerPieces.toArray()[i];
            Pair<Integer, Integer> discreteCoords = this.getDiscreteBoardCoords(p);
            board[discreteCoords.getKey()][discreteCoords.getValue()] = i;
        }
    }

    public Double getCrowdedColumn() {
        return getCrowdedColumn(0, width);
    }

    public Double getCrowdedColumn(double minX, double maxX) {
        final int minXR = (int) ((minX + width / 2) / gridResolution);
        final int maxXR = (int) ((maxX + width / 2) / gridResolution);
        for (int j = minXR; j < maxXR; j++) {
            int count = 0;
            for (int i = 0; i < board.length; i++) {
                if (board[i][j] != null) {
                    count++;
                }
            }
            if ((double) count / board.length > 0.15) {
                return (double) j * gridResolution - width / 2;
            }
        }
        return null;
    }
    
    public boolean isThereAFullColumn(double minX, double maxX) {
        final int minXR = (int) ((minX + width / 2) / gridResolution);
        final int maxXR = (int) ((maxX + width / 2) / gridResolution);
        for (int j = minXR; j < maxXR; j++) {
            int count = 0;
            for (int i = 0; i < board.length; i++) {
                if (board[i][j] != null) {
                    count++;
                }
            }
            if (count == board.length) {
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

}

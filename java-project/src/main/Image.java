package main;

import ecs100.UI;
import ecs100.UIFileChooser;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class Image {
    private static final double MIN_X = 285;
    private static final double WIDTH = 100;//150
    private static final double MIN_Y = 260;
    private static final double HEIGHT = 100;

    private static final int BUFFER = 3;

    private double sizeX;
    private double sizeY;

    private ArrayList<Angle> angles;

    public Image() {
        String file = UIFileChooser.open("Open ASCII PBM image file");
        angles = makeAngles(getPoints(file));
    }

    /**
     * Constructs a semi-optimal path through the image
     * @param points a 2D array of points that need to be drawn
     * @return a list of the points to move through
     */
    private ArrayList<Angle> makeAngles(boolean[][] points) {
        ArrayList<Angle> path = new ArrayList<>();
        boolean[][][] mask = makeMask(points);
        PointXY prevPoint;

        for (PointXY point = findNext(null, points, mask); point != null; point = findNext(point, points, mask)) {
            for (int i =0; i < BUFFER; i++)
                path.add(new Angle(point.get_x(), point.get_y(), false));
            path.add(new Angle(point.get_x(), point.get_y(), true));
            while (true) {
                PointXY deltaPoint = moveNext(point, mask);
                if (deltaPoint == null) {
                    break;
                }
                double x = point.get_x() + deltaPoint.get_x();
                double y = point.get_y() + deltaPoint.get_y();
                prevPoint = point;
                point = new PointXY(x, y, true);
                UI.drawLine(prevPoint.get_x() * 3, prevPoint.get_y() * 3, point.get_x() * 3, point.get_y() * 3);
                path.add(new Angle(point.get_x(), point.get_y(), true));
                UI.printf("%d,%d%n", (int)point.get_x(), (int)point.get_y());

            }
            for (int i =0; i < BUFFER; i++)
                path.add(new Angle(point.get_x(), point.get_y(), false));
        }
        return path;
    }

    /**
     * Constructs an 8-point array (representing a 3x3 grid missing the center point) that stores whether or not
     * neighbouring pixels need to be visited.
     * @param points a 2D array of black/white pixels (black = true, white = false)
     * @return the mask of neighbouring pixels
     */
    private boolean[][][] makeMask(boolean[][] points) {
        // First remove any points that are fully surrounded in left-top-right-bottom directions.
        // This prevents drawing black spots (which would take far too long)
        boolean[][] newPoints = new boolean[(int) sizeX][(int) sizeY];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                if (points[x][y] && ((y <= 0 || !points[x][y - 1]) || (x <= 0 || !points[x - 1][y]) || (y + 1 >= sizeY || !points[x][y + 1]) || (x + 1 >= sizeX || !points[x + 1][y]))) {
                    newPoints[x][y] = true;
                } else {
                    newPoints[x][y] = false;
                }
            }
        }

        // Note that diagonals make sure that there are no points between each other before marking as +
        boolean[][][] mask = new boolean[(int) sizeX][(int) sizeY][8];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                points[x][y] = newPoints[x][y];
                if (y > 0 && newPoints[x][y - 1]) {
                    mask[x][y][0] = true;
                }
                if (x + 1 < sizeX && y > 0 && newPoints[x + 1][y - 1] && !newPoints[x][y - 1] && !newPoints[x + 1][y]) {
                    mask[x][y][1] = true;
                }
                if (x + 1 < sizeX && newPoints[x + 1][y]) {
                    mask[x][y][2] = true;
                }
                if (x + 1 < sizeX && y + 1 < sizeY && newPoints[x + 1][y + 1] && !newPoints[x][y + 1] && !newPoints[x + 1][y]) {
                    mask[x][y][3] = true;
                }

                if (x > 0 && newPoints[x - 1][y]) {
                    mask[x][y][4] = true;
                }
                if (y + 1 < sizeY && x > 0 && newPoints[x - 1][y + 1] && !newPoints[x - 1][y] && !newPoints[x][y + 1]) {
                    mask[x][y][5] = true;
                }
                if (y + 1 < sizeY && newPoints[x][y + 1]) {
                    mask[x][y][6] = true;
                }
                if (y > 0 && x > 0 && newPoints[x - 1][y - 1] && !newPoints[x][y - 1] && !newPoints[x - 1][y]) {
                    mask[x][y][7] = true;
                }
            }
        }
        return mask;
    }

    /**
     * Get the offset to the next point in the line, updating mask to reflect changes in neighbours.
     * @param point the current point in the line
     * @param mask the 3D array of neighbours of points
     * @return a point with coords relative to the current point
     */
    private PointXY moveNext(PointXY point, boolean[][][] mask) {
        int x = (int) point.get_x();
        int y = (int) point.get_y();

        // Remember to clear the neighbour in the opposite direction as well, to prevent backtracking.
        if (mask[x][y][0]) {
            mask[x][y][0] = false;
            mask[x][y-1][6] = false;
            return new PointXY(0, -1, true);
        }
        if (mask[x][y][1]) {
            mask[x][y][1] = false;
            mask[x+1][y-1][5] = false;
            return new PointXY(1, -1, true);
        }
        if (mask[x][y][2]) {
            mask[x][y][2] = false;
            mask[x+1][y][4] = false;
            return new PointXY(1, 0, true);
        }
        if (mask[x][y][3]) {
            mask[x][y][3] = false;
            mask[x+1][y+1][7] = false;
            return new PointXY(1, 1, true);
        }

        if (mask[x][y][4]) {
            mask[x-1][y][2] = false;
            mask[x][y][4] = false;
            return new PointXY(-1, 0, true);
        }
        if (mask[x][y][5]) {
            mask[x-1][y+1][1] = false;
            mask[x][y][5] = false;
            return new PointXY(-1, 1, true);
        }
        if (mask[x][y][6]) {
            mask[x][y+1][0] = false;
            mask[x][y][6] = false;
            return new PointXY(0, 1, true);
        }
        if (mask[x][y][7]) {
            mask[x-1][y-1][3] = false;
            mask[x][y][7] = false;
            return new PointXY(-1, -1, true);
        }
        return null;
    }

    /**
     * Finds the next starting point for a line
     * @param point the current point
     * @param points 2D array of points that exist
     * @param mask 3D array of neighbours of points
     * @return a point representing the beginning of a new line
     */
    private PointXY findNext(PointXY point, boolean[][] points, boolean[][][] mask) {
        // Start in top left corner if no prior point exists
        int x = point != null ? (int) point.get_x() : 0;
        int y = point != null ? (int) point.get_y() : 0;
        int nearX = -1;
        int nearY = -1;
        double dist = -1;

        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                if (points[i][j]) {
                    boolean alreadyDrawn = true;
                    // if any neighbours exist in mask, then this point can be drawn from
                    for (boolean m : mask[i][j]) {
                        if (m) {
                            alreadyDrawn = false;
                        }
                    }
                    if (alreadyDrawn) {
                        continue;
                    }
                    // the make it faster, just find the closest valid point to the current point
                    double newDist = Point2D.distance(x, y, i, j);
                    if (newDist < dist || dist == -1) {
                        nearX = i;
                        nearY = j;
                        dist = newDist;
                    }
                }
            }
        }
        if (dist != -1) {
            return new PointXY(nearX, nearY, true);
        }
        return null;
    }

    /**
     * Reads a PBM file (P1, ASCII type) and produces a 2D grid of black/white points
     * @param file the PBM file url to read from
     * @return a 2D boolean array of points
     */
    private boolean[][] getPoints(String file) {
        try {
            BufferedInputStream scan = new BufferedInputStream(new FileInputStream(file));
            if (!getNextToken(scan).equals("P1")) {
                throw new IllegalArgumentException();
            }
            sizeX = Integer.parseInt(getNextToken(scan));
            sizeY = Integer.parseInt(getNextToken(scan));
            boolean[][] image = new boolean[(int) sizeX][(int) sizeY];
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    try {
                        int value = Integer.parseInt(getNextValue(scan));

                        if (value == 1) {
                            UI.setColor(x == sizeX-1? Color.black:Color.BLUE);
                            image[(int) ((sizeX - x) - 1)][y] = true;
                        } else if (value != 0) {
                            throw new IllegalArgumentException();
                        }
                    } catch (NoSuchElementException | NumberFormatException e) {
                        //throw new IllegalArgumentException();
                    }
                }
            }
            return image;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * A helper method for getPoints() that finds the next metadata to read (ignoring comments and new lines)
     * @param scan The BufferedInputStream to read from
     * @return the next token in the stream
     */
    private String getNextToken(BufferedInputStream scan) {
        String out = "";
        try {
            int value = scan.read();
            while (true) {
                if (value == -1) {
                    return out;
                } else if (Character.isWhitespace((char)value)) {
                    if (out.length() > 0) {
                        return out;
                    }
                    value = scan.read();
                } else if ((char)value == '#') {
                    if (out.length() > 0) {
                        return out;
                    }
                    do {
                        value = scan.read();
                    } while ((char)value != '\n');
                } else {
                    out += String.valueOf((char)value);
                    value = scan.read();
                }
            }
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * A helper method for getPoints() that finds the next pixel to read (ignoring comments and new lines)
     * @param scan The BufferedInputStream to read from
     * @return the next token in the stream
     */
    private String getNextValue(BufferedInputStream scan) {
        try {
            while (true) {
                int value = scan.read();
                if (value == -1) {
                    return "";
                } else if ((char)value == '#') {
                    do {
                        value = scan.read();
                    } while ((char)value != '\n');
                    return String.valueOf((char)value);
                } else {
                    if ((char)value=='\n') continue;
                    return String.valueOf((char)value);
                }
            }
        } catch (IOException e) {
            return "";
        }
    }

    public ArrayList<Angle> getAngles() {
        return angles;
    }

    /**
     * Saves the current set of angles to a PWM file (CSV of pwm1,pwm2,pwm3)
     * @return the file url to save to
     */
    public String savePwmFile() {
        try {
            String filename = UIFileChooser.save("Save PWM file");
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            for (Angle angle : angles) {
                writer.printf("%d,%d,%d%n", angle.pwm1, angle.pwm2, angle.penDown ? 2000 : 1000);
            }
            // Move back to rest position when finished
            writer.print("1500,1500,1000");
            writer.close();
            return filename;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class Angle {
        double x;
        double y;
        double theta1;
        double theta2;
        int pwm1;
        int pwm2;
        boolean penDown;

        public Angle(double ox, double oy, boolean p) {
            x = (ox / sizeX) * WIDTH + MIN_X;
            y = (oy / sizeY) * HEIGHT + MIN_Y;

            Arm arm = new Arm();
            arm.inverseKinematic(x, y);

            theta1 = arm.get_theta1();
            theta2 = arm.get_theta2();
            pwm1 = arm.get_pwm1();
            pwm2 = arm.get_pwm2();
            penDown = p;
        }
    }

    public static void main(String args[]) {
        Image img = new Image();
        String filename = img.savePwmFile();
        if (filename != null) {
            String destFile = filename.substring(filename.lastIndexOf("/") + 1);
            String dest = String.format("pi@%s:~/Arm/%s", PiController.PI_IP, destFile);
            String cmd = String.format("scp -i %s %s %s", PiController.PRIVATE_KEY_FILE, filename, dest);
            try {
                Runtime.getRuntime().exec(cmd);
                UI.printf("DONE: sent to %s (%s) %n", dest, cmd);
            } catch (IOException e) {
                e.printStackTrace();
                UI.println("FAIL");
            }
        }
        else {
            UI.println("CANCELLED");
        }
    }
}

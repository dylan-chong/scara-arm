package main;

import ecs100.UI;
import ecs100.UIFileChooser;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class Image {
    private static final double MIN_X = 270;
    private static final double WIDTH = 150;
    private static final double MIN_Y = 260;
    private static final double HEIGHT = 120;

    private double sizeX;
    private double sizeY;

    private ArrayList<Angle> angles;

    public Image() {
        String file = UIFileChooser.open("Open ASCII PBM image file");
        angles = makeAngles(getPoints(file));
    }

    /*private ArrayList<ArrayList<Angle>> makePaths(boolean[][] points) {
        ArrayList<ArrayList<Angle>> paths = new ArrayList<>();
        boolean[][][] mask = makeMask(points);

        for (PointXY point = findNext(null, points, mask); point != null; point = findNext(point, points, mask)) {
            ArrayList<Angle> path = new ArrayList<>();
            for (int i =0; i < 4; i++)
                path.add(new Angle(point.get_x(), point.get_y(), false));
            path.add(new Angle(point.get_x(), point.get_y(), true));
            while (true) {
                PointXY deltaPoint = moveNext(point, points, mask);
                if (deltaPoint == null) {
                    break;
                }
                double x = point.get_x() + deltaPoint.get_x();
                double y = point.get_y() + deltaPoint.get_y();
                point = new PointXY(x, y, true);
                path.add(new Angle(point.get_x(), point.get_y(), true));
            }
            for (int i =0; i < 4; i++)
                path.add(new Angle(point.get_x(), point.get_y(), false));
            paths.add(path);
        }
        return paths;
    }*/

    private boolean[][][] makeMask(boolean[][] points) {
        boolean[][][] mask = new boolean[(int) sizeX][(int) sizeY][4];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                if (y > 0 && points[x][y - 1]) {
                    mask[x][y][0] = true;
                }
                if (x + 1 < sizeX && y > 0 && points[x + 1][y - 1] && !points[x][y - 1] && !points[x + 1][y]) {
                    mask[x][y][1] = true;
                }
                if (x + 1 < sizeX && points[x + 1][y]) {
                    mask[x][y][2] = true;
                }
                if (x + 1 < sizeX && y + 1 < sizeY && points[x + 1][y + 1] && !points[x][y + 1] && !points[x + 1][y]) {
                    mask[x][y][3] = true;
                }
            }
        }
        return mask;
    }

    private PointXY moveNext(PointXY point, boolean[][][] mask) {
        int x = (int) point.get_x();
        int y = (int) point.get_y();
        if (mask[x][y][0]) {
            mask[x][y][0] = false;
            return new PointXY(0, -1, true);
        }
        if (mask[x][y][1]) {
            mask[x][y][1] = false;
            return new PointXY(1, -1, true);
        }
        if (mask[x][y][2]) {
            mask[x][y][2] = false;
            return new PointXY(1, 0, true);
        }
        if (mask[x][y][3]) {
            mask[x][y][3] = false;
            return new PointXY(1, 1, true);
        }
        return null;
    }

    private ArrayList<Angle> makeAngles(boolean[][] points) {
        ArrayList<Angle> angles = new ArrayList<>();
        boolean[][] mask = new boolean[(int) sizeX][(int) sizeY];
        PointXY last = null;
        for (PointXY point = findNext(null, points, mask); point != null; point = findNext(point, points, mask)) {
            int x = (int) point.get_x();
            int y = (int) point.get_y();
            if (last != null && Point.distance(x,y,last.get_x(),last.get_y()) >= 2) {
                for (int i =0; i < 4; i++)
                angles.add(new Angle(last.get_x(), last.get_y(), false));
            }
            /*if (y > 0 && points[x][y - 1]) {
                angles.add(new Angle(x, y, true));
                angles.add(new Angle(x, y - 1, true));
                angles.add(new Angle(x, y - 1, false));
                angles.add(new Angle(x, y, false));
            }
            if (x + 1 < sizeX && y > 0 && points[x + 1][y - 1]) {
                angles.add(new Angle(x, y, true));
                angles.add(new Angle(x + 1, y - 1, true));
                angles.add(new Angle(x + 1, y - 1, false));
                angles.add(new Angle(x, y, false));
            }
            if (x + 1 < sizeX && points[x + 1][y]) {
                angles.add(new Angle(x, y, true));
                angles.add(new Angle(x + 1, y, true));
                angles.add(new Angle(x + 1, y, false));
                angles.add(new Angle(x, y, false));
            }
            if (x + 1 < sizeX && y + 1 < sizeY && points[x + 1][y + 1]) {
                angles.add(new Angle(x, y, true));
                angles.add(new Angle(x + 1, y - 1, true));
                angles.add(new Angle(x + 1, y - 1, false));
                angles.add(new Angle(x, y, false));
            }*/
            //angles.add(new Angle(x, y, true));
            if (y > 0 && points[x][y - 1]) {
                angles.add(new Angle(x, y - 1, true));
                angles.add(new Angle(x, y, true));
                UI.drawLine(x*3,y*3,x*3,(y-1)*3);
            }
            if (x + 1 < sizeX && y > 0 && points[x + 1][y - 1]) {
                angles.add(new Angle(x + 1, y - 1, true));
                angles.add(new Angle(x, y, true));
                UI.drawLine(x*3,y*3,(x+1)*3,(y-1)*3);
            }
            if (x + 1 < sizeX && points[x + 1][y]) {
                angles.add(new Angle(x + 1, y, true));
                angles.add(new Angle(x, y, true));
                UI.drawLine(x*3,y*3,(x+1)*3,y*3);
            }
            if (x + 1 < sizeX && y + 1 < sizeY && points[x + 1][y + 1]) {
                angles.add(new Angle(x + 1, y + 1, true));
                angles.add(new Angle(x, y, true));
                UI.drawLine(x*3,y*3,(x+1)*3,(y+1)*3);
            }
            last = point;
            //angles.add(new Angle(x, y, true));
            //angles.add(new Angle(x, y, false));
            //angles.add(new Angle(x, y, false));
        }
        return angles;
    }

    private PointXY findNext(PointXY point, boolean[][] points, boolean[][] mask) {
        int x = point != null ? (int) point.get_x() : 0;
        int y = point != null ? (int) point.get_y() : 0;
        int nearX = -1;
        int nearY = -1;
        double dist = -1;

        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                if (points[i][j] && !mask[i][j]) {
                    double newDist = Point2D.distance(x,y,i,j);
                    if (newDist < dist || dist == -1) {
                        nearX = i;
                        nearY = j;
                        dist = newDist;
                    }
                }
            }
        }
        if (dist != -1) {
            mask[nearX][nearY] = true;
            return new PointXY(nearX, nearY, true);
        }
        return null;
    }

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
                            image[x][y] = true;
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

    public void savePwmFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(UIFileChooser.save("Save PWM file")));
            for (Angle angle : angles) {
                writer.printf("%d,%d,%d%n", angle.pwm1, angle.pwm2, angle.penDown ? 2000 : 1000);
            }
            writer.print("1500,1500,1000");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            arm.inverseKinematic(ox, oy);

            theta1 = arm.get_theta1();
            theta2 = arm.get_theta2();
            pwm1 = arm.get_pwm1();
            pwm2 = arm.get_pwm2();
            penDown = p;
        }
    }

    public static void main(String args[]) {
        Image img = new Image();
        img.savePwmFile();
        UI.println("DONE");
    }
}

package main;


import ecs100.UI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ToolPath stores motor control signals (pwm)
 * and motor angles
 * for given drawing and arm configuration.
 * Arm hardware takes sequence of pwm values
 * to drive the motors
 */
public class ToolPath {
    /**
     * Straight line segment will be broken into RESOLUTION per unit
     */
    private static final double RESOLUTION = 1.5;
    private static final int PEN_DOWN_PWM = 2000;
    private static final int PEN_UP_PWM = 1000;
    /**
     * Make sure the pen is up and at the right position for STARTING_IDLE_STEPS
     * before starting to draw. Should be a multiple of 2.
     */
    private static final int STARTING_IDLE_STEPS = 20;
    // into that many sections

    // storage for angles and
    // moto control signals
    private ArrayList<Double> theta1_vector = new ArrayList<Double>();
    private ArrayList<Double> theta2_vector = new ArrayList<Double>();
    private ArrayList<Integer> pen_vector = new ArrayList<Integer>();

    /**
     * Constructor for objects of class ToolPath
     */
    public ToolPath() {
    }

    /**********CONVERT (X,Y) PATH into angles******************/
    public void convert_drawing_to_angles(Drawing drawing, Arm arm) {
        for (int i = 0; i < drawing.get_drawing_size() - 1; i++) {
            // take two points
            PointXY p0 = drawing.get_drawing_point(i);
            PointXY p1 = drawing.get_drawing_point(i + 1);
            double distance = Math.hypot(p0.get_x() - p1.get_x(),
                    p0.get_y() - p1.get_y());
            int steps = (int) (distance * RESOLUTION);
            // break line between points into segments: `steps` of them
            for (int j = 0; j < steps; j++) { // break segment into RESOLUTION str. lines
                double x = p0.get_x() + j * (p1.get_x() - p0.get_x()) / steps;
                double y = p0.get_y() + j * (p1.get_y() - p0.get_y()) / steps;
                arm.inverseKinematic(x, y);
                theta1_vector.add(arm.get_theta1() * 180 / Math.PI);
                theta2_vector.add(arm.get_theta2() * 180 / Math.PI);
                if (p0.get_pen()) {
                    pen_vector.add(PEN_DOWN_PWM);
                } else {
                    pen_vector.add(PEN_UP_PWM);
                }
            }
        }
    }

    public void save_angles(String fname) {
        assert theta1_vector.size() == theta2_vector.size() :
                "Uneven amount of vectors for left and right arms";
        assert theta1_vector.size() == pen_vector.size() :
                "Wrong amount of pen vectors";

        for (int i = 0; i < theta1_vector.size(); i++) {
            UI.printf(" t1=%3.1f t2=%3.1f pen=%d\n",
                    theta1_vector.get(i), theta2_vector.get(i), pen_vector.get(i));
        }

        try {
            //Whatever the file path is.
            File statText = new File(fname);
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            String str_out;
            for (int i = 1; i < theta1_vector.size(); i++) {
                str_out = String.format("%3.1f,%3.1f,%d\n",
                        theta1_vector.get(i), theta2_vector.get(i), pen_vector.get(i));
                w.write(str_out);
            }
            w.close();
        } catch (IOException e) {
            UI.println("Problem writing to the file statsTest.txt");
        }

    }

    /**
     * Saves PWM to string in a comma separated values format.
     * Does not save the file
     *
     * Takes sequence of angles (I think the theta1_vector and similar vars) '
     * and converts it into sequence of motor signals
     *
     * @param arm The arm used to convert PWMs
     */
    public String getPWMString(Arm arm) {
        assert theta1_vector.size() == theta2_vector.size() :
                "Uneven amount of vectors for left and right arms";
        assert theta1_vector.size() == pen_vector.size() :
                "Wrong amount of pen vectors";
        assert hasEnoughDataPoints() : "Not enough points";

        List<Integer> pwm1_vector = new ArrayList<>();
        List<Integer> pwm2_vector = new ArrayList<>();
        List<Integer> pwm3_vector = new ArrayList<>();

        // Convert vectors to PWM
        for (int i = 0; i < theta1_vector.size(); i++) {
            arm.set_angles(theta1_vector.get(i), theta2_vector.get(i));
            pwm1_vector.add(arm.get_pwm1());
            pwm2_vector.add(arm.get_pwm2());
            pwm3_vector.add(pen_vector.get(i));
        }

        // Convert to string
        StringBuilder sb = new StringBuilder();

        // Add pen up at start to avoid drag
        for (int i = 0; i < STARTING_IDLE_STEPS / 2; i++) {
            sb.append(pwm1_vector.get(i))
                    .append(',')
                    .append(pwm2_vector.get(i))
                    .append(',')
                    .append(PEN_UP_PWM)
                    .append('\n');
        }
        for (int i = STARTING_IDLE_STEPS / 2; i > 0; i--) {
            sb.append(pwm1_vector.get(i))
                    .append(',')
                    .append(pwm2_vector.get(i))
                    .append(',')
                    .append(PEN_UP_PWM)
                    .append('\n');
        }

        for (int i = 0; i < pwm1_vector.size(); i++) {
            sb.append(pwm1_vector.get(i))
                    .append(',')
                    .append(pwm2_vector.get(i))
                    .append(',')
                    .append(pwm3_vector.get(i))
                    .append('\n');
        }

        // Add pen up
        int last = pwm1_vector.size() - 1;
        sb.append(pwm1_vector.get(last))
                .append(',')
                .append(pwm2_vector.get(last))
                .append(',')
                .append(PEN_UP_PWM);

        String result = sb.toString();
        assert result.split("\n").length == pwm1_vector.size()
                + STARTING_IDLE_STEPS + 1;
        return result;
    }

    boolean hasEnoughDataPoints() {
        return theta1_vector.size() > STARTING_IDLE_STEPS / 2;
    }

}

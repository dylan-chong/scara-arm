package main;

import ecs100.UI;

import java.awt.*;

/**
 * Class represents the set of SCARA robotic arms.
 */
public class Arm {

    /**
     * The limit of the drawing range
     * <p>
     * Temporary bounds (not expanded to maximium bounds)
     */
    static final double MIN_X = 270;
    static final double WIDTH = 150;
    static final double MIN_Y = 260;
    static final double HEIGHT = 130;

    // fixed arm parameters (coordinates of the motor
    // (measured in pixels of the picture))
    // left motor
    private static final int xm1 = 276;
    private static final int ym1 = 111;
    // right motor
    private static final int xm2 = 374;
    private static final int ym2 = 108;
    private static final double r = 156.0;  // length of the upper/fore arm
    // parameters of servo motors - linear function pwm(angle)
    // each of two motors has unique function which should be measured
    // linear function cam be described by two points
    // motor 1, point1 
    // private double pwm1_val_1;
    // private double theta1_val_1;
    // // motor 1, point 2
    // private double pwm1_val_2;
    // private double theta1_val_2;
    //
    // // motor 2, point 1
    // private double pwm2_val_1;
    // private double theta2_val_1;
    // // motor 2, point 2
    // private double pwm2_val_2;
    // private double theta2_val_2;


    // current state of the arm
    private double theta1; // angle of the upper arm
    private double theta2;

    private double xj1;     // positions of the joints
    private double yj1;
    private double xj2;
    private double yj2;
    private double xt;     // position of the tool
    private double yt;
    private boolean valid_state; // is state of the arm physically possible?

    /**
     * Constructor for objects of class Arm
     */
    Arm() {
        theta1 = -90.0 * Math.PI / 180.0; // initial angles of the upper arms
        theta2 = -90.0 * Math.PI / 180.0;

        inverseKinematic(MIN_X + (WIDTH / 2), MIN_Y + (HEIGHT / 2));
    }

    boolean isValid_state() {
        return valid_state;
    }

    /**
     * @return true if valid_state
     */
    boolean draw() {
        // draw arm
        int height = UI.getCanvasHeight();
        int width = UI.getCanvasWidth();
        xj1 = xm1 + r * Math.cos(theta1);
        yj1 = ym1 + r * Math.sin(theta1);
        xj2 = xm2 + r * Math.cos(theta2);
        yj2 = ym2 + r * Math.sin(theta2);

        //draw motors and write angles
        int mr = 20;
        UI.setLineWidth(5);
        UI.setColor(Color.BLUE);
        UI.drawOval(xm1 - mr / 2, ym1 - mr / 2, mr, mr);
        UI.drawOval(xm2 - mr / 2, ym2 - mr / 2, mr, mr);
        // write parameters of first motor
        String out_str = String.format("t1=%3.1f", theta1 * 180 / Math.PI);
        UI.drawString(out_str, xm1 - 2 * mr, ym1 - mr / 2 + 2 * mr);
        out_str = String.format("xm1=%d", xm1);
        UI.drawString(out_str, xm1 - 2 * mr, ym1 - mr / 2 + 3 * mr);
        out_str = String.format("ym1=%d", ym1);
        UI.drawString(out_str, xm1 - 2 * mr, ym1 - mr / 2 + 4 * mr);
        // ditto for second motor
        out_str = String.format("t2=%3.1f", theta2 * 180 / Math.PI);
        UI.drawString(out_str, xm2 + 2 * mr, ym2 - mr / 2 + 2 * mr);
        out_str = String.format("xm2=%d", xm2);
        UI.drawString(out_str, xm2 + 2 * mr, ym2 - mr / 2 + 3 * mr);
        out_str = String.format("ym2=%d", ym2);
        UI.drawString(out_str, xm2 + 2 * mr, ym2 - mr / 2 + 4 * mr);
        // draw Field Of View
        UI.setColor(Color.GRAY);
        UI.drawRect(0, 0, 640, 480);

        // it can b euncommented later when
        // kinematic equations are derived
        // draw upper arms
        UI.setColor(valid_state ? Color.GREEN : Color.RED);
        UI.drawLine(xm1, ym1, xj1, yj1);
        UI.drawLine(xm2, ym2, xj2, yj2);
        //draw forearms
        UI.drawLine(xj1, yj1, xt, yt);
        UI.drawLine(xj2, yj2, xt, yt);
        // draw tool
        double rt = 20;
        UI.drawOval(xt - rt / 2, yt - rt / 2, rt, rt);

        // Draw bounds
        UI.setColor(Color.GREEN);
        UI.setLineWidth(1);
        UI.drawRect(MIN_X, MIN_Y, WIDTH, HEIGHT);

        return valid_state;
    }

    /**
     * Calculate tool position from motor angles
     * updates variable in the class
     * <p>
     * Not used, but required by questions
     */
    void directKinematic() {

        // midpoint between joints
        double xA = xj1 + 0.5 * getXjDiff();
        double yA = yj1 + 0.5 * getYjDiff();

        // distance between joints
        double dXPart = Math.pow(getXjDiff(), 2);
        double dYPart = Math.pow(getYjDiff(), 2);
        double d = Math.sqrt(dXPart + dYPart);

        if (d < 2 * r) {
            valid_state = true;

            // half distance between tool positions
            double hRPart = Math.pow(r, 2);
            double hAPart = Math.pow(getXjDiff() / 2, 2) + Math.pow(getYjDiff() / 2, 2);
            double h = Math.sqrt(hRPart - hAPart);
            double alpha = Math.atan(getYjDiff() / getXjDiff());

            // tool position
            xt = xA + h * Math.cos(Math.PI / 2 - alpha);
            yt = yA + h * Math.sin(Math.PI / 2 - alpha);
        } else {
            valid_state = false;
        }
    }

    /**
     * Motor angles from tool position updetes variables of the class
     *
     * @param xt_new x position of the mouse
     * @param yt_new y position of the mouse
     * @return true iff valid_state === true
     */
    boolean inverseKinematic(double xt_new, double yt_new) {
        if (xt_new < MIN_X || yt_new < MIN_Y || xt_new > MIN_X + WIDTH
                || yt_new > MIN_Y + HEIGHT) {
            valid_state = false;
            return false;
        }

        xt = xt_new;
        yt = yt_new;

        valid_state = true;

        // distance between pen and motor
        double d1 = getDistanceBetweenPenAndMotor(1);
        if (d1 > 2 * r) {
            UI.println("Arm 1 - can not reach");
            valid_state = false;
            return false;
        }

        double l1 = d1 / 2;
        double h1 = Math.sqrt(Math.pow(r, 2) - Math.pow(l1, 2));

        // elbows positions
        double xA1 = xt + 0.5 * (xm1 - xt);
        double yA1 = yt + 0.5 * (ym1 - yt);
        double alpha1 = Math.atan((yt - ym1) / (xm1 - xt));

        xj1 = xA1 + h1 * Math.cos(Math.PI / 2 - alpha1) * (xt > xm1 ? 1 : -1);
        yj1 = yA1 + h1 * Math.sin(Math.PI / 2 - alpha1) * (xt > xm1 ? 1 : -1);

        theta1 = Math.atan2(yj1 - ym1, xj1 - xm1);
        if ((theta1 < -Math.PI) || (theta1 > Math.PI)) {
            UI.println("Angle 1 - invalid");
            valid_state = false;
            return false;
        }

        // distance between pen and motor
        double d2 = getDistanceBetweenPenAndMotor(2);
        if (d2 > 2 * r) {
            UI.println("Arm 2 - can not reach");
            valid_state = false;
            return false;
        }

        double l2 = d2 / 2;
        double h2 = Math.sqrt(Math.pow(r, 2) - Math.pow(l2, 2));

        // elbows positions
        double xA2 = xt + 0.5 * (xm2 - xt);
        double yA2 = yt + 0.5 * (ym2 - yt);
        double alpha2 = Math.atan((yt - ym2) / (xm2 - xt));

        xj2 = xA2 + h2 * Math.cos(Math.PI / 2 - alpha2) * (xt < xm2 ? 1 : -1);
        yj2 = yA2 + h2 * Math.sin(Math.PI / 2 - alpha2) * (xt < xm2 ? 1 : -1);

        theta2 = Math.atan2(yj2 - ym2, xj2 - xm2);
        if ((theta2 < -Math.PI) || (theta2 > Math.PI)) {
            UI.println("Angle 2 - invalid");
            valid_state = false;
            return false;
        }

        UI.printf("xt:%3.1f, yt:%3.1f\n", xt, yt);
        UI.printf("pwm1:%d, pwm2:%d\n", get_pwm1(), get_pwm2());
        return true;
    }

    // returns angle of motor 1
    double get_theta1() {
        return theta1;
    }

    // returns angle of motor 2
    double get_theta2() {
        return theta2;
    }

    // sets angle of the motors
    void set_angles(double t1, double t2) {
        theta1 = t1;
        theta2 = t2;
    }

    // returns motor control signal
    // for motor to be in position(angle) theta1
    // linear intepolation
    int get_pwm1() {
        int pwm = 0;
        pwm = (int) (11.498464 * Math.toDegrees(theta1) + 221.115182);
        return pwm;
    }

    // ditto for motor 2
    int get_pwm2() {
        int pwm = 0;
        pwm = (int) (10.63547 * Math.toDegrees(theta2) + 974.026574);
        //pwm = (int)(pwm2_90 + (theta2 - 90)*pwm2_slope);
        return pwm;
    }

    /**
     * Just as a shorthand
     *
     * @return
     */
    private double getXjDiff() {
        return xj2 - xj1;
    }

    /**
     * Just as a shorthand
     *
     * @return
     */
    private double getYjDiff() {
        return yj2 - yj1;
    }

    /**
     * @param motorNum 1 or 2
     * @return
     */
    private double getDistanceBetweenPenAndMotor(int motorNum) {
        double xPart;
        double yPart;
        switch (motorNum) {
            case 1:
                xPart = Math.pow(xt - xm1, 2);
                yPart = Math.pow(yt - ym1, 2);
                break;
            case 2:
                xPart = Math.pow(xt - xm2, 2);
                yPart = Math.pow(yt - ym2, 2);
                break;
            default:
                throw new IllegalArgumentException("motorNum must be 1 or 2");
        }
        return Math.sqrt(xPart + yPart);
    }
}

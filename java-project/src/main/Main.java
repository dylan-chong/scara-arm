package main;

/* Code for Assignment ?? 
 * Name:
 * Usercode:
 * ID:
 */

// TODO DYLAN:
// TODO pen up at end/start, n stsp
// TODO fix auto send data
// TODO find rect bounds

import ecs100.UI;
import ecs100.UIFileChooser;

import java.awt.*;


/**
 * <description of class Main>
 */
public class Main {

    private Arm arm;
    private Drawing drawing;

    // state of the GUI
    private int state; // 0 - nothing
    // 1 - inverse point kinematics - point
    // 2 - enter path. Each click adds point
    // 3 - enter path pause. Click does not add the point to the path

    /**      */
    public Main() {
        UI.initialise();
        UI.addButton("xy to angles", this::inverse);
        UI.addButton("Enter path XY", this::enter_path_xy);
        UI.addButton("Save path XY", this::save_xy);
        UI.addButton("Load path XY", this::load_xy);
        UI.addButton("Save path Ang", this::save_ang);
        UI.addButton("Load path Ang:Play", this::load_ang);
        UI.addButton("Send PWM pulses to Pi", this::sendPWMToPi);

        UI.setMouseMotionListener(this::doMouse);
        UI.setKeyListener(this::doKeys);

        UI.setWindowSize(1400, 800);
        UI.setDivider(0.3);

        //ServerSocket serverSocket = new ServerSocket(22); 

        this.arm = new Arm();
        this.drawing = new Drawing();
        this.run();
        arm.draw();
    }

    public static void main(String[] args) {
        Main obj = new Main();
    }


    public void doKeys(String action) {
        UI.printf("Key :%s \n", action);
        if (action.equals("b")) {
            // break - stop entering the lines
            state = 3;
            //

        }

    }

    public void doMouse(String action, double x, double y) {
        //UI.printf("Mouse Click:%s, state:%d  x:%3.1f  y:%3.1f\n",
        //   action,state,x,y);
        UI.clearGraphics();
        String out_str = String.format("%3.1f %3.1f", x, y);
        UI.drawString(out_str, x + 10, y + 10);
        //
        if ((state == 1) && (action.equals("clicked"))) {
            // draw as

            arm.inverseKinematic(x, y);
            arm.draw();
            return;
        }

        if (((state == 2) || (state == 3)) && action.equals("moved")) {
            // draw arm and path
            arm.inverseKinematic(x, y);
            arm.draw();

            // draw segment from last entered point to current mouse position
            if ((state == 2) && (drawing.get_path_size() > 0)) {
                PointXY lp = new PointXY();
                lp = drawing.get_path_last_point();
                //if (lp.get_pen()){
                UI.setColor(Color.GRAY);
                UI.drawLine(lp.get_x(), lp.get_y(), x, y);
                // }
            }
            drawing.draw();
        }

        // add point
        if ((state == 2) && (action.equals("clicked"))) {
            // add point(pen down) and draw
            UI.printf("Adding point x=%f y=%f\n", x, y);
            drawing.add_point_to_path(x, y, true); // add point with pen down

            arm.inverseKinematic(x, y);
            arm.draw();
            drawing.draw();
            drawing.print_path();
        }


        if ((state == 3) && (action.equals("clicked"))) {
            // add point and draw
            //UI.printf("Adding point x=%f y=%f\n",x,y);
            drawing.add_point_to_path(x, y, false); // add point wit pen up

            arm.inverseKinematic(x, y);
            arm.draw();
            drawing.draw();
            drawing.print_path();
            state = 2;
        }


    }

    public void save_xy() {
        state = 0;
        String fname = UIFileChooser.save();
        if (fname == null || fname.length() == 0) return;
        drawing.save_path(fname);
    }

    public void enter_path_xy() {
        state = 2;
    }

    public void inverse() {
        state = 1;
        arm.draw();
    }

    public void load_xy() {
        state = 0;
        String fname = UIFileChooser.open();
        if (fname == null || fname.length() == 0) return;

        drawing.load_path(fname);
        drawing.draw();

        arm.draw();
    }

    // save angles into the file
    public void save_ang() {
        String fname = UIFileChooser.save("angles.txt");
        if (fname == null || fname.length() == 0) return;

        ToolPath tp = createToolPath();
        tp.save_angles(fname);
    }

    public void load_ang() {
        UI.println("This does nothing at the moment");
    }

    public void run() {
        while (true) {
            arm.draw();
            UI.sleep(20);
        }
    }

    /**
     * Note: creates temporary graphical arm glitch
     * @return A new ToolPath object with all the data from Drawing
     */
    private ToolPath createToolPath() {
        ToolPath tp = new ToolPath();
        tp.convert_drawing_to_angles(drawing, arm);

        if (tp.hasAnyDataPoints())
            UI.println("[TOOLPATH] Ignore graphical glitch please");
        return tp;
    }

    private void sendPWMToPi() {
        ToolPath tp = createToolPath();
        if (!tp.hasAnyDataPoints()) {
            UI.println("[PWM] No data points to send");
            return;
        }

        UI.println("[PWM] Attempting to send data...");

        try {
            PiController.getInstance().sendDataToPi(
                    tp.getPWMString(arm),
                    () -> UI.println("[PWM] Probably successfully sending data")
            );
        } catch (Exception e) {
            UI.println("[PWM] Error could not send data to Pi:\n" + e);
            e.printStackTrace();
        }
    }
}

package main;

/* Code for Assignment ?? 
 * Name:
 * Usercode:
 * ID:
 */

// TODO BRANDON:
// TODO find rect bounds (maybe try draw grid and see wonkiness or something)
// TODO fix drawing comes out horizontally flipped

import ecs100.UI;
import ecs100.UIFileChooser;

import java.awt.*;

/**
 * <description of class Main>
 */
public class Main {

    /**
     * Decides max frame rate (1000 / fps)
     */
    private static final int MIN_FRAME_INTERVAL = 1000 / 60;

    private Arm arm;
    private Drawing drawing;

    // state of the GUI
    private int state; // 0 - nothing
    // 1 - inverse point kinematics - point
    // 2 - enter path. Each click adds point

    /**
     * For reducing CPU load due to drawing
     */
    private int drawEventCounter = 0;
    private boolean isDrawing = false;

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
        UI.addButton("Enter test shape", this::addTestShape);
        UI.addButton("Enter point", this::enterPoint);

        UI.setMouseMotionListener(this::doMouse);
        UI.setKeyListener(this::doKeys);

        UI.setWindowSize(1400, 800);
        UI.setDivider(0.3);

        //ServerSocket serverSocket = new ServerSocket(22); 

        this.arm = new Arm();
        this.drawing = new Drawing();
        draw();
    }

    public static void main(String[] args) {
        Main obj = new Main();
    }

    private void addTestShape() {
        final int LEFT = 270;
        final int RIGHT = 370;
        final int TOP = 270;
        final int BOTTOM = 370;

        enter_path_xy();
        // Rect
        addPoint(LEFT, TOP);
        addPoint(LEFT, BOTTOM);
        addPoint(RIGHT, BOTTOM);
        addPoint(RIGHT, TOP);
        addPoint(LEFT, TOP);
        // Cross
        addPoint(RIGHT, BOTTOM);
        addPoint(LEFT, BOTTOM);
        addPoint(RIGHT, TOP);
    }

    private void enterPoint() {
        enter_path_xy();
        addPoint(UI.askInt("Enter x"), UI.askInt("Enter y"));
    }

    private void addPoint(int x, int y) {
        doMouse("moved", x, y);
        doMouse("clicked", x, y);
    }

    public void doKeys(String action) {
        UI.printf("Key :%s \n", action);
        if (action.equals("b")) {
            // Press b key after clicking at the point you want to
            // toggle the pen at
            if (drawing.togglePen()) UI.println("Pen is down");
            else UI.println("Pen is up");
        }
    }

    public void doMouse(String action, double x, double y) {
        //UI.printf("Mouse Click:%s, state:%d  x:%3.1f  y:%3.1f\n",
        //   action,state,x,y);
        UI.clearGraphics();
        String out_str = String.format("%3.1f %3.1f", x, y);
        UI.drawString(out_str, x + 10, y + 10);

        if ((state == 1) && (action.equals("clicked"))) {
            // draw as

            arm.inverseKinematic(x, y);
            return;
        }

        if (state == 2 && action.equals("moved")) {
            // draw arm and path
            arm.inverseKinematic(x, y);

            // draw segment from last entered point to current mouse position
            if (drawing.get_path_size() > 0) {
                PointXY lp = drawing.get_path_last_point();
                //if (lp.get_pen()){
                UI.setColor(Color.GRAY);
                UI.drawLine(lp.get_x(), lp.get_y(), x, y);
                // }
            }
        }

        // add point
        if ((state == 2) && (action.equals("clicked"))) {
            // add point(pen down) and draw
            if (arm.isValid_state()) {
                UI.printf("Adding point x=%f y=%f\n", x, y);
                drawing.add_point_to_path(x, y); // add point with pen down

                arm.inverseKinematic(x, y);
                drawing.print_path();
            }
        }

        draw();
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
        draw();
    }

    public void load_xy() {
        state = 0;
        String fname = UIFileChooser.open();
        if (fname == null || fname.length() == 0) return;

        drawing.load_path(fname);

        draw();
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

    void draw() {
        if (isDrawing) {
            drawEventCounter++;
            return;
        }
        isDrawing = true;
        if (drawEventCounter == 0) drawEventCounter++;
        new Thread(() -> {
            while (drawEventCounter > 0) {
                long preDrawTime = System.currentTimeMillis();

                if (!arm.draw()) UI.println("Invalid arm position (cannot draw)");
                drawing.draw();
                drawEventCounter--;

                long duration = System.currentTimeMillis() - preDrawTime;
                if (duration > MIN_FRAME_INTERVAL) continue;

                UI.sleep(MIN_FRAME_INTERVAL - duration);
            }

            isDrawing = false;
        }).run();
    }

    /**
     * Note: creates temporary graphical arm glitch
     *
     * @return A new ToolPath object with all the data from Drawing
     */
    private ToolPath createToolPath() {
        ToolPath tp = new ToolPath();
        tp.convert_drawing_to_angles(drawing, arm);
        return tp;
    }

    private void sendPWMToPi() {
        ToolPath tp = createToolPath();
        if (!tp.hasEnoughDataPoints()) {
            UI.println("[PWM] Not enough data points to send");
            return;
        }

        UI.println("[PWM] Attempting to send data...");

        try {
            PiController.getInstance().sendDataToPi(
                    tp.getPWMString(arm),
                    () -> UI.println("[PWM] Hopefully successfully sent data")
            );
        } catch (Exception e) {
            UI.println("[PWM] Error could not send data to Pi:\n" + e);
            e.printStackTrace();
        }
    }
}

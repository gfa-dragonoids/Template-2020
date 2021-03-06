package org.firstinspires.ftc.teamcode;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.SwitchableLight;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

/**
 * This file contains an example of an iterative (Non-Linear) "OpMode".
 * An OpMode is a 'program' that runs in either the autonomous or the teleop period of an FTC match.
 * The names of OpModes appear on the menu of the FTC Driver Station.
 * When an selection is made from the menu, the corresponding OpMode
 * class is instantiated on the Robot Controller and executed.
 *
 * This particular OpMode just executes a basic Tank Drive Teleop for a two wheeled robot
 * It includes all the skeletal structure that all iterative OpModes contain.
 *
 * Use Android Studios to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list
 */

@TeleOp(name="Mecanum2020", group="TeleOp")
public class Mechybois2020 extends OpMode {

    // Global Variables
    static final double TICKS_PER_ROTATION = 1120.0 * 0.75;

    // Declare OpMode members.
    private ElapsedTime runtime = new ElapsedTime();

    // drive
    private DcMotor lf = null;
    private DcMotor rf = null;
    private DcMotor lb = null;
    private DcMotor rb = null;
    
    // controller values
    private float reducedMovementMultiplier = 0.2f;
    private float controllerDriftReductionThreshold = 0.05f;

    // arm
    private DcMotor extend = null;
    private Servo grab = null;
    private DcMotor rotateL = null;
    private DcMotor rotateR = null;

        // Arm Safety System
        public boolean armRaised = false;
        public boolean firstTick = false;
        public float armRotationTolerance = 10;

    //movePlatform
    private Servo movePlat1 = null;
    private Servo movePlat2 = null;

    /*
     * Code to run ONCE when the driver hits INIT
     */
    @Override
    public void init() {

        // Get the Motors to Drive the Movement System
        lf = hardwareMap.get(DcMotor.class, "lf");
        lb = hardwareMap.get(DcMotor.class, "lb");
        rf = hardwareMap.get(DcMotor.class, "rf");
        rb = hardwareMap.get(DcMotor.class, "rb");

        // Get the motors that control the Arm Mechanism
        extend = hardwareMap.get(DcMotor.class, "extend");
        grab = hardwareMap.get(Servo.class, "grab");
        rotateL = hardwareMap.get(DcMotor.class, "rotateL");
        rotateR = hardwareMap.get(DcMotor.class, "rotateR");

        // Get the Motors that control the Platform Grabbing System
        movePlat1 = hardwareMap.get(Servo.class, "movePlat1");
        movePlat2 = hardwareMap.get(Servo.class, "movePlat2");

        // Set the direction of the Driving Motors
            // REASON: For the Mechanim Wheels to work simply, we Invert the Left Wheels.
        lf.setDirection(DcMotor.Direction.REVERSE);
        lb.setDirection(DcMotor.Direction.REVERSE);
        rf.setDirection(DcMotor.Direction.FORWARD);
        rb.setDirection(DcMotor.Direction.FORWARD);

        // Make it so that if there is no power to motors, they break.
            // REASON: Makes the robot stop much faster.
        rf.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rb.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        lf.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        lb.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Make the Motors so they run using the Encoder
            // REASON: This Leads To More Dependable Movement/ We are Now Able to Track Our Movement
        lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        lb.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rb.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Log the Status of the Robot and Tell the Driver that We Are Ready
            // REASON: It adds a bit more fun to the robot.
            // ALSO: Sorry Ethan, It was Too Much Fun.

        String[] possibleSayings = new String[]{"Let's roll.", "Ready To Rumble.", "Beep Boop.", "Taking Over The World", "About to Win The Contest"};
        telemetry.addData("Status", possibleSayings[(int)(Math.random() * possibleSayings.length)]);
    }

    // Implement inherited initial loop function.
    @Override
    public void init_loop() {
    
    }

    // Implement inherited start function.
    @Override
    public void start() {
    
        // When we start, we want to reset the runtime information
        runtime.reset();
    
    }


    /* CONTROLS LIST

    DRIVE
    G1 left stick y             STRAIGHT
    G1 left stick x             STRAFE
    G1 right stick x            ROTATE
    G1 X                        SLOW

     */

    // Implement Inherited Loop Function.
    
    @Override
    public void loop() {

        //region drivetrain power
        
        // Set the Movement Variables to Scaled Input Values
            // REASON: If We Give the Variables the Gamepad Inputs Directly, it Will Not Scale Correctly by Itself
        
        float drive = scaleInput(-gamepad1.left_stick_y);
        float strafe = scaleInput(gamepad1.left_stick_x);
        float rotate = scaleInput(gamepad1.right_stick_x);

        // Log Information About the Movement that we are Doing Currently.
            // REASON: This Lets the Drivers Know that if there is a Problem, it is not the Controllers.
        
        telemetry.addData("drive", + drive);
        telemetry.addData("strafe", + strafe);
        telemetry.addData("rotate", + rotate);

        // Round Down the Variables if they are Close to Certain Thresholds
            // REASON: This Reduces the Amount of Drift that can Accur in the Controllers. 
        
        if(Math.abs(drive) < controllerDriftReductionThreshold) drive = 0.0f;
        if(Math.abs(strafe) < controllerDriftReductionThreshold) strafe = 0.0f;
        if(Math.abs(rotate) < controllerDriftReductionThreshold) rotate = 0.0f;

        // Check if the "X" Button is Being Pressed on the Driving Controller
            // REASON: We Want the Robot to Move Much Slower if "X" is Pressed
        
        if(!gamepad1.x) {
            
            // Set the Driving Values for the Motors
            
            lf.setPower(Range.clip(drive + strafe + rotate, -1.0, 1.0));
            lb.setPower(Range.clip(drive - strafe + rotate, -1.0, 1.0));
            rf.setPower(Range.clip(drive - strafe - rotate, -1.0, 1.0));
            rb.setPower(Range.clip(drive + strafe - rotate, -1.0, 1.0));
        
        } else {
            
            // Set the Reduced Driving Values for the Motors
            
            lf.setPower(reducedMovementMultiplier * Range.clip(drive + strafe + rotate, -1.0, 1.0));
            lb.setPower(reducedMovementMultiplier * Range.clip(drive - strafe + rotate, -1.0, 1.0));
            rf.setPower(reducedMovementMultiplier * Range.clip(drive - strafe - rotate, -1.0, 1.0));
            rb.setPower(reducedMovementMultiplier * Range.clip(drive + strafe - rotate, -1.0, 1.0));
        
        }
        
        //endregion

        //arm

        // Check if the D-Pad Up or D-Pad Down is Being Pressed on Either Controller
        
        if(gamepad2.dpad_up || gamepad1.dpad_up) {
        
            // If the D-Pad is Being Pressed Up, We Want to Extend the Arm
            extend.setPower(1);
            
        } else if (gamepad2.dpad_down || gamepad1.dpad_down) {
        
            // If the D-Pad is Being Pressed Down, We Want to Retract the Arm 
            extend.setPower(-1);
        
        } else {
        
            // If None of these Buttons are Pressed, We don't Want to do Anything.
            extend.setPower(0);
        
        }
        
        // Check if the Left or Right Bumper are Being Pressed.
        
        if (gamepad2.left_bumper) {
           
            // If The Left Bumper is Pressed, We Want it to Clamp Down.
            grab.setPosition(0);
        
        } else if (gamepad2.right_bumper) {
            
            // If The Right Bumper is Pressed, We Want it to Open Up.
            grab.setPosition(1);

        }

       /*

        // See if the button is pressed to change if the arm is raised or not
        if(gamepad2.a){

            // Set the arm raised status to the opposite of what it originally was. I.E. a toggle button.
            armRaised = !armRaised;

            // Set the first tick variable to true. This makes it so that we can reset the encoders.
            firstTick = true;

        }

        // See if the button is pressed to change if the arm is raised or not
        if(gamepad2.b){

            // Set the arm raised status to the opposite of what it originally was. I.E. a toggle button.
            armRaised = !armRaised;

            // Set the first tick variable to true. This makes it so that we can reset the encoders.
            firstTick = true;

        }

        // Check if arm is raised or not
        if (armRaised) {

            rotateL.setPower(0.5f);
            rotateR.setPower(0.5f);

        } else {



            rotateL.setPower(-0.5f);
            rotateR.setPower(-0.5f);

        }

        telemetry.addData("rotateL", rotateR.getCurrentPosition() * TICKS_PER_ROTATION);
        telemetry.addData("rotateR", rotateL.getCurrentPosition() * TICKS_PER_ROTATION);

        if (firstTick) {

            // Reset all of the rotation motor encoders, that means that we can track the difference much easier.
            rotateL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            rotateR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

            // rotateL.setTargetPosition(0);
            // rotateR.setTargetPosition(0);

            // rotateL.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            // rotateR.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            if (armRaised) {



            } else {



            }

            // Set firstTick to false because afterwards, it is no longer the first tick
            firstTick = false;

        }

        */

        // Check if the "B" Button is Pressed, or if the "A" Button is Pressed.
        if (gamepad2.b) {

            // If "B" is Pressed, Rotate Up the Crane.
                // NOTE: The "rotateR" Motor is inverted.g
            rotateL.setPower(0.5);
            rotateR.setPower(-0.5);

        } else if (gamepad2.a) {

            // If "B" is Pressed, Rotate Down the Crane.
                // NOTE: The "rotateR" Motor is inverted.
            rotateL.setPower(-0.5);
            rotateR.setPower(0.5);

        } else {

            // If Nothing Happens, Don't Move the Motors at All
            rotateL.setPower(0);
            rotateR.setPower(0);

        }

        // Check if the Right Bumper or Left Bumper Are Pressed.
        if (gamepad1.right_bumper) {
        
            // If the Right Bumper is Pressed, Move Down the Platform Grabber Hands
            movePlat1.setPosition(1);
            movePlat2.setPosition(.5);

        } else if (gamepad1.left_bumper) {
            
            // If the Left Bumper is Pressed, Move Up the Platform Grabber Hands
            movePlat1.setPosition(0);
            movePlat2.setPosition(1);
        
        }

        //region telemetry
        
        // Log All of the Movement Data.
            // REASON: This Allows the Driver to See if the Motors are Working or Not
        telemetry.addData("Status", "Run Time: " + runtime.toString());
        telemetry.addData("rb power", + rb.getPower());
        telemetry.addData("rf power", + rf.getPower());
        telemetry.addData("lf power", + lf.getPower());
        telemetry.addData("lb power", + lb.getPower());

        // Log Position of Movement Data
            // REASON: This Helps the Driver see Where the Motors are Currently Positioned
        telemetry.addData("rb pos", + rb.getCurrentPosition());
        telemetry.addData("rf pos", + rf.getCurrentPosition());
        telemetry.addData("lf pos", + lf.getCurrentPosition());
        telemetry.addData("lb pos", + lb.getCurrentPosition());

        //telemetry.addData("Motors", "left (%.2f), right (%.2f)", leftPower, rightPower);
        
        // Push Telementry Data to Phone Display
        telemetry.update();
        
        //endregion
    }



    //Implement Inherited "Stop" Function
    @Override
    public void stop() {
    
        // Create an Array of Possible Sayings the Robot can Say When it Shuts Down
        String[] possibleSayings = new String[]{"Goodbye", "Sweet Dreams", "Boop Beep.", "No Longer Taking Over The World", "Thinking About Our Win", "Preparing for the Post-Win Party"};
        telemetry.addData("Status", possibleSayings[(int)(Math.random() * possibleSayings.length)]);
        
    }

    // Function to Scale the Inputs
        // REASON: This makes Inputs Take a Parabolic Shape
    float scaleInput(float in) {

        // Create a Variable Named "out" that is "in" to the Power of Two
        float out = in*in;

        // Check if "in" is a Negative Number. If it is, make "out" into a Negative Number
            // REASON: The Negative Value of "in" multiplied by itself will result in a positive.
            // REASON: If it is a negative number, we have to make sure the result is negative.
        if (in < 0)
            out = -out;
        
        // We Want to Return the End Result of this Function.
        return out;
        
    }
    
    // TODO: Make Another Function that Scales Inputs with a Different Function

    float smartScaleInput (in) {

        float[] smartScale = { -1f, -1f, -0.9f, -0.5f, -0.1f, 0, 0.1f, 0.5f, 0.9f, 1f, 1f };
        float out = 0;
    
        int closest = -1;
        
        for (int i = 0; i < smartScale.length; i++) {
        
            if (Math.Abs(smartScale[closest] - in) > Math.Abs(smartScale[i] - in)) {
            
                closest = i;
                
            }
        
        }    
        
        if (closest == smartScale.length - 1) {
        
            out = lerp(closest, closest - 1, (smartScale[closest] - in) + (smartScale[closest - 1] - in));
        
        } else {
        
            out = lerp(closest, closest + 1, (smartScale[closest] - in) + (smartScale[closest + 1] - in));
        
        }
     
        return out;
        
    }
    
    float lerp(start, end, t) {
    
        return start * (1 - t) + end * t;
    
    }
    
}

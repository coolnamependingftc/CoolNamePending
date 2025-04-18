package bot.fsmExamples;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name = "FSM Slide Control", group = "FSM")
public class fsmBetaTele extends LinearOpMode {

    // FSM States
    public enum SlideState {
        HOME,
        INTAKE,
        OUTTAKE
    }

    private SlideState currentState = SlideState.HOME;

    // Motors
    private DcMotor frontLeft, frontRight, backLeft, backRight = null;
    private DcMotor misumiSlide, linearSlide = null;

    // Dynamic positions (can be changed during TeleOp)
    private int homePos = 0;
    private int intakePos = 600;
    private int outtakePos = 1200;

    @Override
    public void runOpMode() {
        // Hardware map
        frontLeft = hardwareMap.get(DcMotor.class, "fl");
        frontRight = hardwareMap.get(DcMotor.class, "fr");
        backLeft = hardwareMap.get(DcMotor.class, "bl");
        backRight = hardwareMap.get(DcMotor.class, "br");

        misumiSlide = hardwareMap.get(DcMotor.class, "ms");
        linearSlide = hardwareMap.get(DcMotor.class, "ls");

        // Reverse right side
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        // Init slides
        for (DcMotor slide : new DcMotor[]{misumiSlide, linearSlide}) {
            slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            slide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        telemetry.addLine("Ready - Waiting for Start");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            // ----------- DRIVE CONTROL (gamepad1) -----------
            double drive = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double turn = gamepad1.right_stick_x;

            double fl = drive + strafe + turn;
            double fr = drive - strafe - turn;
            double bl = drive - strafe + turn;
            double br = drive + strafe - turn;

            frontLeft.setPower(Range.clip(fl, -1.0, 1.0));
            frontRight.setPower(Range.clip(fr, -1.0, 1.0));
            backLeft.setPower(Range.clip(bl, -1.0, 1.0));
            backRight.setPower(Range.clip(br, -1.0, 1.0));

            // ----------- SLIDE FSM CONTROL (gamepad2 D-pad) -----------
            if (gamepad2.dpad_down) setSlideState(SlideState.HOME);
            else if (gamepad2.dpad_left) setSlideState(SlideState.INTAKE);
            else if (gamepad2.dpad_up) setSlideState(SlideState.OUTTAKE);

            // ----------- SLIDE MANUAL OVERRIDE (gamepad2 stick) -----------
            double manualSlidePower = -gamepad2.left_stick_y;
            manualSlidePower = Range.clip(manualSlidePower, -1.0, 1.0);

            // Only allow manual control if slides aren't moving to a position
            if (!misumiSlide.isBusy() && !linearSlide.isBusy()) {
                for (DcMotor slide : new DcMotor[]{misumiSlide, linearSlide}) {
                    slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    slide.setPower(manualSlidePower);
                }
            }

            // ----------- SAVE POSITIONS (gamepad2 A/B/Y) -----------
            if (gamepad2.a) {
                homePos = misumiSlide.getCurrentPosition();
                telemetry.addLine("Saved HOME position.");
            }
            if (gamepad2.b) {
                intakePos = misumiSlide.getCurrentPosition();
                telemetry.addLine("Saved INTAKE position.");
            }
            if (gamepad2.y) {
                outtakePos = misumiSlide.getCurrentPosition();
                telemetry.addLine("Saved OUTTAKE position.");
            }

            // ----------- TELEMETRY -----------
            telemetry.addData("FSM State", currentState);
            telemetry.addData("Misumi Slide Pos", misumiSlide.getCurrentPosition());
            telemetry.addData("Linear Slide Pos", linearSlide.getCurrentPosition());
            telemetry.addData("Targets", "HOME: %d | INTAKE: %d | OUTTAKE: %d", homePos, intakePos, outtakePos);
            telemetry.update();
        }
    }

    // FSM Slide Transition
    private void setSlideState(SlideState newState) {
        if (newState != currentState) {
            currentState = newState;
            int target;

            switch (newState) {
                case HOME:
                    target = homePos;
                    break;
                case INTAKE:
                    target = intakePos;
                    break;
                case OUTTAKE:
                    target = outtakePos;
                    break;
                default:
                    target = 0;
            }

            for (DcMotor slide : new DcMotor[]{misumiSlide, linearSlide}) {
                slide.setTargetPosition(target);
                slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                slide.setPower(0.7);
            }
        }
    }
}

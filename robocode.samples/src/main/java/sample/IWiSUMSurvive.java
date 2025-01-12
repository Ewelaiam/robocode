package sample;

import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.norm.s.DrasticSum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.*;

import java.awt.*;

public class IWiSUMSurvive extends AdvancedRobot {
    private boolean movingForward;
    private Engine engine;
    private InputVariable enemyDistance;
    private OutputVariable velocity;

    @Override
    public void run() {
        initializeFuzzyLogic();
        customizeAppearance();

        while (true) {
            fullScan();
            moveAhead();
        }
    }

    private void initializeFuzzyLogic() {
        engine = new Engine("EnergyManagement");

        // Input Variable: Enemy Distance
        enemyDistance = new InputVariable("enemyDistance", 0, 1000.0);
        enemyDistance.addTerm(new Ramp("close", 0.0, 600));
        enemyDistance.addTerm(new Ramp("far", 600, 0));
        engine.addInputVariable(enemyDistance);

        // Output Variable: Velocity
        velocity = new OutputVariable("velocity", 1.0, Rules.MAX_VELOCITY);
        velocity.fuzzyOutput().setAggregation(new DrasticSum());
        velocity.setDefuzzifier(new WeightedAverage());
        velocity.addTerm(new Ramp("slow", 1.0, Rules.MAX_VELOCITY));
        velocity.addTerm(new Ramp("fast", Rules.MAX_VELOCITY, 1.0));
        engine.addOutputVariable(velocity);

        // Rule Block
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setImplication(new AlgebraicProduct());
        ruleBlock.setActivation(new General());
        ruleBlock.addRule(Rule.parse("if enemyDistance is close then velocity is fast", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far then velocity is slow", engine));
        engine.addRuleBlock(ruleBlock);
    }

    private void customizeAppearance() {
        setBodyColor(new Color(140, 130, 50));
        setGunColor(new Color(4, 150, 50));
        setRadarColor(new Color(30, 200, 15));
        setBulletColor(new Color(255, 123, 100));
        setScanColor(new Color(5, 200, 4));
    }

    private void fullScan() {
        for (int i = 0; i < 30; i++) {
            turnGunLeft(12);
        }
    }

    private void moveAhead() {
        setAhead(40000);
        movingForward = true;
    }

    private void reverseDirection() {
        if (movingForward) {
            setBack(40000);
        } else {
            setAhead(40000);
        }
        movingForward = !movingForward;
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        reverseDirection();
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        enemyDistance.setValue(e.getDistance());
        engine.process();
        setMaxVelocity(velocity.getValue());
        fire(1);
        adjustHeading(e.getHeading());
    }

    private void adjustHeading(double enemyHeading) {
        double desiredHeading = (enemyHeading + 90) % 360;
        double turnDegree = desiredHeading - getHeading();
        if (turnDegree > 0) {
            turnRight(turnDegree);
        } else {
            turnLeft(-turnDegree);
        }
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        if (e.isMyFault()) {
            reverseDirection();
        }
    }
}

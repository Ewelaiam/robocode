package sample;

import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.WeightedAverage;
import com.fuzzylite.norm.s.DrasticSum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.norm.t.Minimum;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.term.Trapezoid;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import robocode.*;

import java.awt.*;

public class IWiSUMMerged extends AdvancedRobot {
    private boolean movingForward;
    private Engine engine;
    private InputVariable enemyDistance;
    private InputVariable myEnergy;
    private OutputVariable shootEnergy;
    private OutputVariable velocity;

    private void initializeFuzzyLogic() {
        FuzzyLite.setDebugging(true);
        engine = new Engine();
        engine.setName("EnergyManagement");

        // Define Input: My Energy
        myEnergy = new InputVariable();
        myEnergy.setName("myEnergy");
        myEnergy.setRange(0, 100);
        myEnergy.addTerm(new Ramp("lowEnergy", 0.0, 40.0));
        myEnergy.addTerm(new Ramp("highEnergy", 100.0, 30.0));
        engine.addInputVariable(myEnergy);

        // Define Output: Shoot Energy
        shootEnergy = new OutputVariable();
        shootEnergy.setName("shootEnergy");
        shootEnergy.setRange(1, Rules.MAX_BULLET_POWER);
        shootEnergy.fuzzyOutput().setAggregation(new DrasticSum());
        shootEnergy.setDefuzzifier(new WeightedAverage());
        shootEnergy.addTerm(new Ramp("shootLowEnergy", 1.0, Rules.MAX_BULLET_POWER));
        shootEnergy.addTerm(new Trapezoid("shootHighEnergy", Rules.MAX_BULLET_POWER, Rules.MAX_BULLET_POWER, 2.0, 1.0));
        engine.addOutputVariable(shootEnergy);

        // Define Input: Enemy Distance
        enemyDistance = new InputVariable();
        enemyDistance.setName("enemyDistance");
        enemyDistance.setRange(0, 1000.0);
        enemyDistance.addTerm(new Ramp("close", 0.0, 600));
        enemyDistance.addTerm(new Ramp("far", 600, 0));
        engine.addInputVariable(enemyDistance);

        // Define Output: Velocity
        velocity = new OutputVariable();
        velocity.setName("velocity");
        velocity.setRange(1.0, Rules.MAX_VELOCITY);
        velocity.fuzzyOutput().setAggregation(new DrasticSum());
        velocity.setDefuzzifier(new WeightedAverage());
        velocity.addTerm(new Ramp("slow", 1.0, Rules.MAX_VELOCITY));
        velocity.addTerm(new Ramp("fast", Rules.MAX_VELOCITY, 1.0));
        engine.addOutputVariable(velocity);

        // Define Rule Block
        RuleBlock ruleBlock = new RuleBlock();
        ruleBlock.setConjunction(new Minimum());
        ruleBlock.setImplication(new AlgebraicProduct());
        ruleBlock.setActivation(new General());

        ruleBlock.addRule(Rule.parse("if enemyDistance is close and myEnergy is lowEnergy then shootEnergy is shootLowEnergy", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is close and myEnergy is highEnergy then shootEnergy is shootHighEnergy", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far and myEnergy is lowEnergy then shootEnergy is shootLowEnergy", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far and myEnergy is highEnergy then shootEnergy is shootLowEnergy", engine));

        ruleBlock.addRule(Rule.parse("if enemyDistance is close then velocity is fast", engine));
        ruleBlock.addRule(Rule.parse("if enemyDistance is far then velocity is slow", engine));

        engine.addRuleBlock(ruleBlock);
    }

    public void run() {
        initializeFuzzyLogic();
        setColors(new Color(217, 181, 0), new Color(0, 0, 0), new Color(138, 135, 135), new Color(255, 0, 0), new Color(255, 206, 101));

        movingForward = true;
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        while (true) {
            fullScan();
            if (movingForward) {
                setAhead(40000);
            } else {
                setBack(40000);
            }
            execute();
        }
    }

    private void fullScan() {
        setTurnRadarRight(360);
        execute();
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        enemyDistance.setValue(e.getDistance());
        myEnergy.setValue(getEnergy());
        engine.process();

        double newVelocity = velocity.getValue();
        setMaxVelocity(newVelocity);

        double calculatedShootEnergy = shootEnergy.getValue();
        if (!Double.isNaN(calculatedShootEnergy)) {
            fire(calculatedShootEnergy);
        } else {
            fire(1);
        }

        // Improved movement: Strafe around enemy
        double angleToEnemy = e.getBearing();
        setTurnRight(angleToEnemy + 90 - 30 * (movingForward ? 1 : -1));
    }

    public void onHitWall(HitWallEvent e) {
        reverseDirection();
    }

    public void onHitRobot(HitRobotEvent e) {
        reverseDirection();
    }

    private void reverseDirection() {
        if (movingForward) {
            setBack(40000);
            movingForward = false;
        } else {
            setAhead(40000);
            movingForward = true;
        }
    }
}

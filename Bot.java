package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import java.security.SecureRandom;

public class Bot {

    public static final Map<Integer,Integer> speedMap = new HashMap<>();
    public static final Map<Integer,Integer> reverseSpeedMap = new HashMap<>();
    static {
        speedMap.put(0,0);      reverseSpeedMap.put(0,0);
        speedMap.put(1,3);      reverseSpeedMap.put(3,1);
        speedMap.put(2,6);      reverseSpeedMap.put(6,2);
        speedMap.put(3,8);      reverseSpeedMap.put(8,3);
        speedMap.put(4,9);      reverseSpeedMap.put(9,4);
        speedMap.put(5,15);     reverseSpeedMap.put(15,5);
    }

    private static int maxSpeed = 15;
    private List<Command> directionList = new ArrayList<>();

    private static final int FORWARD = 0;
    private static final int LEFT = -1;
    private static final int RIGHT = 1;

    private final Random random;
    private static GameState gameState;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();
    private final static Command TURN_RIGHT = new ChangeLaneCommand(RIGHT);
    private final static Command TURN_LEFT = new ChangeLaneCommand(LEFT);

    private static Position cyberTruck;

    public Bot() {
        this.random = new SecureRandom();
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    public Command run(GameState gameState) {
        Bot.gameState = gameState;
        Car myCar = gameState.player;
        Car theirCar = gameState.opponent;
        Bot.maxSpeed = speedMap.get(5- myCar.damage);

        if (gameState.currentRound == 1) {
            cyberTruck = myCar.position;
            cyberTruck.lane = -1;
            cyberTruck.block = -1;
        }

        // Blocks View
        List<Terrain> blocksForward = getBlocksInFront(FORWARD, 0);
        List<Terrain> blocksBoost = getBlocksInFront(FORWARD, 1);
        List<Terrain> blocksBoostMax = getBlocksInFront(FORWARD, 2);
        List<Terrain> blocksLeft = getBlocksInFront(LEFT, 0);
        List<Terrain> blocksRight = getBlocksInFront(RIGHT, 0);

        //Fix first if damaged
        if (myCar.damage >= 2) {
            return FIX;
        }

        //Use boost if no blocks in front
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            if (!obstacleInLane(blocksBoostMax) && myCar.damage == 1)
                return FIX;
            if (!obstacleInLane(blocksBoost) && myCar.speed <= speedMap.get(3))
                return BOOST;
        }

        //If no boost then accelerate
        if (myCar.speed == speedMap.get(0)) {
            return ACCELERATE;
        }

        Double leftPoint = totalPoints(blocksLeft);
        Double forwardPoint = totalPoints(blocksForward) + 0.15;
        Double rightPoint = totalPoints(blocksRight);


        if (myCar.speed >= speedMap.get(4)) {
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {

                if (!obstacleInLane(blocksForward)) {
                    if (hasPowerUp(PowerUps.BOOST, myCar.powerups)
                            && !obstacleInLane(blocksBoost) && myCar.speed != maxSpeed)
                        return BOOST;
                }

                if (!obstacleInLane(blocksLeft) && leftPoint > forwardPoint && leftPoint > rightPoint) {
                    return TURN_LEFT;
                }

                if (!obstacleInLane(blocksRight) && rightPoint > forwardPoint && rightPoint > leftPoint) {
                    return TURN_RIGHT;
                }

                if (!obstacleInLane(blocksForward)) {
                    if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
                        if (myCar.position.block > theirCar.position.block) {
                            cyberTruck = myCar.position;
                        }
                        else {
                            cyberTruck.lane = theirCar.position.lane;
                            if (theirCar.speed == 5)
                                theirCar.speed = 6;
                            cyberTruck.block = theirCar.position.block +
                                    speedMap.get(reverseSpeedMap.get(theirCar.speed)) + 1;
                            if (cyberTruck.block >= 1499)
                                cyberTruck.block = 1498;
                        }
                        return new TweetCommand(cyberTruck.lane, cyberTruck.block);
                    }
                    if (hasPowerUp(PowerUps.OIL, myCar.powerups))
                        return OIL;

                    return ACCELERATE;
                }

                if (!obstacleInLane(blocksLeft))
                    return TURN_LEFT;

                if (!obstacleInLane(blocksRight))
                    return TURN_RIGHT;

                return LIZARD;
            }
        }

        if (myCar.speed <= speedMap.get(2) && theirCar.speed >= speedMap.get(3)
                && myCar.position.block < theirCar.position.block) {
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)
                    && myCar.position.lane == theirCar.position.lane) {
                return EMP;
            }

            if (forwardPoint >= 0.1) {
                if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
                    return BOOST;
                }

                if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
                    if (myCar.position.block > theirCar.position.block) {
                        cyberTruck = myCar.position;
                    }
                    else {
                        cyberTruck.lane = theirCar.position.lane;
                        if (theirCar.speed == 5)
                            theirCar.speed = 6;
                        cyberTruck.block = theirCar.position.block +
                                speedMap.get(reverseSpeedMap.get(theirCar.speed)) + 1;
                        if (cyberTruck.block >= 1499)
                            cyberTruck.block = 1498;
                    }
                    return new TweetCommand(cyberTruck.lane, cyberTruck.block);
                }
            }
        }

        // Kalau stuck di belakang mobil lawan
        // Pindah lane
        if (myCar.position.block + 1 == theirCar.position.block &&
            myCar.position.lane == theirCar.position.lane) {
            if (leftPoint >= rightPoint)
                return TURN_LEFT;
            else
                return TURN_RIGHT;
        }

        if (leftPoint < 0 && forwardPoint < 0 && rightPoint < 0
                && hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
            return LIZARD;
        }

        if (leftPoint > forwardPoint && leftPoint > rightPoint) {
            return TURN_LEFT;
        }

        if (rightPoint >= leftPoint && rightPoint > forwardPoint) {
            return TURN_RIGHT;
        }

        if(hasPowerUp(PowerUps.EMP, myCar.powerups)) {
            if (
                    (myCar.position.lane == theirCar.position.lane ||
                    myCar.position.lane == theirCar.position.lane+1 ||
                    myCar.position.lane == theirCar.position.lane-1)
                    && (myCar.position.block < theirCar.position.block)
            ) {
                return EMP;
            }
        }

        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && myCar.speed >= speedMap.get(4))
            return OIL;

        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.speed <= speedMap.get(2))
            return BOOST;

        return ACCELERATE;
    }


    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Compute terrains that would be traversed by player car, based on the assumption
     * that the car will ACCELERATE if the car moves FORWARD and not to be boosted.
     * @param direction FORWARD, LEFT, or RIGHT
     * @param boost 1 if boost is to be activated, 2 if boost is to be activated by fix.
     *              Should only be true if direction is FORWARD
     * @return terrains of a lane that would be traversed
     */
    private static List<Terrain> getBlocksInFront(int direction, int boost) {
        Car myCar = gameState.player;
        int carLane = myCar.position.lane - 1;
        int carBlock = myCar.position.block;
        int carBlockIndex = carBlock - gameState.lanes.get(0)[0].position.block;
        int cyberTruckBlockIndex = cyberTruck.block;
        int cyberTruckLane = cyberTruck.lane;

        if (carLane + direction < 0 || carLane + direction > 3) {
            return null;
        }

        Lane[] laneBlocks = gameState.lanes.get(carLane + direction);

        List<Terrain> blocks = new ArrayList<>();

        int speed;
        if (direction == FORWARD) {
            if (boost == 1 || boost == 2)
                speed = speedMap.get(5);
            else {
                if (myCar.speed == 5)
                    speed = speedMap.get(2);
                else if (myCar.speed == 15)
                    speed = myCar.speed;
                else
                    speed = speedMap.get(reverseSpeedMap.get(myCar.speed) + 1);
            }
            if (boost != 2)
                if (speed > maxSpeed) speed = maxSpeed;

            for (int i = carBlockIndex + 1; i <= carBlockIndex + speed; i++) {
                if (laneBlocks[i] == null || laneBlocks[i].terrain == Terrain.FINISH)
                    break;
                if (cyberTruckLane == carLane && cyberTruckBlockIndex == i)
                    blocks.add(Terrain.CYBERTRUCK);
                else
                    blocks.add(laneBlocks[i].terrain);
            }
        }

        else {
            speed = myCar.speed;
            for (int i = carBlockIndex; i <= carBlockIndex + speed - 1; i++) {
                if (laneBlocks[i] == null || laneBlocks[i].terrain == Terrain.FINISH)
                    break;
                if (cyberTruckLane == carLane + direction && cyberTruckBlockIndex == i)
                    blocks.add(Terrain.CYBERTRUCK);
                else
                    blocks.add(laneBlocks[i].terrain);
            }
        }

        return blocks;
    }


    /**
     * Compute the points of terrains in which each terrain has its own point value.
     * Negative points caused by obstacles are adjusted based on the damage cap of 5.
     * @param terrains terrain of one lane
     * @return points of the lane
     */
    private static Double totalPoints(List<Terrain> terrains) {
        if (terrains == null)
            return -1000d;

        Double point = 0d;
        Integer[] obstacleCount = {0, 0, 0, 0};    // CYBERTRUCK, WALL, OIL SPILL, MUD

        for (Terrain t: terrains) {
            if (t == Terrain.OIL_POWER) point += 0.2;
            else if (t == Terrain.BOOST) point += 2.15;
            else if (t == Terrain.LIZARD) point += 1.2;
            else if (t == Terrain.TWEET) point += 1.1;
            else if (t == Terrain.EMP){
                if (gameState.player.position.block < gameState.opponent.position.block) point += 1.47;
                else point += 1.03;
            }
            else {
                if (t == Terrain.MUD) obstacleCount[3] += 1;
                else if (t == Terrain.OIL_SPILL) obstacleCount[2] += 1;
                else if (t == Terrain.WALL) obstacleCount[1] += 1;
                else if (t == Terrain.CYBERTRUCK) obstacleCount[0] += 1;
            }
        }

        Integer damage = 0;
        for (int i = 0; i < 4; i++) {
            while (obstacleCount[i] > 0 && damage < 5) {
                obstacleCount[i] -= 1;
                if (i == 0) {
                    point -= 100;  // No cyber truck >:V
                    damage += 2;
                }
                else if (i == 1) {
                    point -= 2;
                    damage += 2;
                }
                else if (i == 2) {
                    point -= 0.82;
                    damage += 1;
                }
                else {
                    point -= 0.8;
                    damage += 1;
                }
            }
        }

        return point;
    }


    /**
     * Check if the terrains contains any obstacles
     * @param terrains terrains of a lane
     * @return true or false
     */
    private static boolean obstacleInLane(List<Terrain> terrains) {
        if (terrains == null)
            return true;

        for (Terrain t: terrains) {
            if (t == Terrain.WALL || t == Terrain.MUD || t == Terrain.OIL_SPILL || t == Terrain.CYBERTRUCK)
                return true;
        }

        return false;
    }

}

package client;

import client.model.*;

import java.sql.Array;
import java.util.Arrays;

public class AI {
    private World world;
    private final int WALL = 99;
    private int[] heroSelect = {0, 4, 0, 0};
    private CellInformation[][] initialCellInformation;
    private int targetRow = 500, targetColumn = 500;
    private int firstRowOfObjectiveZone = 40, lastRowOfObjectiveZone = 0, firstColumnOfObjectiveZone = 40, lastColumnOfObjectiveZone = 0;
    private boolean[] useFindPath = {false, false, false, false};
    private BlasterPlanInformation[] blasterPlanInformation = new BlasterPlanInformation[4];
    private Cell[] targetCells = new Cell[4];
    private int[][] weightsOfAttack;
    private int[][] weightsOfBomb;
    private int currentAP = 100;
    private int currentTurn;
    private Hero myHero;
    private Cell currentCell;
    private BlasterPlanInformation blaster;
    private static Map map;
    public static RespawnInformation[] respawnInformation = new RespawnInformation[4];
    private boolean isTargetSet = false;

    public void preProcess(World world) {
        this.world = world;
        map = world.getMap();
        boolean[][] objectivePoints = new boolean[map.getRowNum()][map.getColumnNum()];
        setObjectivePoints(objectivePoints);
        for (int i = 0; i < 4; i++) {
            blasterPlanInformation[i] = new BlasterPlanInformation();
        }
        Cell[] respawnZone = map.getMyRespawnZone();
        for (int i = 0; i < 4; i++) {
            respawnInformation[i] = new RespawnInformation(respawnZone[i]);
        }
        RespawnInformation.setTargetCell(world, objectivePoints, 0);
        setCellsInformation();
    }

    public static void setObjectivePoints(boolean[][] objectivePoints) {
        RespawnInformation.numberOfObjectivePoints = 0;
        for (int i = 0; i < map.getRowNum(); i++) {
            for (int j = 0; j < map.getColumnNum(); j++) {
                Cell cell = map.getCell(i, j);
                if (!cell.isWall() && cell.isInObjectiveZone()) {
                    RespawnInformation.numberOfObjectivePoints++;
                    objectivePoints[i][j] = true;
                }
            }
        }
        System.out.println("Objective Point Set!");
    }

    public void pickTurn(World world) {
        this.world = world;
        for (int i = 0; i < 4; i++) {
            if (heroSelect[i] > 0) {
                heroSelect[i]--;
                switch (i) {
                    case 0:
                        world.pickHero(HeroName.SENTRY);
                        break;
                    case 1:
                        world.pickHero(HeroName.BLASTER);
                        break;
                    case 2:
                        world.pickHero(HeroName.HEALER);
                        break;
                    case 3:
                        world.pickHero(HeroName.GUARDIAN);
                        break;
                }
                break;
            }
        }
    }

    public void moveTurn(World world) {
        this.world = world;
        currentTurn = world.getCurrentTurn();
        if (!isTargetSet) {
            for (int i = 0; i < 4; i++) {
                targetCells[world.getMyHero(respawnInformation[i].getRespawnCell()[0], respawnInformation[i].getRespawnCell()[1]).getId() / 2] = map.getCell(respawnInformation[i].getTargetCell()[0], respawnInformation[i].getTargetCell()[1]);
            }
            System.out.printf("-target0:%d %d%n", targetCells[0].getRow(), targetCells[0].getColumn());
            System.out.printf("-target1:%d %d%n", targetCells[1].getRow(), targetCells[1].getColumn());
            System.out.printf("-target2:%d %d%n", targetCells[2].getRow(), targetCells[2].getColumn());
            System.out.printf("-target3:%d %d%n", targetCells[3].getRow(), targetCells[3].getColumn());
            isTargetSet = true;
        }
        switch (world.getMovePhaseNum()) {
            case 0:
                makeDodgeFlagsFalse();
                currentAP = 100;
                setMoveNums();
                currentAP -= (blasterPlanInformation[0].getMoveNum() + blasterPlanInformation[1].getMoveNum() + blasterPlanInformation[2].getMoveNum() + blasterPlanInformation[3].getMoveNum()) * world.getMyHeroes()[0].getMoveAPCost();
                resolveShortageOfAP();
                break;
            case 5:
                setWeightForOffensivePlan();
                setOffensivePlans(currentTurn);
                break;
        }

        for (int i = 0; i < 4; i++) {
            myHero = world.getMyHeroes()[i];
            blaster = blasterPlanInformation[i];
            currentCell = myHero.getCurrentCell();
            int[] currentCellArray = {currentCell.getRow(), currentCell.getColumn()};
            blaster.setCurrentCell(currentCellArray);
            blaster.setAsWall(false);
            if (currentCellArray[0] == targetCells[i].getRow() && currentCellArray[1] == targetCells[i].getColumn()) {
                blaster.setAsWall(true);
                System.out.println("Hero " + myHero.getId() + " is in target");
            }
        }

        for (int i = 0; i < 4; i++) {
            myHero = world.getMyHeroes()[i];
            blaster = blasterPlanInformation[i];
            currentCell = myHero.getCurrentCell();
            if (blaster.getCurrentCell()[0] > 0 && blaster.getCurrentCell()[1] > 0) { // Hero is alive
                if (currentCell.isInObjectiveZone()) { // in the objective zone
                    switch (blaster.getPlan()) {
                        case DEFAULT:
                        case ATTACK:
                        case BOMB:
                            defaultMove(i);
                            break;
                    }
                } else { // out of objective zone
                    if (world.getMovePhaseNum() == 0)
                        setFlagDodgeOrWalk(myHero.getDodgeAbilities()[0].getRange());
                    switch (blaster.getPlan()) {
                        case DEFAULT:
                            defaultMove(i);
                            break;
                        case DODGE:
                            break;
                    }
                }
            } else {
                blaster.setPlan(PlanOfBlaster.DEFAULT);
                blaster.setFirstTimeLayout(false);
                useFindPath[i] = false;
            }
        }
    }

    public void actionTurn(World world) {
        this.world = world;
        setWeightForOffensivePlan();
        for (int i = 0; i < 4; i++) {
            myHero = world.getMyHeroes()[i];
            blaster = blasterPlanInformation[i];
            if (myHero.getCurrentCell().getRow() == -1) continue; // Hero is dead
            if(blaster.getPlan() == PlanOfBlaster.DEFAULT && myHero.getCurrentCell().isInObjectiveZone()) {
                if (currentTurn % 4 == (i + 1) % 4) {
                    //Blaster Bomb
                    blasterPlanInformation[i].setPlanOfBlaster(world, myHero, PlanOfBlaster.BOMB, weightsOfBomb, 5);
                } else {
                    //Blaster Attack
                    blasterPlanInformation[i].setPlanOfBlaster(world, myHero, PlanOfBlaster.ATTACK, weightsOfAttack, 4);
                }
            }
            switch (blaster.getPlan()) {
                case DODGE:
                    world.castAbility(myHero, myHero.getDodgeAbilities()[0], blaster.getDodgeTargetCell()[0], blaster.getDodgeTargetCell()[1]);
                    blaster.setPlan(PlanOfBlaster.DEFAULT);
                    break;
                case ATTACK:
                case BOMB:
                    if (currentTurn % 4 == (i + 1) % 4) {
                        //Blaster Bomb
                        blasterPlanInformation[i].setPlanOfBlaster(world, myHero, PlanOfBlaster.BOMB, weightsOfBomb, 5);
                    } else {
                        //Blaster Attack
                        blasterPlanInformation[i].setPlanOfBlaster(world, myHero, PlanOfBlaster.ATTACK, weightsOfAttack, 4);
                    }
                    blasterAction(myHero);
                    break;
            }
        }
    }

    private int absolute(int x) {
        return x >= 0 ? x : -x;
    }

    private int calculateTargetDistanceForDodge(int[] currentCellArray, int maxTargetDistance, int[] finalTargetCell, int row, int column) {
        if (map.isInMap(row, column) && !map.getCell(row, column).isWall() && !initialCellInformation[row][column].isReservedForDodge) {
            if (maxTargetDistance <= initialCellInformation[currentCellArray[0]][currentCellArray[1]].layerNumber - initialCellInformation[row][column].layerNumber) {
                maxTargetDistance = initialCellInformation[currentCellArray[0]][currentCellArray[1]].layerNumber - initialCellInformation[row][column].layerNumber;
                finalTargetCell[0] = row;
                finalTargetCell[1] = column;
            }
        }
        return maxTargetDistance;
    }

    private void setFlagDodgeOrWalk(int range) {
        map = world.getMap();
        blaster = blasterPlanInformation[myHero.getId() / 2];
        int[] currentCellArray = {myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn()};
        int maxTargetDistance = 0;
        int[] finalTargetCell = new int[2];
        for (int column = currentCellArray[1] - range; column <= currentCellArray[1] + range; column++) {
            int rowUp = currentCellArray[0] + range - absolute(column - currentCellArray[1]);
            maxTargetDistance = calculateTargetDistanceForDodge(currentCellArray, maxTargetDistance, finalTargetCell, rowUp, column);
            int rowDown = currentCellArray[0] - range + absolute(column - currentCellArray[1]);
            maxTargetDistance = calculateTargetDistanceForDodge(currentCellArray, maxTargetDistance, finalTargetCell, rowDown, column);
        }
        blaster.setPlan(PlanOfBlaster.DEFAULT);
        if (maxTargetDistance > 6) {
            blaster.setDodgeTargetCell(finalTargetCell);
            blaster.setPlan(PlanOfBlaster.DODGE);
            initialCellInformation[finalTargetCell[0]][finalTargetCell[1]].isReservedForDodge = true;
        }
    }

    private void setCellLayers(CellInformation[][] cellInformation, int layer, int i, int j) {
        if (layer == cellInformation[i][j].layerNumber) {
            if (map.isInMap(i + 1, j) && !cellInformation[i + 1][j].isLayerSet) {
                cellInformation[i + 1][j].setCellInfo(layer + 1);
            }
            if (map.isInMap(i - 1, j) && !cellInformation[i - 1][j].isLayerSet) {
                cellInformation[i - 1][j].setCellInfo(layer + 1);
            }
            if (map.isInMap(i, j + 1) && !cellInformation[i][j + 1].isLayerSet) {
                cellInformation[i][j + 1].setCellInfo(layer + 1);
            }
            if (map.isInMap(i, j - 1) && !cellInformation[i][j - 1].isLayerSet) {
                cellInformation[i][j - 1].setCellInfo(layer + 1);
            }
        }
    }

    private void initializeCellInformation(CellInformation[][] cellInformation) {
        int row = map.getRowNum(), column = map.getColumnNum();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                cellInformation[i][j] = new CellInformation();
                if (map.getCell(i, j).isWall()) cellInformation[i][j].setCellInfo(WALL);
            }
        }
    }

    private void setCellsInformation() {
        map = world.getMap();
        int row = map.getRowNum(), column = map.getColumnNum();
        initialCellInformation = new CellInformation[row][column];
        for (int i = 2; i < row - 2; i++) {
            for (int j = 2; j < column - 2; j++) {
                boolean cellIsInObjectiveZone = map.getCell(i, j).isInObjectiveZone();
                if (i < firstRowOfObjectiveZone && cellIsInObjectiveZone) firstRowOfObjectiveZone = i;
                if (i > lastRowOfObjectiveZone && cellIsInObjectiveZone) lastRowOfObjectiveZone = i;
                if (j < firstColumnOfObjectiveZone && cellIsInObjectiveZone) firstColumnOfObjectiveZone = j;
                if (j > lastColumnOfObjectiveZone && cellIsInObjectiveZone) lastColumnOfObjectiveZone = j;
            }
        }
        targetRow = (firstRowOfObjectiveZone + lastRowOfObjectiveZone) / 2;
        targetColumn = (firstColumnOfObjectiveZone + lastColumnOfObjectiveZone) / 2;
        if (map.getCell(targetRow, targetColumn).isWall()) targetRow++;
        initializeCellInformation(initialCellInformation);
        initialCellInformation[targetRow][targetColumn].setCellInfo(0);
        boolean allCellsIsSet = false;
        int layer = 0;
        while (!allCellsIsSet) {
            allCellsIsSet = true;
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    if (!map.getCell(i, j).isWall()) {
                        if (initialCellInformation[i][j].isLayerSet) {
                            setCellLayers(initialCellInformation, layer, i, j);
                        } else allCellsIsSet = false;
                    }
                }
            }
            layer++;
        }
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                System.out.printf("%2d ", initialCellInformation[i][j].layerNumber);
            }
            System.out.println();
        }
    }

    private void setMyHeroesAsWall(CellInformation[][] cellInformation, int currentHeroNum) {
        for (int i = 0; i < 4; i++) {
            blaster = blasterPlanInformation[i];
            int[] currentCellArray = blaster.getCurrentCell();
            if (blaster.isSetAsWall() && currentHeroNum != i && currentCellArray[0] > 0 && currentCellArray[1] > 0) { // Hero is alive
                cellInformation[currentCellArray[0]][currentCellArray[1]].setCellInfo(WALL);
            }
        }
    }

    private Direction findPath(int heroNum, Cell targetCell) {
        map = world.getMap();
        int row = map.getRowNum(), column = map.getColumnNum();
        CellInformation[][] tempCellInformation = new CellInformation[row][column];
        initializeCellInformation(tempCellInformation);
        Hero[] myHeroes = world.getMyHeroes();
        setMyHeroesAsWall(tempCellInformation, heroNum);
        tempCellInformation[targetCell.getRow()][targetCell.getColumn()].setCellInfo(0);
        int layer = 0;
        int firstCellRow = myHeroes[heroNum].getCurrentCell().getRow(), firstCellColumn = myHeroes[heroNum].getCurrentCell().getColumn();
        if (firstCellColumn == -1)
            return Direction.values()[0];
        while (!tempCellInformation[firstCellRow][firstCellColumn].isLayerSet) {
            for (int i = firstRowOfObjectiveZone - 3; i <= lastRowOfObjectiveZone + 3; i++) {
                for (int j = firstColumnOfObjectiveZone - 3; j <= lastColumnOfObjectiveZone + 3; j++) {
                    if (!map.getCell(i, j).isWall() && tempCellInformation[i][j].isLayerSet) {
                        setCellLayers(tempCellInformation, layer, i, j);
                    }
                }
            }
            layer++;
        }
        int firstCellLayerNum = tempCellInformation[firstCellRow][firstCellColumn].layerNumber;
        int[] currentCell = blaster.getCurrentCell();
        if (tempCellInformation[firstCellRow - 1][firstCellColumn].isLayerSet && tempCellInformation[firstCellRow - 1][firstCellColumn].layerNumber < firstCellLayerNum) {
            currentCell[0]--;
            blaster.setCurrentCell(currentCell);
            blaster.setAsWall(true);
            return Direction.UP;
        }
        if (tempCellInformation[firstCellRow][firstCellColumn - 1].isLayerSet && tempCellInformation[firstCellRow][firstCellColumn - 1].layerNumber < firstCellLayerNum) {
            currentCell[1]--;
            blaster.setCurrentCell(currentCell);
            blaster.setAsWall(true);
            return Direction.LEFT;
        }
        if (tempCellInformation[firstCellRow + 1][firstCellColumn].isLayerSet && tempCellInformation[firstCellRow + 1][firstCellColumn].layerNumber < firstCellLayerNum) {
            currentCell[0]++;
            blaster.setCurrentCell(currentCell);
            blaster.setAsWall(true);
            return Direction.DOWN;
        }
        if (tempCellInformation[firstCellRow][firstCellColumn + 1].isLayerSet && tempCellInformation[firstCellRow][firstCellColumn + 1].layerNumber < firstCellLayerNum) {
            currentCell[1]++;
            blaster.setCurrentCell(currentCell);
            blaster.setAsWall(true);
            return Direction.RIGHT;
        }
        currentCell[0]--;
        blaster.setCurrentCell(currentCell);
        blaster.setAsWall(true);
        return Direction.UP;
    }

    private void moveHeroToOBJ(Hero myHero) {
        Direction targetDirection = null;
        map = world.getMap();
        int[] currentCellArray = {myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn()};
        int mapRow = map.getRowNum(), mapColumn = map.getColumnNum();
        int minLayerNumber = initialCellInformation[currentCellArray[0]][currentCellArray[1]].layerNumber;
        if (currentCellArray[0] > 0 && initialCellInformation[currentCellArray[0] - 1][currentCellArray[1]].layerNumber < minLayerNumber && !map.getCell(currentCellArray[0] - 1, currentCellArray[1]).isWall()) {
            minLayerNumber = initialCellInformation[currentCellArray[0] - 1][currentCellArray[1]].layerNumber;
            targetDirection = Direction.UP;
        }
        if (currentCellArray[1] > 0 && initialCellInformation[currentCellArray[0]][currentCellArray[1] - 1].layerNumber < minLayerNumber && !map.getCell(currentCellArray[0], currentCellArray[1] - 1).isWall()) {
            minLayerNumber = initialCellInformation[currentCellArray[0]][currentCellArray[1] - 1].layerNumber;
            targetDirection = Direction.LEFT;
        }
        if (currentCellArray[0] < mapRow - 1 && initialCellInformation[currentCellArray[0] + 1][currentCellArray[1]].layerNumber < minLayerNumber && !map.getCell(currentCellArray[0] + 1, currentCellArray[1]).isWall()) {
            minLayerNumber = initialCellInformation[currentCellArray[0] + 1][currentCellArray[1]].layerNumber;
            targetDirection = Direction.DOWN;
        }
        if (currentCellArray[0] < mapColumn - 1 && initialCellInformation[currentCellArray[0]][currentCellArray[1] + 1].layerNumber < minLayerNumber && !map.getCell(currentCellArray[0], currentCellArray[1] + 1).isWall()) {
            minLayerNumber = initialCellInformation[currentCellArray[0]][currentCellArray[1] + 1].layerNumber;
            targetDirection = Direction.RIGHT;
        }
        if (targetDirection != null) world.moveHero(myHero, targetDirection);
        System.out.println(myHero.getId() + " " + targetDirection + " " + initialCellInformation[currentCellArray[0]][currentCellArray[1]].layerNumber + " -> " + minLayerNumber);
    }

    private void printWeights(int[][] weights) {
        for (int row = firstRowOfObjectiveZone; row <= lastRowOfObjectiveZone; row++) {
            for (int column = firstColumnOfObjectiveZone; column <= lastColumnOfObjectiveZone; column++) {
                System.out.print(weights[row][column] + " ");
            }
            System.out.println();
        }
    }

    private void increaseCellWeight(int[][] weights, int[] cell, int currentHP, int maxHP, int areaOfEffect) {
        for (int column = cell[1] - areaOfEffect; column <= cell[1] + areaOfEffect; column++) { // attack
            int rowDown = cell[0] + (areaOfEffect - absolute(column - cell[1]));
            int rowUp = cell[0] - (areaOfEffect - absolute(column - cell[1]));
            for (int row = rowUp; row <= rowDown; row++) {
                weights[row][column] += 20 - 10 * (maxHP - currentHP) / maxHP;
            }
        }
    }

    private void setWeightForOffensivePlan() {
        weightsOfAttack = new int[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        weightsOfBomb = new int[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        final int attackAOE = world.getMyHeroes()[0].getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect();
        final int bombAOE = world.getMyHeroes()[0].getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect();
        for (Hero enemyHero : world.getOppHeroes()) {
            int[] cell = {enemyHero.getCurrentCell().getRow(), enemyHero.getCurrentCell().getColumn()};
            if (cell[0] == -1) continue; // Hero is dead!
            increaseCellWeight(weightsOfAttack, cell, enemyHero.getCurrentHP(), enemyHero.getMaxHP(), attackAOE);
            increaseCellWeight(weightsOfBomb, cell, enemyHero.getCurrentHP(), enemyHero.getMaxHP(), bombAOE);
        }
        System.out.println("Weight Of Attack:");
        printWeights(weightsOfAttack);
        System.out.println();
        System.out.println("Weight Of Bomb:");
        printWeights(weightsOfBomb);
    }

    private void defaultMove(int index) {
        myHero = world.getMyHeroes()[index];
        blaster = blasterPlanInformation[index];
        int[] currentCellArray = {myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn()};
        int currentMoveNum = blaster.getMoveNum();
        if (initialCellInformation[currentCellArray[0]][currentCellArray[1]].layerNumber < 7) useFindPath[index] = true;
        if (!useFindPath[index]) {
            if (currentMoveNum > 0) {
                moveHeroToOBJ(myHero);
                blaster.setMoveNum(currentMoveNum - 1);
            }
        } else {
            if (currentCellArray[0] != targetCells[index].getRow() || currentCellArray[1] != targetCells[index].getColumn()) {
                if (currentMoveNum > 0) {
                    Direction nextDirection = findPath(index, targetCells[index]);
                    world.moveHero(myHero, nextDirection);
                    System.out.println(myHero.getId() + " " + nextDirection);
                    blaster.setMoveNum(currentMoveNum - 1);
                }
            } else {
                if (!blaster.isFirstTimeLayout()) blaster.setFirstTimeLayout(true);
                System.out.println("in Position");
            }
        }
    }

    private void resolveShortageOfAP() {
        while (currentAP < 0) {
            int maxMoveIndex = 0, maxMoveNum = blasterPlanInformation[0].getMoveNum();
            for (int i = 1; i < 4; i++) {
                int currentMoveNum = blasterPlanInformation[i].getMoveNum();
                if (currentMoveNum > maxMoveNum) {
                    maxMoveIndex = i;
                    maxMoveNum = currentMoveNum;
                }
            }
            blasterPlanInformation[maxMoveIndex].setMoveNum(maxMoveNum - 1);
            currentAP += world.getMyHeroes()[0].getMoveAPCost();
        }
    }

    private void setMoveNums() {
        for (int i = 0; i < 4; i++) {
            myHero = world.getMyHeroes()[i];
            blaster = blasterPlanInformation[i];
            int[] currentCellArray = {myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn()};
            int[] targetCellArray = blaster.getObjectiveTargetCell();
            blaster.setMoveNum(0);
            if (currentCellArray[0] == -1) continue; // Hero is dead
            switch (blaster.getPlan()) {
                case BOMB:
                case ATTACK:
                    currentAP -= myHero.getAbility(blaster.getAbilityName()).getAPCost();
                    blaster.setMoveNum(world.manhattanDistance(currentCellArray[0], currentCellArray[1], targetCellArray[0], targetCellArray[1]) - blaster.getRangeOfCasting());
                    if (blaster.getMoveNum() < 0) blaster.setMoveNum(0);
                    break;
                case DODGE:
                    currentAP -= myHero.getAbility(AbilityName.BLASTER_DODGE).getAPCost();
                    blaster.setMoveNum(0);
                    break;
                case DEFAULT:
                    blaster.setMoveNum(6);
                    break;
            }
        }
    }

    private void makeDodgeFlagsFalse() {
        for (int i = 0; i < world.getMap().getRowNum(); i++) {
            for (int j = 0; j < world.getMap().getColumnNum(); j++) {
                initialCellInformation[i][j].isReservedForDodge = false;
            }
        }
    }

    private void setOffensivePlans(int currentTurn) {
        for (Hero hero : world.getMyHeroes()) {
            if (hero.getCurrentCell().getRow() == -1) continue; // Hero is dead
            int i = hero.getId() / 2;
            if (blasterPlanInformation[i].getPlan() == PlanOfBlaster.DODGE || !hero.getCurrentCell().isInObjectiveZone())
                continue;
            if (currentTurn % 4 == (i + 1) % 4) {
                //Blaster Bomb
                blasterPlanInformation[i].setPlanOfBlasterAndMove(world, hero, PlanOfBlaster.BOMB, weightsOfBomb);
            } else {
                //Blaster Attack
                blasterPlanInformation[i].setPlanOfBlasterAndMove(world, hero, PlanOfBlaster.ATTACK, weightsOfAttack);
            }
        }
    }

    private void blasterAction(Hero myHero) {
        blaster = blasterPlanInformation[myHero.getId() / 2];
        int[] currentCellArray = {myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn()};
        int[] targetCellArray = blaster.getOffensiveTargetCell();
        world.castAbility(myHero, blaster.getAbilityName(), targetCellArray[0], targetCellArray[1]);
        System.out.println("Turn " + world.getCurrentTurn() + ", Cast ability for Hero " + myHero.getId() + " done:\n" + blaster.getAbilityName() + " from (" + currentCellArray[0] + ", " + currentCellArray[1] + ") to (" + targetCellArray[0] + ", " + targetCellArray[1] + ")");
        blaster.setPlan(PlanOfBlaster.DEFAULT);
    }
}
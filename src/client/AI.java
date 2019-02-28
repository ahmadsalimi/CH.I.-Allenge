package client;

import client.model.*;

public class AI {
    private final int WALL = 99;
    private int[] championSelect = {0, 4, 0, 0};
    private CellInformation[][] initialCellInformation;
    private int targetRow = 500, targetColumn = 500;
    private int firstRowOfObjectiveZone = 40, lastRowOfObjectiveZone = 0, firstColumnOfObjectiveZone = 40, lastColumnOfObjectiveZone = 0;
    private boolean[] targetReach = {false, false, false, false};
    private BlasterPlanInformation[] blasterPlanInformation = new BlasterPlanInformation[4];
    private Cell[] targetCells = new Cell[4];
    private int[][] weightsOfAttack;
    private int[][] weightsOfBomb;

    private int currentAP = 100;

    private int absolute(int x) {
        return x >= 0 ? x : -x;
    }

    private void setFlagDodgeOrWalk(World world, Hero hero, int range) {
        Map map = world.getMap();
        BlasterPlanInformation blaster = blasterPlanInformation[hero.getId() / 2];
        int[] cell = {hero.getCurrentCell().getRow(), hero.getCurrentCell().getColumn()};
        int maxTargetDistance = 0;
        int[] finalTargetCell = new int[2];
        for (int column = cell[1] - range; column <= cell[1] + range; column++) {
            int rowUp = cell[0] + range - absolute(column - cell[1]);
            if (map.isInMap(rowUp, column) && !map.getCell(rowUp, column).isWall() && !initialCellInformation[rowUp][column].isReservedForDodge) {
                if (maxTargetDistance <= initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowUp][column].layerNumber) {
                    maxTargetDistance = initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowUp][column].layerNumber;
                    finalTargetCell[0] = rowUp;
                    finalTargetCell[1] = column;
                }
            }
            int rowDown = cell[0] - range + absolute(column - cell[1]);
            if (map.isInMap(rowDown, column) && !map.getCell(rowDown, column).isWall() && !initialCellInformation[rowDown][column].isReservedForDodge) {
                if (maxTargetDistance <= initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowDown][column].layerNumber) {
                    maxTargetDistance = initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowDown][column].layerNumber;
                    finalTargetCell[0] = rowDown;
                    finalTargetCell[1] = column;
                }
            }
        }
        blaster.setPlan(PlanOfBlaster.DEFAULT);
        if (maxTargetDistance > 6) {
            blaster.setDodgeTargetCell(finalTargetCell);
            blaster.setPlan(PlanOfBlaster.DODGE);
            initialCellInformation[finalTargetCell[0]][finalTargetCell[1]].isReservedForDodge = true;
        }
    }

    private void setCellsInformation(World world) {
        Map map = world.getMap();
        int row = map.getRowNum(), column = map.getColumnNum();
        initialCellInformation = new CellInformation[row][column];
        int respawnRow = map.getMyRespawnZone()[0].getRow(), respawnColumn = map.getMyRespawnZone()[0].getColumn();
        for (int i = 2; i < row - 2; i++) {
            for (int j = 2; j < column - 2; j++) {
                boolean cellIsInObjectiveZone = map.getCell(i, j).isInObjectiveZone();
                if (i < firstRowOfObjectiveZone && cellIsInObjectiveZone) firstRowOfObjectiveZone = i;
                if (i > lastRowOfObjectiveZone && cellIsInObjectiveZone) lastRowOfObjectiveZone = i;
                if (j < firstColumnOfObjectiveZone && cellIsInObjectiveZone) firstColumnOfObjectiveZone = j;
                if (j > lastColumnOfObjectiveZone && cellIsInObjectiveZone) lastColumnOfObjectiveZone = j;
                if (cellIsInObjectiveZone && map.getCell(i - 2, j).isInObjectiveZone() && map.getCell(i + 2, j).isInObjectiveZone() && map.getCell(i, j - 2).isInObjectiveZone() && map.getCell(i, j + 2).isInObjectiveZone()) {
                    if (absolute(targetRow - respawnRow) + absolute(targetColumn - respawnColumn) > absolute(i - respawnRow) + absolute(j - respawnColumn)) {
                        targetRow = i;
                        targetColumn = j;
                    }
                }
            }
        }
        targetCells[0] = map.getCell(targetRow - 2, targetColumn);
        targetCells[1] = map.getCell(targetRow + 2, targetColumn);
        targetCells[2] = map.getCell(targetRow, targetColumn - 2);
        targetCells[3] = map.getCell(targetRow, targetColumn + 2);
        System.out.printf("-target0:%d %d%n", targetCells[0].getRow(), targetCells[0].getColumn());
        System.out.printf("-target1:%d %d%n", targetCells[1].getRow(), targetCells[1].getColumn());
        System.out.printf("-target2:%d %d%n", targetCells[2].getRow(), targetCells[2].getColumn());
        System.out.printf("-target3:%d %d%n", targetCells[3].getRow(), targetCells[3].getColumn());
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                initialCellInformation[i][j] = new CellInformation();
                if (map.getCell(i, j).isWall()) initialCellInformation[i][j].setCellInfo(WALL);
            }
        }
        initialCellInformation[targetRow][targetColumn].setCellInfo(0);
        boolean allCellsIsSet = false;
        int layer = 0;
        while (!allCellsIsSet) {
            allCellsIsSet = true;
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    if (!map.getCell(i, j).isWall()) {
                        if (initialCellInformation[i][j].isLayerSet) {
                            if (layer == initialCellInformation[i][j].layerNumber) {
                                if (map.isInMap(i + 1, j) && !initialCellInformation[i + 1][j].isLayerSet) {
                                    initialCellInformation[i + 1][j].setCellInfo(layer + 1);
                                }
                                if (map.isInMap(i - 1, j) && !initialCellInformation[i - 1][j].isLayerSet) {
                                    initialCellInformation[i - 1][j].setCellInfo(layer + 1);
                                }
                                if (map.isInMap(i, j + 1) && !initialCellInformation[i][j + 1].isLayerSet) {
                                    initialCellInformation[i][j + 1].setCellInfo(layer + 1);
                                }
                                if (map.isInMap(i, j - 1) && !initialCellInformation[i][j - 1].isLayerSet) {
                                    initialCellInformation[i][j - 1].setCellInfo(layer + 1);
                                }
                            }
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

    private Direction findPath(World world, int heroNum, Cell targetCell) {
        Map map = world.getMap();
        int row = map.getRowNum(), column = map.getColumnNum();
        CellInformation[][] tempCellInformation = new CellInformation[row][column];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                tempCellInformation[i][j] = new CellInformation();
                if (map.getCell(i, j).isWall()) tempCellInformation[i][j].setCellInfo(WALL);
            }
        }
        Hero[] myHeroes = world.getMyHeroes();
        if (heroNum != 0 && myHeroes[0].getCurrentCell().getRow() != -1) // Hero is alive
            tempCellInformation[myHeroes[0].getCurrentCell().getRow()][myHeroes[0].getCurrentCell().getColumn()].setCellInfo(WALL);
        if (heroNum != 1 && myHeroes[1].getCurrentCell().getRow() != -1) // Hero is alive
            tempCellInformation[myHeroes[1].getCurrentCell().getRow()][myHeroes[1].getCurrentCell().getColumn()].setCellInfo(WALL);
        if (heroNum != 2 && myHeroes[2].getCurrentCell().getRow() != -1) // Hero is alive
            tempCellInformation[myHeroes[2].getCurrentCell().getRow()][myHeroes[2].getCurrentCell().getColumn()].setCellInfo(WALL);
        if (heroNum != 3 && myHeroes[3].getCurrentCell().getRow() != -1) // Hero is alive
            tempCellInformation[myHeroes[3].getCurrentCell().getRow()][myHeroes[3].getCurrentCell().getColumn()].setCellInfo(WALL);

        tempCellInformation[targetCell.getRow()][targetCell.getColumn()].setCellInfo(0);
        int layer = 0;
        int firstCellRow = world.getMyHeroes()[heroNum].getCurrentCell().getRow(), firstCellColumn = world.getMyHeroes()[heroNum].getCurrentCell().getColumn();
        if (firstCellColumn == -1)
            return Direction.values()[0];
        while (!tempCellInformation[firstCellRow][firstCellColumn].isLayerSet) {
            for (int i = firstRowOfObjectiveZone - 3; i <= lastRowOfObjectiveZone + 3; i++) {
                for (int j = firstColumnOfObjectiveZone - 3; j <= lastColumnOfObjectiveZone + 3; j++) {
                    if (!map.getCell(i, j).isWall()) {
                        if (tempCellInformation[i][j].isLayerSet) {
                            if (layer == tempCellInformation[i][j].layerNumber) {
                                if (map.isInMap(i + 1, j) && !tempCellInformation[i + 1][j].isLayerSet) {
                                    tempCellInformation[i + 1][j].setCellInfo(layer + 1);
                                }
                                if (map.isInMap(i - 1, j) && !tempCellInformation[i - 1][j].isLayerSet) {
                                    tempCellInformation[i - 1][j].setCellInfo(layer + 1);
                                }
                                if (map.isInMap(i, j + 1) && !tempCellInformation[i][j + 1].isLayerSet) {
                                    tempCellInformation[i][j + 1].setCellInfo(layer + 1);
                                }
                                if (map.isInMap(i, j - 1) && !tempCellInformation[i][j - 1].isLayerSet) {
                                    tempCellInformation[i][j - 1].setCellInfo(layer + 1);
                                }
                            }
                        }
                    }
                }
            }
            layer++;
        }
        int firstCellLayerNum = tempCellInformation[firstCellRow][firstCellColumn].layerNumber;
        if (tempCellInformation[firstCellRow - 1][firstCellColumn].isLayerSet && tempCellInformation[firstCellRow - 1][firstCellColumn].layerNumber < firstCellLayerNum)
            return Direction.UP;
        if (tempCellInformation[firstCellRow][firstCellColumn - 1].isLayerSet && tempCellInformation[firstCellRow][firstCellColumn - 1].layerNumber < firstCellLayerNum)
            return Direction.LEFT;
        if (tempCellInformation[firstCellRow + 1][firstCellColumn].isLayerSet && tempCellInformation[firstCellRow + 1][firstCellColumn].layerNumber < firstCellLayerNum)
            return Direction.DOWN;
        if (tempCellInformation[firstCellRow][firstCellColumn + 1].isLayerSet && tempCellInformation[firstCellRow][firstCellColumn + 1].layerNumber < firstCellLayerNum)
            return Direction.RIGHT;
        return Direction.UP;
    }

    public void preProcess(World world) {
        for (int i = 0; i < 4; i++) {
            blasterPlanInformation[i] = new BlasterPlanInformation();
        }
        setCellsInformation(world);
    }

    public void pickTurn(World world) {
        for (int i = 0; i < 4; i++) {
            if (championSelect[i] > 0) {
                championSelect[i]--;
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

    private void moveHeroToOBJ(World world, Hero hero) {
        Direction targetDirection = null;
        int cellRow = hero.getCurrentCell().getRow(), cellColumn = hero.getCurrentCell().getColumn();
        Map map = world.getMap();
        int mapRow = map.getRowNum(), mapColumn = map.getColumnNum();
        int minLayerNumber = initialCellInformation[cellRow][cellColumn].layerNumber;
        if (cellRow > 0 && initialCellInformation[cellRow - 1][cellColumn].layerNumber < minLayerNumber && !map.getCell(cellRow - 1, cellColumn).isWall()) {
            minLayerNumber = initialCellInformation[cellRow - 1][cellColumn].layerNumber;
            targetDirection = Direction.UP;
        }
        if (cellColumn > 0 && initialCellInformation[cellRow][cellColumn - 1].layerNumber < minLayerNumber && !map.getCell(cellRow, cellColumn - 1).isWall()) {
            minLayerNumber = initialCellInformation[cellRow][cellColumn - 1].layerNumber;
            targetDirection = Direction.LEFT;
        }
        if (cellRow < mapRow - 1 && initialCellInformation[cellRow + 1][cellColumn].layerNumber < minLayerNumber && !map.getCell(cellRow + 1, cellColumn).isWall()) {
            minLayerNumber = initialCellInformation[cellRow + 1][cellColumn].layerNumber;
            targetDirection = Direction.DOWN;
        }
        if (cellRow < mapColumn - 1 && initialCellInformation[cellRow][cellColumn + 1].layerNumber < minLayerNumber && !map.getCell(cellRow, cellColumn + 1).isWall()) {
            minLayerNumber = initialCellInformation[cellRow][cellColumn + 1].layerNumber;
            targetDirection = Direction.RIGHT;
        }
        System.out.println(hero.getId() + " " + targetDirection + " " + initialCellInformation[cellRow][cellColumn].layerNumber + " -> " + minLayerNumber);
        if (targetDirection != null)
            world.moveHero(hero, targetDirection);
    }

    private void setWeightForOffensivePlan(World world) {
        weightsOfAttack = new int[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        weightsOfBomb = new int[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        final int attackAOF = world.getMyHeroes()[0].getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect();
        final int bombAOF = world.getMyHeroes()[0].getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect();
        for (Hero enemyHero : world.getOppHeroes()) {
            int[] cell = {enemyHero.getCurrentCell().getRow(), enemyHero.getCurrentCell().getColumn()};
            if (cell[0] == -1) continue; // Hero is dead!
            for (int column = cell[1] - attackAOF; column <= cell[1] + attackAOF; column++) { // attack
                int rowDown = cell[0] + (attackAOF - absolute(column - cell[1]));
                int rowUp = cell[0] - (attackAOF - absolute(column - cell[1]));
                for (int row = rowUp; row <= rowDown; row++) {
                    weightsOfAttack[row][column]++;
                }
            }
            for (int column = cell[1] - bombAOF; column <= cell[1] + bombAOF; column++) { // bomb
                int rowDown = cell[0] + (bombAOF - absolute(column - cell[1]));
                int rowUp = cell[0] - (bombAOF - absolute(column - cell[1]));
                for (int row = rowUp; row <= rowDown; row++) {
                    weightsOfBomb[row][column]++;
                }
            }
        }
        System.out.println("Weight Of Attack:");
        for (int row = firstRowOfObjectiveZone; row <= lastRowOfObjectiveZone; row++) {
            for (int column = firstColumnOfObjectiveZone; column <= lastColumnOfObjectiveZone; column++) {
                System.out.print(weightsOfAttack[row][column] + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("Weight Of Bomb:");
        for (int row = firstRowOfObjectiveZone; row <= lastRowOfObjectiveZone; row++) {
            for (int column = firstColumnOfObjectiveZone; column <= lastColumnOfObjectiveZone; column++) {
                System.out.print(weightsOfBomb[row][column] + " ");
            }
            System.out.println();
        }
    }

    private void offensiveBlasterMove(World world, Hero myHero) {
        BlasterPlanInformation blaster = blasterPlanInformation[myHero.getId() / 2];
        if (world.manhattanDistance(myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn(), blaster.getOffensiveTargetCell()[0], blaster.getOffensiveTargetCell()[1]) > blaster.getRangeOfCasting() ||
                (blaster.getPlan() == PlanOfBlaster.ATTACK && !world.isInVision(myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn(), blaster.getOffensiveTargetCell()[0], blaster.getOffensiveTargetCell()[1]))) {
            Direction nextDirection = findPath(world, myHero.getId() / 2, world.getMap().getCell(blaster.getOffensiveTargetCell()[0], blaster.getOffensiveTargetCell()[1]));
            world.moveHero(myHero, nextDirection);
            System.out.println(myHero.getId() + " " + nextDirection);
        } else {
            System.out.println("in Range");
        }
    }

    public void moveTurn(World world) {
        int currentTurn = world.getCurrentTurn();
        switch (world.getMovePhaseNum()) {
            case 0:
                setWeightForOffensivePlan(world);
                for (Hero hero : world.getMyHeroes()) {
                    if (hero.getCurrentCell().getRow() == -1) continue; // Hero is dead
                    int i = hero.getId() / 2;
                    if (blasterPlanInformation[i].getPlan() == PlanOfBlaster.DODGE || !hero.getCurrentCell().isInObjectiveZone())
                        continue;
                    if (currentTurn % 4 == (i + 1) % 4) {
                        //Blaster Bomb
                        blasterPlanInformation[i].setPlanOfBlaster(world, hero, PlanOfBlaster.BOMB, weightsOfBomb, firstRowOfObjectiveZone, firstColumnOfObjectiveZone, lastRowOfObjectiveZone, lastColumnOfObjectiveZone);
                    } else {
                        //Blaster Attack
                        blasterPlanInformation[i].setPlanOfBlaster(world, hero, PlanOfBlaster.ATTACK, weightsOfAttack, firstRowOfObjectiveZone, firstColumnOfObjectiveZone, lastRowOfObjectiveZone, lastColumnOfObjectiveZone);
                    }
                }
                for (int i = 0; i < world.getMap().getRowNum(); i++) {
                    for (int j = 0; j < world.getMap().getColumnNum(); j++) {
                        initialCellInformation[i][j].isReservedForDodge = false;
                    }
                }
                //new part:
                currentAP = 100;
                for (int i = 0; i < 4; i++) {
                    Hero hero = world.getMyHeroes()[i];
                    BlasterPlanInformation blaster = blasterPlanInformation[i];
                    blaster.setMoveNum(0);
                    if (hero.getCurrentCell().getColumn() == -1)
                        continue;
                    switch (blaster.getPlan()) {
                        case BOMB:
                            currentAP -= hero.getAbility(AbilityName.BLASTER_BOMB).getAPCost();
                            break;
                        case ATTACK:
                            currentAP -= hero.getAbility(AbilityName.BLASTER_ATTACK).getAPCost();
                            break;
                        case DODGE:
                            currentAP -= hero.getAbility(AbilityName.BLASTER_DODGE).getAPCost();
                            break;
                    }
                    switch (blasterPlanInformation[i].getPlan()) {
                        case DEFAULT:
                            blaster.setMoveNum(6);
                            break;
                        case ATTACK:
                        case BOMB:
                            blaster.setMoveNum(world.manhattanDistance(hero.getCurrentCell().getRow(), hero.getCurrentCell().getColumn(), blasterPlanInformation[i].getOffensiveTargetCell()[0], blasterPlanInformation[i].getOffensiveTargetCell()[1]) - blasterPlanInformation[i].getRangeOfCasting());
                            if (blaster.getMoveNum() < 0)
                                blaster.setMoveNum(0);
                            break;
                        case DODGE:
                            blaster.setMoveNum(0);
                            break;
                    }
                }
                currentAP -= (blasterPlanInformation[0].getMoveNum() + blasterPlanInformation[1].getMoveNum() + blasterPlanInformation[2].getMoveNum() + blasterPlanInformation[3].getMoveNum()) * world.getMyHeroes()[0].getMoveAPCost();
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

                //end of new part
                break;
            case 3:
                setWeightForOffensivePlan(world);
                for (Hero hero : world.getMyHeroes()) {
                    if (hero.getCurrentCell().getRow() == -1) continue; // Hero is dead
                    int i = hero.getId() / 2;
                    if (blasterPlanInformation[i].getPlan() == PlanOfBlaster.DODGE || !hero.getCurrentCell().isInObjectiveZone())
                        continue;
                    if (currentTurn % 4 == (i + 1) % 4) {
                        //Blaster Bomb
                        blasterPlanInformation[i].setPlanOfBlaster(world, hero, PlanOfBlaster.BOMB, weightsOfBomb, firstRowOfObjectiveZone, firstColumnOfObjectiveZone, lastRowOfObjectiveZone, lastColumnOfObjectiveZone);
                    } else {
                        //Blaster Attack
                        blasterPlanInformation[i].setPlanOfBlaster(world, hero, PlanOfBlaster.ATTACK, weightsOfAttack, firstRowOfObjectiveZone, firstColumnOfObjectiveZone, lastRowOfObjectiveZone, lastColumnOfObjectiveZone);
                    }
                }
                break;
        }
        for (int i = 0; i < 4; i++) {
            Hero hero = world.getMyHeroes()[i];
            BlasterPlanInformation blaster = blasterPlanInformation[hero.getId() / 2];
            Cell currentCell = hero.getCurrentCell();
            if (currentCell.getRow() != -1) {
                int currentMoveNum = blaster.getMoveNum();
                if (currentCell.isInObjectiveZone()) { // in the objective zone
                    switch (blaster.getPlan()) {
                        case DEFAULT:
                            if (initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 5) {
                                targetReach[i] = true;
                            }
                            if (!targetReach[i]) {
                                if (currentMoveNum > 0) {
                                    moveHeroToOBJ(world, hero);
                                    blaster.setMoveNum(currentMoveNum - 1);
                                }
                            } else {
                                if (currentCell.getColumn() != targetCells[i].getColumn() || currentCell.getRow() != targetCells[i].getRow()) {
                                    if (currentMoveNum > 0) {
                                        world.moveHero(hero, findPath(world, i, targetCells[i]));
                                        System.out.println(hero.getId() + " " + findPath(world, i, targetCells[i]));
                                        blaster.setMoveNum(currentMoveNum - 1);
                                    }
                                } else {
                                    blaster.setFirstTimeLayout(true);
                                    System.out.println("in Position");
                                }
                            }
                            break;
                        case ATTACK:
                        case BOMB:
                            if (currentMoveNum > 0) {
                                offensiveBlasterMove(world, hero);
                                blaster.setMoveNum(currentMoveNum - 1);
                            }
                            break;
                    }
                } else { // out of objective zone
                    if (world.getMovePhaseNum() == 0)
                        setFlagDodgeOrWalk(world, hero, hero.getDodgeAbilities()[0].getRange());
                    switch (blaster.getPlan()) {
                        case DEFAULT:
                            if (initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 5)
                                targetReach[i] = true;
                            if (!targetReach[i]) {
                                if (currentMoveNum > 0) {
                                    moveHeroToOBJ(world, hero);
                                    blaster.setMoveNum(currentMoveNum - 1);
                                }
                            } else {
                                if (currentMoveNum > 0) {
                                    world.moveHero(hero, findPath(world, i, targetCells[i]));
                                    blaster.setMoveNum(currentMoveNum - 1);
                                }
                            }
                            break;
                        case DODGE:
                            break;
                    }
                }
            } else {
                blaster.setPlan(PlanOfBlaster.DEFAULT);
                blaster.setFirstTimeLayout(false);
                targetReach[i] = false;
            }
        }
    }

    private void blasterAction(World world, Hero hero) {
        BlasterPlanInformation blaster = blasterPlanInformation[hero.getId() / 2];
        int[] currentCell = {hero.getCurrentCell().getRow(), hero.getCurrentCell().getColumn()};
        int[] targetCell = blaster.getOffensiveTargetCell();
        int rangeOfCasting = blaster.getRangeOfCasting();
        if (world.manhattanDistance(currentCell[0], currentCell[1], targetCell[0], targetCell[1]) < rangeOfCasting && (blaster.getPlan() == PlanOfBlaster.BOMB || world.isInVision(currentCell[0], currentCell[1], targetCell[0], targetCell[1]))) {
            world.castAbility(hero, blaster.getAbilityName(), targetCell[0], targetCell[1]);
        } else { // not reached to target yet. so it should do a attack
            int maxWeight = 0;
            targetCell = new int[2];
            for (int column = currentCell[1] - rangeOfCasting; column <= currentCell[1] + rangeOfCasting; column++) { // attack
                int rowUp = currentCell[0] - (rangeOfCasting - absolute(column - currentCell[1]));
                int rowDown = currentCell[0] + (rangeOfCasting - absolute(column - currentCell[1]));
                for (int row = rowUp; row <= rowDown; row++) {
                    switch (blaster.getPlan()) {
                        case ATTACK:
                            if (weightsOfAttack[row][column] > maxWeight && world.isInVision(currentCell[0], currentCell[1], targetCell[0], targetCell[1])) {
                                maxWeight = weightsOfAttack[row][column];
                                targetCell[0] = row;
                                targetCell[1] = column;
                            }
                            break;
                        case BOMB:
                            if (weightsOfBomb[row][column] > maxWeight) {
                                maxWeight = weightsOfBomb[row][column];
                                targetCell[0] = row;
                                targetCell[1] = column;
                            }
                            break;
                    }
                }
            }
            if (maxWeight != 0) {
                world.castAbility(hero, blaster.getAbilityName(), targetCell[0], targetCell[1]);
            }
        }
        blaster.setPlan(PlanOfBlaster.DEFAULT);
    }

    public void actionTurn(World world) {
        setWeightForOffensivePlan(world);
        for (int i = 0; i < 4; i++) {
            Hero hero = world.getMyHeroes()[i];
            BlasterPlanInformation blaster = blasterPlanInformation[hero.getId() / 2];
            if (hero.getCurrentCell().getRow() == -1) continue; // hero is dead
            switch (blaster.getPlan()) {
                case DODGE:
                    world.castAbility(hero, hero.getDodgeAbilities()[0], blaster.getDodgeTargetCell()[0], blaster.getDodgeTargetCell()[1]);
                    blaster.setPlan(PlanOfBlaster.DEFAULT);
                    break;
                case ATTACK:
                case BOMB:
                    blasterAction(world, hero);
                    break;
            }
        }
    }
}
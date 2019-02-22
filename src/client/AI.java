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
                /*if (cellIsInObjectiveZone && map.getCell(i - 2, j).isInObjectiveZone() && map.getCell(i + 2, j).isInObjectiveZone() && map.getCell(i - 2, j - 2).isInObjectiveZone() && map.getCell(i + 2, j - 2).isInObjectiveZone() && map.getCell(i - 2, j + 2).isInObjectiveZone() && map.getCell(i + 2, j + 2).isInObjectiveZone() && map.getCell(i, j - 2).isInObjectiveZone() && map.getCell(i, j + 2).isInObjectiveZone()) {
                    if (absolute(targetRow - respawnRow) + absolute(targetColumn - respawnColumn) > absolute(i - respawnRow) + absolute(j - respawnColumn)) {
                        targetRow = i;
                        targetColumn = j;
                    }
                }*/
            }
        }
         /*
        |-||0||-|
        |3||T||1|
        |-||2||-|
        */
        /*targetCells[0] = map.getCell(targetRow - 2, targetColumn);
        targetCells[1] = map.getCell(targetRow, targetColumn + 2);
        targetCells[2] = map.getCell(targetRow + 2, targetColumn);
        targetCells[3] = map.getCell(targetRow, targetColumn - 2);*/
        targetRow = (firstRowOfObjectiveZone + lastRowOfObjectiveZone) / 2;
        targetColumn = (firstColumnOfObjectiveZone + lastColumnOfObjectiveZone) / 2;
        targetCells[0] = map.getCell((firstRowOfObjectiveZone + lastRowOfObjectiveZone) / 2, firstColumnOfObjectiveZone);
        targetCells[1] = map.getCell((firstRowOfObjectiveZone + lastRowOfObjectiveZone) / 2, lastColumnOfObjectiveZone);
        targetCells[2] = map.getCell(firstRowOfObjectiveZone, (firstColumnOfObjectiveZone + lastColumnOfObjectiveZone) / 2);
        targetCells[3] = map.getCell(lastRowOfObjectiveZone, (firstColumnOfObjectiveZone + lastColumnOfObjectiveZone) / 2);
        System.out.printf("res:%d %d%ntarget:%d %d%n", respawnColumn, respawnRow, targetColumn, targetRow);
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
                int rowUp = cell[0] + attackAOF - absolute(column - cell[1]);
                int rowDown = cell[0] - attackAOF + absolute(column - cell[1]);
                for (int row = rowDown; row <= rowUp; row++) {
                    weightsOfAttack[row][column]++;
                }
            }
            for (int column = cell[1] - bombAOF; column <= cell[1] + bombAOF; column++) { // bomb
                int rowUp = cell[0] + bombAOF - absolute(column - cell[1]);
                int rowDown = cell[0] - bombAOF + absolute(column - cell[1]);
                for (int row = rowDown; row <= rowUp; row++) {
                    weightsOfBomb[row][column]++;
                }
            }
        }
        for (int row = firstRowOfObjectiveZone; row <= lastRowOfObjectiveZone; row++) {
            for (int column = firstColumnOfObjectiveZone; column <= lastColumnOfObjectiveZone; column++) {
                System.out.print(weightsOfAttack[row][column] + " ");
            }
            System.out.println();
        }
        System.out.println();
        for (int row = firstRowOfObjectiveZone; row <= lastRowOfObjectiveZone; row++) {
            for (int column = firstColumnOfObjectiveZone; column <= lastColumnOfObjectiveZone; column++) {
                System.out.print(weightsOfBomb[row][column] + " ");
            }
            System.out.println();
        }
    }

    private void offensiveBlasterMove(World world, Hero myHero) {
        BlasterPlanInformation blaster = blasterPlanInformation[myHero.getId() / 2];
        if (world.manhattanDistance(myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn(), blaster.getOffensiveTargetCell()[0], blaster.getOffensiveTargetCell()[1]) > blaster.getRangeOfCasting() - 1) {
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
                    if (blasterPlanInformation[i].getPlan() == PlanOfBlaster.DODGE || !hero.getCurrentCell().isInObjectiveZone()) continue;
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
                break;
            case 3:
                setWeightForOffensivePlan(world);
                for (Hero hero : world.getMyHeroes()) {
                    if (hero.getCurrentCell().getRow() == -1) continue; // Hero is dead
                    int i = hero.getId() / 2;
                    if (blasterPlanInformation[i].getPlan() == PlanOfBlaster.DODGE || !hero.getCurrentCell().isInObjectiveZone()) continue;
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
                if (currentCell.isInObjectiveZone()) { // in the objective zone
                    switch (blaster.getPlan()) {
                        case DEFAULT:
                            if (initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 5)
                                targetReach[i] = true;
                            if (!targetReach[i]) moveHeroToOBJ(world, hero);
                            else {
                                if (currentCell.getColumn() != targetCells[i].getColumn() || currentCell.getRow() != targetCells[i].getRow()) {
                                    world.moveHero(hero, findPath(world, i, targetCells[i]));
                                    System.out.println(hero.getId() + " " + findPath(world, i, targetCells[i]));
                                } else {
                                    blaster.setFirstTimeLayout(true);
                                    System.out.println("in Position");
                                }
                            }
                            break;
                        case ATTACK:
                        case BOMB: // TODO: check motion bugs.
                            if (world.getMovePhaseNum() == 4 || world.getMovePhaseNum() == 5)
                                offensiveBlasterMove(world, hero);
                            break;
                    }
                } else { // out of objective zone
                    if (world.getMovePhaseNum() == 0)
                        setFlagDodgeOrWalk(world, hero, hero.getDodgeAbilities()[0].getRange());
                    switch (blaster.getPlan()) {
                        case DEFAULT:
                            if (initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 5)
                                targetReach[i] = true;
                            if (!targetReach[i]) moveHeroToOBJ(world, hero);
                            else world.moveHero(hero, findPath(world, i, targetCells[i]));
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
        if (world.manhattanDistance(currentCell[0], currentCell[1], targetCell[0], targetCell[1]) < rangeOfCasting) {
            if (blaster.getPlan() == PlanOfBlaster.BOMB || world.isInVision(currentCell[0], currentCell[1], targetCell[0], targetCell[1])) {
                world.castAbility(hero, blaster.getAbilityName(), targetCell[0], targetCell[1]); }
        } else { // not reached to target yet. so it should do a attack
            int maxWeight = 0;
            targetCell = new int[2];
            for (int column = currentCell[1] - rangeOfCasting; column <= currentCell[1] + rangeOfCasting; column++) { // attack
                int rowUp = currentCell[0] + rangeOfCasting - absolute(column - currentCell[1]);
                int rowDown = currentCell[0] - rangeOfCasting + absolute(column - currentCell[1]);
                for (int row = rowDown; row <= rowUp; row++) {
                    switch (blaster.getPlan()) {
                        case ATTACK:
                            if (weightsOfAttack[row][column] > maxWeight) {
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
                case ATTACK: case BOMB:
                    blasterAction(world, hero);
                    break;
            }
        }
    }
}
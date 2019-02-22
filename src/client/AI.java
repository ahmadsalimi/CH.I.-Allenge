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
        blaster.setPlan(PlanOfBlaster.INITIAL);
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
                if (cellIsInObjectiveZone && map.getCell(i - 2, j).isInObjectiveZone() && map.getCell(i + 2, j).isInObjectiveZone() && map.getCell(i - 2, j - 2).isInObjectiveZone() && map.getCell(i + 2, j - 2).isInObjectiveZone() && map.getCell(i - 2, j + 2).isInObjectiveZone() && map.getCell(i + 2, j + 2).isInObjectiveZone() && map.getCell(i, j - 2).isInObjectiveZone() && map.getCell(i, j + 2).isInObjectiveZone()) {
                    if (absolute(targetRow - respawnRow) + absolute(targetColumn - respawnColumn) > absolute(i - respawnRow) + absolute(j - respawnColumn)) {
                        targetRow = i;
                        targetColumn = j;
                    }
                }
            }
        }
         /*
        |-||0||-|
        |3||T||1|
        |-||2||-|
        */
        targetCells[0] = map.getCell(targetRow - 2, targetColumn);
        targetCells[1] = map.getCell(targetRow, targetColumn + 2);
        targetCells[2] = map.getCell(targetRow + 2, targetColumn);
        targetCells[3] = map.getCell(targetRow, targetColumn - 2);
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

    public Direction findPath(World world, int heroNum, Cell targetCell) {
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

    public void moveHeroToOBJ(World world, Hero hero) {
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

    public void setWeightForOffensivePlan(World world) {
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
    }

    public void offensiveBlasterMove(World world, Hero myHero) {
        BlasterPlanInformation blaster = blasterPlanInformation[myHero.getId() / 2];
        if (world.manhattanDistance(myHero.getCurrentCell().getRow(), myHero.getCurrentCell().getColumn(), blaster.getOffensiveTargetCell()[0], blaster.getOffensiveTargetCell()[1]) > blaster.getRangeOfCasting() - 1) {
            Direction nextDirection = findPath(world, myHero.getId(), world.getMap().getCell(blaster.getOffensiveTargetCell()[0], blaster.getOffensiveTargetCell()[1]));
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
                    if (blasterPlanInformation[i].getPlan() == PlanOfBlaster.INITIAL || blasterPlanInformation[i].getPlan() == PlanOfBlaster.DODGE) continue;
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
            System.out.println("Hero" + hero.getId() + "'s plan: " + blaster.getPlan());
            Cell currentCell = hero.getCurrentCell();
            if (currentCell.getRow() != -1) {
                if (currentCell.isInObjectiveZone()) {
                    switch (blaster.getPlan()) {
                        case INITIAL:
                            if (initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 5)
                                targetReach[i] = true;
                            if (!targetReach[i]) moveHeroToOBJ(world, hero);
                            else {
                                if (currentCell.getColumn() != targetCells[i].getColumn() || currentCell.getRow() != targetCells[i].getRow()) {
                                    world.moveHero(hero, findPath(world, i, targetCells[i]));
                                    System.out.println(hero.getId() + " " + findPath(world, i, targetCells[i]));
                                } else {
                                    blaster.setPlan(PlanOfBlaster.DEFAULT);
                                    System.out.println("in Position");
                                }
                            }
                            break;
                        case DEFAULT:

                            break;
                        case ATTACK:
                            switch (world.getMovePhaseNum()) {
                                case 4:
                                case 5:
                                    offensiveBlasterMove(world, hero);
                                    break;
                            }
                    }
                    // TODO: add moving for different plans.
                } else {
                    if (world.getMovePhaseNum() == 0) {
                        setFlagDodgeOrWalk(world, hero, hero.getDodgeAbilities()[0].getRange());
                    }
                    switch (blaster.getPlan()) {
                        case INITIAL:
                            if (initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 5)
                                targetReach[i] = true;
                            if (!targetReach[i]) moveHeroToOBJ(world, hero);
                            else {
                                world.moveHero(hero, findPath(world, i, targetCells[i]));
                            }
                            break;
                        case DEFAULT:

                            break;
                    }
                }
            } else {
                blaster.setPlan(PlanOfBlaster.INITIAL);
                targetReach[i] = false;
            }
        }

        /*Map map = world.getMap();
         *//*
        |-||0||-|
        |3||T||1|
        |-||2||-|
        *//*
        Cell[] targetCells = new Cell[4];
        targetCells[0] = map.getCell(targetRow - 2, targetColumn);
        targetCells[1] = map.getCell(targetRow, targetColumn + 2);
        targetCells[2] = map.getCell(targetRow + 2, targetColumn);
        targetCells[3] = map.getCell(targetRow, targetColumn - 2);

        Hero[] enemyHeroes = world.getOppHeroes();
        System.out.printf("E%d %d %d  E%d %d %d  E%d %d %d  E%d %d %d%n", enemyHeroes[0].getId(), enemyHeroes[0].getCurrentCell().getRow(), enemyHeroes[0].getCurrentCell().getColumn(),
                enemyHeroes[1].getId(), enemyHeroes[1].getCurrentCell().getRow(), enemyHeroes[1].getCurrentCell().getColumn(),
                enemyHeroes[2].getId(), enemyHeroes[2].getCurrentCell().getRow(), enemyHeroes[2].getCurrentCell().getColumn(),
                enemyHeroes[3].getId(), enemyHeroes[3].getCurrentCell().getRow(), enemyHeroes[3].getCurrentCell().getColumn());

        for (int i = 0; i < 4; i++) {
            Hero hero = world.getMyHeroes()[i];
            Cell currentCell = hero.getCurrentCell();
            if (currentCell.getRow() == -1) {
                targetReach[i] = false;
                continue;
            }
            if (currentCell.getRow() != -1 && initialCellInformation[currentCell.getRow()][currentCell.getColumn()].layerNumber < 3)
                targetReach[i] = true;
            if (!currentCell.isInObjectiveZone()) {
                if (currentCell.getRow() != -1) { // Hero is alive
                    moveHeroToOBJ(world, hero);
                }
            } else {
                //War Moves
                if (currentTurn % 4 == (i + 1) % 4) {
                    //Blaster Bomb

                }
                if (currentTurn % 4 == (i + 2) % 4 || currentTurn % 4 == (i + 3) % 4 || currentTurn % 4 == (i + 4) % 4) {
                    //Blaster Attack
                    offensiveBlasterMove(i, world);
                }
            }
        }*/
    }

    public void attackBlasterAction(int heroNum, World world) {
        Hero[] enemyHeroes = world.getOppHeroes();
        Hero[] myHeroes = world.getMyHeroes();
        int nearestEnemyManhattanDistance = 10;
        Cell targetEnemyCell = enemyHeroes[0].getCurrentCell();
        for (int i = 0; i < 4; i++) {
            Cell enemyHeroCurrentCell = enemyHeroes[i].getCurrentCell();
            Cell myHeroCurrentCell = myHeroes[heroNum].getCurrentCell();
            if (enemyHeroCurrentCell.isInObjectiveZone() && world.manhattanDistance(enemyHeroCurrentCell, myHeroCurrentCell) < nearestEnemyManhattanDistance) {
                nearestEnemyManhattanDistance = world.manhattanDistance(enemyHeroCurrentCell, myHeroCurrentCell);
                targetEnemyCell = enemyHeroCurrentCell;
            }
        }
        if (nearestEnemyManhattanDistance <= 4)
            world.castAbility(myHeroes[heroNum], AbilityName.BLASTER_ATTACK, targetEnemyCell);
    }

    public void actionTurn(World world) {
        setWeightForOffensivePlan(world);
        Hero[] enemyHeroes = world.getOppHeroes();
        System.out.printf("E%d %d %d  E%d %d %d  E%d %d %d  E%d %d %d%n", enemyHeroes[0].getId(), enemyHeroes[0].getCurrentCell().getRow(), enemyHeroes[0].getCurrentCell().getColumn(),
                enemyHeroes[1].getId(), enemyHeroes[1].getCurrentCell().getRow(), enemyHeroes[1].getCurrentCell().getColumn(),
                enemyHeroes[2].getId(), enemyHeroes[2].getCurrentCell().getRow(), enemyHeroes[2].getCurrentCell().getColumn(),
                enemyHeroes[3].getId(), enemyHeroes[3].getCurrentCell().getRow(), enemyHeroes[3].getCurrentCell().getColumn());

        for (int i = 0; i < 4; i++) {
            Hero hero = world.getMyHeroes()[i];
            BlasterPlanInformation blaster = blasterPlanInformation[hero.getId() / 2];
            int currentTurn = world.getCurrentTurn();
            if (!hero.getCurrentCell().isInObjectiveZone()) {
                if (blaster.getPlan() == PlanOfBlaster.DODGE) {
                    world.castAbility(hero, hero.getDodgeAbilities()[0], blaster.getDodgeTargetCell()[0], blaster.getDodgeTargetCell()[1]);
                    blaster.setPlan(PlanOfBlaster.INITIAL);
                }
            } else {
                //War Action
                if (currentTurn % 4 == (i + 1) % 4) {
                    //Blaster Bomb
                }
                if (currentTurn % 4 == (i + 2) % 4) {
                    //Blaster Attack
                    attackBlasterAction(i, world);
                }
                if (currentTurn % 4 == (i + 3) % 4) {

                }
                if (currentTurn % 4 == (i + 4) % 4) {
                    //Blaster Attack
                    attackBlasterAction(i, world);
                }
            }
        }
    }
}
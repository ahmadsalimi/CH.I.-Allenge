package client;

import client.model.*;

class CellInformation {
  boolean isLayerSet;
  int layerNumber;
  boolean isReservedForDodge = false;

  void setCellInfo(int distance) {
    isLayerSet = true;
    layerNumber = distance;
  }
}

class FlagDodgeOrWalk {
  boolean flag;
  int[] cell = new int[2];
  int[] targetCell = new int[2];
}

public class AI {
  private final int WALL = 99;
  private FlagDodgeOrWalk[] flagDOW;
  private int[] championSelect = {0, 1, 3, 0};
  private CellInformation[][] initialCellInformation;
  private int targetRow = 500, targetColumn = 500;
  private boolean targetReach[] = {false, false, false, false};

  private int absolute(int x) {
    return x >= 0 ? x : -x;
  }

  private void setFlagDodgeOrWalk(World world) {
    flagDOW = new FlagDodgeOrWalk[4];
    Map map = world.getMap();
    for (int i = 0; i < 4; i++) {
      int[] cell = {map.getMyRespawnZone()[i].getRow(), map.getMyRespawnZone()[i].getColumn()};
      flagDOW[i] = new FlagDodgeOrWalk();
      flagDOW[i].cell[0] = cell[0];
      flagDOW[i].cell[1] = cell[1];
      int maxTargetDistance = 0;
      int[] finalTargetCell = new int[2];
      for (int column = cell[1] - 4; column <= cell[1] + 4; column++) {
        int rowUp = cell[0] + 4 - absolute(column - cell[1]);
        if (map.isInMap(rowUp, column) && !map.getCell(rowUp, column).isWall() && !initialCellInformation[rowUp][column].isReservedForDodge) {
          if (maxTargetDistance <= initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowUp][column].layerNumber) {
            maxTargetDistance = initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowUp][column].layerNumber;
            finalTargetCell[0] = rowUp;
            finalTargetCell[1] = column;
          }
        }
        int rowDown = cell[0] - 4 + absolute(column - cell[1]);
        if (map.isInMap(rowDown, column) && !map.getCell(rowDown, column).isWall() && !initialCellInformation[rowDown][column].isReservedForDodge) {
          if (maxTargetDistance <= initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowDown][column].layerNumber) {
            maxTargetDistance = initialCellInformation[cell[0]][cell[1]].layerNumber - initialCellInformation[rowDown][column].layerNumber;
            finalTargetCell[0] = rowDown;
            finalTargetCell[1] = column;
          }
        }
      }
      flagDOW[i].flag = false;
      if (maxTargetDistance > 6) {
        flagDOW[i].targetCell[0] = finalTargetCell[0];
        flagDOW[i].targetCell[1] = finalTargetCell[1];
        flagDOW[i].flag = true;
        initialCellInformation[finalTargetCell[0]][finalTargetCell[1]].isReservedForDodge = true;
      }
      System.out.println(flagDOW[i].flag);
    }

  }

  private void setCellsInformation(World world) {
    Map map = world.getMap();
    int row = map.getRowNum(), column = map.getColumnNum();
    initialCellInformation = new CellInformation[row][column];
    int respawnRow = map.getMyRespawnZone()[0].getRow(), respawnColumn = map.getMyRespawnZone()[0].getColumn();
    for (int i = 2; i < row - 2; i++) {
      for (int j = 2; j < column - 2; j++) {
        if (map.getCell(i, j).isInObjectiveZone() && map.getCell(i - 2, j).isInObjectiveZone() && map.getCell(i + 2, j).isInObjectiveZone() && map.getCell(i - 2, j - 2).isInObjectiveZone() && map.getCell(i + 2, j - 2).isInObjectiveZone() && map.getCell(i - 2, j + 2).isInObjectiveZone() && map.getCell(i + 2, j + 2).isInObjectiveZone() && map.getCell(i, j - 2).isInObjectiveZone() && map.getCell(i, j + 2).isInObjectiveZone()) {
          if (absolute(targetRow - respawnRow) + absolute(targetColumn - respawnColumn) >
                  absolute(i - respawnRow) + absolute(j - respawnColumn)) {
            targetColumn = j;
            targetRow = i;
          }
        }
      }
    }
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

  public Direction FindPath(World world, int heroNum, Cell targetCell) {
    Map map = world.getMap();
    int row = map.getRowNum(), column = map.getColumnNum();
    CellInformation[][] tempCellInformation = new CellInformation[row][column];
    for (int i = 0; i < row; i++) {
      for (int j = 0; j < column; j++) {
        tempCellInformation[i][j] = new CellInformation();
        if (map.getCell(i, j).isWall()) tempCellInformation[i][j].setCellInfo(WALL);
      }
    }

    if (heroNum != 0)
      tempCellInformation[world.getMyHeroes()[0].getCurrentCell().getRow()][world.getMyHeroes()[0].getCurrentCell().getColumn()].setCellInfo(WALL);
    if (heroNum != 1)
      tempCellInformation[world.getMyHeroes()[1].getCurrentCell().getRow()][world.getMyHeroes()[1].getCurrentCell().getColumn()].setCellInfo(WALL);
    if (heroNum != 2)
      tempCellInformation[world.getMyHeroes()[2].getCurrentCell().getRow()][world.getMyHeroes()[2].getCurrentCell().getColumn()].setCellInfo(WALL);
    if (heroNum != 3)
      tempCellInformation[world.getMyHeroes()[3].getCurrentCell().getRow()][world.getMyHeroes()[3].getCurrentCell().getColumn()].setCellInfo(WALL);

    tempCellInformation[targetCell.getRow()][targetCell.getColumn()].setCellInfo(0);
    boolean firstCellIsSet = false;
    int layer = 0;
    int firstCellRow = world.getMyHeroes()[heroNum].getCurrentCell().getRow(), firstCellColumn = world.getMyHeroes()[heroNum].getCurrentCell().getColumn();
    if (firstCellColumn==-1)
      return Direction.values()[0];
    while (!tempCellInformation[firstCellRow][firstCellColumn].isLayerSet) {
      for (int i = 0; i < row; i++) {
        for (int j = 0; j < column; j++) {
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
    setCellsInformation(world);
    setFlagDodgeOrWalk(world);
  }

  public void pickTurn(World world) {
    for (int i = 0; i < 4; i++) {
      if (championSelect[i] > 0) {
        championSelect[i]--;
        if (i == 0) world.pickHero(HeroName.SENTRY);
        if (i == 1) world.pickHero(HeroName.BLASTER);
        if (i == 2) world.pickHero(HeroName.HEALER);
        if (i == 3) world.pickHero(HeroName.GUARDIAN);
        break;
      }
    }
  }

  private void printCurrentMap(World world) {
    String ANSI_RESET = "\u001B[0m";
    String ANSI_RED = "\u001B[31m";
    String ANSI_PURPLE = "\u001B[35m";
    String ANSI_GREEN = "\u001B[32m";
    String ANSI_CYAN = "\u001B[36m";
    for (int i = 0; i < 31; i++) {
      for (int j = 0; j < 31; j++) {
        boolean flag = true;
        Cell thisCell = world.getMap().getCell(i, j);
        if (thisCell.isWall()) {
          System.out.print(ANSI_GREEN + "|#|" + ANSI_RESET);
          flag = false;
        }
        if (flag) {
          for (Hero hero : world.getMyHeroes()) {
            if (hero.getCurrentCell().equals(thisCell)) {
              System.out.print(ANSI_CYAN + "H:" + hero.getId() + ANSI_RESET);
              flag = false;
              break;
            }
          }
        }
        if (flag) {
          for (Hero hero : world.getOppHeroes()) {
            if (hero.getCurrentCell().equals(thisCell)) {
              System.out.print(ANSI_RED + "E:" + hero.getId() + ANSI_RESET);
              flag = false;
              break;
            }
          }
        }
        if (flag && thisCell.isInObjectiveZone()) {
          System.out.print(ANSI_PURPLE + " * " + ANSI_RESET);
          flag = false;
        }
        if (flag && thisCell.isInVision()) {
          System.out.print(" . ");
          flag = false;
        }
        if (flag)
          System.out.print("   ");
      }
      if (i < 4) {
        System.out.print(world.getMyHeroes()[i].toString());
      } else if (i == 4) {
        System.out.print(world.getMyScore());
      } else if (i == 5)
        System.out.print(world.getAP());
      System.out.println();
    }
  }

  public void moveHeroToOBJ(World world, Hero hero) {
    Direction targetDirection = null;
    int cellRow = hero.getCurrentCell().getRow(), cellColumn = hero.getCurrentCell().getColumn();
    int mapRow = world.getMap().getRowNum(), mapColumn = world.getMap().getColumnNum();
    int minLayerNumber = initialCellInformation[cellRow][cellColumn].layerNumber;
    if (cellRow > 0 && initialCellInformation[cellRow - 1][cellColumn].layerNumber < minLayerNumber && !world.getMap().getCell(cellRow - 1, cellColumn).isWall()) {
      minLayerNumber = initialCellInformation[cellRow - 1][cellColumn].layerNumber;
      targetDirection = Direction.UP;
    }
    if (cellColumn > 0 && initialCellInformation[cellRow][cellColumn - 1].layerNumber < minLayerNumber && !world.getMap().getCell(cellRow, cellColumn - 1).isWall()) {
      minLayerNumber = initialCellInformation[cellRow][cellColumn - 1].layerNumber;
      targetDirection = Direction.LEFT;
    }
    if (cellRow < mapRow - 1 && initialCellInformation[cellRow + 1][cellColumn].layerNumber < minLayerNumber && !world.getMap().getCell(cellRow + 1, cellColumn).isWall()) {
      minLayerNumber = initialCellInformation[cellRow + 1][cellColumn].layerNumber;
      targetDirection = Direction.DOWN;
    }
    if (cellRow < mapColumn - 1 && initialCellInformation[cellRow][cellColumn + 1].layerNumber < minLayerNumber && !world.getMap().getCell(cellRow, cellColumn + 1).isWall()) {
      minLayerNumber = initialCellInformation[cellRow][cellColumn + 1].layerNumber;
      targetDirection = Direction.RIGHT;
    }
    System.out.println(hero.getId() + " " + targetDirection + " " + initialCellInformation[cellRow][cellColumn].layerNumber + " -> " + minLayerNumber);
    //world.moveHero(hero, targetDirection);
    if (targetDirection != null)
      world.moveHero(hero, targetDirection);
  }

  private boolean firstTimeDodge = true;

  public void moveTurn(World world) {
    int enemyNum = 0;
    double enemyX = 0, enemyY = 0;
    int battleGroundNum = 0, battleGroundColumn = targetColumn - 1, battleGroundRow = targetRow - 1;
    Map map = world.getMap();
        /*
        |-||0||-|
        |3||T||1|
        |-||2||-|
        */
    Cell[] targetCells = new Cell[4];
    targetCells[0] = map.getCell(targetRow - 2, targetColumn);
    targetCells[1] = map.getCell(targetRow, targetColumn + 2);
    targetCells[2] = map.getCell(targetRow + 2, targetColumn);
    targetCells[3] = map.getCell(targetRow, targetColumn - 2);

    Hero enemyHeroes[] = world.getOppHeroes();
    System.out.printf("E%d %d %d  E%d %d %d  E%d %d %d  E%d %d %d%n", enemyHeroes[0].getId(), enemyHeroes[0].getCurrentCell().getRow(), enemyHeroes[0].getCurrentCell().getColumn(),
            enemyHeroes[1].getId(), enemyHeroes[1].getCurrentCell().getRow(), enemyHeroes[1].getCurrentCell().getColumn(),
            enemyHeroes[2].getId(), enemyHeroes[2].getCurrentCell().getRow(), enemyHeroes[2].getCurrentCell().getColumn(),
            enemyHeroes[3].getId(), enemyHeroes[3].getCurrentCell().getRow(), enemyHeroes[3].getCurrentCell().getColumn());

    for (int i = 0; i < 4; i++) {
      Hero hero = world.getMyHeroes()[i];
      if (hero.getCurrentCell().getRow() != -1 && initialCellInformation[hero.getCurrentCell().getRow()][hero.getCurrentCell().getColumn()].layerNumber < 5)
        targetReach[i] = true;
      if (!targetReach[i]) {
        boolean isInRespawnZone = false;
        if (firstTimeDodge) {
          for (FlagDodgeOrWalk flagDow : flagDOW) {
            if (flagDow.flag && flagDow.cell[0] == hero.getCurrentCell().getRow() && flagDow.cell[1] == hero.getCurrentCell().getColumn()) {
              isInRespawnZone = true;
              break;
            }
          }
        }
        if (!isInRespawnZone)
          moveHeroToOBJ(world, hero);
      } else {
        if (hero.getCurrentCell().getColumn() >= 0 && world.getMyHeroes()[i].getCurrentCell().getRow() >= 0 && targetCells[i].getColumn() >= 0 && hero.getCurrentCell().getColumn() != targetCells[i].getColumn() || hero.getCurrentCell().getRow() != targetCells[i].getRow()) {
          world.moveHero(hero, FindPath(world, i, targetCells[i]));
          System.out.println(hero.getId() + " " + FindPath(world, i, targetCells[i]));
        } else
          System.out.println("in Position");
      }
    }
    printCurrentMap(world);
  }

  public void actionTurn(World world) {

    Hero enemyHeroes[] = world.getOppHeroes();
    System.out.printf("E%d %d %d  E%d %d %d  E%d %d %d  E%d %d %d%n", enemyHeroes[0].getId(), enemyHeroes[0].getCurrentCell().getRow(), enemyHeroes[0].getCurrentCell().getColumn(),
            enemyHeroes[1].getId(), enemyHeroes[1].getCurrentCell().getRow(), enemyHeroes[1].getCurrentCell().getColumn(),
            enemyHeroes[2].getId(), enemyHeroes[2].getCurrentCell().getRow(), enemyHeroes[2].getCurrentCell().getColumn(),
            enemyHeroes[3].getId(), enemyHeroes[3].getCurrentCell().getRow(), enemyHeroes[3].getCurrentCell().getColumn());
    for (Hero hero : world.getMyHeroes()) {
      if (hero.getName().equals(HeroName.BLASTER)) {
        for (Hero enemy : enemyHeroes) {
          if (enemy.getCurrentCell().getColumn() != -1 && world.manhattanDistance(hero.getCurrentCell(), enemy.getCurrentCell()) <= 4 && hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
            world.castAbility(hero, AbilityName.BLASTER_ATTACK, enemy.getCurrentCell());
          }
          if (enemy.getCurrentCell().getColumn() != -1 && world.manhattanDistance(hero.getCurrentCell(), enemy.getCurrentCell()) == 5 && hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
            world.castAbility(hero, AbilityName.BLASTER_BOMB, enemy.getCurrentCell());
          }
          break;
        }
        break;
      }
    }
    if (firstTimeDodge) {
      for (FlagDodgeOrWalk flag : flagDOW) {
        if (flag.flag) {

          world.castAbility(world.getMyHero(flag.cell[0], flag.cell[1]), AbilityName.HEALER_DODGE, flag.targetCell[0], flag.targetCell[1]);
          System.out.println("Cast done {dodge from " + flag.cell[0] + " , " + flag.cell[1] + " to " + flag.targetCell[0] + " to " + flag.targetCell[1]);
        }
      }
      firstTimeDodge = false;
    }

  }
}

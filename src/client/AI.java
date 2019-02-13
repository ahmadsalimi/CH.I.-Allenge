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
  private CellInformation[][] cellInformation;
  private FlagDodgeOrWalk[] flagDOW;

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
        if (map.isInMap(rowUp, column) && !map.getCell(rowUp, column).isWall() && !cellInformation[rowUp][column].isReservedForDodge) {
          if (maxTargetDistance <= cellInformation[cell[0]][cell[1]].layerNumber - cellInformation[rowUp][column].layerNumber) {
            maxTargetDistance = cellInformation[cell[0]][cell[1]].layerNumber - cellInformation[rowUp][column].layerNumber;
            finalTargetCell[0] = rowUp;
            finalTargetCell[1] = column;
          }
        }
        int rowDown = cell[0] - 4 + absolute(column - cell[1]);
        if (map.isInMap(rowDown, column) && !map.getCell(rowDown, column).isWall() && !cellInformation[rowDown][column].isReservedForDodge) {
          if (maxTargetDistance <= cellInformation[cell[0]][cell[1]].layerNumber - cellInformation[rowDown][column].layerNumber) {
            maxTargetDistance = cellInformation[cell[0]][cell[1]].layerNumber - cellInformation[rowDown][column].layerNumber;
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
        cellInformation[finalTargetCell[0]][finalTargetCell[1]].isReservedForDodge = true;
      }
      System.out.println(flagDOW[i].flag);
    }

  }

  private void setCellsInformation(World world) {
    Map map = world.getMap();
    int row = map.getRowNum(), column = map.getColumnNum();
    cellInformation = new CellInformation[row][column];
    for (int i = 0; i < row; i++) {
      for (int j = 0; j < column; j++) {
        cellInformation[i][j] = new CellInformation();
        if (map.getCell(i, j).isInObjectiveZone()) cellInformation[i][j].setCellInfo(0);
        if (map.getCell(i, j).isWall()) cellInformation[i][j].setCellInfo(WALL);
      }
    }
    boolean allCellsIsSet = false;
    int layer = 0;
    while (!allCellsIsSet) {
      allCellsIsSet = true;
      for (int i = 0; i < row; i++) {
        for (int j = 0; j < column; j++) {
          if (!map.getCell(i, j).isWall()) {
            if (cellInformation[i][j].isLayerSet) {
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
            } else allCellsIsSet = false;
          }
        }
      }
      layer++;
    }
    for (int i = 0; i < row; i++) {
      for (int j = 0; j < column; j++) {
        System.out.printf("%2d ", cellInformation[i][j].layerNumber);
      }
      System.out.println();
    }
  }

  public void preProcess(World world) {
    setCellsInformation(world);
    setFlagDodgeOrWalk(world);
  }

  public void pickTurn(World world) {
    world.pickHero(HeroName.HEALER);
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
        if (thisCell.isWall()) System.out.print(ANSI_GREEN + "|#|" + ANSI_RESET);
        else {
          for (Hero hero : world.getMyHeroes()) {
            if (hero.getCurrentCell().equals(thisCell)) {
              System.out.print(ANSI_CYAN + "\"" + hero.getId() + "\"" + ANSI_RESET);
              flag = false;
              break;
            }
          }
          if (!flag) continue;
          for (Hero hero : world.getOppHeroes()) {
            if (hero.getCurrentCell().equals(thisCell)) {
              System.out.print(ANSI_RED + "\"" + hero.getId() + "\"" + ANSI_RESET);
              flag = false;
              break;
            }
          }
          if (!flag) continue;
          if (thisCell.isInVision()) System.out.print(" . ");
          else if (thisCell.isInObjectiveZone()) System.out.print(ANSI_PURPLE + " * " + ANSI_RESET);
          else System.out.print("   ");
        }
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

  private boolean firstTimeDodge = true;

  public void moveHeroToOBJ(World world, Hero hero) {
    Direction targetDirection = null;
    int cellRow = hero.getCurrentCell().getRow(), cellColumn = hero.getCurrentCell().getColumn();
    int minLayerNumber = cellInformation[cellRow][cellColumn].layerNumber;
    int[][] targetCandidates = {
            {cellRow - 1, cellColumn},
            {cellRow + 1, cellColumn},
            {cellRow, cellColumn - 1},
            {cellRow, cellColumn + 1}
    };
    for (int i = 0; i < 4; i++) {
      if (cellInformation[targetCandidates[i][0]][targetCandidates[i][1]].layerNumber < minLayerNumber) {
        targetDirection = Direction.values()[i];
        minLayerNumber = cellInformation[targetCandidates[i][0]][targetCandidates[i][1]].layerNumber;
      }
    }
    System.out.println(hero.getId() + " " + targetDirection + " " + cellInformation[cellRow][cellColumn].layerNumber + " -> " + minLayerNumber);
    world.moveHero(hero, targetDirection);
  }

  public void moveTurn(World world) {
    for (Hero hero : world.getMyHeroes()) {
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
    }
    printCurrentMap(world);
  }

  public void actionTurn(World world) {
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
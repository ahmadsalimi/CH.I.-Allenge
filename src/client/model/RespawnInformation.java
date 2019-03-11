package client.model;

import client.AI;

import java.util.Arrays;

public class RespawnInformation {
    private int[] respawnCell;
    private int[] targetCell;
    private static boolean finishTarget = false;

    public RespawnInformation(Cell respawnCell) {
        int[] respawnArray = {respawnCell.getRow(), respawnCell.getColumn()};
        System.out.println("start of respawn info!");
        setRespawnCell(respawnArray);
        System.out.println("respawn cell set");
    }

    public int[] getRespawnCell() {
        return respawnCell;
    }

    public int[] getTargetCell() {
        return targetCell;
    }

    public void setRespawnCell(int[] respawnCell) {
        this.respawnCell = respawnCell;
    }

    public void setTargetCell(int... targetCell) {
        this.targetCell = targetCell;
    }

    private static int absolute(int x) {
        return x >= 0 ? x : -x;
    }

    private static boolean checkInObjectivePoint(boolean[][] objectivePoints, int i, int j, int index) {
        if (objectivePoints[i][j]) {
            AI.respawnInformation[index].setTargetCell(i, j);
            return true;
        }
        return false;
    }

    private static boolean setDistanceOfHeroes(boolean[][] objectivePoints, int i, int j) {
        for (int column = j - 4; column <= j + 4; column++) { // attack
            int rowDown = i + (4 - absolute(column - j));
            int rowUp = i - (4 - absolute(column - j));
            for (int row = rowUp; row <= rowDown; row++) {
                if (objectivePoints[row][column]) {
                    objectivePoints[row][column] = false;
                }
            }
        }
        return true;
    }

    static private boolean setCellLayers(Map map, CellInformation[][] cellInformation, boolean[][] objectivePoints, int layer, int i, int j, int index) {

        if (layer == cellInformation[i][j].layerNumber) {
            if (map.isInMap(i + 1, j) && !cellInformation[i + 1][j].isLayerSet) {
                cellInformation[i + 1][j].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i + 1, j, index)) {
                    return setDistanceOfHeroes(objectivePoints, i + 1, j);
                }
            }
            if (map.isInMap(i - 1, j) && !cellInformation[i - 1][j].isLayerSet) {
                cellInformation[i - 1][j].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i - 1, j, index)) {
                    return setDistanceOfHeroes(objectivePoints, i - 1, j);
                }

            }
            if (map.isInMap(i, j + 1) && !cellInformation[i][j + 1].isLayerSet) {
                cellInformation[i][j + 1].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i, j + 1, index)) {
                    return setDistanceOfHeroes(objectivePoints, i, j + 1);
                }
            }
            if (map.isInMap(i, j - 1) && !cellInformation[i][j - 1].isLayerSet) {
                cellInformation[i][j - 1].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i, j - 1, index)) {
                    return setDistanceOfHeroes(objectivePoints, i, j - 1);
                }
            }
        }
        return false;
    }


    static public void setTargetCell(World world, boolean[][] objectivePoints, int index) {
        final int WALL = 99;
        Map map = world.getMap();
        int row = map.getRowNum(), column = map.getColumnNum();
        RespawnInformation respawnInformation = AI.respawnInformation[index];
        CellInformation[][] cellInformation = new CellInformation[row][column];


        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                cellInformation[i][j] = new CellInformation();
                if (map.getCell(i, j).isWall()) {
                    cellInformation[i][j].setCellInfo(WALL);
                }
            }
        }
        cellInformation[respawnInformation.respawnCell[0]][respawnInformation.respawnCell[1]].setCellInfo(0);
        int layer = 0;
        boolean setTarget = false;
        while (true) {
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    if (cellInformation[i][j].isLayerSet) {
                        System.out.println(i + ", " + j + " layer = " + layer);
                        boolean[][] objectivePointsCopy = copyObjectivePoints(objectivePoints, row, column);
                        if (setCellLayers(map, cellInformation, objectivePointsCopy, layer, i, j, index)) {
                            System.out.println("set done!, index = " + index + " target = " + Arrays.toString(AI.respawnInformation[index].getTargetCell()));
                            if (index == 3) {
                                finishTarget = true;
                                return;
                            }
                            setTargetCell(world, objectivePointsCopy, index + 1);
                            if (finishTarget) return;
                            setTarget = true;
                        }
                    }
                }
            }
            if (setTarget || layer > 200) break;
            layer++;
        }
    }

    private static boolean[][] copyObjectivePoints(boolean[][] objectivePoints, int row, int column) {
        boolean[][] objectivePointsCopy = new boolean[row][column];

        for (int i = 0; i < row; i++) {
            System.arraycopy(objectivePoints[i], 0, objectivePointsCopy[i], 0, column);
        }
        return objectivePointsCopy;
    }
}

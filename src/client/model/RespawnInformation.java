package client.model;

public class RespawnInformation {
    private int[] respawnCell;
    private int[] targetCell;

    public RespawnInformation(World world, boolean[][] objectivePoints, Cell respawnCell) {
        int[] respawnArray = {respawnCell.getRow(), respawnCell.getColumn()};
        setRespawnCell(respawnArray);
        setTargetCell(world, objectivePoints);
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

    public void setTargetCell(int[] targetCell) {
        this.targetCell = targetCell;
    }

    private int absolute(int x) {
        return x >= 0 ? x : -x;
    }

    private boolean checkInObjectivePoint(boolean[][] objectivePoints, int i, int j) {
        if (objectivePoints[i][j]) {
            int[] targetCell = {i, j};
            this.setTargetCell(targetCell);
            return true;
        }
        return false;
    }

    private boolean setDistanceOfHeroes(boolean[][] objectivePoints, int i, int j) {
        for (int column = j - 4; column <= j + 4; column++) { // attack
            int rowDown = i + (4 - absolute(column - j));
            int rowUp = i - (4 - absolute(column - j));
            for (int row = rowUp; row <= rowDown; row++) {
                objectivePoints[row][column] = false;
            }
        }
        return true;
    }

    private boolean setCellLayers(Map map, CellInformation[][] cellInformation, boolean[][] objectivePoints, int layer, int i, int j) {

        if (layer == cellInformation[i][j].layerNumber) {
            if (map.isInMap(i + 1, j) && !cellInformation[i + 1][j].isLayerSet) {
                cellInformation[i + 1][j].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i + 1, j)) {
                    return setDistanceOfHeroes(objectivePoints, i + 1, j);
                }
            }
            if (map.isInMap(i - 1, j) && !cellInformation[i - 1][j].isLayerSet) {
                cellInformation[i - 1][j].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i - 1, j)) {
                    return setDistanceOfHeroes(objectivePoints, i - 1, j);
                }

            }
            if (map.isInMap(i, j + 1) && !cellInformation[i][j + 1].isLayerSet) {
                cellInformation[i][j + 1].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i, j + 1)) {
                    return setDistanceOfHeroes(objectivePoints, i, j + 1);
                }
            }
            if (map.isInMap(i, j - 1) && !cellInformation[i][j - 1].isLayerSet) {
                cellInformation[i][j - 1].setCellInfo(layer + 1);
                if (checkInObjectivePoint(objectivePoints, i, j - 1)) {
                    return setDistanceOfHeroes(objectivePoints, i, j - 1);
                }
            }
        }
        return false;
    }


    public void setTargetCell(World world, boolean[][] objectivePoints) {
        final int WALL = 99;
        Map map = world.getMap();
        int row = map.getRowNum(), column = map.getColumnNum();
        CellInformation[][] cellInformation = new CellInformation[row][column];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                cellInformation[i][j] = new CellInformation();
                if (map.getCell(i, j).isWall()) {
                    cellInformation[i][j].setCellInfo(WALL);
                }
            }
        }
        cellInformation[respawnCell[0]][respawnCell[1]].setCellInfo(0);
        int layer = 0;
        while (true) {
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    if (cellInformation[i][j].isLayerSet && setCellLayers(map, cellInformation, objectivePoints, layer, i, j))
                        return;
                }
            }
            layer++;
        }
    }
}

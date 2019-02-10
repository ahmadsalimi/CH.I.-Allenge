package client;

import client.model.*;

import java.util.Random;

public class AI {
    private Direction[][] cellDirections;

    public void preProcess(World world) {
        int rowNum = world.getMap().getRowNum(), colNum = world.getMap().getColumnNum();
        cellDirections = new Direction[rowNum][colNum];
        boolean[][] isDirectionSet = new boolean[rowNum][colNum];
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < colNum; j++) {
                if (world.getMap().getCell(i, j).isInObjectiveZone() || world.getMap().getCell(i, j).isWall())
                    isDirectionSet[i][j] = true;
            }
        }
        boolean allCellsIsSet = false;
        while (!allCellsIsSet) {
            allCellsIsSet = true;
            for (int i = 0; i < rowNum; i++) {
                for (int j = 0; j < colNum; j++) {
                    if (!world.getMap().getCell(i, j).isWall()) {
                        if (isDirectionSet[i][j]) {
                            if (world.getMap().isInMap(i + 1, j) && !isDirectionSet[i + 1][j]) {
                                cellDirections[i + 1][j] = Direction.UP;
                                isDirectionSet[i + 1][j] = true;
                            }
                            if (world.getMap().isInMap(i - 1, j) && !isDirectionSet[i - 1][j]) {
                                cellDirections[i - 1][j] = Direction.DOWN;
                                isDirectionSet[i - 1][j] = true;
                            }
                            if (world.getMap().isInMap(i, j + 1) && !isDirectionSet[i][j + 1]) {
                                cellDirections[i][j + 1] = Direction.LEFT;
                                isDirectionSet[i][j + 1] = true;
                            }
                            if (world.getMap().isInMap(i, j - 1) && !isDirectionSet[i][j - 1]) {
                                cellDirections[i][j - 1] = Direction.RIGHT;
                                isDirectionSet[i][j - 1] = true;
                            }
                        } else {
                            allCellsIsSet = false;
                        }
                    }
                }
            }
        }
    }

    public void pickTurn(World world) {
        Random rand = new Random();
        world.pickHero(HeroName.values()[rand.nextInt(4)]);
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
                    if (thisCell.isInVision()) System.out.print(" @ ");
                    else if (thisCell.isInObjectiveZone()) System.out.print(ANSI_PURPLE + " * " + ANSI_RESET);
                    else System.out.print("   ");
                }
            }
            if (i < 4) {
                System.out.print(world.getMyHeroes()[i].toString());
            } else if (i == 4) {
                System.out.print(world.getMyScore());
            }
            System.out.println();
        }
    }

    public void moveTurn(World world) {
        Random rand = new Random();
        for (Hero hero : world.getMyHeroes()) {
            if (cellDirections[hero.getCurrentCell().getRow()][hero.getCurrentCell().getColumn()] != null)
                world.moveHero(hero, cellDirections[hero.getCurrentCell().getRow()][hero.getCurrentCell().getColumn()]);
            else
                world.moveHero(hero, Direction.values()[rand.nextInt(4)]);
        }
        printCurrentMap(world);
    }

    public void actionTurn(World world) {
        Random rand = new Random();
        Hero hero = world.getMyHeroes()[rand.nextInt(4)];
        AbilityName abilityName = hero.getAbilityNames()[rand.nextInt(3)];
        int deltaRow = rand.nextInt(hero.getAbility(abilityName).getRange());
        int deltaColumn = rand.nextInt(hero.getAbility(abilityName).getRange() - deltaRow);
        int row = hero.getCurrentCell().getRow() + (int) Math.pow(-1, rand.nextInt()) * deltaRow;
        int column = hero.getCurrentCell().getColumn() + (int) Math.pow(-1, rand.nextInt()) * deltaColumn;
        Cell target = world.getMap().getCell(row, column);
        world.castAbility(hero.getId(), abilityName, target);
    }
}


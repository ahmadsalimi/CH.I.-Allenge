package client;

import client.model.*;

import java.util.Random;

public class AI {

    public void preProcess(World world) {
        //System.out.println("pre process started");

    }


    public void pickTurn(World world) {
        Random rand = new Random();
        //System.out.println("pick started");
        world.pickHero(HeroName.values()[rand.nextInt(4)]);
    }

    private void printCurrentMap(World world) {
        String ANSI_RESET = "\u001B[0m"; String ANSI_RED = "\u001B[31m"; String ANSI_PURPLE = "\u001B[35m"; String ANSI_GREEN = "\u001B[32m"; String ANSI_CYAN = "\u001B[36m";
        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 31; j++) {
                boolean flag = true;
                Cell thisCell = world.getMap().getCell(i, j);
                if (thisCell.isWall()) System.out.print(ANSI_GREEN + "|#|" + ANSI_RESET);
                if (!thisCell.isWall()) {
                    if (thisCell.isInObjectiveZone()) System.out.print(ANSI_PURPLE + " * " + ANSI_RESET);
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
                        else System.out.print("   ");
                    }
                }
            }
            if (i < 4) {
                System.out.print(world.getMyHeroes()[i].toString());
            }
            System.out.println();
        }
    }

    public void moveTurn(World world) {
        //System.out.println("move started");
        Random rand = new Random();
        world.moveHero(world.getMyHeroes()[rand.nextInt(4)], Direction.values()[rand.nextInt(4)]);
        printCurrentMap(world);
    }

    public void actionTurn(World world) {
        //System.out.println("action started");
        Random rand = new Random();
        Hero hero = world.getMyHeroes()[rand.nextInt(4)];
        AbilityName abilityName = hero.getAbilityNames()[rand.nextInt(3)];
        /*if (hero.getAbility(abilityName).getType() == AbilityType.DEFENSIVE) {

        }*/
        int deltaRow = rand.nextInt(hero.getAbility(abilityName).getRange());
        int deltaColumn = rand.nextInt(hero.getAbility(abilityName).getRange() - deltaRow);
        int row = hero.getCurrentCell().getRow() + (int) Math.pow(-1, rand.nextInt()) * deltaRow;
        int column = hero.getCurrentCell().getColumn() + (int) Math.pow(-1, rand.nextInt()) * deltaColumn;
        Cell target = world.getMap().getCell(row, column);
        world.castAbility(hero.getId(), abilityName, target);
    }
}

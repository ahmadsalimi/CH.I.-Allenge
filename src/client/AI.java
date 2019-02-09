package client;

import client.model.Hero;
import client.model.World;

import java.io.IOException;

public class AI {

    public void preProcess(World world) {
        //System.out.println("pre process started");

    }

    public void pickTurn(World world) {
        //System.out.println("pick started");

    }

    public void moveTurn(World world) {
        //System.out.println("move started");
        String ANSI_RESET = "\u001B[0m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_PURPLE = "\u001B[35m";
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_CYAN = "\u001B[36m";
        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 31; j++) {
                boolean flag = true;
                if (world.getMap().getCell(i, j).isWall()) {
                    System.out.print(ANSI_RED + "|#|" + ANSI_RESET);
                }
                if (!world.getMap().getCell(i, j).isWall()) {
                    if (world.getMap().getCell(i, j).isInObjectiveZone()) {
                        System.out.print(ANSI_PURPLE + " * " + ANSI_RESET);
                    } else if (world.getMap().getCell(i, j).isInOppRespawnZone()) {
                        System.out.print(ANSI_GREEN + " O " + ANSI_RESET);
                    } else {
                        for (Hero hero : world.getMyHeroes()) {
                            if (hero.getCurrentCell().equals(world.getMap().getCell(i, j))) {
                                System.out.print(ANSI_CYAN + "\"" + hero.getId() + "\"" + ANSI_RESET);
                                flag = false;
                                break;
                            }
                        }
                        if (!flag) continue;
                        if (world.getMap().getCell(i, j).isInVision()) {
                            System.out.print(" @ ");
                        } else {
                            System.out.print("   ");
                        }
                    }
                }
            }
            System.out.println();
        }

    }

    public void actionTurn(World world) {
        //System.out.println("action started");

    }

}

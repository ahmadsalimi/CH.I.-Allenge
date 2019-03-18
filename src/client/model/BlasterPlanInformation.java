package client.model;

public class BlasterPlanInformation {
    private PlanOfBlaster plan = PlanOfBlaster.DEFAULT;
    private int[] offensiveTargetCell = new int[2];
    private int[] dodgeTargetCell = new int[2];
    private int[] objectiveTargetCell = new int[2];
    private int rangeOfCasting;
    private AbilityName abilityName;
    private int moveNum;
    private int[] currentCell = new int[2];
    private boolean setAsWall;

    private int absolute(int x) {
        return x >= 0 ? x : -x;
    }

    public int[] getCurrentCell() {
        return currentCell;
    }

    public boolean isSetAsWall() {
        return setAsWall;
    }

    public AbilityName getAbilityName() {
        return abilityName;
    }

    public int getRangeOfCasting() {
        return rangeOfCasting;
    }

    public PlanOfBlaster getPlan() {
        return plan;
    }

    public int[] getOffensiveTargetCell() {
        return offensiveTargetCell;
    }

    public int[] getDodgeTargetCell() {
        return dodgeTargetCell;
    }

    public int[] getObjectiveTargetCell() {
        return objectiveTargetCell;
    }

    public int getMoveNum() {
        return moveNum;
    }

    private void setRangeOfCasting(int rangeOfCasting) {
        this.rangeOfCasting = rangeOfCasting;
    }

    public void setPlan(PlanOfBlaster plan) {
        this.plan = plan;
        switch (plan) {
            case BOMB:
                this.abilityName = AbilityName.BLASTER_BOMB;
                break;
            case ATTACK:
                this.abilityName = AbilityName.BLASTER_ATTACK;
                break;
        }
    }

    private void setOffensiveTargetCell(int[] offensiveTargetCell) {
        this.offensiveTargetCell = offensiveTargetCell;
    }

    public void setDodgeTargetCell(int[] dodgeTargetCell) {
        this.dodgeTargetCell = dodgeTargetCell;
    }

    public void setObjectiveTargetCell(int[] objectiveTargetCell) {
        this.objectiveTargetCell = objectiveTargetCell;
    }

    public void setMoveNum(int moveNum) {
        this.moveNum = moveNum;
    }

    public void setCurrentCell(int[] currentCell) {
        this.currentCell = currentCell;
    }

    public void setAsWall(boolean setAsWall) {
        this.setAsWall = setAsWall;
    }

    public void setPlanOfBlaster(World world, Hero hero, PlanOfBlaster currentPlan, int[][] weights, int rangeOfCasting) {
        int maxWeight = 0, minDistance = 100;
        int[] bestCell = new int[2];
        int[] heroCell = {hero.getCurrentCell().getRow(), hero.getCurrentCell().getColumn()};
        for (int column = heroCell[1] - rangeOfCasting; column <= heroCell[1] + rangeOfCasting; column++) { // attack
            int rowDown = heroCell[0] + (rangeOfCasting - absolute(column - heroCell[1]));
            int rowUp = heroCell[0] - (rangeOfCasting - absolute(column - heroCell[1]));
            for (int row = rowUp; row <= rowDown; row++) {
                if ((currentPlan == PlanOfBlaster.BOMB || world.isInVision(heroCell[0], heroCell[1], row, column))) {
                    if (weights[row][column] > maxWeight) {
                        maxWeight = weights[row][column];
                        bestCell[0] = row;
                        bestCell[1] = column;
                    }
                }
            }
        }
        if (maxWeight == 0 || !world.getMap().getCell(heroCell[0], heroCell[1]).isInObjectiveZone()) this.setPlan(PlanOfBlaster.DEFAULT);
        else {
            this.setPlan(currentPlan);
            this.setOffensiveTargetCell(bestCell);
            switch (currentPlan) {
                case ATTACK:
                    this.setRangeOfCasting(world.getMyHeroes()[0].getAbility(AbilityName.BLASTER_ATTACK).getRange());
                    break;
                case BOMB:
                    this.setRangeOfCasting(world.getMyHeroes()[0].getAbility(AbilityName.BLASTER_BOMB).getRange());
                    break;
            }
        }
        System.out.println("Hero" + hero.getId() + "'s plan: " + this.getPlan());
    }

    public void setPlanOfBlasterAndMove(World world, Hero hero, PlanOfBlaster currentPlan, int[][] weights) {
        int maxWeight = 0, minDistance = 100;
        int[] bestCell = new int[2];
        int[] heroCell = {hero.getCurrentCell().getRow(), hero.getCurrentCell().getColumn()};
        switch (currentPlan) {
            case ATTACK:
                this.setRangeOfCasting(3);
                break;
            case BOMB:
                this.setRangeOfCasting(5);
                break;
        }
        int range = this.rangeOfCasting + 1;
        Direction nextDirection = null;
        Map map = world.getMap();
        for (int column = heroCell[1] - range; column <= heroCell[1] + range; column++) { // attack
            int rowDown = heroCell[0] + (range - absolute(column - heroCell[1]));
            int rowUp = heroCell[0] - (range - absolute(column - heroCell[1]));
            for (int row = rowUp; row <= rowDown; row++) {
                if ((currentPlan == PlanOfBlaster.BOMB || world.isInVision(heroCell[0], heroCell[1], row, column)) && world.manhattanDistance(heroCell[0], heroCell[1], row, column) < range) {
                    if (weights[row][column] > maxWeight) {
                        maxWeight = weights[row][column];
                        minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                        bestCell[0] = row;
                        bestCell[1] = column;
                        nextDirection = null;

                    } else if (weights[row][column] == maxWeight) {
                        if (world.manhattanDistance(heroCell[0], heroCell[1], row, column) < minDistance) {
                            minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                            bestCell[0] = row;
                            bestCell[1] = column;
                            nextDirection = null;

                        }
                    }
                } else if (!map.getCell(heroCell[0] + 1, heroCell[1]).isWall() && (currentPlan == PlanOfBlaster.BOMB || world.isInVision(heroCell[0] + 1, heroCell[1], row, column)) && world.manhattanDistance(heroCell[0] + 1, heroCell[1], row, column) < range) {
                    if (weights[row][column] > maxWeight) {
                        maxWeight = weights[row][column];
                        minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                        bestCell[0] = row;
                        bestCell[1] = column;
                        nextDirection = Direction.DOWN;

                    } else if (weights[row][column] == maxWeight) {
                        if (world.manhattanDistance(heroCell[0], heroCell[1], row, column) < minDistance) {
                            minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                            bestCell[0] = row;
                            bestCell[1] = column;
                            nextDirection = Direction.DOWN;
                        }
                    }
                } else if (!map.getCell(heroCell[0] - 1, heroCell[1]).isWall() && (currentPlan == PlanOfBlaster.BOMB || world.isInVision(heroCell[0] - 1, heroCell[1], row, column)) && world.manhattanDistance(heroCell[0] - 1, heroCell[1], row, column) < range) {
                    if (weights[row][column] > maxWeight) {
                        maxWeight = weights[row][column];
                        minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                        bestCell[0] = row;
                        bestCell[1] = column;
                        nextDirection = Direction.UP;

                    } else if (weights[row][column] == maxWeight) {
                        if (world.manhattanDistance(heroCell[0], heroCell[1], row, column) < minDistance) {
                            minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                            bestCell[0] = row;
                            bestCell[1] = column;
                            nextDirection = Direction.UP;
                        }
                    }
                } else if (!map.getCell(heroCell[0], heroCell[1] + 1).isWall() && (currentPlan == PlanOfBlaster.BOMB || world.isInVision(heroCell[0], heroCell[1] + 1, row, column)) && world.manhattanDistance(heroCell[0], heroCell[1] + 1, row, column) < range) {
                    if (weights[row][column] > maxWeight) {

                        maxWeight = weights[row][column];
                        minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                        bestCell[0] = row;
                        bestCell[1] = column;
                        nextDirection = Direction.RIGHT;


                    } else if (weights[row][column] == maxWeight) {
                        if (world.manhattanDistance(heroCell[0], heroCell[1], row, column) < minDistance) {

                            minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                            bestCell[0] = row;
                            bestCell[1] = column;
                            nextDirection = Direction.RIGHT;

                        }
                    }
                } else if (!map.getCell(heroCell[0], heroCell[1] - 1).isWall() && (currentPlan == PlanOfBlaster.BOMB || world.isInVision(heroCell[0], heroCell[1] - 1, row, column)) && world.manhattanDistance(heroCell[0], heroCell[1] - 1, row, column) < range) {
                    if (weights[row][column] > maxWeight) {
                        maxWeight = weights[row][column];
                        minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                        bestCell[0] = row;
                        bestCell[1] = column;
                        nextDirection = Direction.LEFT;

                    } else if (weights[row][column] == maxWeight) {
                        if (world.manhattanDistance(heroCell[0], heroCell[1], row, column) < minDistance) {
                            minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                            bestCell[0] = row;
                            bestCell[1] = column;
                            nextDirection = Direction.LEFT;
                        }
                    }
                }
            }
        }
        if (maxWeight == 0 || !world.getMap().getCell(heroCell[0], heroCell[1]).isInObjectiveZone()) this.setPlan(PlanOfBlaster.DEFAULT);
        else {
            this.setPlan(currentPlan);
            this.setOffensiveTargetCell(bestCell);
            //if (nextDirection != null) world.moveHero(hero, nextDirection); // TODO: make it smarter. it may conflict
        }
        System.out.println("Hero" + hero.getId() + "'s plan: " + this.getPlan());
    }
}
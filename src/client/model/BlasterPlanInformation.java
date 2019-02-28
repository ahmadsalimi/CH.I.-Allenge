package client.model;

public class BlasterPlanInformation {
    private PlanOfBlaster plan = PlanOfBlaster.DEFAULT;
    private int[] offensiveTargetCell = new int[2];
    private int[] dodgeTargetCell = new int[2];
    private int rangeOfCasting;
    private AbilityName abilityName;
    private boolean firstTimeLayout = false;
    private int moveNum;

    public boolean isFirstTimeLayout() {
        return firstTimeLayout;
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

    public int getMoveNum() {
        return moveNum;
    }

    public void setFirstTimeLayout(boolean firstTimeLayout) {
        this.firstTimeLayout = firstTimeLayout;
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

    public void setMoveNum(int moveNum) {
        this.moveNum = moveNum;
    }

    public void setPlanOfBlaster(World world, Hero hero, PlanOfBlaster currentPlan, int[][] weights, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        int maxWeight = 0, minDistance = 100;
        int[] bestCell = new int[2];
        int[] heroCell = {hero.getCurrentCell().getRow(), hero.getCurrentCell().getColumn()};
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                if (weights[row][column] > maxWeight) {
                    maxWeight = weights[row][column];
                    minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                    bestCell[0] = row;
                    bestCell[1] = column;

                } else if (weights[row][column] == maxWeight) {
                    if (world.manhattanDistance(heroCell[0], heroCell[1], row, column) < minDistance) {
                        minDistance = world.manhattanDistance(heroCell[0], heroCell[1], row, column);
                        bestCell[0] = row;
                        bestCell[1] = column;
                    }
                }
            }
        }
        if (maxWeight == 0 || !firstTimeLayout) this.setPlan(PlanOfBlaster.DEFAULT);
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
}

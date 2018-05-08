package com.raoulvdberge.refinedstorage.gui;

import com.google.common.primitives.Ints;
import com.raoulvdberge.refinedstorage.gui.grid.GuiCraftingStart;
import com.raoulvdberge.refinedstorage.tile.data.TileDataManager;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.Tuple;

public class GuiPriority extends GuiCraftingStart {
    private TileDataParameter<Integer, ?> priorityInsert;
    private TileDataParameter<Integer, ?> priorityExtract;

    public GuiPriority(GuiBase parent, TileDataParameter<Integer, ?> priorityInsert, TileDataParameter<Integer, ?> priorityExtract) {
        super(parent, null, new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 164, 92);

        this.priorityInsert = priorityInsert;
        this.priorityExtract = priorityExtract;
    }

    @Override
    public void init(int x, int y) {
        super.init(x, y);
        amountField.setText(getInsertAmount() + "," + getExtractAmount());
    }

    private int getInsertAmount() {
        return priorityInsert.getValue();
    }

    private int getExtractAmount() {
        return priorityExtract.getValue();
    }

    @Override
    protected int getAmount() {
        return 0;
    }

    @Override
    protected String getStartButtonText() {
        return t("misc.refinedstorage:set");
    }

    @Override
    protected String getTitle() {
        return t("misc.refinedstorage:priority");
    }

    @Override
    protected String getTexture() {
        return "gui/priority.png";
    }

    @Override
    protected Tuple<Integer, Integer> getAmountPos() {
        return new Tuple<>(18 + 1, 47 + 1);
    }

    @Override
    protected Tuple<Integer, Integer> getIncrementButtonPos(int x, int y) {
        return new Tuple<>(6 + (x * (30 + 3)), y + (y == 0 ? 20 : 64));
    }

    @Override
    protected Tuple<Integer, Integer> getStartCancelPos() {
        return new Tuple<>(107, 30);
    }

    @Override
    protected boolean canAmountGoNegative() {
        return true;
    }

    @Override
    protected int[] getIncrements() {
        return new int[]{
            1, 5, 10,
            -1, -5, -10
        };
    }

    @Override
    protected void startRequest(boolean noPreview) {
        String[] split = amountField.getText().split(",");
        Integer amountInsert = Ints.tryParse(split.length < 1 ? "" : split[0]);
        Integer amountExtract = Ints.tryParse(split.length < 2 ? "" : split[1]);

        if (amountInsert != null) {
            TileDataManager.setParameter(priorityInsert, amountInsert);
        }
        if (amountExtract != null) {
            TileDataManager.setParameter(priorityExtract, amountExtract);
        }
        if (amountExtract != null || amountInsert != null) {
            close();
        }
    }
}

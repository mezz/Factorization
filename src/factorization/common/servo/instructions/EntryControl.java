package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.TileEntityServoRail;

public class EntryControl extends Instruction {
    public boolean blocking = false;
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        blocking = data.asSameShare("block").putBoolean(blocking);
        return this;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        TileEntityServoRail sr = block.getTE(TileEntityServoRail.class);
        if (sr.priority >= 0) {
            sr.priority = -1;
            blocking = true;
        } else {
            sr.priority = 1;
            blocking = false;
        }
        return true;
    }
    
    @Override
    public void onPlacedOnRail(TileEntityServoRail sr) {
        sr.priority = 1;
        blocking = false;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.fenceGate);
    }

    @Override
    public void motorHit(ServoMotor motor) { }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return blocking ? BlockIcons.servo$entry_forbid : BlockIcons.servo$entry_require;
    }

    @Override
    public String getName() {
        return "fz.instruction.entryControl";
    }
}

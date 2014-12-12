package factorization.fzds;

import factorization.api.Coord;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.FzUtil;
import gnu.trove.set.hash.THashSet;

import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

class ShadowRenderGlobal implements IWorldAccess {
    /***
     * Inspired, obviously, by RenderGlobal.
     * The World has a list of IWorldAccess, which it passes various events to.  
     */
    private World realWorld;
    public ShadowRenderGlobal(World realWorld) {
        this.realWorld = realWorld;
    }
    //The coord arguments are always in shadowspace.
    
    @Override
    public void markBlockForUpdate(int var1, int var2, int var3) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var1 + 1, var2 + 1, var3 + 1);
    }

    @Override
    public void markBlockForRenderUpdate(int var1, int var2, int var3) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var1 + 1, var2 + 1, var3 + 1);
    }

    @Override
    public void markBlockRangeForRenderUpdate(int var1, int var2, int var3,
            int var4, int var5, int var6) {
        markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var4 + 1, var5 + 1, var6 + 1);
    }
    
    void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
        World realClientWorld = DeltaChunk.getClientRealWorld();
        for (IDeltaChunk idc : DeltaChunk.getSlicesInRange(realClientWorld, lx, ly, lz, hx, hy, hz)) {
            DimensionSliceEntity dse = (DimensionSliceEntity) idc;
            Coord near = dse.getCorner(), far = dse.getFarCorner();
            if (FzUtil.intersect(near.x, far.x, lx, hx)
                    && FzUtil.intersect(near.y, far.y, ly, hy)
                    && FzUtil.intersect(near.z, far.z, lz, hz)) {
                RenderDimensionSliceEntity.markBlocksForUpdate(dse, lx, ly, lz, hx, hy, hz);
                dse.blocksChanged(lx, ly, lz);
                dse.blocksChanged(hx, hy, hz);
            }
        }
    }

    @Override
    public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
        if (realCoords == null) {
            return;
        }
        realWorld.playSound(realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, sound, volume, pitch, false);
    }
    
    @Override
    public void playSoundToNearExcept(EntityPlayer entityplayer, String s, double d0, double d1, double d2, float f, float f1) { }

    @Override
    public void spawnParticle(String particle, double x, double y, double z, double vx, double vy, double vz) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
        if (realCoords == null) {
            return;
        }
        if (!Hammer.proxy.isInShadowWorld()) {
            // Shouldn't happen
            realWorld.spawnParticle(particle, realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, vx, vy, vz);
        } else {
            Hammer.proxy.restoreRealWorld();
            realWorld.spawnParticle(particle, realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, vx, vy, vz);
            Hammer.proxy.setShadowWorld();
        }
    }
    
    @Override
    public void onEntityCreate(Entity entity) {
        Minecraft.getMinecraft().renderGlobal.onEntityCreate(entity);
    }
    
    @Override
    public void onEntityDestroy(Entity entity) {
        Minecraft.getMinecraft().renderGlobal.onEntityDestroy(entity);
    }

    @Override
    public void playRecord(String recordName, int x, int y, int z) {
        Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
        if (realCoords == null) {
            return;
        }
        Minecraft.getMinecraft().renderGlobal.playRecord(recordName, (int)realCoords.xCoord, (int)realCoords.yCoord, (int)realCoords.zCoord);
    }

    @Override
    public void broadcastSound(int var1, int var2, int var3, int var4,
            int var5) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4,
            int var5, int var6) {
        // TODO Auto-generated method stub
        
    }

    HashMap<Integer, DestroyBlockProgress> damagedBlocks = new HashMap();
    
    @Override
    public void destroyBlockPartially(int breakerId, int blockX, int blockY, int blockZ, int damageProgress) {
        if (!(damageProgress >= 0 && damageProgress < 10)) {
            damagedBlocks.remove(Integer.valueOf(breakerId));
            return;
        }
        DestroyBlockProgress destroyblockprogress = (DestroyBlockProgress) damagedBlocks.get(Integer.valueOf(breakerId));

        if (destroyblockprogress == null
                || destroyblockprogress.getPartialBlockX() != blockX
                || destroyblockprogress.getPartialBlockY() != blockY
                || destroyblockprogress.getPartialBlockZ() != blockZ) {
            destroyblockprogress = new DestroyBlockProgress(breakerId, blockX, blockY, blockZ);
            damagedBlocks.put(Integer.valueOf(breakerId), destroyblockprogress);
        }

        destroyblockprogress.setPartialBlockDamage(damageProgress);
        destroyblockprogress.setCloudUpdateTick(tickTime);
    }
    
    int tickTime = 0;
    public void removeStaleDamage() {
        if (tickTime++ % 20 != 0) return;
        
        Iterator<DestroyBlockProgress> iterator = damagedBlocks.values().iterator();
        while (iterator.hasNext()) {
            DestroyBlockProgress destroyblockprogress = iterator.next();
            int createTime = destroyblockprogress.getCreationCloudUpdateTick();

            if (tickTime - createTime > 400) {
                iterator.remove();
            }
        }
    }

    @Override
    public void onStaticEntitiesChanged() {
        // TODO Auto-generated method stub
    }
    
}
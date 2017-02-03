package de.ellpeck.pumpkisplode;

import com.google.common.base.Predicate;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class EventHandler{

    private static final Predicate<Entity> IS_PLAYER_OR_SNOWMAN = new Predicate<Entity>(){
        @Override
        public boolean apply(Entity input){
            return input.isEntityAlive() && (input instanceof EntityPlayer || input instanceof EntitySnowman);
        }
    };

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event){
        EntityLivingBase eventity = event.getEntityLiving();
        if(!eventity.world.isRemote && eventity instanceof EntityVillager){
            EntityVillager villager = (EntityVillager)eventity;

            if(!villager.isDead && !villager.isChild()){
                float range = 3.0F;
                AxisAlignedBB aabb = new AxisAlignedBB(villager.posX-range, villager.posY-range, villager.posZ-range, villager.posX+range, villager.posY+range, villager.posZ+range);
                List<EntityLivingBase> entities = villager.world.getEntitiesWithinAABB(EntityLivingBase.class, aabb, IS_PLAYER_OR_SNOWMAN);

                for(EntityLivingBase entity : entities){
                    if(entity instanceof EntityPlayer){
                        ItemStack stack = ((EntityPlayer)entity).inventory.armorInventory[3];
                        if(stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN)){
                            if(isLookingAt(villager, entity)){
                                explodeVillager(villager);
                                return;
                            }
                        }
                    }
                    else if(entity instanceof EntitySnowman){
                        if(!((EntitySnowman)entity).isPumpkinEquipped()){ //This is the wrong way around, why >_>
                            if(isLookingAt(villager, entity)){
                                explodeVillager(villager);
                                return;
                            }
                        }
                    }
                }

                RayTraceResult ray = ForgeHooks.rayTraceEyes(villager, 5F);
                if(ray != null){
                    BlockPos pos = ray.getBlockPos();
                    if(pos != null){
                        IBlockState state = villager.world.getBlockState(pos);
                        if(state.getBlock() == Blocks.PUMPKIN){
                            EnumFacing rotation = state.getValue(BlockPumpkin.FACING);
                            if(rotation == ray.sideHit){
                                explodeVillager(villager);
                            }
                        }
                    }
                }
            }
        }

    }

    private static void explodeVillager(EntityVillager villager){
        Random rand = villager.getRNG();
        villager.world.createExplosion(null, villager.posX, villager.posY, villager.posZ, rand.nextFloat()*3F+1F, true);
        villager.setDead();

        if(rand.nextFloat() >= 0.25F){
            villager.entityDropItem(new ItemStack(Items.EMERALD, rand.nextInt(5)+1), 0F);
        }

        IInventory inv = villager.getVillagerInventory();
        for(int i = 0; i < inv.getSizeInventory(); i++){
            ItemStack stack = inv.getStackInSlot(i);
            if(stack != null){
                villager.entityDropItem(stack, 0F);
            }
        }
    }

    private static boolean isLookingAt(EntityVillager villager, EntityLivingBase other){
        Vec3d vec3d = villager.getLook(1.0F).normalize();
        Vec3d vec3d1 = new Vec3d(other.posX-villager.posX, other.posY+(double)other.getEyeHeight()-(villager.posY+(double)villager.getEyeHeight()), other.posZ-villager.posZ);
        double d0 = vec3d1.lengthVector();
        vec3d1 = vec3d1.normalize();
        double d1 = vec3d.dotProduct(vec3d1);

        return d1 > 1.0D-0.025D/d0 && villager.canEntityBeSeen(other);
    }
}

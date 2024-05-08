package mod.acgaming.universaltweaks.tweaks.entities.voidteleport.mixin;

import mod.acgaming.universaltweaks.config.UTConfigTweaks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

// Courtesy of WaitingIdly
@Mixin(EntityLivingBase.class)
public abstract class UTVoidTeleportEntity
{
    @Unique
    private boolean universalTweaks$hasFallenVoidTeleport = false;

    @Unique
    private static boolean isEnabledForDimension(int dimension)
    {
        List<String> list = Arrays.asList(UTConfigTweaks.ENTITIES.VOID_TELEPORT.utDimensionList);
        DimensionType dimensionType = DimensionManager.getProviderType(dimension);
        String dimensionName = dimensionType == null ? null : dimensionType.getName();

        boolean isWhitelist = UTConfigTweaks.ENTITIES.VOID_TELEPORT.utDimensionListMode == UTConfigTweaks.EnumLists.WHITELIST;
        return isWhitelist == (list.contains(dimensionName) || list.contains(String.valueOf(dimension)));
    }

    @Unique
    private static boolean isEnabledForEntity(Entity entity)
    {
        List<String> list = Arrays.asList(UTConfigTweaks.ENTITIES.VOID_TELEPORT.utEntityList);
        ResourceLocation resourceLocation = EntityList.getKey(entity);
        if (resourceLocation == null) return false;

        boolean isWhitelist = UTConfigTweaks.ENTITIES.VOID_TELEPORT.utEntityListMode == UTConfigTweaks.EnumLists.WHITELIST;
        return list.contains(resourceLocation.toString()) == isWhitelist;
    }

    @Inject(method = "outOfWorld", at = @At("HEAD"), cancellable = true)
    public void utTransferPlayerToDimension(CallbackInfo ci)
    {
        if (!UTConfigTweaks.ENTITIES.VOID_TELEPORT.utVoidTeleportToggle) return;
        Entity entity = (Entity) (Object) this;
        if (!isEnabledForDimension(entity.dimension)) return;

        if ((UTConfigTweaks.ENTITIES.VOID_TELEPORT.utForgivePlayers && entity instanceof EntityPlayer) || isEnabledForEntity(entity))
        {
            if (UTConfigTweaks.ENTITIES.VOID_TELEPORT.utPreventVoidDamage) ci.cancel();
            if (UTConfigTweaks.ENTITIES.VOID_TELEPORT.utTeleportBlindness) ((EntityLivingBase) (Object) this).addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
            entity.setLocationAndAngles(entity.posX, UTConfigTweaks.ENTITIES.VOID_TELEPORT.utTargetYLevel, entity.posZ, entity.rotationYaw, entity.rotationPitch);
            entity.motionY = Math.max(UTConfigTweaks.ENTITIES.VOID_TELEPORT.utClampSpeedTo, Math.min(0, entity.motionY));
            universalTweaks$hasFallenVoidTeleport = true;
        }
    }

    @Inject(method = "updateFallState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updateFallState(DZLnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;)V"))
    public void utUpdateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos, CallbackInfo ci)
    {
        if (!UTConfigTweaks.ENTITIES.VOID_TELEPORT.utVoidTeleportToggle) return;
        if (onGroundIn && universalTweaks$hasFallenVoidTeleport)
        {
            EntityLivingBase entity = (EntityLivingBase) (Object) this;
            universalTweaks$hasFallenVoidTeleport = false;

            if (entity.isInWater()) return;
            if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying || entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.allowFlying) return;

            if (UTConfigTweaks.ENTITIES.VOID_TELEPORT.utFallHeight < 0)
            {
                entity.fallDistance = 0;
                float setting = UTConfigTweaks.ENTITIES.VOID_TELEPORT.utFallDamageTaken;
                if (setting != 0)
                {
                    float damage = setting < 0 ? entity.getMaxHealth() + setting : setting;
                    if (!UTConfigTweaks.ENTITIES.VOID_TELEPORT.utAllowSpecificFallDamageToKill) damage = Float.min(entity.getHealth() - 1, damage);
                    if (damage > 0)
                    {
                        entity.playSound(SoundEvents.ENTITY_GENERIC_BIG_FALL, 1.0F, 1.0F);
                        entity.attackEntityFrom(DamageSource.FALL, damage);
                    }
                }
            }
            else
            {
                entity.fallDistance = UTConfigTweaks.ENTITIES.VOID_TELEPORT.utFallHeight;
            }
        }
    }
}
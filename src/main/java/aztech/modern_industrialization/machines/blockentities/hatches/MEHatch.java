/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package aztech.modern_industrialization.machines.blockentities.hatches;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.util.ConfigInventory;
import aztech.modern_industrialization.MICapabilities;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineComponent;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.jspecify.annotations.Nullable;

public class MEHatch extends HatchBlockEntity implements IGridConnectedBlockEntity {
    private final MEHatchType type;
    private final ConfigInventory config;
    private final IManagedGridNode mainNode;
    private final IActionSource source;

    // todo: respect machine lock? and make a config UI

    public MEHatch(BEP bep, MachineGuiParameters guiParams, MEHatchType type) {
        super(bep, guiParams, OrientationComponent.Params.noFacingNoOutput());

        this.type = type;
        this.config = ConfigInventory.configTypes(4).supportedType(switch (type) {
            case ITEM_INPUT, ITEM_OUTPUT -> AEKeyType.items();
            case FLUID_INPUT, FLUID_OUTPUT -> AEKeyType.fluids();
        }).build();
        this.mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
                .setVisualRepresentation(bep.state().getBlock())
                .setInWorldNode(true)
                .setTagName("proxy");
        this.source = IActionSource.ofMachine(this);
        registerComponents(new MachineComponent.ServerOnly() {
            @Override
            public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
                getMainNode().saveToNBT(tag);
                config.writeToChildTag(tag, "config", registries);
            }

            @Override
            public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
                getMainNode().loadFromNBT(tag);
                config.readFromChildTag(tag, "config", registries);
            }
        });
    }

    public static void registerME(BlockEntityType<? extends MEHatch> bet) {
        MICapabilities.onEvent(event -> {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, bet, (be, direction) -> be);
        });
    }

    @Override
    public HatchType getHatchType() {
        return type.mi;
    }

    @Override
    public boolean upgradesToSteel() {
        return true;
    }

    @Override
    public MIInventory getInventory() {
        return MIInventory.EMPTY;
    }

    // todo: custom line
    @Override
    public List<Component> getTooltips() {
        return super.getTooltips();
    }

    @Override
    public void appendItemInputs(List<ConfigurableItemStack> list) {
        if (type == MEHatchType.ITEM_INPUT) {
            for (int slot = 0; slot < config.size(); slot++) {
                ConfigurableItemStack stack = new ConfigurableItemStack();
                stack.me = new Slot(slot);
                // shouldn't be necessary but just in case
                stack.adjustedCapacity = 0;
                list.add(stack);
            }
        }
    }

    @Override
    public void appendItemOutputs(List<ConfigurableItemStack> list) {
        if (type == MEHatchType.ITEM_OUTPUT) {
            for (int slot = 0; slot < config.size(); slot++) {
                ConfigurableItemStack stack = new ConfigurableItemStack();
                stack.me = new Slot(slot);
                // shouldn't be necessary but just in case
                stack.adjustedCapacity = 0;
                list.add(stack);
            }
        }
    }

    @Override
    public void appendFluidInputs(List<ConfigurableFluidStack> list) {
        if (type == MEHatchType.FLUID_INPUT) {
            for (int slot = 0; slot < config.size(); slot++) {
                ConfigurableFluidStack stack = new ConfigurableFluidStack(0L);
                stack.me = new Slot(slot);
                list.add(stack);
            }
        }
    }

    @Override
    public void appendFluidOutputs(List<ConfigurableFluidStack> list) {
        if (type == MEHatchType.FLUID_OUTPUT) {
            for (int slot = 0; slot < config.size(); slot++) {
                ConfigurableFluidStack stack = new ConfigurableFluidStack(0L);
                stack.me = new Slot(slot);
                list.add(stack);
            }
        }
    }

    // == ae2 stuff ===

    @Override
    public final IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.getMainNode().destroy();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.getMainNode().destroy();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, MEHatch::onReady);
    }

    private void onReady() {
        getMainNode().create(getLevel(), getBlockPos());
    }

    public enum MEHatchType {
        ITEM_INPUT(HatchTypes.ITEM_INPUT),
        ITEM_OUTPUT(HatchTypes.ITEM_OUTPUT),
        FLUID_INPUT(HatchTypes.FLUID_INPUT),
        FLUID_OUTPUT(HatchTypes.FLUID_OUTPUT),
        ;

        private final HatchType mi;

        MEHatchType(HatchType mi) {
            this.mi = mi;
        }
    }

    public class Slot {
        private final int slot;

        public Slot(int slot) {
            this.slot = slot;
        }

        public @Nullable Item available() {
            if (config.getKey(slot) instanceof AEItemKey itemKey) {
                IGrid grid = getMainNode().getGrid();

                if (grid != null && grid.getStorageService().getCachedInventory().get(itemKey) > 0) {
                    return itemKey.getItem();
                }
            }

            return null;
        }

        public int extract(Ingredient ingredient, int amount, boolean simulate) {
            if (type != MEHatchType.ITEM_INPUT
                    || !(config.getKey(slot) instanceof AEItemKey itemKey)
                    || !ingredient.test(itemKey.toStack())) {
                return 0;
            }

            IGrid grid = getMainNode().getGrid();

            if (grid == null) {
                return 0;
            }

            return (int) StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(), itemKey, amount, source, Actionable.ofSimulate(simulate));
        }

        public long extract(FluidIngredient fluid, long amount, boolean simulate) {
            if (type != MEHatchType.FLUID_INPUT
                    || !(config.getKey(slot) instanceof AEFluidKey fluidKey)
                    || !fluid.test(fluidKey.toStack(1))) {
                return 0L;
            }

            IGrid grid = getMainNode().getGrid();

            if (grid == null) {
                return 0;
            }

            return StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(), fluidKey, amount, source, Actionable.ofSimulate(simulate));
        }

        public int insert(ItemVariant variant, int amount, boolean simulate) {
            if (type != MEHatchType.ITEM_OUTPUT) {
                return 0;
            }

            AEKey configKey = config.getKey(slot);
            AEKey key = null;

            if (configKey == null) {
                key = AEItemKey.of(variant.toStack());
            } else if (configKey instanceof AEItemKey itemKey && (simulate || variant.matches(itemKey.toStack()))) {
                // that `simulate ||` is to passthrough inserts if the filter changed mid-operation
                key = itemKey;
            }

            // todo: no way to slot lock as air /shrug

            if (key == null) {
                return 0;
            }

            IGrid grid = getMainNode().getGrid();

            if (grid == null) {
                return 0;
            }

            return (int) StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(), key, amount, source, Actionable.ofSimulate(simulate));
        }

        public long insert(Fluid variant, long amount, boolean simulate) {
            if (type != MEHatchType.FLUID_OUTPUT) {
                return 0;
            }

            AEKey configKey = config.getKey(slot);
            AEKey key = null;

            if (configKey == null) {
                key = AEFluidKey.of(variant);
            } else if (configKey instanceof AEFluidKey fluidKey && (simulate || variant.equals(fluidKey.getFluid()))) {
                // that `simulate ||` is to passthrough inserts if the filter changed mid-operation
                key = fluidKey;
            }

            // todo: no way to slot lock as air /shrug

            if (key == null) {
                return 0;
            }

            IGrid grid = getMainNode().getGrid();

            if (grid == null) {
                return 0;
            }

            return StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(), key, amount, source, Actionable.ofSimulate(simulate));
        }
    }
}

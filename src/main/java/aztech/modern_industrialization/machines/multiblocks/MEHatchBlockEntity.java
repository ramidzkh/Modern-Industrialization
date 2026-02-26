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

package aztech.modern_industrialization.machines.multiblocks;

import appeng.api.AECapabilities;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.util.AECableType;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import aztech.modern_industrialization.MICapabilities;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineComponent;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public abstract class MEHatchBlockEntity extends HatchBlockEntity implements IGridConnectedBlockEntity {
    private final MIInventory inventory;
    private final IManagedGridNode mainNode;
    public final IActionSource source;

    public MEHatchBlockEntity(BEP bep, MachineGuiParameters guiParams, OrientationComponent.@Nullable Params orientationParams, MIInventory inventory) {
        super(bep, guiParams, orientationParams);

        this.inventory = inventory;
        mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
                .setVisualRepresentation(bep.state().getBlock())
                .setInWorldNode(true)
                .setTagName("proxy");
        source = IActionSource.ofMachine(this);
        registerComponents(new MachineComponent.ServerOnly() {
            @Override
            public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
                getMainNode().saveToNBT(tag);
            }

            @Override
            public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
                getMainNode().loadFromNBT(tag);
                setSources();
            }
        });

        setSources();
    }

    // todo: set slot sources properly after replaced by MIInventory.readNbt
    private void setSources() {
        for (ConfigurableItemStack stack : inventory.getItemStacks()) {
            stack.source = this;
        }

        for (ConfigurableFluidStack stack : inventory.getFluidStacks()) {
            stack.source = this;
        }
    }

    @Override
    public final IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Override
    public void saveChanges() {
        setChanged();
        // todo: no idea if this helps
        setSources();
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
        GridHelper.onFirstTick(this, MEHatchBlockEntity::onReady);
    }

    private void onReady() {
        getMainNode().create(getLevel(), getBlockPos());
        setSources();
    }

    public static void registerME(BlockEntityType<? extends MEHatchBlockEntity> bet) {
        MICapabilities.onEvent(event -> {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, bet, (be, direction) -> be);
        });
    }
}

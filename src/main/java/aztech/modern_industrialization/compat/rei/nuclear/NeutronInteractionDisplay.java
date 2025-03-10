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
package aztech.modern_industrialization.compat.rei.nuclear;

import aztech.modern_industrialization.nuclear.INuclearComponent;
import java.util.Collections;
import java.util.List;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;

class NeutronInteractionDisplay implements Display {

    final INuclearComponent nuclearComponent;
    final CategoryType type;

    NeutronInteractionDisplay(INuclearComponent nuclearComponent, CategoryType type) {
        this.nuclearComponent = nuclearComponent;
        this.type = type;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        if (nuclearComponent.getVariant() instanceof ItemVariant itemVariant) {
            return Collections.singletonList(EntryIngredient.of(EntryStacks.of(itemVariant.getItem())));
        } else if (nuclearComponent.getVariant() instanceof FluidVariant fluidVariant) {
            return Collections.singletonList(EntryIngredient.of(EntryStacks.of(fluidVariant.getFluid(), 81000)));
        } else {
            throw new IllegalStateException("Unreachable");
        }
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        if (type == CategoryType.NEUTRON_PRODUCT) {

            TransferVariant output = nuclearComponent.getNeutronProduct();
            if (output instanceof ItemVariant itemVariant) {
                return Collections
                        .singletonList(EntryIngredient.of(EntryStacks.of(itemVariant.getItem(), (int) nuclearComponent.getNeutronProductAmount())));
            } else if (output instanceof FluidVariant fluidVariant) {
                return Collections
                        .singletonList(EntryIngredient.of(EntryStacks.of(fluidVariant.getFluid(), nuclearComponent.getNeutronProductAmount())));
            } else {
                throw new IllegalStateException("Unreachable");
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public CategoryIdentifier<NeutronInteractionDisplay> getCategoryIdentifier() {
        return NeutronInteractionPlugin.NEUTRON_CATEGORY;
    }

    public static enum CategoryType {
        FAST_NEUTRON_INTERACTION,
        THERMAL_NEUTRON_INTERACTION,
        FISSION,
        NEUTRON_PRODUCT,
    }
}

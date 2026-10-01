package com.eternalfurnace.recipe;

import com.eternalfurnace.init.ModRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/**
 * A normal data-driven shapeless recipe whose Flint and Steel remainder keeps
 * its components and takes one durability point instead of being consumed.
 */
public final class DurableShapelessRecipe extends NormalCraftingRecipe {
    private static final Codec<List<Ingredient>> INGREDIENTS_CODEC = Codec.lazyInitialized(
            () -> Ingredient.CODEC.listOf(1, ShapedRecipePattern.getMaxHeight() * ShapedRecipePattern.getMaxWidth())
    );

    public static final MapCodec<DurableShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
    ).apply(instance, DurableShapelessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DurableShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC,
            recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
            recipe -> recipe.bookInfo,
            ItemStackTemplate.STREAM_CODEC,
            recipe -> recipe.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
            recipe -> recipe.ingredients,
            DurableShapelessRecipe::new
    );

    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final boolean simple;

    public DurableShapelessRecipe(
            Recipe.CommonInfo commonInfo,
            CraftingRecipe.CraftingBookInfo bookInfo,
            ItemStackTemplate result,
            List<Ingredient> ingredients
    ) {
        super(commonInfo, bookInfo);
        this.result = result;
        this.ingredients = List.copyOf(ingredients);
        this.simple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        }
        if (simple) {
            return input.size() == 1 && ingredients.size() == 1
                    ? ingredients.getFirst().test(input.getItem(0))
                    : input.stackedContents().canCraft(this, null);
        }

        List<ItemStack> nonEmptyItems = new ArrayList<>(input.ingredientCount());
        for (ItemStack item : input.items()) {
            if (!item.isEmpty()) {
                nonEmptyItems.add(item);
            }
        }
        return net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(nonEmptyItems, ingredients) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return result.create();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = CraftingRecipe.defaultCraftingReminder(input);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack ingredient = input.getItem(slot);
            if (ingredient.is(Items.FLINT_AND_STEEL)) {
                remaining.set(slot, damageFlintAndSteel(ingredient));
            }
        }
        return remaining;
    }

    private static ItemStack damageFlintAndSteel(ItemStack original) {
        ItemStack remainder = original.copyWithCount(1);
        if (!remainder.isDamageableItem()) {
            return remainder;
        }

        // Recipe previews, automation and ResultSlot player crafting must produce
        // the same remainder. Flint and Steel always spends exactly one durability.
        if (remainder.nextDamageWillBreak()) {
            return ItemStack.EMPTY;
        }
        remainder.setDamageValue(remainder.getDamageValue() + 1);
        return remainder;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(ingredients);
    }

    @Override
    public RecipeSerializer<DurableShapelessRecipe> getSerializer() {
        return ModRecipeSerializers.DURABLE_SHAPELESS.get();
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(
                ingredients.stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }
}

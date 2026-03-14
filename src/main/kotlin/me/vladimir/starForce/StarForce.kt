package me.vladimir.starForce

import me.vladimir.starForce.commands.Scroll
import me.vladimir.starForce.commands.Star
import me.vladimir.starForce.listeners.Anvil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin

class StarForce : JavaPlugin() {

    override fun onEnable() {
        logger.info("[StarForce] Loaded.")
        server.pluginManager.registerEvents(Anvil(),this)
        getCommand("star")!!.setExecutor(Star())
        getCommand("scroll")!!.setExecutor(Scroll())
        val eBook = ItemStack(Material.FLOW_BANNER_PATTERN)
        val meta = eBook.itemMeta
        meta.customName(
            Component.text("강화 주문서")
                .decoration(TextDecoration.ITALIC, false)
                .color(TextColor.color(150, 0, 255))
        )
        meta.setMaxStackSize(64)
        meta.setCustomModelData(686868)
        eBook.itemMeta = meta
        val key = NamespacedKey(this, "scroll")
        val recipe = ShapedRecipe(key, eBook).apply {
            shape(
                " L ",
                "LBL",
                " L "
            )
            setIngredient('L', Material.IRON_NUGGET)
            setIngredient('B', Material.PAPER)
        }
        server.addRecipe(recipe)
    }

    override fun onDisable() {
        logger.info("[StarForce] Unloaded.")
    }
}

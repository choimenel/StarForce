package me.vladimir.starForce.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class Scroll: CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player && sender.isOp) {
            val amount = args.getOrNull(0)?.toIntOrNull() ?: 64

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

            sender.giveInt(eBook, amount)
            sender.sendMessage("강화 주문서를 ${amount}개 지급했습니다.")
        } else {
            sender.sendMessage(
                Component.text("이 명령어를 실행할 권한이 없습니다!")
                    .color(TextColor.color(255, 0, 0))
            )
            (sender as Player).playSound(sender.location, Sound.ENTITY_VILLAGER_NO,1.0f,1.0f)
        }
        return true
    }

    private fun Player.giveInt(item: ItemStack, amount: Int) {
        val stack = item.clone()
        stack.amount = amount
        this.inventory.addItem(stack)
    }
}
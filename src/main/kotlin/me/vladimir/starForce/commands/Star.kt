package me.vladimir.starForce.commands

import me.vladimir.starForce.listeners.Anvil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class Star : CommandExecutor {

    private val starKey = NamespacedKey("starforce", "stars")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || !sender.isOp) {
            sender.sendMessage(Component.text("이 명령어를 실행할 권한이 없습니다!").color(TextColor.color(255,0,0)))
            return true
        }


        val item = sender.inventory.itemInMainHand
        if (item.type.isAir) {
            sender.sendMessage(Component.text("손에 아이템을 들고 있어야 합니다!").color(TextColor.color(255, 50, 50)))
            return true
        }

        val starAmount = args.getOrNull(0)?.toIntOrNull()
        if (starAmount == null || starAmount !in 1..10) {
            sender.sendMessage(Component.text("사용법: /star <1~10>").color(TextColor.color(255, 255, 0)))
            return true
        }

        val meta = item.itemMeta
        meta.persistentDataContainer.set(starKey, PersistentDataType.INTEGER, starAmount)
        item.itemMeta = meta

        val updatedItem = Anvil().updateStarforceLore(item, false)
        sender.inventory.setItemInMainHand(updatedItem)

        sender.sendMessage(Component.text("★$starAmount 강화 적용 완료!").color(TextColor.color(0, 255, 0)))
        return true
    }
}

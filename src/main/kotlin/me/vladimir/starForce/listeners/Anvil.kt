package me.vladimir.starForce.listeners

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import io.github.monun.invfx.InvFX
import io.github.monun.invfx.openFrame
import org.bukkit.event.entity.EntityDamageEvent

class Anvil : Listener {
    private val playerItemMap = mutableMapOf<UUID, ItemStack?>()
    private val successChances = listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)
    private val destroyChances = listOf(0, 0, 0, 0, 0, 10, 20, 30, 40, 50)
    private val downgradeChances = listOf(0, 10, 20, 30, 40, 40, 40, 40, 40, 40)
    private val starKey = NamespacedKey("starforce", "stars")
    private val doubleJumpCooldowns = mutableMapOf<UUID, Long>()
    private val fallImmunePlayers = mutableSetOf<UUID>()


    @EventHandler
    fun table(event: PlayerInteractEvent) {
        val block = event.clickedBlock
        if (event.action == Action.RIGHT_CLICK_BLOCK && block?.type == Material.ANVIL || block?.type == Material.CHIPPED_ANVIL || block?.type == Material.DAMAGED_ANVIL) {
            event.isCancelled = true
            open(event.player)
        }
    }

    private fun open(player: Player) {
        val uuid = player.uniqueId

        val frame = InvFX.frame(
            4,
            Component.text("ꈂꈂꈂꈂꈂꈂꈂꈂы").color(TextColor.color(255, 255, 255))
        ) {
            val centerSlot = slot(4, 1) {
                item = playerItemMap[uuid]?.let { updateStarforceLore(it, true) }

                onClick { e ->
                    e.isCancelled = true

                    if (e.click == ClickType.SHIFT_LEFT || e.click == ClickType.SHIFT_RIGHT) {
                        playerItemMap[uuid]?.let {
                            player.inventory.addItem(updateStarforceLore(it, false))
                            playerItemMap[uuid] = null
                            item = null
                        }
                        return@onClick
                    }

                    val itemToEnchant = playerItemMap[uuid]
                    if (itemToEnchant != null && itemToEnchant.isEnchantable) {
                        val meta = itemToEnchant.itemMeta
                        val container = meta.persistentDataContainer
                        var starLevel = container.get(starKey, PersistentDataType.INTEGER) ?: 0

                        if (starLevel >= 10) {
                            player.sendMessage(Component.text("최대 강화 수치입니다!").color(TextColor.color(255, 255, 0)))
                            player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f)
                            return@onClick
                        }

                        val requiredScroll = player.inventory.contents.firstOrNull {
                            it != null &&
                                    it.type == Material.FLOW_BANNER_PATTERN &&
                                    it.itemMeta?.customName() == Component.text("강화 주문서")
                                .decoration(TextDecoration.ITALIC, false)
                                .color(TextColor.color(150, 0, 255)) &&
                                    it.itemMeta?.hasCustomModelData() == true &&
                                    it.itemMeta?.customModelData == 686868
                        }

                        if (requiredScroll == null) {
                            player.sendMessage(Component.text("강화 주문서가 필요합니다!").color(TextColor.color(255, 50, 50)))
                            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                            return@onClick
                        }

                        requiredScroll.amount -= 1
                        if (requiredScroll.amount <= 0) {
                            player.inventory.removeItem(requiredScroll)
                        }

                        val roll = Random().nextInt(100) + 1
                        val successChance = successChances[starLevel]
                        val destroyChance = destroyChances[starLevel]
                        val downgradeChance = downgradeChances[starLevel]

                        when {
                            roll <= successChance -> {
                                starLevel += 1
                                player.sendMessage(
                                    Component.text("강화 성공! ★$starLevel").color(TextColor.color(0, 255, 0))
                                )
                                player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f)
                            }

                            roll <= successChance + destroyChance -> {
                                player.sendMessage(Component.text("강화 실패 - 아이템 파괴됨!").color(TextColor.color(255, 0, 0)))
                                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
                                playerItemMap[uuid] = null
                                item = null
                                return@onClick
                            }

                            else -> {
                                val downRoll = Random().nextInt(100) + 1
                                if (downRoll <= downgradeChance && starLevel > 0) {
                                    starLevel -= 1
                                    player.sendMessage(
                                        Component.text("강화 실패 - 강화 수치 감소! ★$starLevel")
                                            .color(TextColor.color(255, 165, 0))
                                    )
                                } else {
                                    player.sendMessage(Component.text("강화 실패").color(TextColor.color(255, 100, 100)))
                                }
                                player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f)
                            }
                        }

                        container.set(starKey, PersistentDataType.INTEGER, starLevel)
                        itemToEnchant.itemMeta = meta
                        item = updateStarforceLore(itemToEnchant, true)
                        playerItemMap[uuid] = itemToEnchant
                    }
                }
            }

            onClickBottom { e ->
                val clicked = e.currentItem ?: return@onClickBottom
                if (!clicked.isEnchantable) return@onClickBottom

                // Disallowed materials
                val type = clicked.type
                if (
                    type in listOf(
                        Material.WOODEN_SWORD,
                        Material.STONE_SWORD,
                        Material.GOLDEN_SWORD,
                        Material.WOODEN_AXE,
                        Material.STONE_AXE,
                        Material.GOLDEN_AXE,
                        Material.LEATHER_HELMET,
                        Material.LEATHER_CHESTPLATE,
                        Material.LEATHER_LEGGINGS,
                        Material.LEATHER_BOOTS,
                        Material.CHAINMAIL_HELMET,
                        Material.CHAINMAIL_CHESTPLATE,
                        Material.CHAINMAIL_LEGGINGS,
                        Material.CHAINMAIL_BOOTS
                    )
                ) {
                    player.sendMessage(Component.text("이 아이템은 강화할 수 없습니다!").color(TextColor.color(255, 100, 100)))
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                    e.isCancelled = true
                    return@onClickBottom
                }

                val clone = clicked.clone()
                if (player.inventory.contains(clicked)) {
                    player.inventory.removeItem(clicked)
                }

                playerItemMap[uuid] = clone
                centerSlot.item = updateStarforceLore(clone, true)
                e.isCancelled = true
            }

            onClose {
                playerItemMap[uuid]?.let {
                    player.inventory.addItem(updateStarforceLore(it, false))
                    playerItemMap.remove(uuid)
                }
            }

            for (x in 0..8) {
                for (y in 0..3) {
                    if (x == 4 && y == 1) continue
                    slot(x, y) {
                        item = ItemStack(Material.IRON_NUGGET)
                        val e = item!!.itemMeta
                        e.isHideTooltip = true
                        e.itemModel = NamespacedKey("minecraft", "air")
                        item!!.itemMeta = e
                    }
                }
            }
        }

        player.openFrame(frame)
    }

    private val ItemStack.isEnchantable: Boolean
        get() = type != Material.AIR && Enchantment.values().any { it.canEnchantItem(this) }

    public fun updateStarforceLore(item: ItemStack, showChances: Boolean): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer
        val starLevel = container.get(starKey, PersistentDataType.INTEGER) ?: 0

        val lore = mutableListOf<Component>()

        // Adding Starforce lore (stars displayed)
        lore.add(
            Component.text("★".repeat(starLevel) + "☆".repeat(10 - starLevel))
                .color(TextColor.color(255, 215, 0))
                .decoration(TextDecoration.ITALIC, false)
        )

        if (showChances && starLevel < 10) {
            // Adding success, failure, and downgrade chances
            lore.add(
                Component.text("성공 확률: ${successChances[starLevel]}%")
                    .color(TextColor.color(0, 255, 0)).decoration(TextDecoration.ITALIC, false)
            )
            lore.add(
                Component.text("파괴 확률: ${destroyChances[starLevel]}%")
                    .color(TextColor.color(255, 0, 0)).decoration(TextDecoration.ITALIC, false)
            )
            lore.add(
                Component.text("하락 확률: ${downgradeChances[starLevel]}%")
                    .color(TextColor.color(255, 165, 0)).decoration(TextDecoration.ITALIC, false)
            )
        }

        // Setting the updated lore to the item
        meta.lore(lore)

        meta.attributeModifiers = null

        val baseAttackDamage = when (item.type) {
            Material.NETHERITE_SWORD -> 8.0
            Material.DIAMOND_SWORD -> 7.0
            Material.IRON_SWORD -> 6.0
            Material.NETHERITE_AXE -> 10.0
            Material.DIAMOND_AXE -> 9.0
            Material.IRON_AXE -> 9.0
            Material.BOW -> 0.0
            Material.CROSSBOW -> 0.0
            Material.TRIDENT -> 8.0
            else -> 0.0
        }

        val baseArmor = when (item.type) {
            Material.NETHERITE_HELMET -> 3.0
            Material.DIAMOND_HELMET -> 3.0
            Material.IRON_HELMET -> 2.0
            Material.NETHERITE_CHESTPLATE -> 8.0
            Material.DIAMOND_CHESTPLATE -> 8.0
            Material.IRON_CHESTPLATE -> 6.0
            Material.NETHERITE_LEGGINGS -> 6.0
            Material.DIAMOND_LEGGINGS -> 6.0
            Material.IRON_LEGGINGS -> 5.0
            Material.NETHERITE_BOOTS -> 3.0
            Material.DIAMOND_BOOTS -> 3.0
            Material.IRON_BOOTS -> 2.0
            else -> 0.0
        }

        val baseArmorToughness = when (item.type) {
            Material.NETHERITE_HELMET -> 3.0
            Material.DIAMOND_HELMET -> 2.0
            Material.NETHERITE_CHESTPLATE -> 3.0
            Material.DIAMOND_CHESTPLATE -> 2.0
            Material.NETHERITE_LEGGINGS -> 3.0
            Material.DIAMOND_LEGGINGS -> 2.0
            Material.NETHERITE_BOOTS -> 3.0
            Material.DIAMOND_BOOTS -> 2.0
            else -> 0.0
        }

        val baseAttackSpeed = when (item.type) {
            Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD -> 1.6
            else -> 0.0
        }

        // Add attribute modifiers based on star level (the same as in your original code)
        when (item.type) {
            Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD -> {
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        NamespacedKey("minecraft", "attack_damage"),
                        baseAttackDamage + (starLevel * 0.2),
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                meta.addAttributeModifier(
                    Attribute.ATTACK_SPEED,
                    AttributeModifier(
                        NamespacedKey("minecraft", "attack_speed"),
                        baseAttackSpeed + (starLevel * 0.05),
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.IRON_AXE -> {
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        NamespacedKey("minecraft", "attack_damage"),
                        baseAttackDamage + (starLevel * 0.3),
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            Material.BOW, Material.CROSSBOW -> {
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        NamespacedKey("minecraft", "attack_damage"),
                        (starLevel * 0.5),
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            Material.TRIDENT -> {
                meta.addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        NamespacedKey("minecraft", "attack_damage"),
                        baseAttackDamage + (starLevel * 0.4),
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            Material.NETHERITE_HELMET, Material.DIAMOND_HELMET, Material.IRON_HELMET -> {
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(
                        NamespacedKey("minecraft", "armor"),
                        baseArmor + starLevel * 0.2,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(
                        NamespacedKey("minecraft", "armor_toughness"),
                        baseArmorToughness,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            Material.NETHERITE_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE -> {
                meta.addAttributeModifier(
                    Attribute.MAX_HEALTH,
                    AttributeModifier(
                        NamespacedKey("minecraft", "max_health"),
                        starLevel * 1.0,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(
                        NamespacedKey("minecraft", "armor"),
                        baseArmor + starLevel * 0.2,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(
                        NamespacedKey("minecraft", "armor_toughness"),
                        baseArmorToughness,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            Material.NETHERITE_BOOTS, Material.DIAMOND_BOOTS, Material.IRON_BOOTS -> {
                meta.addAttributeModifier(
                    Attribute.MOVEMENT_SPEED,
                    AttributeModifier(
                        NamespacedKey("minecraft", "movement_speed"),
                        starLevel * 0.01,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR,
                    AttributeModifier(
                        NamespacedKey("minecraft", "armor"),
                        baseArmor + starLevel * 0.2,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                meta.addAttributeModifier(
                    Attribute.ARMOR_TOUGHNESS,
                    AttributeModifier(
                        NamespacedKey("minecraft", "armor_toughness"),
                        baseArmorToughness,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            else -> {}
        }
        if (starLevel >= 10) {
            meta.isUnbreakable = true
            if (item.type == Material.DIAMOND_BOOTS || item.type == Material.NETHERITE_BOOTS || item.type == Material.IRON_BOOTS) {
                lore.add(
                    Component.text("10성 특별 능력: 더블 점프")
                        .color(TextColor.color(255, 215, 0))
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
            if (item.type == Material.DIAMOND_SWORD || item.type == Material.NETHERITE_SWORD || item.type == Material.IRON_SWORD) {
                lore.add(
                    Component.text("10성 특별 능력: 무적 시간 무시")
                        .color(TextColor.color(255, 215, 0))
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
        }

        item.itemMeta = meta
        meta.lore(lore)
        return item
    }


    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (player.gameMode != GameMode.SURVIVAL && player.gameMode != GameMode.ADVENTURE) return

        val boots = player.inventory.boots ?: return
        val meta = boots.itemMeta ?: return
        val starLevel = meta.persistentDataContainer.get(starKey, PersistentDataType.INTEGER) ?: return
        if (starLevel < 10) return

        // Enable flight when in air, disable when on ground
        if (!player.isOnGround && !player.allowFlight) {
            player.allowFlight = true
        } else if (player.isOnGround && player.allowFlight) {
            player.allowFlight = false
        }
    }

    @EventHandler
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        if (player.gameMode != GameMode.SURVIVAL && player.gameMode != GameMode.ADVENTURE) return

        val boots = player.inventory.boots ?: return
        val meta = boots.itemMeta ?: return
        val starLevel = meta.persistentDataContainer.get(starKey, PersistentDataType.INTEGER) ?: return
        if (starLevel < 10) return

        val lastUsed = doubleJumpCooldowns[player.uniqueId] ?: 0L
        if (System.currentTimeMillis() - lastUsed < 30_000) {
            player.sendMessage("§c더블 점프는 쿨다운 중입니다!")
            event.isCancelled = true
            player.allowFlight = false
            return
        }

        event.isCancelled = true
        player.allowFlight = false
        player.isFlying = false

        player.velocity = player.location.direction.setY(1.0).multiply(1.2)
        player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f)
        player.world.spawnParticle(Particle.CLOUD, player.location, 15, 0.2, 0.5, 0.2, 0.05)

        doubleJumpCooldowns[player.uniqueId] = System.currentTimeMillis()
        fallImmunePlayers.add(player.uniqueId) // mark for fall damage immunity
    }



    @EventHandler
    fun onDamage(e: EntityDamageByEntityEvent) {
        val damager = e.damager as? Player ?: return
        val damaged = e.entity
        val item = damager.inventory.itemInMainHand
        if (!item.type.name.contains("SWORD")) return
        val starLevel = item.itemMeta?.persistentDataContainer?.get(starKey, PersistentDataType.INTEGER) ?: return
        if (starLevel < 10) return
        (damaged as Player).noDamageTicks = 0
    }
    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        if (event.cause == EntityDamageEvent.DamageCause.FALL && fallImmunePlayers.contains(player.uniqueId)) {
            event.isCancelled = true
            fallImmunePlayers.remove(player.uniqueId)
        }
    }
}

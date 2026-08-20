package com.odtheking.odinaddon.features.impl.nether

import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.nether.Vesuvius
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.modMessage
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.world.item.Items

object AutoVesuvius : Module(
    name = "Auto Vesuvius",
    description = "Opens Kuudra Chests for you"
) {
    private val delay by NumberSetting("Delay", 4, min = 1, max = 10, desc = "Delay", unit = "t")
    private val minProfit by NumberSetting("Minimum profit", 200, min = 0, max = 1000, desc = "Minimum profit", unit = "k")
    private val action by ActionSetting("Start", "") {
        active = true
    }

    private val chestScreenRegex = Regex("^.*Croesus|.*Vesuvius$")
    private val selectionRegex = Regex("^(Kuudra - .+)$")
    private val chestRegex = Regex("^((Free|Paid) Chest)$")

    private var active = false

    init {
        onReceive<ClientboundOpenScreenPacket> {
            if (!enabled || !active) return@onReceive

            when {
                chestScreenRegex.matches(title.string) -> { handleChestScreen(containerId) }
                selectionRegex.matches(title.string) -> { handleSelection(containerId) }
                chestRegex.matches(title.string) -> { handleChest(containerId) }
            }
        }
        on<ChatPacketEvent> {
            if (!active) return@on

            if (component.string.contains("You cannot afford this!")) {
                active = false
            }

            if (component.string.contains("CHEST REWARDS")) {
                mc.options.keyUse.clickCount++
            }
        }
        on<LevelEvent.Load> {
            active = false
        }
    }
    private fun handleChestScreen(id: Int) {
        schedule(delay, true) {
            val sc = mc.screen as? AbstractContainerScreen<*> ?: return@schedule

            if (id != sc.menu.containerId) {
                modMessage("lock in twin")
                return@schedule
            }

            val slot = sc.menu.slots.firstOrNull { items ->
                items.item.hoverName.string == "Kuudra's Hollow" && items.item.loreString.none { it == "No more chests to open!" }
            } ?: run {
                if (sc.menu.slots[53].item.item == Items.ARROW) {
                    mc.player!!.clickSlot(sc.menu.containerId, 53)
                } else {
                    modMessage("All Chests opened!")
                    active = false
                }
                return@schedule
            }

            mc.player!!.clickSlot(sc.menu.containerId, slot.index)
        }
    }
    private fun handleSelection(id: Int) {
        schedule(delay, true) {
            val sc = mc.screen as? AbstractContainerScreen<*> ?: return@schedule

            if (id != sc.menu.containerId) {
                modMessage("lock in twin")
                return@schedule
            }

            if (sc.menu.slots[13].item.item == Items.PLAYER_HEAD) {
                mc.player!!.clickSlot(sc.menu.containerId, 13)
                return@schedule
            }

            cycleProfit(id)
        }
    }
    fun handleChest(id: Int) {
        schedule(delay, true) {
            val sc = mc.screen as? AbstractContainerScreen<*> ?: return@schedule

            if (id != sc.menu.containerId) {
                modMessage("lock in twin")
                return@schedule
            }

            mc.player!!.clickSlot(sc.menu.containerId, 31)
        }
    }

    fun cycleProfit(id: Int) {
        val sc = mc.screen as? AbstractContainerScreen<*> ?: return

        if (id != sc.menu.containerId) {
            modMessage("lock in twin")
            return
        }

        val profit = getProfit() ?: run {
            modMessage("Failed to get profit. Retrying in 1 second.")
            schedule(20, true) {
                val sc1 = mc.screen as? AbstractContainerScreen<*> ?: return@schedule
                if (id == sc1.menu.containerId) cycleProfit(id)
            }
            return
        }

        if (profit >= minProfit * 1_000) {
            mc.player!!.clickSlot(sc.menu.containerId, 14)
        } else {
            mc.player!!.clickSlot(sc.menu.containerId, 12)
        }
    }

    fun getProfit(): Double? {
        val chestField = Vesuvius::class.java.getDeclaredField("currentChest")
        chestField.isAccessible = true
        val currentChest = chestField.get(Vesuvius) ?: return null

        val chestClass = currentChest::class.java
        val profit = chestClass.getDeclaredField("profit").also { it.isAccessible = true }.get(currentChest) as Double
        return profit
    }
}

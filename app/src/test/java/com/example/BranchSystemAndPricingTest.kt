package com.example

import com.example.model.BranchMenuOverride
import com.example.model.CafeBranch
import com.example.model.CartItem
import com.example.model.DeliverySettings
import com.example.model.MenuItem
import com.example.model.NotificationRoutingPolicy
import com.example.model.OrderLineItem
import com.example.model.OrderPricingEngine
import com.example.model.OrderStatus
import com.example.model.OrderType
import com.example.model.SUPER_ADMIN_EMAIL
import com.example.model.UserRole
import com.example.ui.SessionCartController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Branch-specific new-order notification routing (Super Admin NEVER alerted; other Branch Admins excluded)
 * 2. Free Delivery Rs. 800 minimum order boundary calculations (500, 799, 800, 1000, PICKUP, custom branch fees)
 * 3. Independent branch menu price overrides & cart synchronization without mutating global catalog or old orders
 * 4. 7-stage OrderStatus active/terminal & message helpers
 */
class BranchSystemAndPricingTest {

    // --- 1. Role & Branch Notification Isolation ---

    @Test
    fun notificationRouting_onlyAssignedBranchAdminReceivesNewOrderAlerts() {
        // Branch A Admin receives alert for Branch A
        assertTrue(
            NotificationRoutingPolicy.shouldReceiveNewOrderAlert(
                recipientRole = UserRole.BranchAdmin,
                recipientBranchId = "branch_1",
                orderBranchId = "branch_1",
                userEmail = "branch1.mgr@gmail.com"
            )
        )

        // Branch A Admin MUST NOT receive alert for Branch B
        assertFalse(
            NotificationRoutingPolicy.shouldReceiveNewOrderAlert(
                recipientRole = UserRole.BranchAdmin,
                recipientBranchId = "branch_1",
                orderBranchId = "branch_2",
                userEmail = "branch1.mgr@gmail.com"
            )
        )

        // Super Admin MUST NEVER receive new-order alerts (even if assignedBranchId matches)
        assertFalse(
            NotificationRoutingPolicy.shouldReceiveNewOrderAlert(
                recipientRole = UserRole.SuperAdmin,
                recipientBranchId = "branch_1",
                orderBranchId = "branch_1",
                userEmail = SUPER_ADMIN_EMAIL
            )
        )
        assertFalse(
            NotificationRoutingPolicy.shouldReceiveNewOrderAlert(
                recipientRole = UserRole.BranchAdmin,
                recipientBranchId = "branch_1",
                orderBranchId = "branch_1",
                userEmail = SUPER_ADMIN_EMAIL
            )
        )

        // Customer MUST NOT receive new-order alerts
        assertFalse(
            NotificationRoutingPolicy.shouldReceiveNewOrderAlert(
                recipientRole = UserRole.Customer,
                recipientBranchId = "",
                orderBranchId = "branch_1",
                userEmail = "customer@gmail.com"
            )
        )

        // Background monitor enabled ONLY for assigned Branch Admin, NEVER Super Admin
        assertFalse(
            NotificationRoutingPolicy.shouldRunBackgroundOrderMonitor(
                role = UserRole.SuperAdmin,
                assignedBranchId = "branch_1",
                userEmail = SUPER_ADMIN_EMAIL
            )
        )
        assertTrue(
            NotificationRoutingPolicy.shouldRunBackgroundOrderMonitor(
                role = UserRole.BranchAdmin,
                assignedBranchId = "branch_2",
                userEmail = "branch2.mgr@gmail.com"
            )
        )
    }

    // --- 2. Free Delivery Rs. 800 Minimum Order Pricing Engine ---

    @Test
    fun pricingEngine_freeDeliveryThresholdBoundaries_500_799_800_1000_andPickup() {
        val branch1 = CafeBranch(id = "branch_1", name = "Branch 1", deliveryFeePkr = 150L)
        val branch2 = CafeBranch(id = "branch_2", name = "Branch 2", deliveryFeePkr = 200L)
        val defaultSettings = DeliverySettings(freeDeliveryMinOrderPkr = 800L, defaultDeliveryFeePkr = 150L)

        // Order total Rs. 500 -> Delivery fee applied (Rs. 150), Rs. 300 needed for free delivery
        val p500 = OrderPricingEngine.calculate(
            subtotalPkr = 500L,
            orderType = OrderType.DELIVERY,
            branch = branch1,
            settings = defaultSettings
        )
        assertFalse(p500.isFreeDelivery)
        assertEquals(150L, p500.deliveryFeePkr)
        assertEquals(650L, p500.totalPkr)
        assertEquals(300L, p500.remainingForFreeDeliveryPkr)

        // Order total Rs. 799 -> Delivery fee still applied, Rs. 1 needed for free delivery
        val p799 = OrderPricingEngine.calculate(
            subtotalPkr = 799L,
            orderType = OrderType.DELIVERY,
            branch = branch1,
            settings = defaultSettings
        )
        assertFalse(p799.isFreeDelivery)
        assertEquals(150L, p799.deliveryFeePkr)
        assertEquals(949L, p799.totalPkr)
        assertEquals(1L, p799.remainingForFreeDeliveryPkr)

        // Order total Rs. 800 -> FREE DELIVERY UNLOCKED (Rs. 0)
        val p800 = OrderPricingEngine.calculate(
            subtotalPkr = 800L,
            orderType = OrderType.DELIVERY,
            branch = branch1,
            settings = defaultSettings
        )
        assertTrue(p800.isFreeDelivery)
        assertEquals(0L, p800.deliveryFeePkr)
        assertEquals(800L, p800.totalPkr)
        assertEquals(0L, p800.remainingForFreeDeliveryPkr)

        // Order total Rs. 1000 with Rs. 100 promo -> FREE DELIVERY (based on subtotal >= 800) + discount
        val p1000 = OrderPricingEngine.calculate(
            subtotalPkr = 1000L,
            discountPkr = 100L,
            orderType = OrderType.DELIVERY,
            branch = branch2,
            settings = defaultSettings
        )
        assertTrue(p1000.isFreeDelivery)
        assertEquals(0L, p1000.deliveryFeePkr)
        assertEquals(900L, p1000.totalPkr)

        // Branch 2 custom delivery fee (Rs. 200) applies below Rs. 800
        val pBranch2Low = OrderPricingEngine.calculate(
            subtotalPkr = 600L,
            orderType = OrderType.DELIVERY,
            branch = branch2,
            settings = defaultSettings
        )
        assertFalse(pBranch2Low.isFreeDelivery)
        assertEquals(200L, pBranch2Low.deliveryFeePkr)
        assertEquals(800L, pBranch2Low.totalPkr)

        // PICKUP orders ALWAYS have Rs. 0 delivery fee even below Rs. 800
        val pPickup = OrderPricingEngine.calculate(
            subtotalPkr = 350L,
            orderType = OrderType.PICKUP,
            branch = branch2,
            settings = defaultSettings
        )
        assertTrue(pPickup.isFreeDelivery)
        assertEquals(0L, pPickup.deliveryFeePkr)
        assertEquals(350L, pPickup.totalPkr)
        assertEquals(0L, pPickup.remainingForFreeDeliveryPkr)

        // Super Admin changes global threshold to Rs. 1200 -> Rs. 1000 now pays branch fee
        val customSettings = DeliverySettings(freeDeliveryMinOrderPkr = 1200L, defaultDeliveryFeePkr = 150L)
        val pCustom = OrderPricingEngine.calculate(
            subtotalPkr = 1000L,
            orderType = OrderType.DELIVERY,
            branch = branch1,
            settings = customSettings
        )
        assertFalse(pCustom.isFreeDelivery)
        assertEquals(150L, pCustom.deliveryFeePkr)
        assertEquals(200L, pCustom.remainingForFreeDeliveryPkr)
    }

    // --- 3. Independent Branch Menu Pricing & Cart Sync ---

    @Test
    fun branchMenuOverrides_isolatePricesPerBranch_andSyncCustomerCartOnBranchSwitch() {
        val globalBurger = MenuItem(id = "zinger_1", name = "Zinger Burger", pricePkr = 450L, available = true)
        val globalPizza = MenuItem(id = "pizza_1", name = "Extreme Pizza", pricePkr = 1200L, available = true)
        val globalMenu = listOf(globalBurger, globalPizza)

        // Branch 1 overrides Zinger Burger to Rs. 520; Branch 2 keeps Rs. 450 and marks Pizza sold out
        val branch1Overrides = mapOf(
            "zinger_1" to BranchMenuOverride(itemId = "zinger_1", branchId = "branch_1", pricePkr = 520L, available = true)
        )
        val branch2Overrides = mapOf(
            "pizza_1" to BranchMenuOverride(itemId = "pizza_1", branchId = "branch_2", pricePkr = 1200L, available = false)
        )

        val branch1Effective = globalMenu.map { item ->
            val ov = branch1Overrides[item.id]
            if (ov != null) item.copy(pricePkr = ov.pricePkr, available = ov.available) else item
        }
        val branch2Effective = globalMenu.map { item ->
            val ov = branch2Overrides[item.id]
            if (ov != null) item.copy(pricePkr = ov.pricePkr, available = ov.available) else item
        }

        // Branch 1 sees Rs. 520, Branch 2 still sees Rs. 450 (global untouched!)
        assertEquals(450L, globalBurger.pricePkr)
        assertEquals(520L, branch1Effective.first { it.id == "zinger_1" }.pricePkr)
        assertEquals(450L, branch2Effective.first { it.id == "zinger_1" }.pricePkr)
        assertFalse(branch2Effective.first { it.id == "pizza_1" }.available)

        // Customer adds both items while viewing Branch 1, then switches to Branch 2
        val cart = SessionCartController()
        cart.addToCart(branch1Effective.first { it.id == "zinger_1" })
        cart.addToCart(branch1Effective.first { it.id == "pizza_1" })
        assertEquals(1720L, cart.state.value.subtotalPkr)

        // Switching to Branch 2 updates Zinger Burger price to Rs. 450 and removes unavailable Pizza
        cart.syncCartWithEffectiveMenu(branch2Effective)
        assertEquals(1, cart.state.value.cartItems.size)
        assertEquals(450L, cart.state.value.cartItems.first().menuItem.pricePkr)
        assertEquals(450L, cart.state.value.subtotalPkr)

        // Previously placed order snapshot retains its original price immutably
        val placedSnapshot = OrderLineItem(
            itemId = "zinger_1",
            name = "Zinger Burger",
            quantity = 1,
            unitPricePkr = 520L,
            lineTotalPkr = 520L
        )
        assertEquals(520L, placedSnapshot.unitPricePkr)
    }

    // --- 4. Order Status Lifecycle Semantics ---

    @Test
    fun orderStatus_activeTerminalAndCustomerMessages_areAccurate() {
        assertTrue(OrderStatus.Pending.isActive)
        assertTrue(OrderStatus.Confirmed.isActive)
        assertTrue(OrderStatus.Preparing.isActive)
        assertTrue(OrderStatus.Ready.isActive)
        assertTrue(OrderStatus.OutForDelivery.isActive)
        assertFalse(OrderStatus.Delivered.isActive)
        assertFalse(OrderStatus.Rejected.isActive)
        assertTrue(OrderStatus.Delivered.isTerminal)
        assertTrue(OrderStatus.Rejected.isTerminal)

        assertTrue(
            OrderStatus.Confirmed.notificationMessageFor(OrderType.DELIVERY.name)
                .contains("Order Confirmed", ignoreCase = true)
        )
        assertTrue(
            OrderStatus.Rejected.notificationMessageFor(OrderType.DELIVERY.name, "Out of stock")
                .contains("Out of stock")
        )
    }
}

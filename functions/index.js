const {
  onDocumentCreated,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-lahorifo-70b911b7-52d3-4eb9-88ba-3940a68e0641";
const SUPER_ADMIN_EMAIL = "uk9798989@gmail.com";

function sanitizeTopicSegment(branchId) {
  return String(branchId || "branch_1").replace(/[^a-zA-Z0-9\-_.~%]/g, "_");
}

function formatOrderTime(timestamp) {
  try {
    const date = timestamp && typeof timestamp.toDate === "function"
      ? timestamp.toDate()
      : new Date();
    return date.toLocaleTimeString("en-PK", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
      timeZone: "Asia/Karachi",
    });
  } catch (_) {
    return new Date().toISOString();
  }
}

/**
 * Triggers real-time FCM push notifications ONLY to the Branch Admin(s) assigned
 * to orderData.branchId whenever a new customer order is placed.
 *
 * Strict Role Isolation:
 * - Super Admin (uk9798989@gmail.com) is explicitly excluded and NEVER receives new-order alerts.
 * - Branch Admins of OTHER branches NEVER receive notifications for this order.
 * - Sends to direct assigned Branch Admin tokens (with topic fallback ONLY if no tokens exist)
 *   to prevent duplicate push alerts on the same device.
 */
exports.notifyBranchAdminsNewOrder = onDocumentCreated(
  {
    document: "orders/{orderId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const orderData = event.data?.data();
    if (!orderData) return;

    const orderId = event.params.orderId;
    const branchId = String(orderData.branchId || "").trim();
    if (!branchId) {
      logger.warn("New order missing branchId; skipping branch admin FCM dispatch", { orderId });
      return;
    }

    const branchName = String(orderData.branchName || "Lahori Food Cafe");
    const customerName = String(orderData.customerName || "Customer");
    const customerPhone = String(orderData.customerPhone || "");
    const deliveryAddress = String(orderData.deliveryAddress || "");
    const paymentMethod = String(orderData.paymentMethod || "Cash on Delivery");
    const orderType = String(orderData.orderType || "DELIVERY").toUpperCase();
    const totalPkr = Number(orderData.totalPkr || 0);
    const itemsSummary = String(orderData.itemsSummary || "New Order");
    const orderTime = formatOrderTime(orderData.createdAt);
    const shortId = orderId.slice(-6).toUpperCase();
    const modeLabel = orderType === "PICKUP" ? "Pickup" : "Delivery";

    const title = `New ${modeLabel} Order #${shortId} — Rs. ${totalPkr}`;
    const body = `${customerName} (${customerPhone || "No phone"}) • ${itemsSummary} • ${paymentMethod}`;
    const tag = `new_order_${orderId}`;

    const commonData = {
      eventType: "NEW_ORDER",
      orderId,
      orderNumber: `#${shortId}`,
      branchId,
      branchName,
      orderType,
      totalPkr: String(totalPkr),
      customerName,
      customerPhone,
      deliveryAddress,
      paymentMethod,
      itemsSummary,
      orderTime,
      status: String(orderData.status || "Pending"),
      title,
      body,
    };

    const androidConfig = {
      priority: "high",
      notification: {
        channelId: "lahori_branch_orders",
        color: "#E4002B",
        sound: "default",
        tag,
      },
    };

    const db = getFirestore(DATABASE_ID);
    const messaging = getMessaging();

    // 1. Resolve ONLY assigned Branch Admins for this exact branchId from authoritative branch_admins collection.
    // Never trust client-supplied orderData.branchAdminEmails and explicitly exclude Super Admin.
    const adminSnapshot = await db
      .collection("branch_admins")
      .where("branchId", "==", branchId)
      .where("role", "==", "BranchAdmin")
      .get();

    const adminEmails = new Set();
    adminSnapshot.docs.forEach((doc) => {
      const email = String(doc.get("email") || "").trim().toLowerCase();
      if (email && email !== SUPER_ADMIN_EMAIL) {
        adminEmails.add(email);
      }
    });

    // 2. Collect unique FCM tokens for matching Branch Admin accounts only (skipping any SuperAdmin profile)
    const tokenEntries = [];
    const seenTokens = new Set();

    await Promise.all(
      Array.from(adminEmails).map(async (email) => {
        const usersSnap = await db
          .collection("users")
          .where("email", "==", email)
          .get();

        await Promise.all(
          usersSnap.docs.map(async (userDoc) => {
            const profileRole = String(userDoc.get("role") || "");
            if (profileRole === "SuperAdmin") return;

            const tokensSnap = await userDoc.ref.collection("fcm_tokens").get();
            tokensSnap.docs.forEach((tokenDoc) => {
              const token = tokenDoc.get("token");
              if (typeof token === "string" && token.length >= 10 && !seenTokens.has(token)) {
                seenTokens.add(token);
                tokenEntries.push({ token, ref: tokenDoc.ref });
              }
            });
          })
        );
      })
    );

    let multicastSuccess = 0;
    let multicastFailure = 0;
    let topicMessageId = "";
    const topic = `branch_${sanitizeTopicSegment(branchId)}_orders`;

    // 3. Dispatch once (direct tokens preferred; topic fallback ONLY if no direct tokens exist)
    // to guarantee zero duplicate push alerts per Branch Admin device.
    if (tokenEntries.length > 0) {
      const tokens = tokenEntries.map((entry) => entry.token);
      const response = await messaging.sendEachForMulticast({
        tokens,
        notification: { title, body },
        data: commonData,
        android: androidConfig,
      });
      multicastSuccess = response.successCount;
      multicastFailure = response.failureCount;

      const cleanupPromises = [];
      response.responses.forEach((sendResult, index) => {
        if (!sendResult.success) {
          const errorCode = sendResult.error?.code;
          if (
            errorCode === "messaging/registration-token-not-registered" ||
            errorCode === "messaging/invalid-registration-token"
          ) {
            cleanupPromises.push(tokenEntries[index].ref.delete());
          }
        }
      });
      if (cleanupPromises.length > 0) {
        await Promise.allSettled(cleanupPromises);
      }
    } else if (adminEmails.size > 0) {
      try {
        topicMessageId = await messaging.send({
          topic,
          notification: { title, body },
          data: commonData,
          android: androidConfig,
        });
      } catch (topicErr) {
        logger.warn("Topic fallback dispatch warning for new branch order", {
          orderId,
          branchId,
          topic,
          error: String(topicErr),
        });
      }
    }

    logger.info("Dispatched branch-only new order FCM notifications", {
      orderId,
      branchId,
      adminCount: adminEmails.size,
      directTokens: tokenEntries.length,
      multicastSuccess,
      multicastFailure,
      topicFallbackUsed: Boolean(topicMessageId),
    });
  }
);

function buildCustomerStatusCopy(nextStatus, isPickup, shortId, branchName, rejectionReason) {
  switch (nextStatus) {
    case "Confirmed":
      return {
        title: `Order Confirmed! (#${shortId})`,
        body: `Order Confirmed! ${branchName} has accepted your order and sent it to the kitchen.`,
      };
    case "Preparing":
      return {
        title: `Your Order Is Being Prepared (#${shortId})`,
        body: `Chefs at ${branchName} are freshly preparing your order right now!`,
      };
    case "Ready":
      return {
        title: `Your Order Is Ready (#${shortId})`,
        body: isPickup
          ? `Order #${shortId} is hot and ready for collection at ${branchName}!`
          : `Order #${shortId} is freshly packed and ready for rider pickup at ${branchName}.`,
      };
    case "OutForDelivery":
    case "Dispatched":
      return {
        title: isPickup
          ? `Ready for Pickup (#${shortId})`
          : `Your Order Is on the Way (#${shortId})`,
        body: isPickup
          ? `Order #${shortId} is waiting at ${branchName} pickup counter!`
          : `Your rider has left ${branchName} and is heading to your doorstep!`,
      };
    case "Delivered":
      return {
        title: isPickup ? `Order Picked Up (#${shortId})` : `Order Delivered (#${shortId})`,
        body: isPickup
          ? `Order #${shortId} picked up from ${branchName}. Enjoy your meal!`
          : `Order #${shortId} delivered! Enjoy your Lahori Food Cafe feast.`,
      };
    case "Rejected":
      return {
        title: `Order Rejected (#${shortId})`,
        body: rejectionReason
          ? `${branchName} could not accept Order #${shortId}: ${rejectionReason}`
          : `${branchName} could not accept Order #${shortId}. Please contact the branch or try again.`,
      };
    default:
      return null;
  }
}

/**
 * Triggers a real-time FCM push notification to the customer whenever their order
 * status transitions (Confirmed, Preparing, Ready, OutForDelivery/Dispatched, Delivered, Rejected).
 */
exports.notifyOrderReadyForPickup = onDocumentUpdated(
  {
    document: "orders/{orderId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();
    if (!beforeData || !afterData) return;

    const previousStatus = String(beforeData.status || "");
    const nextStatus = String(afterData.status || "");
    if (!nextStatus || previousStatus === nextStatus) {
      return;
    }

    const orderId = event.params.orderId;
    const userId = String(afterData.userId || "");
    if (!userId) {
      logger.warn("Order missing userId; skipping customer FCM dispatch", { orderId });
      return;
    }

    const orderType = String(afterData.orderType || "DELIVERY").toUpperCase();
    const isPickup = orderType === "PICKUP";
    const branchId = String(afterData.branchId || "");
    const branchName = String(afterData.branchName || "Lahori Food Cafe");
    const itemsSummary = String(afterData.itemsSummary || "Your order");
    const rejectionReason = String(afterData.rejectionReason || "").trim();
    const shortId = orderId.slice(-6).toUpperCase();

    const copy = buildCustomerStatusCopy(nextStatus, isPickup, shortId, branchName, rejectionReason);
    if (!copy) return;

    const db = getFirestore(DATABASE_ID);
    const tokenSnapshot = await db
      .collection("users")
      .doc(userId)
      .collection("fcm_tokens")
      .get();

    if (tokenSnapshot.empty) {
      logger.info("No registered FCM tokens for customer", { userId, orderId, nextStatus });
      return;
    }

    const tokenDocs = tokenSnapshot.docs;
    const tokens = tokenDocs
      .map((doc) => doc.get("token"))
      .filter((t) => typeof t === "string" && t.length >= 10);

    if (tokens.length === 0) return;

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: copy.title,
        body: copy.body,
      },
      data: {
        eventType: "ORDER_STATUS",
        orderId,
        branchId,
        previousStatus,
        status: nextStatus,
        orderType,
        branchName,
        itemsSummary,
        rejectionReason,
        title: copy.title,
        body: copy.body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "lahori_order_updates",
          color: "#E4002B",
          sound: "default",
          tag: `order_status_${orderId}_${nextStatus.toLowerCase()}`,
        },
      },
    });

    const cleanupPromises = [];
    response.responses.forEach((sendResult, index) => {
      if (!sendResult.success) {
        const errorCode = sendResult.error?.code;
        if (
          errorCode === "messaging/registration-token-not-registered" ||
          errorCode === "messaging/invalid-registration-token"
        ) {
          cleanupPromises.push(tokenDocs[index].ref.delete());
        }
      }
    });
    if (cleanupPromises.length > 0) {
      await Promise.allSettled(cleanupPromises);
    }

    logger.info("Sent customer FCM order status notification", {
      orderId,
      userId,
      previousStatus,
      nextStatus,
      isPickup,
      successCount: response.successCount,
      failureCount: response.failureCount,
    });
  }
);

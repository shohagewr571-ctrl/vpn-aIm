/**
 * Firebase Cloud Function: validateAccessKey
 *
 * Deploy with Firebase CLI after setting up the project.
 * Keys are stored hashed; never return raw key material.
 *
 * Firestore collection "accessKeys":
 * {
 *   keyHash: "<sha256 of key>",
 *   active: true,
 *   expiresAt: Timestamp | null,
 *   revokedAt: Timestamp | null,
 *   createdAt: Timestamp
 * }
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

exports.validateAccessKey = functions.https.onCall(async (data, context) => {
  const key = (data && data.key ? String(data.key) : "").trim();
  if (!key) {
    return { valid: false, message: "Access key cannot be empty" };
  }

  // Simple rate-limit by IP (best-effort)
  const ip = context.rawRequest && context.rawRequest.ip
    ? context.rawRequest.ip
    : "unknown";
  const rateRef = db.collection("rateLimits").doc(ip);
  const rateSnap = await rateRef.get();
  const now = Date.now();
  if (rateSnap.exists) {
    const last = rateSnap.data().last || 0;
    const count = rateSnap.data().count || 0;
    if (now - last < 60000 && count >= 10) {
      return { valid: false, message: "Too many attempts. Try later." };
    }
    await rateRef.set({
      last: now,
      count: now - last < 60000 ? count + 1 : 1
    });
  } else {
    await rateRef.set({ last: now, count: 1 });
  }

  const hash = crypto.createHash("sha256").update(key).digest("hex");
  const snap = await db.collection("accessKeys")
    .where("keyHash", "==", hash)
    .limit(1)
    .get();

  if (snap.empty) {
    return { valid: false, message: "Invalid Access Key" };
  }

  const doc = snap.docs[0];
  const d = doc.data();
  if (!d.active) {
    return { valid: false, message: "Invalid Access Key" };
  }
  if (d.revokedAt) {
    return { valid: false, message: "Invalid Access Key" };
  }
  if (d.expiresAt && d.expiresAt.toMillis() < now) {
    return { valid: false, message: "Invalid Access Key" };
  }

  return {
    valid: true,
    message: "OK",
    keyId: doc.id
  };
});

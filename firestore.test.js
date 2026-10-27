const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validItemPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    name: "MacBook Pro 16",
    weightGrams: 2150,
    source: "AI_SCANNED",
    category: "COMPUTE",
    isShiftedToPersonal: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or write baggage_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("baggage_items").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("baggage_items")
      .doc("item_1")
      .set(validItemPayload(ALICE_UID))
  );
});

test("Authenticated user: can create, read, list, update, and delete own baggage item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("baggage_items")
    .doc("item_1");

  const payload = validItemPayload(ALICE_UID);
  await assertSucceeds(docRef.set(payload));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("baggage_items").get()
  );
  await assertSucceeds(
    docRef.update({
      weightGrams: 2300,
      isShiftedToPersonal: true,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Authenticated user: cannot read, list, or write another user's baggage items", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("baggage_items")
      .doc("bob_item")
      .set(validItemPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("baggage_items")
      .doc("bob_item")
      .get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("baggage_items").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("baggage_items")
      .doc("spoof_item")
      .set(validItemPayload(ALICE_UID))
  );
});

test("Schema validation: rejects ghost fields, invalid weightGrams, and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("baggage_items");

  // Ghost field injection
  await assertFails(
    colRef.doc("bad_ghost").set(validItemPayload(ALICE_UID, { isAdmin: true }))
  );

  // Invalid weightGrams
  await assertFails(
    colRef.doc("bad_weight").set(validItemPayload(ALICE_UID, { weightGrams: -10 }))
  );

  // Invalid enum
  await assertFails(
    colRef.doc("bad_enum").set(validItemPayload(ALICE_UID, { source: "UNKNOWN" }))
  );
});

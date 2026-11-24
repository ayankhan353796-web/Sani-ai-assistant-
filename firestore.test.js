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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Ayan",
    bio: "Building with SANI",
    youtubeLink: "https://youtube.com",
    instagramLink: "https://instagram.com",
    songLink: "https://open.spotify.com",
    selectedThemeId: "solar_flare",
    selectedAvatarId: "original_sani",
    selectedOrbId: "orb_cyber_purple",
    codeColorId: "cyan",
    languageCode: "hi-Latn",
    emotionStyle: "Friendly",
    emotionLevel: 3,
    keepChatting: false,
    heySaniEnabled: false,
    voiceName: "default",
    voiceSensitivity: 5,
    micStyleId: "neon_ring",
    premiumTier: "free",
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write user profile or conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get());
});

test("Authenticated owner can create, read, and partially update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      selectedThemeId: "midnight_gold",
      emotionStyle: "Professional",
      emotionLevel: 4,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field & invalid enum & createdAt tampering
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    profileRef.update({
      languageCode: "invalid_lang",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user access is strictly denied across profile, memories, conversations, and messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      content: "User prefers Hinglish coding tips",
      category: "Preference",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_2").set({
      userId: BOB_UID,
      content: "Injected",
      category: "Hack",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner can create conversation and messages, and list own subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const convRef = aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1");

  // Message without parent conversation must fail
  await assertFails(
    convRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello SANI",
      hasAttachment: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Create conversation then message
  await assertSucceeds(
    convRef.set({
      userId: ALICE_UID,
      title: "Morning Greeting",
      lastMessagePreview: "Hello SANI",
      languageCode: "hi-Latn",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    convRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello SANI",
      hasAttachment: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("conversations").get());
  await assertSucceeds(convRef.collection("messages").get());

  // Future timestamp must fail
  await assertFails(
    convRef.collection("messages").doc("msg_future").set({
      userId: ALICE_UID,
      role: "user",
      text: "Tampered",
      hasAttachment: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

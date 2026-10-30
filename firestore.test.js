const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";
const TEST_VIN = "1HGCR2F83HA123456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Public user: can read vehicles and events (public reports)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("vehicles").doc(TEST_VIN).set({
      vin: TEST_VIN,
      make: "Honda",
      model: "Accord",
      year: 2017,
      engine: "2.4L I4",
      hasLowConfidence: false,
      createdBy: ALICE_UID,
      createdAt: new Date(),
    });
    await db.collection("vehicles").doc(TEST_VIN).collection("events").doc("event_1").set({
      id: "event_1",
      vin: TEST_VIN,
      eventType: "service",
      title: "Oil Change",
      date: "2024-01-15",
      odometerKm: 45000,
      verificationLevel: "verified",
      prevHash: "0000000000000000000000000000000000000000000000000000000000000000",
      eventHash: "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
      createdBy: ALICE_UID,
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("vehicles").doc(TEST_VIN).get());
  await assertSucceeds(unauthDb.collection("vehicles").doc(TEST_VIN).collection("events").get());
});

test("Unauthenticated user: cannot create vehicle or event", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("vehicles").doc(TEST_VIN).set({
    vin: TEST_VIN,
    make: "Toyota",
    model: "Corolla",
    year: 2020,
    engine: "1.8L",
    hasLowConfidence: false,
    createdBy: "someone",
    createdAt: new Date(),
  }));
});

test("Authenticated user: can create vehicle and event with valid data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("vehicles").doc(TEST_VIN).set({
    vin: TEST_VIN,
    make: "Honda",
    model: "Civic",
    year: 2021,
    engine: "2.0L",
    hasLowConfidence: false,
    createdBy: ALICE_UID,
    createdAt: new Date(),
  }));

  await assertSucceeds(aliceDb.collection("vehicles").doc(TEST_VIN).collection("events").doc("evt_101").set({
    id: "evt_101",
    vin: TEST_VIN,
    eventType: "odometer",
    title: "Routine Check",
    date: "2024-05-10",
    odometerKm: 52000,
    verificationLevel: "verified",
    prevHash: "0000000000000000000000000000000000000000000000000000000000000000",
    eventHash: "c0ffeec0ffeec0ffeec0ffeec0ffeec0ffeec0ffeec0ffeec0ffeec0ffeec0ff",
    createdBy: ALICE_UID,
    createdAt: new Date(),
  }));
});

test("Strict Immutability: Nobody can edit or delete an existing event", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("vehicles").doc(TEST_VIN).set({
      vin: TEST_VIN,
      make: "Honda",
      model: "Civic",
      year: 2021,
      engine: "2.0L",
      hasLowConfidence: false,
      createdBy: ALICE_UID,
      createdAt: new Date(),
    });
    await db.collection("vehicles").doc(TEST_VIN).collection("events").doc("evt_immutable").set({
      id: "evt_immutable",
      vin: TEST_VIN,
      eventType: "repair",
      title: "Brake Pads",
      date: "2024-06-01",
      odometerKm: 55000,
      verificationLevel: "dealer_certified",
      prevHash: "0000000000000000000000000000000000000000000000000000000000000000",
      eventHash: "beefbeefbeefbeefbeefbeefbeefbeefbeefbeefbeefbeefbeefbeefbeefbeef",
      createdBy: ALICE_UID,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Attempt to edit event
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("events").doc("evt_immutable").update({
    odometerKm: 30000,
  }));
  // Attempt to delete event
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("events").doc("evt_immutable").delete());

  // Attempt to delete vehicle
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).delete());
});

test("Inspection: Public read, authenticated create, immutable update/delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Create vehicle first
  await aliceDb.collection("vehicles").doc(TEST_VIN).set({
    vin: TEST_VIN,
    make: "Honda",
    model: "Civic",
    year: 2021,
    engine: "2.0L",
    hasLowConfidence: false,
    createdBy: ALICE_UID,
    createdAt: new Date(),
  });

  // Unauthenticated create fails
  await assertFails(unauthDb.collection("vehicles").doc(TEST_VIN).collection("inspections").doc("insp_1").set({
    id: "insp_1",
    vin: TEST_VIN,
    overallConditionScore: 85,
    createdBy: "anon",
    createdAt: new Date(),
  }));

  // Authenticated create succeeds
  await assertSucceeds(aliceDb.collection("vehicles").doc(TEST_VIN).collection("inspections").doc("insp_1").set({
    id: "insp_1",
    vin: TEST_VIN,
    overallConditionScore: 85,
    odometerKmFromDashboard: 45000,
    odometerConfidence: 0.95,
    panels: [
      { panel: "hood", condition: "good", findings: [], confidence: 0.9, note: "clean" }
    ],
    limitations: ["undercarriage not visible"],
    photoUrls: ["https://example.com/p1.jpg"],
    modelVersion: "gemini-3.8-flash",
    createdBy: ALICE_UID,
    createdAt: new Date(),
  }));

  // Public read succeeds
  await assertSucceeds(unauthDb.collection("vehicles").doc(TEST_VIN).collection("inspections").doc("insp_1").get());

  // Edit or delete fails
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("inspections").doc("insp_1").update({
    overallConditionScore: 90
  }));
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("inspections").doc("insp_1").delete());
});

test("PriceListings: Public read, admin write, rejects seller personal info", async () => {
  const adminDb = testEnv.authenticatedContext("admin_user", { email: "melloukabdou450@gmail.com" }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com" }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Non-admin cannot create priceListing
  await assertFails(aliceDb.collection("priceListings").doc("listing_1").set({
    id: "listing_1",
    make: "Renault",
    model: "Clio",
    year: 2019,
    km: 85000,
    price: 2100000,
    wilaya: "16 - Alger",
    fuel: "Essence",
    transmission: "Manuelle",
    source: "manual",
    createdBy: ALICE_UID,
    createdAt: new Date(),
  }));

  // Admin cannot store seller phone or name
  await assertFails(adminDb.collection("priceListings").doc("listing_1").set({
    id: "listing_1",
    make: "Renault",
    model: "Clio",
    year: 2019,
    km: 85000,
    price: 2100000,
    wilaya: "16 - Alger",
    fuel: "Essence",
    transmission: "Manuelle",
    source: "manual",
    sellerPhone: "0555123456",
    createdBy: "admin_user",
    createdAt: new Date(),
  }));

  // Admin can create valid priceListing
  await assertSucceeds(adminDb.collection("priceListings").doc("listing_1").set({
    id: "listing_1",
    make: "Renault",
    model: "Clio",
    year: 2019,
    km: 85000,
    price: 2100000,
    wilaya: "16 - Alger",
    fuel: "Essence",
    transmission: "Manuelle",
    source: "manual",
    createdBy: "admin_user",
    createdAt: new Date(),
  }));

  // Public can read priceListing
  await assertSucceeds(unauthDb.collection("priceListings").doc("listing_1").get());

  // Admin can delete priceListing
  await assertSucceeds(adminDb.collection("priceListings").doc("listing_1").delete());
});

test("Vehicle Estimates: Public read, authenticated create, immutable update/delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Create vehicle first
  await aliceDb.collection("vehicles").doc(TEST_VIN).set({
    vin: TEST_VIN,
    make: "Honda",
    model: "Civic",
    year: 2021,
    engine: "2.0L",
    hasLowConfidence: false,
    createdBy: ALICE_UID,
    createdAt: new Date(),
  });

  // Authenticated user can create estimate
  await assertSucceeds(aliceDb.collection("vehicles").doc(TEST_VIN).collection("estimates").doc("est_1").set({
    id: "est_1",
    vin: TEST_VIN,
    inputs: { askingPrice: 2200000, targetKm: 50000, targetYear: 2021 },
    outputs: { lowPrice: 2000000, fairPrice: 2150000, highPrice: 2300000, verdict: "عادل" },
    comparablesCount: 15,
    comparisonLevel: "strict",
    confidence: "medium",
    createdBy: ALICE_UID,
    createdAt: new Date(),
  }));

  // Public can read estimate
  await assertSucceeds(unauthDb.collection("vehicles").doc(TEST_VIN).collection("estimates").doc("est_1").get());

  // Edit or delete fails
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("estimates").doc("est_1").update({
    confidence: "high"
  }));
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("estimates").doc("est_1").delete());
});

test("Garage Accounts: Registration pending, admin approve, reject direct status escalation", async () => {
  const garageUid = "garage_auto_express";
  const garageDb = testEnv.authenticatedContext(garageUid).firestore();
  const adminDb = testEnv.authenticatedContext("admin_user", { email: "melloukabdou450@gmail.com" }).firestore();

  // Garage cannot register directly with status 'approved'
  await assertFails(garageDb.collection("garages").doc(garageUid).set({
    uid: garageUid,
    garageName: "Auto Express",
    wilaya: "16 - Alger",
    commune: "Bab Ezzouar",
    address: "Zone Industrielle Route 5",
    phone: "0550123456",
    status: "approved",
    createdAt: new Date(),
  }));

  // Garage registers with status 'pending'
  await assertSucceeds(garageDb.collection("garages").doc(garageUid).set({
    uid: garageUid,
    garageName: "Auto Express",
    wilaya: "16 - Alger",
    commune: "Bab Ezzouar",
    address: "Zone Industrielle Route 5",
    phone: "0550123456",
    status: "pending",
    createdAt: new Date(),
  }));

  // Admin approves garage
  await assertSucceeds(adminDb.collection("garages").doc(garageUid).update({
    status: "approved"
  }));
});

test("Garage Service Entry: Approved garage creates event, customer confirmation update works", async () => {
  const approvedGarageUid = "garage_certified_1";
  const adminDb = testEnv.authenticatedContext("admin_user", { email: "melloukabdou450@gmail.com" }).firestore();
  const garageDb = testEnv.authenticatedContext(approvedGarageUid).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Register and approve garage via security rules disabled
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("garages").doc(approvedGarageUid).set({
      uid: approvedGarageUid,
      garageName: "Garage Moderne",
      wilaya: "16 - Alger",
      commune: "Hydra",
      address: "12 Rue des Pins",
      phone: "0550998877",
      status: "approved",
      createdAt: new Date(),
    });

    await db.collection("vehicles").doc(TEST_VIN).set({
      vin: TEST_VIN,
      make: "Honda",
      model: "Civic",
      year: 2021,
      engine: "2.0L",
      hasLowConfidence: false,
      createdBy: ALICE_UID,
      createdAt: new Date(),
    });
  });

  const eventId = "evt_garage_001";
  const confirmationHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  // Rejects plain customer phone
  await assertFails(garageDb.collection("vehicles").doc(TEST_VIN).collection("events").doc(eventId).set({
    id: eventId,
    vin: TEST_VIN,
    eventType: "service",
    title: "Oil Change + Filters",
    date: "2024-07-01",
    odometerKm: 60000,
    verificationLevel: "GARAGE_DECLARED",
    sourceType: "GARAGE",
    garageId: approvedGarageUid,
    customerPhone: "0555123456", // FORBIDDEN!
    confirmationCodeHash: confirmationHash,
    confirmationStatus: "PENDING",
    prevHash: "0000000000000000000000000000000000000000000000000000000000000000",
    eventHash: "1122334455667788112233445566778811223344556677881122334455667788",
    createdBy: approvedGarageUid,
    createdAt: new Date(),
  }));

  // Valid garage event creation with hashed phone only
  await assertSucceeds(garageDb.collection("vehicles").doc(TEST_VIN).collection("events").doc(eventId).set({
    id: eventId,
    vin: TEST_VIN,
    eventType: "service",
    title: "Oil Change + Filters",
    date: "2024-07-01",
    odometerKm: 60000,
    verificationLevel: "GARAGE_DECLARED",
    sourceType: "GARAGE",
    garageId: approvedGarageUid,
    customerPhoneHash: "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
    confirmationCodeHash: confirmationHash,
    confirmationStatus: "PENDING",
    prevHash: "0000000000000000000000000000000000000000000000000000000000000000",
    eventHash: "1122334455667788112233445566778811223344556677881122334455667788",
    createdBy: approvedGarageUid,
    createdAt: new Date(),
  }));

  // Customer confirmation update succeeds
  await assertSucceeds(aliceDb.collection("vehicles").doc(TEST_VIN).collection("events").doc(eventId).update({
    verificationLevel: "VERIFIED_BY_PARTNER + CUSTOMER_CONFIRMED",
    confirmationStatus: "CONFIRMED",
    confirmedAt: new Date()
  }));

  // Attempting to alter core mileage or delete fails
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("events").doc(eventId).update({
    odometerKm: 40000
  }));
  await assertFails(aliceDb.collection("vehicles").doc(TEST_VIN).collection("events").doc(eventId).delete());
});




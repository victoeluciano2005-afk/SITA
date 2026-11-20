const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_horta_123";
const BOB_UID = "bob_horta_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validFavoritePayload(uid, timestamp = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    hortalica: "Alface Crespa",
    estado: "SP",
    cidade: "Campinas",
    mesPlantio: "Abril",
    localCultivo: "Vaso",
    adequacao: "Ideal",
    vegetacaoBioma: "Mata Atlântica / Transição Cerrado",
    soloTipo: "Solo leve, fértil e bem drenado com matéria orgânica",
    soloPh: "6,0 a 6,8",
    soloAdubacao: "Húmus de minhoca e composto orgânico a cada 15 dias",
    tempMin: "15°C",
    tempMax: "24°C",
    luz: "4 a 6 horas de sol direto por dia",
    melhoresMeses: "Março a Outubro",
    toleranciaClima: "Prefere clima ameno; evitar calor extremo",
    alternativas: "Rúcula ou cebolinha em meses mais quentes",
    irrigacaoInicio: "2x ao dia com borrifador mantendo substrato úmido",
    irrigacaoMeio: "1x ao dia pela manhã, cerca de 300 ml por vaso",
    irrigacaoFim: "1x ao dia sem molhar excessivamente as folhas",
    irrigacaoHorario: "Início da manhã (06h-08h) ou fim de tarde",
    cicloInicioDias: 15,
    cicloInicioCuidados: "Germinação em sementeira e transplante com 4 folhas",
    cicloMeioDias: 25,
    cicloMeioCuidados: "Adubação de cobertura e controle de pulgões",
    cicloFimDias: 15,
    cicloFimCuidados: "Colher folhas externas ou pé inteiro pela manhã",
    totalDias: 55,
    espacamento: "25 cm x 25 cm entre plantas",
    consorcio: "Companheiras: cenoura, cebolinha. Evitar: salsa em excesso.",
    pragas: "Pulgões e lesmas; usar calda de sabão de coco neutro.",
    dicas: "Estimativa baseada em práticas Embrapa; consulte a EMATER local.",
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

function validGardenPlantPayload(uid, timestamp = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    hortalica: "Tomate Cereja",
    localCultivo: "Canteiro",
    cidadeEstado: "Belo Horizonte - MG",
    adequacao: "Ideal",
    wateringIntervalDays: 1,
    wateringScheduleLabel: "1x ao dia pela manhã (500 ml na base)",
    reminderEnabled: true,
    reminderTime: "07:30",
    plantedAtEpochMillis: Date.now() - 86400000,
    lastWateredEpochMillis: 0,
    wateredCount: 0,
    cicloInicioDias: 20,
    cicloMeioDias: 40,
    totalDias: 90,
    status: "ACTIVE",
    notes: "Plantado com composto orgânico no canteiro norte.",
    createdAt: timestamp,
    updatedAt: timestamp,
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

// --- UNAUTHENTICATED ACCESS DENIAL ---

test("Unauthenticated user cannot read or write favorites or garden_plants", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("fav_1")
      .set(validFavoritePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garden_plants")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garden_plants")
      .doc("plant_1")
      .set(validGardenPlantPayload(ALICE_UID))
  );
});

// --- FAVORITES COLLECTION PARITY & SECURITY ---

test("Owner can create, list, get, and delete favorites; update and ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const favCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("favorites");

  // Empty list query succeeds
  await assertSucceeds(favCol.get());

  // Valid create (matches Kotlin FavoriteGuide map without @DocumentId 'id')
  await assertSucceeds(
    favCol.doc("fav_alface_1").set(validFavoritePayload(ALICE_UID))
  );

  // Get and list succeed
  await assertSucceeds(favCol.doc("fav_alface_1").get());
  await assertSucceeds(favCol.get());

  // Update is forbidden on immutable favorites
  await assertFails(
    favCol.doc("fav_alface_1").update({
      hortalica: "Alface Americana",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field / @DocumentId 'id' rejected on create
  await assertFails(
    favCol.doc("fav_ghost").set({
      ...validFavoritePayload(ALICE_UID),
      id: "fav_ghost",
    })
  );

  // Future timestamp rejected
  await assertFails(
    favCol
      .doc("fav_future")
      .set(validFavoritePayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // Owner can delete
  await assertSucceeds(favCol.doc("fav_alface_1").delete());
});

// --- GARDEN PLANTS COLLECTION PARITY, ACTIONS & TERMINAL STATE LOCKING ---

test("Owner can create, water, configure reminders, harvest, and delete garden_plants; terminal state locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const plantsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("garden_plants");

  const created = new Date(Date.now() - 10000);

  // Empty list succeeds
  await assertSucceeds(plantsCol.get());

  // Valid create
  await assertSucceeds(
    plantsCol
      .doc("plant_tomate_1")
      .set(validGardenPlantPayload(ALICE_UID, created))
  );

  // Action 1: Log watering partial update succeeds
  await assertSucceeds(
    plantsCol.doc("plant_tomate_1").update({
      lastWateredEpochMillis: Date.now() - 500,
      wateredCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action 2: Update reminder & care settings succeeds
  await assertSucceeds(
    plantsCol.doc("plant_tomate_1").update({
      wateringIntervalDays: 2,
      wateringScheduleLabel: "A cada 2 dias no fim da tarde",
      reminderEnabled: false,
      reminderTime: "17:30",
      notes: "Estaqueamento concluído.",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    plantsCol.doc("plant_tomate_1").update({
      createdAt: new Date(Date.now() - 300),
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject ghost field injection
  await assertFails(
    plantsCol.doc("plant_tomate_1").update({
      notes: "ok",
      isAdmin: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Action 3: Mark HARVESTED succeeds
  await assertSucceeds(
    plantsCol.doc("plant_tomate_1").update({
      status: "HARVESTED",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Terminal State Locking: further updates after HARVESTED must fail
  await assertFails(
    plantsCol.doc("plant_tomate_1").update({
      lastWateredEpochMillis: Date.now() - 100,
      wateredCount: 2,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    plantsCol.doc("plant_tomate_1").update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Owner can still delete harvested plant
  await assertSucceeds(plantsCol.doc("plant_tomate_1").delete());
});

// --- CROSS-USER ISOLATION ---

test("Cross-user access to favorites and garden_plants is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("favorites")
      .doc("bob_fav")
      .set(validFavoritePayload(BOB_UID));
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("garden_plants")
      .doc("bob_plant")
      .set(validGardenPlantPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("favorites").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("favorites")
      .doc("bob_fav")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("favorites")
      .doc("bob_fav")
      .delete()
  );

  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("garden_plants").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("garden_plants")
      .doc("bob_plant")
      .update({
        wateredCount: 1,
        lastWateredEpochMillis: Date.now() - 100,
        updatedAt: new Date(Date.now() - 100),
      })
  );
});

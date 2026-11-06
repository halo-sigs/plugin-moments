import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import ts from "typescript";
import { computed, ref } from "vue";

const source = readFileSync(
  new URL("../src/utils/emotional-attachment.ts", import.meta.url),
  "utf8"
);
function loadUtils(bindings = {}) {
  const js = ts.transpile(source, {
    target: ts.ScriptTarget.ES2022,
    module: ts.ModuleKind.CommonJS,
  });
  const exports = {};
  new Function("exports", ...Object.keys(bindings), js)(exports, ...Object.values(bindings));
  return exports;
}

test("optional emotion stays compatible with old moments and never stores coordinates", () => {
  const { normalizeEmotion } = loadUtils();
  assert.equal(normalizeEmotion(), undefined);
  assert.equal(normalizeEmotion({ mood: " ", weatherSource: "open-meteo" }), undefined);
  assert.deepEqual(normalizeEmotion({ mood: " 😌 平静 ", location: " 杭州市 ", latitude: 30.2 }), {
    mood: "😌 平静",
    location: "杭州市",
  });
  assert.deepEqual(
    normalizeEmotion({ mood: "😊 开心", weather: " ", weatherSource: "open-meteo" }),
    {
      mood: "😊 开心",
    }
  );
});

test("weather handles zero, subzero and uncommon WMO codes, rejecting invalid responses", async () => {
  let payload;
  const { fetchWeather } = loadUtils({
    fetch: async (url, options) => {
      assert.equal(url.hostname, "api.open-meteo.com");
      assert.equal(url.searchParams.get("temperature_unit"), "celsius");
      assert.equal(options.credentials, "omit");
      return { ok: true, json: async () => payload };
    },
  });
  const coords = { latitude: 30, longitude: 120 };
  const signal = new AbortController().signal;
  for (const [code, temp, expected] of [
    [0, 0, "晴 · 0°C"],
    [75, -5.2, "大雪 · -5°C"],
    [97, 18, "强雷雨 · 18°C"],
  ]) {
    payload = { current: { temperature_2m: temp, weather_code: code } };
    assert.equal(await fetchWeather(coords, signal), expected);
  }
  for (const current of [
    {},
    { temperature_2m: null, weather_code: 0 },
    { temperature_2m: 20, weather_code: 999 },
  ]) {
    payload = { current };
    await assert.rejects(fetchWeather(coords, signal));
  }
});

test("location prefers the city and falls back to a locality without persisting coordinates", async () => {
  let payload = { city: "杭州市", locality: "西湖区", latitude: 30, longitude: 120 };
  const { fetchLocation } = loadUtils({
    fetch: async (url) => {
      assert.equal(url.hostname, "api.bigdatacloud.net");
      assert.equal(url.searchParams.get("localityLanguage"), "zh");
      return { ok: true, json: async () => payload };
    },
  });
  const signal = new AbortController().signal;
  assert.equal(await fetchLocation({ latitude: 30, longitude: 120 }, signal), "杭州市");
  payload = { city: "", locality: "西湖区" };
  assert.equal(await fetchLocation({ latitude: 30, longitude: 120 }, signal), "西湖区");
  payload = {};
  await assert.rejects(fetchLocation({ latitude: 30, longitude: 120 }, signal));
});

test("geolocation has actionable errors and is only requested when invoked", async () => {
  let requests = 0;
  const bindings = {
    window: { isSecureContext: true },
    navigator: {
      geolocation: {
        getCurrentPosition: (_resolve, reject, options) => {
          requests++;
          assert.equal(options.timeout, 12000);
          reject({ code: 1 });
        },
      },
    },
  };
  const { getCurrentPosition } = loadUtils(bindings);
  assert.equal(requests, 0);
  await assert.rejects(getCurrentPosition(), /定位权限/);
  assert.equal(requests, 1);
  bindings.window.isSecureContext = false;
  await assert.rejects(getCurrentPosition(), /HTTPS/);
  assert.equal(requests, 1);
});

function loadSubmitHandler(file, bindings) {
  const script = readFileSync(new URL(file, import.meta.url), "utf8").match(
    /<script[^>]*>([\s\S]*?)<\/script>/
  )[1];
  const ast = ts.createSourceFile(file, script, ts.ScriptTarget.Latest, true);
  const handler = ast.statements.find((node) =>
    node.getText(ast).startsWith("const handlerCreateOrUpdateMoment =")
  );
  const js = ts.transpile(handler.getText(ast), { target: ts.ScriptTarget.ES2022 });
  return new Function(...Object.keys(bindings), `${js}; return handlerCreateOrUpdateMoment;`)(
    ...Object.values(bindings)
  );
}

for (const file of ["../src/components/MomentEdit.vue", "../src/uc/MomentEdit.vue"]) {
  test(`${file}: publishing waits for success and retains emotion on failure`, async () => {
    for (const fails of [false, true]) {
      let finish;
      let resets = 0;
      let saves = 0;
      const pending = new Promise((resolve, reject) => {
        finish = fails ? reject : resolve;
      });
      const saving = ref(false);
      const formState = ref({
        spec: { emotionalAttachment: { mood: "😌 平静", weather: "晴 · 22°C" } },
      });
      const submit = loadSubmitHandler(file, {
        saving,
        formState,
        saveDisable: computed(() => saving.value),
        isUpdateMode: ref(false),
        queryEditorTags() {},
        normalizeEmotion: loadUtils().normalizeEmotion,
        handleSave: () => {
          saves++;
          return pending;
        },
        handleReset: () => {
          resets++;
          formState.value = { spec: {} };
        },
        console: { error() {} },
      });
      const request = submit();
      assert.equal(saving.value, true);
      assert.equal(formState.value.spec.emotionalAttachment.mood, "😌 平静");
      assert.equal(resets, 0);
      await submit();
      assert.equal(saves, 1);
      finish(fails ? new Error("offline") : undefined);
      await request;
      assert.equal(saving.value, false);
      assert.equal(resets, fails ? 0 : 1);
      if (fails) assert.equal(formState.value.spec.emotionalAttachment.weather, "晴 · 22°C");
    }
  });
}

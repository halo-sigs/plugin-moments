import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import ts from "typescript";
import { computed, ref } from "vue";

const source = readFileSync(
  new URL("../src/components/FollowUpEditor.vue", import.meta.url),
  "utf8"
).match(/<script[^>]*>([\s\S]*?)<\/script>/)[1];
const ast = ts.createSourceFile("FollowUpEditor.vue", source, ts.ScriptTarget.Latest, true);
const saveCode = ast.statements
  .find((node) => node.getText(ast).startsWith("async function save("))
  .getText(ast);

function setup(uc, request) {
  const content = ref("  现在已经放下了。  ");
  const saving = ref(false);
  const events = [];
  const props = {
    uc,
    moment: { metadata: { name: "one" }, spec: { content: { raw: "original" } } },
  };
  const bindings = {
    content,
    saving,
    props,
    valid: computed(() => !!content.value.trim() && content.value.trim().length <= 1000),
    momentsConsoleApiClient: {
      moment: { appendMomentFollowUp: (params) => request(params, false) },
    },
    momentsUcApiClient: { moment: { appendMyMomentFollowUp: (params) => request(params, true) } },
    emit: (...args) => events.push(args),
    Toast: { success() {} },
    console: { error() {} },
  };
  const js = ts.transpile(saveCode, { target: ts.ScriptTarget.ES2022 });
  const save = new Function(...Object.keys(bindings), `${js}; return save;`)(
    ...Object.values(bindings)
  );
  return { save, content, saving, events, props };
}

for (const uc of [false, true]) {
  test(`${uc ? "UC" : "Console"}: append blocks duplicate submission and preserves the original`, async () => {
    let complete;
    let calls = 0;
    const pending = new Promise((resolve) => {
      complete = resolve;
    });
    const editor = setup(uc, (params, endpointUc) => {
      calls++;
      assert.equal(endpointUc, uc);
      assert.deepEqual(params, { name: "one", followUpRequest: { content: "现在已经放下了。" } });
      return pending;
    });
    const savedMoment = {
      spec: { followUps: [{ id: "new", content: "现在已经放下了。" }], approved: true },
    };
    const submission = editor.save();
    await editor.save();
    assert.equal(calls, 1);
    assert.equal(editor.saving.value, true);
    assert.deepEqual(editor.events, []);
    complete({ data: savedMoment });
    await submission;
    assert.equal(editor.saving.value, false);
    assert.deepEqual(editor.events, [["saved", savedMoment], ["close"]]);
    assert.equal(editor.props.moment.spec.content.raw, "original");
  });

  test(`${uc ? "UC" : "Console"}: failure retains draft for retry and emits no success`, async () => {
    let fails = true;
    const editor = setup(uc, async () => {
      if (fails) throw new Error("offline");
      return { data: { spec: { approved: true } } };
    });
    await editor.save();
    assert.equal(editor.content.value, "  现在已经放下了。  ");
    assert.equal(editor.saving.value, false);
    assert.deepEqual(editor.events, []);
    fails = false;
    await editor.save();
    assert.equal(editor.events[0][0], "saved");
  });
}

test("empty and oversized follow-ups never call the API", async () => {
  const editor = setup(false, () => {
    assert.fail("invalid text sent to API");
  });
  for (const text of ["", "   \n ", "x".repeat(1001)]) {
    editor.content.value = text;
    await editor.save();
  }
  assert.deepEqual(editor.events, []);
});

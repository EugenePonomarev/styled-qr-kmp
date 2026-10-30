import assert from "node:assert/strict";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import vm from "node:vm";

import {
  StyledQrOptions,
  StyledQrScanabilityOptions,
  analyzeStyledQrScanability,
  renderStyledQrSvg,
} from "../../build/docs-site/web/styled-qr-kmp-qrcode-web.mjs";

const html = await readFile(
  new URL("../../build/docs-site/playground.html", import.meta.url),
  "utf8",
);

const expectedModules = [
  "kotlin_org_jetbrains_kotlin_kotlin_dom_api_compat.mjs",
  "kotlin-kotlin-stdlib.mjs",
  "styled-qr-kmp-qrcode-core.mjs",
  "styled-qr-kmp-qrcode-web.mjs",
];

for (const fileName of expectedModules) {
  const assembled = await readFile(
    new URL(
      `../../qrcode-web/build/packages/js/${fileName}`,
      import.meta.url,
    ),
  );

  const published = await readFile(
    new URL(
      `../../build/docs-site/web/${fileName}`,
      import.meta.url,
    ),
  );

  assert.deepEqual(
    published,
    assembled,
    `Site module differs from assembled package: ${fileName}`,
  );
}

assert.ok(
  html.includes("./web/styled-qr-kmp-qrcode-web.mjs"),
  "Built playground does not reference the local Web module",
);

const scripts = [
  ...html.matchAll(/<script\s+type="module">([\s\S]*?)<\/script>/g),
];

assert.equal(scripts.length, 1, "Expected one module script");

const script = scripts[0][1].replace(
  /import\s*\{[\s\S]*?\}\s*from\s*["'][^"']+["']\s*;/,
  "",
);

assert.notEqual(script, scripts[0][1], "Could not remove the module import");

function element(initial = {}) {
  const listeners = new Map();

  return {
    value: "",
    checked: false,
    disabled: false,
    hidden: false,
    textContent: "",
    src: "",

    classList: {
      add() {},
      remove() {},
    },

    addEventListener(name, callback) {
      listeners.set(name, callback);
    },

    removeAttribute(name) {
      if (name === "src") {
        this.src = "";
      }
    },

    dispatch(name) {
      const callback = listeners.get(name);
      assert.ok(callback, `Missing ${name} listener`);
      callback();
    },

    ...initial,
  };
}

const elements = {
  content: element({ value: "https://example.com" }),
  theme: element({ value: "fintech" }),
  shape: element({ value: "" }),
  ecc: element({ value: "H" }),
  "code-platform": element({ value: "web" }),
  "custom-colors": element(),
  foreground: element({ value: "#0b3a82", disabled: true }),
  background: element({ value: "#ffffff", disabled: true }),
  preview: element({ hidden: true }),
  status: element(),
  "scanability-result": element(),
  download: element({ disabled: true }),
  "generated-code": element(),
  "platform-note": element(),
  "copy-code": element(),
  "copy-status": element(),
};

let forceAnalysisFailure = false;
const context = {
  StyledQrOptions,
  StyledQrScanabilityOptions,
  analyzeStyledQrScanability(...args) {
    if (forceAnalysisFailure) throw new Error("Simulated analysis failure");
    return analyzeStyledQrScanability(...args);
  },
  renderStyledQrSvg,

  document: {
    querySelector(selector) {
      assert.ok(selector.startsWith("#"), `Unexpected selector: ${selector}`);

      const found = elements[selector.slice(1)];
      assert.ok(found, `Unknown element: ${selector}`);
      return found;
    },
  },

  navigator: {
    clipboard: {
      async writeText() {},
    },
  },

  Blob,
  URL,
  setTimeout,
  console,
};

vm.runInNewContext(script, context, {
  filename: "playground-preview.html",
});

function previewSvg() {
  const prefix = "data:image/svg+xml;charset=utf-8,";
  assert.ok(elements.preview.src.startsWith(prefix), "Missing SVG preview");

  return decodeURIComponent(elements.preview.src.slice(prefix.length));
}

function expectedSvg() {
  const options = new StyledQrOptions();

  options.themeId = elements.theme.value;
  options.errorCorrection = elements.ecc.value;

  if (elements.shape.value !== "") {
    options.moduleShape = elements.shape.value;
  }

  if (elements["custom-colors"].checked) {
    options.foregroundHex = elements.foreground.value;
    options.backgroundHex = elements.background.value;
  }

  const result = renderStyledQrSvg(elements.content.value, options);

  assert.equal(result.error, null, result.error?.message);
  assert.ok(result.svg, "Expected a generated SVG");

  return result.svg;
}

// Initial render and Web example.
assert.equal(elements.preview.hidden, false);
assert.equal(elements.download.disabled, false);
assert.equal(previewSvg(), expectedSvg());
assert.match(elements["generated-code"].textContent, /renderStyledQrSvg/);
assert.match(elements["scanability-result"].textContent, /No errors found/);
assert.match(elements["scanability-result"].textContent, /Minimum contrast:/);
assert.match(elements["scanability-result"].textContent, /Pixels per module: unavailable/);

// A diagnostics failure must not break the real preview or download.
forceAnalysisFailure = true;
elements.content.dispatch("input");
assert.match(elements["scanability-result"].textContent, /analysis is unavailable/);
assert.equal(elements.preview.hidden, false);
assert.equal(elements.download.disabled, false);
forceAnalysisFailure = false;
elements.content.dispatch("input");
assert.match(elements["scanability-result"].textContent, /No errors found/);

// Theme, shape, ECC and custom colors must affect the real preview.
elements.theme.value = "aurora";
elements.theme.dispatch("change");

elements.shape.value = "circle";
elements.shape.dispatch("change");

elements.ecc.value = "Q";
elements.ecc.dispatch("change");

elements["custom-colors"].checked = true;
elements["custom-colors"].dispatch("change");

elements.foreground.value = "#123456";
elements.foreground.dispatch("input");

elements.background.value = "#ffffff";
elements.background.dispatch("input");

assert.equal(elements.foreground.disabled, false);
assert.equal(elements.background.disabled, false);
assert.equal(previewSvg(), expectedSvg());

const webCode = elements["generated-code"].textContent;
assert.ok(webCode.includes('options.themeId = "aurora"'));
assert.ok(webCode.includes('options.moduleShape = "circle"'));
assert.ok(webCode.includes('options.errorCorrection = "Q"'));
assert.ok(webCode.includes('options.foregroundHex = "#123456"'));

// Low contrast is a warning; it does not prevent SVG generation.
elements.theme.value = "minimal";
elements.theme.dispatch("change");
elements.foreground.value = "#eeeeee";
elements.foreground.dispatch("input");
assert.match(elements["scanability-result"].textContent, /LOW_CONTRAST/);
assert.equal(elements.preview.hidden, false);
assert.equal(elements.download.disabled, false);
elements.theme.value = "aurora";
elements.theme.dispatch("change");
elements.foreground.value = "#123456";
elements.foreground.dispatch("input");
assert.doesNotMatch(elements["scanability-result"].textContent, /LOW_CONTRAST/);

// Empty content must clear the preview and disable download.
elements.content.value = "";
elements.content.dispatch("input");

assert.equal(elements.preview.hidden, true);
assert.equal(elements.preview.src, "");
assert.equal(elements.download.disabled, true);
assert.match(elements["scanability-result"].textContent, /Enter content/);
assert.doesNotMatch(elements["scanability-result"].textContent, /Minimum contrast/);

// Switching platforms must still work when content is empty.
const platformCases = [
  ["android", "QrThemes.Aurora"],
  ["android-view", "StyledQrView"],
  ["compose", "StyledQrCode"],
  ["ios", "StyledQrImageRenderer"],
  ["web", "renderStyledQrSvg"],
];

for (const [platform, expected] of platformCases) {
  elements["code-platform"].value = platform;
  elements["code-platform"].dispatch("change");

  assert.ok(
    elements["generated-code"].textContent.includes(expected),
    `Missing ${expected} in ${platform} example`,
  );
}

// Check representative special characters in generated Kotlin.
elements.content.value = 'Цена $5, "QR", \\f\n😀';
elements["code-platform"].value = "compose";
elements.content.dispatch("input");

const kotlinCode = elements["generated-code"].textContent;

assert.ok(kotlinCode.includes("\\$5"), "Kotlin dollar escape is missing");
assert.ok(kotlinCode.includes('\\"QR\\"'), "Kotlin quote escape is missing");
assert.ok(kotlinCode.includes("\\\\f"), "Literal backslash was changed");
assert.ok(kotlinCode.includes("\\n"), "Kotlin newline escape is missing");
assert.ok(
  kotlinCode.includes("\\ud83d\\ude00"),
  "Kotlin supplementary character escapes are missing",
);

// Oversized input should produce an error and remove the previous QR.
elements.content.value = "x".repeat(4000);
elements.content.dispatch("input");

assert.equal(elements.preview.hidden, true);
assert.equal(elements.preview.src, "");
assert.equal(elements.download.disabled, true);
assert.ok(elements.status.textContent.includes("PayloadTooLong"));
assert.match(elements["scanability-result"].textContent, /PayloadTooLong/);
assert.doesNotMatch(elements["scanability-result"].textContent, /Minimum contrast/);

// A valid value should recover after the error.
elements.content.value = "https://example.com";
elements.content.dispatch("input");

assert.equal(elements.preview.hidden, false);
assert.equal(elements.download.disabled, false);
assert.equal(previewSvg(), expectedSvg());
assert.match(elements["scanability-result"].textContent, /No errors found/);

console.log("Playground preview and platform checks: PASS");

if (process.argv.includes("--emit-kotlin")) {
  // Generate a real example with non-default settings and characters
  // that require escaping in Kotlin.
  elements.content.value = 'Привет, $QR "example" \\path\n😀';
  elements.theme.value = "aurora";
  elements.shape.value = "circle";
  elements.ecc.value = "Q";

  elements["custom-colors"].checked = true;
  elements.foreground.value = "#123456";
  elements.background.value = "#ffffff";

  elements["code-platform"].value = "android";
  elements.content.dispatch("input");

  const generatedKotlin = elements["generated-code"].textContent;

  assert.ok(generatedKotlin.includes("QrThemes.Aurora"));
  assert.ok(generatedKotlin.includes("QrModuleShape.Circle"));
  assert.ok(generatedKotlin.includes("QrErrorCorrectionLevel.Q"));

  const fixtureDirectory = new URL(
    "../../tools/gallery-generator/build/generated/playground-tests/",
    import.meta.url,
  );

  await mkdir(fixtureDirectory, { recursive: true });

  const separator = generatedKotlin.indexOf("\n\n");

  assert.ok(separator > 0, "Generated Kotlin imports were not found");

  const imports = generatedKotlin.slice(0, separator);
  const body = generatedKotlin.slice(separator + 2);

  assert.ok(
    imports.startsWith("import "),
    "Generated Kotlin must start with imports",
  );

  const indentedBody = body
    .split("\n")
    .map((line) => line === "" ? "" : `        ${line}`)
    .join("\n");

  const fixture = [
    "package playground.generated",
    "",
    imports,
    "",
    "class GeneratedPlaygroundKotlinTest {",
    "    @kotlin.test.Test",
    "    fun generatedExampleProducesSvg() {",
    indentedBody,
    "",
    "        val actualSvg = kotlin.test.assertNotNull(svg)",
    '        kotlin.test.assertTrue(actualSvg.startsWith("<svg"))',
    "    }",
    "}",
    "",
  ].join("\n");

  await writeFile(
    new URL("GeneratedPlaygroundKotlinTest.kt", fixtureDirectory),
    fixture,
    "utf8",
  );
  elements["code-platform"].value = "compose";
  elements["code-platform"].dispatch("change");

  const generatedCompose = elements["generated-code"].textContent;

  assert.ok(
    generatedCompose.includes("fun QrExample()"),
    "Generated Compose function is missing",
  );
  assert.ok(
    generatedCompose.includes("QrThemes.Aurora"),
    "Generated Compose theme is missing",
  );
  assert.ok(
    generatedCompose.includes("QrModuleShape.Circle"),
    "Generated Compose shape is missing",
  );

  const composeFixtureDirectory = new URL(
    "../../qrcode-compose/build/generated/playground-tests/",
    import.meta.url,
  );

  await mkdir(composeFixtureDirectory, { recursive: true });

  await writeFile(
    new URL("GeneratedPlaygroundCompose.kt", composeFixtureDirectory),
    [
      "package playground.generated",
      "",
      generatedCompose,
      "",
    ].join("\n"),
    "utf8",
  );
  elements["code-platform"].value = "android-view";
  elements["code-platform"].dispatch("change");

  const generatedView = elements["generated-code"].textContent;
  const activityMarker = "// In your Activity, after setContentView(...):";
  const activityStart = generatedView.indexOf(activityMarker);

  assert.ok(activityStart > 0, "Generated Activity section is missing");

  const xml = generatedView.slice(0, activityStart).trim();
  const activityBody = generatedView.slice(activityStart).trim();

  assert.ok(
    xml.includes("io.github.eugeneponomarev.styledqr.android.StyledQrView"),
    "Generated XML view is missing",
  );
  assert.ok(
    activityBody.includes("qrView.setQrCode("),
    "Generated View configuration is missing",
  );

  const indentedActivityBody = activityBody
    .split("\n")
    .map((line) => line === "" ? "" : `        ${line}`)
    .join("\n");

  const xmlBase64 = Buffer.from(xml, "utf8").toString("base64");

  const viewFixtureDirectory = new URL(
    "../../qrcode-android-view/build/generated/playground-tests/",
    import.meta.url,
  );

  await mkdir(viewFixtureDirectory, { recursive: true });

  const viewFixture = [
    "package playground.generated",
    "",
    "import android.app.Activity",
    "import io.github.eugeneponomarev.styledqr.android.StyledQrView",
    "import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel",
    "import io.github.eugeneponomarev.styledqr.render.QrModuleShape",
    "import io.github.eugeneponomarev.styledqr.render.QrStyleConfiguration",
    "import io.github.eugeneponomarev.styledqr.theme.QrThemes",
    "import javax.xml.parsers.DocumentBuilderFactory",
    "import org.xml.sax.InputSource",
    "import java.io.StringReader",
    "",
    "// A test-only ID: the generated XML is checked separately below.",
    "private object R {",
    "    object id {",
    "        const val qrView: Int = 1",
    "    }",
    "}",
    "",
    "class GeneratedViewActivityFixture : Activity() {",
    "    fun configureGeneratedView() {",
    indentedActivityBody,
    "    }",
    "}",
    "",
    "class GeneratedViewXmlTest {",
    "    @kotlin.test.Test",
    "    fun generatedXmlIsWellFormed() {",
    `        val bytes = java.util.Base64.getDecoder().decode("${xmlBase64}")`,
    "        val xml = bytes.toString(Charsets.UTF_8)",
    "        val factory = DocumentBuilderFactory.newInstance().apply {",
    "            isNamespaceAware = true",
    '            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)',
    "        }",
    "        val document = factory.newDocumentBuilder().parse(",
    "            InputSource(StringReader(xml)),",
    "        )",
    "        kotlin.test.assertEquals(",
    '            "io.github.eugeneponomarev.styledqr.android.StyledQrView",',
    "            document.documentElement.tagName,",
    "        )",
    "        kotlin.test.assertEquals(",
    '            "@+id/qrView",',
    "            document.documentElement.getAttributeNS(",
    '                "http://schemas.android.com/apk/res/android",',
    '                "id",',
    "            ),",
    "        )",
    "    }",
    "}",
    "",
  ].join("\n");

  await writeFile(
    new URL("GeneratedPlaygroundViewTest.kt", viewFixtureDirectory),
    viewFixture,
    "utf8",
  );

  console.log("Generated Android View fixture: PASS");
  console.log("Generated Compose fixture: PASS");
elements["code-platform"].value = "ios";
elements["code-platform"].dispatch("change");

const generatedSwift = elements["generated-code"].textContent;

assert.ok(
  generatedSwift.includes("func makeQrImage()"),
  "Generated Swift function is missing",
);
assert.ok(
  generatedSwift.includes("QrStyleConfiguration.companion.fromStyle"),
  "Generated Swift theme configuration is missing",
);

const swiftFixtureDirectory = new URL(
  "../../tools/gallery-generator/build/generated/playground-swift/",
  import.meta.url,
);

await mkdir(swiftFixtureDirectory, { recursive: true });

await writeFile(
  new URL("GeneratedPlayground.swift", swiftFixtureDirectory),
  generatedSwift + "\n",
  "utf8",
);

console.log("Generated Swift fixture: PASS");
  console.log("Generated Kotlin fixture: PASS");
}
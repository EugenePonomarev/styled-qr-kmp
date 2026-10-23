import { execFileSync } from "node:child_process";
import {
  copyFileSync,
  existsSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../", import.meta.url));
const source = fileURLToPath(new URL("./", import.meta.url));
const assembled = join(root, "qrcode-web/build/packages/js");

let workspace;

function archivesIn(directory, expectedName) {
  const result = [];

  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === "node_modules") continue;

    const path = join(directory, entry.name);

    if (entry.isDirectory()) {
      result.push(...archivesIn(path, expectedName));
    } else if (entry.isFile() && entry.name === expectedName) {
      result.push(path);
    }
  }

  return result;
}

function runNode(args, cwd) {
  execFileSync(process.execPath, args, {
    cwd,
    stdio: "inherit",
  });
}

try {
  const npmCli = process.env.npm_execpath;

  if (!npmCli || !existsSync(npmCli)) {
    throw new Error(
      "Run this check with: npm --prefix tests/web-typescript test",
    );
  }

  const expected = JSON.parse(
    readFileSync(join(assembled, "package.json"), "utf8"),
  );

  const expectedName = "styled-qr-kmp-web-" + expected.version + ".tgz";

  const candidates = process.argv[2]
    ? [resolve(root, process.argv[2])]
    : archivesIn(join(root, "qrcode-web/build"), expectedName).sort(
        (a, b) => statSync(b).mtimeMs - statSync(a).mtimeMs,
      );

  const archive = candidates[0];

  if (!archive || !existsSync(archive)) {
    throw new Error(
      "Run :qrcode-web:packJsPackage first; npm archive was not found",
    );
  }

  workspace = mkdtempSync(join(tmpdir(), "styled-qr-ts-"));

  console.log("Checking archive: " + archive);
  console.log("Consumer workspace: " + workspace);

  copyFileSync(
    join(source, "consumer.mts"),
    join(workspace, "consumer.mts"),
  );
  copyFileSync(
    join(source, "tsconfig.json"),
    join(workspace, "tsconfig.json"),
  );
  copyFileSync(
    join(root, "tests/fixtures/qr-parity.json"),
    join(workspace, "qr-parity.json"),
  );

  writeFileSync(
    join(workspace, "package.json"),
    JSON.stringify(
      {
        name: "styled-qr-packed-consumer",
        private: true,
        type: "module",
      },
      null,
      2,
    ),
  );

  runNode(
    [
      npmCli,
      "install",
      "--ignore-scripts",
      "--no-audit",
      "--no-fund",
      "--save-exact",
      "typescript@5.9.3",
      archive,
    ],
    workspace,
  );

  const installed = join(
    workspace,
    "node_modules/styled-qr-kmp-web",
  );

  const actual = JSON.parse(
    readFileSync(join(installed, "package.json"), "utf8"),
  );

  for (const field of ["name", "version", "main", "types"]) {
    if (actual[field] !== expected[field]) {
      throw new Error(
        "Installed package metadata differs: " + field,
      );
    }
  }

  for (const entry of readdirSync(assembled, { withFileTypes: true })) {
    if (!entry.isFile()) continue;

    const name = entry.name;

    if (
      !name.endsWith(".mjs") &&
      !name.endsWith(".d.mts") &&
      !name.endsWith(".map")
    ) {
      continue;
    }

    const expectedBytes = readFileSync(join(assembled, name));
    const actualBytes = readFileSync(join(installed, name));

    if (!expectedBytes.equals(actualBytes)) {
      throw new Error(
        "Archive differs from the assembled package: " + name,
      );
    }
  }

  runNode(
    [
      join(workspace, "node_modules/typescript/bin/tsc"),
      "-p",
      "tsconfig.json",
    ],
    workspace,
  );

  runNode([join(workspace, "dist/consumer.mjs")], workspace);

  rmSync(workspace, { recursive: true, force: true });

  console.log("Packed TypeScript consumer: PASS");
} catch (error) {
  console.error(
    error instanceof Error ? error.message : String(error),
  );

  if (workspace) {
    console.error("Failed consumer preserved at: " + workspace);
  }

  process.exitCode = 1;
}
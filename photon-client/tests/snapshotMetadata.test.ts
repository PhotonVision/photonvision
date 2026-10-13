import assert from "node:assert/strict";
import test from "node:test";

import { getSnapshotMetadataFromName } from "../src/components/cameras/snapshotMetadata.ts";

void test("getSnapshotMetadataFromName parses camera nicknames with underscores", () => {
  const snapshotName = "Microsoft_LifeCam_HD-3000_output_2026-07-25T103348701_NONE-0-UNKNOWN";

  const metadata = getSnapshotMetadataFromName(snapshotName);

  assert.equal(metadata.cameraNickname, "Microsoft_LifeCam_HD-3000");
  assert.equal(metadata.streamType, "output");
  assert.equal(metadata.snapshotName, snapshotName);
  assert.equal(metadata.timeCreated.getFullYear(), 2026);
  assert.equal(metadata.timeCreated.getMonth(), 6);
  assert.equal(metadata.timeCreated.getDate(), 25);
  assert.equal(metadata.timeCreated.getHours(), 10);
  assert.equal(metadata.timeCreated.getMinutes(), 33);
  assert.equal(metadata.timeCreated.getSeconds(), 48);
  assert.equal(metadata.timeCreated.getMilliseconds(), 701);
});

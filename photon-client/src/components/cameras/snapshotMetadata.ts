export interface SnapshotMetadata {
  snapshotName: string;
  cameraNickname: string;
  streamType: "input" | "output";
  timeCreated: Date;
}

export const getSnapshotMetadataFromName = (snapshotName: string): SnapshotMetadata => {
  // Keep in sync with FileSaveFrameConsumer.java
  const normalizedSnapshotName = snapshotName.replace(/\.[^/.]+$/, "");

  const match = normalizedSnapshotName.match(
    /^(?<cameraNickname>.+)_(?<streamType>input|output)_(?<dateStr>\d{4}-\d{2}-\d{2}T\d{2}\d{2}\d{2}\d{1,3})_(?<matchData>.+)$/
  );

  const cameraNickname = match?.groups?.cameraNickname ?? normalizedSnapshotName;
  const streamType = (match?.groups?.streamType as "input" | "output" | undefined) ?? "input";
  const dateStr = match?.groups?.dateStr ?? "";

  const [datePart, timePart] = dateStr.split("T");
  const [year, month, day] = datePart.split("-").map((value) => parseInt(value, 10));
  const hours = parseInt(timePart.slice(0, 2), 10);
  const minutes = parseInt(timePart.slice(2, 4), 10);
  const seconds = parseInt(timePart.slice(4, 6), 10);
  const milliseconds = parseInt(timePart.slice(6), 10);

  return {
    snapshotName: normalizedSnapshotName,
    cameraNickname,
    streamType,
    timeCreated: new Date(year, month - 1, day, hours, minutes, seconds, milliseconds)
  };
};

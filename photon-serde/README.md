# Photon Serde Autocode

Like Rosmsg. But worse.

![](https://private-user-images.githubusercontent.com/29715865/350732914-ab8026ad-2861-49ad-b5b2-0fe7cf920d44.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MjIyMjY1NTIsIm5iZiI6MTcyMjIyNjI1MiwicGF0aCI6Ii8yOTcxNTg2NS8zNTA3MzI5MTQtYWI4MDI2YWQtMjg2MS00OWFkLWI1YjItMGZlN2NmOTIwZDQ0LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDA3MjklMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwNzI5VDA0MTA1MlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWI2YmQwZDQ3ZGQ3ODc5NWE0YTRhYTJkMmVmNmU4MTY2M2RiZTQ4NDIwNzQyMDdiOWJkZmMxNzQxNTgwYjE2MDYmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.dhfk3QkC04gIF_MKxFGKaYUNY__AmhB6wMHSZsQadZ4)

## Goals

- As fast as possible (only slightly slower than packed structs, ideally)
- Support for variable length arrays and optional types
- Allow deserialization into user-defined, possibly nested, types. See [ResultList](src/targeting/resultlist.h) for an example of this.

## Design

The code for a single type is split across 3 files. Let's look at PnpResult:
- [The struct definition](src/struct/pnpresult_struct.h): This is the data the object holds. Auto-generated. The data this object holds can be primitives or other, fully-deserialized types (like Vec2)
- [The user class](src/targeting/pnpresult_struct.h): This is the fully-deserialized PnpResult type. This contains extra functions users might need to expose like `Amgiguity`, or other computed helper things.
- [The serde interface](src/serde/pnpresult_struct.h): This is a template specialization for converting the user class to/from bytes

## Prior art

- Protobuf: slow on embedded platforms (at least quickbuf is)
- Wpi's struct: no VLAs/optionals
- Rosmsg: I'm not using ros, but I'm stealing their message hash idea

## Deviations from WPI's Struct Schema Typestrings

- Enum types are disallowed
- Bitfields and bit packing are disallowed
- Only variable length arrays are supported (no fixed-length arrays)
- Arrays must be no more than 127 elements long
- Members can be either VLAs or optional, but not both
- A top-level NT topic type shall be a single type (eg TargetCorner), and cannot an array of types (eg TargetCorner[] or TargetCorner[?])
- `float` and `double` types will be replaced with float32/float64 when generating message schema strings. This means that `float32 x;` and `float x;` will result in the same message hash.

For example, this is a valid PhotonStruct schema. Note the WPILib `Transform3d`, the Photon-defined `TargetCorner`, optional prefix, and VLA suffix.

```
float64 poseAmbiguity;
optional Transform3d altCameraToTarget;
TargetCorner:16f6ac0dedc8eaccb951f4895d9e18b6 minAreaRectCorners[?];
```

## Dynamic Decoding

Dynamic decoding is facilitated by publishing schemas to the `.schema` table in NT, and by encoding the `message_uuid` as a property on a `photonstruct` publisher. Schema names in the .schema table shall be formatted as `photonstruct:{Type Name}:{Message UUID}`. For example, here I've published Photon results to `/photonvision/WPI2024/rawBytes`. This topic has the typestring `photonstruct:PhotonPipelineResult:ed36092eb95e9fc254ebac897e2a74df`, with properties `{message_uuid': 'ed36092eb95e9fc254ebac897e2a74df'}`. It shall be legal to have published multiple versions of the same message, as long as their UUIDs are unique (which they'd better be).

| Topic Name | Type | Type String |
|------|------|-------|
| /.schema/photonstruct:PhotonPipelineResult:ed36092eb95e9fc254ebac897e2a74df   | kRaw |  photonstructschema |
| /.schema/photonstruct:PhotonTrackedTarget:4387ab389a8a78b7beb4492f145831b4    | kRaw |  photonstructschema |
| /.schema/photonstruct:TargetCorner:16f6ac0dedc8eaccb951f4895d9e18b6           | kRaw |  photonstructschema |
| /.schema/photonstruct:MultiTargetPNPResult:af2056aaab740eeb889a926071cae6ee   | kRaw |  photonstructschema |
| /.schema/photonstruct:PnpResult:ae4d655c0a3104d88df4f5db144c1e86              | kRaw |  photonstructschema |
| /.schema/photonstruct:PhotonPipelineMetadata:626e70461cbdb274fb43ead09c255f4e | kRaw |  photonstructschema |
| /.schema/proto:geometry3d.proto                                               | kRaw  |  proto:FileDescriptorProto |
| /.schema/proto:photon.proto    | kRaw | proto:FileDescriptorProto |

The struct definition for PhotonPipelineResult we retrieved from the struct schema database shown above (via the command `python.exe scripts/catnt.py --echo /.schema/photonstruct:PhotonPipelineResult:ed36092eb95e9fc254ebac897e2a74df`) is:

```
PhotonPipelineMetadata:626e70461cbdb274fb43ead09c255f4e metadata;
PhotonTrackedTarget:4387ab389a8a78b7beb4492f145831b4[?] targets;
MultiTargetPNPResult:af2056aaab740eeb889a926071cae6ee? multitagResult;
```

If we were decoding this, we'd go retrieve the struct definitions for all our nested types. For example, `PhotonTrackedTarget:4387ab389a8a78b7beb4492f145831b4` is defined by it's .schema table entry be the following. This type also demonstrates a mix of WPILib struct types (such as Transform3d), intrinsic types (such as float64), and Photon struct types (such as TargetCorner).

```
float64 yaw;
float64 pitch;
float64 area;
float64 skew;
int32 fiducialId;
int32 objDetectId;
float32 objDetectConf;
Transform3d bestCameraToTarget;
Transform3d altCameraToTarget;
float64 poseAmbiguity;
TargetCorner:16f6ac0dedc8eaccb951f4895d9e18b6[?] minAreaRectCorners;
TargetCorner:16f6ac0dedc8eaccb951f4895d9e18b6[?] detectedCorners;
```

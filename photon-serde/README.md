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
- [The user class](src/targeting/pnpresult_struct.h): This is the fully-deserialized PnpResult type. This contains extra functions users might need to expose like `Ambiguity`, or other computed helper things.
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

## Serialization Protocol

photon-serde works by decomposing every message into a few intrinsic types: these are `int8` `int16` `int32` `int64` `float32` `float64` `boolean`. These intrinsic types are essentially copied directly, with no special logic for ser/de.

Shimmed messages are a special case where we need to encode/decode a type that is not part of Photonvision (e.g. WPILib types like Transform3d, Rotation2d, etc.), so we send them to a custom shim that isn't automatically generated by photon-serde

Custom messages essentially serialize their fields next to each other in memory. Fields can either be an intrinsic type, in which it'll be copied directly, a shimmed type, in which case it'll be encoded with the corresponding shim, or a nested custom message, in which case the corresponding ser/de will be called, which will invoke the same rules on the custom type field. Any field can also be a Variable-length array, in which case the values will be stored contiguously in binary, and prepended with a length byte telling the ser/de the length of the array (and then normal ser/de rules apply to the array elements), or an Optional, in which case the value will be prepended with a boolean flag that signal whether or not the value is present. If the flag is set, the normal ser/de rules apply. Otherwise, the flag will be followed immediately by the next field or the end of the message, without any sort of sentinal or placeholder value. However, a field cannot be BOTH a vla and optional at the same time

Custom messages are defined in the `messages.yaml` file

### A Worked Example

As an example of how the serde protocol works, let's consider the following messages
```yaml
---
- name: foo
# Every message type has a unique name
  fields:
  - name: foo_foo
    type: int32
  - name: foo_bar
    type: int16
    vla: true
- name: bar
# A single yaml file can define multiple message types
  fields:
  - name: bar_foo
    type: int32
  - name: bar_bar
    type: foo
  - name: bar_baz
    type: foo
    vla: true
  - name: bar_qux
    type: foo
    optional: true
  - name: bar_quux
    type: Transform3d
    # This is an example of a shimmed type, corresponding to WPILib's Transform3d. The message definition is omitted here for brevity
```

`foo` would be encoded as
```
foo_foo (int32) | foo_bar size (int8) | foo_bar data (int16[])
```

and `bar` would be encoded as
```
bar_foo (int32) | bar_bar (foo) | bar_baz size (int8) | bar_baz data (foo[]) | bar_qux present (bool) | bar_qux data (foo?) | bar_quux (Transform3d)
```
> `bar_qux data` is skipped entirely if `bar_qux present` is `false`

Below is C-style pseudocode for a hypothetical ser/de generated from these messages (The actual implementations of photon-serde doesn't look exactly like this, but the base logic is the same)

```c
struct foo {
    int32_t foo_foo
    int16_t[size] foo_bar
}

void serialize_foo(packet& p, foo val){
    p.packInt32(val.foo_foo)

    p.packInt8(val.foo_bar size)
    for (ele in val.foo_bar) {
        p.packInt16(ele)
    }
}

// fields are serialized and deserialized in the same order
foo deserialize_foo(packet& p){
    foo out

    out.foo_foo p.unpackInt32()

    int8_t size = p.unpackInt8()
    out.foo_bar = int16_t[size]
    for (int i; i < size; i++) {
        out.foo_bar[i] = p.unpackInt16()
    }

    return foo
}
```

```c
struct bar {
    int32_t bar_foo
    foo bar_bar
    foo[size] bar_baz
    foo? bar_quz
    foo bar_quuz
}

void serialize_bar(packet& p, foo val){
    p.packInt32(val.bar_foo)
    serialize_foo(p,val.bar_bar)

    p.packInt8(val.bar_baz size)
    for (ele in val.bar_baz) {
        serialize_foo(p,ele)
    }

    p.packBool(val.bar_quz present)
    if (val.bar_quz present) {
        serialize_foo(p,val.bar_quz value)
    }

    Transform3d_encode_shim(p,val.bar_quuz)
}

foo deserialize_bar(packet& p){
    bar out

    out.bar_foo = p.unpackInt32()

    out.bar_bar = deserialize_foo(p)

    int8_t bar_baz size = p.unpackInt8()
    out.bar_baz = int16_t[bar_baz size]
    for (int i; i < bar_baz size; i++) {
        out.bar_baz[i] = deserialize_foo(p)
    }

    bool bar_quz present = p.unpackBool()
    if (bar_quz present) {
        out.bar_quz = deserialize_foo(p)
    } else {
        out.bar_quz = empty
    }

    out.bar_quuz = Transform3d_decode_shim(p)

    return foo
}
```

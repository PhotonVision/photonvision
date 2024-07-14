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
- [The serde interface](src/serde/pnpresult_struct.h): This is a template specilization for converting the user class to/from bytes

## Prior art

- Protobuf: slow on embedded platforms (at least quickbuf is)
- Wpi's struct: no VLAs/optionals
- Rosmsg: I'm not using ros, but I'm stealing their message hash idea

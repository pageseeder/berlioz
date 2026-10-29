# 11. Binary Serialization Module

**Status:** Exploring · **Target:** 1.1+

## Motivation

Berlioz's generator hierarchy (`XmlGenerator`, `JsonGenerator`, `RawGenerator`) already supports multiple output formats dispatched by URL extension. A future optional module could extend this to high-performance binary serialization formats, following the same pattern as the existing JSON adapters.

## Proposal

Recommended first candidate:

- **MessagePack**: schema-less and structurally similar to JSON, so the generator API can mirror `JsonGenerator` closely. A `pso-berlioz-msgpack` module would provide a `MessagePackGenerator` base class and wire the `*.msgpack` extension into servlet dispatch.

Other candidates to evaluate later:

- **Protocol Buffers**: requires pre-defined `.proto` schemas compiled to Java classes. Higher friction for users but strong ecosystem support and compact wire format.
- **Apache Avro**: schema-based with optional schema evolution. Natural fit for data interchange but heavier setup than MessagePack.
- **FlatBuffers**: zero-copy deserialization for performance-critical paths. Niche use case within a web framework.

Schema-based formats (Protobuf, Avro, FlatBuffers) would need a different generator contract where the user returns a pre-built typed message rather than writing fields dynamically, so they represent a larger API design decision and are deferred until the generator contract is clearer.

No core changes are expected: the existing extension-based dispatch and generator hierarchy should accommodate binary formats without modification.

meta:
  id: kaitai_netty_tcp_example
  endian: be
seq:
  - id: magic
    contents:
      - 0x5F
      - 0x24
      - 0x7B
      - 0x11
      - 0x5F
  - id: uuid
    size: 16
    doc: |
      w/o type attribute it means byte, with 'size: 16' it turns into byte[16]
  - id: name
    type: str
    size: 24
    encoding: UTF-8
  - id: birth_year
    type: u2
  - id: weight
    type: f8
  - id: rating
    type: s4
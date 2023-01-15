meta:
  id: kaitai_netty_tcp_example
  endian: be
seq:
  - id: header
    type: msg_header_ype
  - id: body
    type: msg_body_type
  - id: footer
    type: msg_footer_type
types:
  msg_header_ype:
    seq:
      - id: preamble
        contents: [ 0x5F,0x24,0x7B,0x11,0x5F ]
      - id: message_number
        type: u1
        doc: |
          w/o type attribute it means byte, with 'size: 16' it turns into byte[16]
      - id: message_length
        type: u1
  msg_footer_type:
    seq:
      - id: postamble
        size: 5
        contents: [ 0x5E,0x23,0x7A,0x10,0x5E ]
  msg_body_type:
    seq:
      - id: num_rows
        type: u2
      - id: rows
        type: s4
        repeat: expr
        repeat-expr: num_rows
      - id: num_columns
        type: u2
      - id: columns
        type: s4
        repeat: expr
        repeat-expr: num_columns
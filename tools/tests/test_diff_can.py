from tools.diff_can import byte_values, changed_bytes, load_frames, new_ids, parse_candump
from tools.rdp import Frame, parse_line


def frames(*lines):
    return [parse_line(l) for l in lines]


def test_byte_fixed_in_base_and_changed_in_action_is_reported():
    base = byte_values(frames("C,0,1A0,s,0010", "C,10,1A0,s,0010"))
    action = byte_values(frames("C,0,1A0,s,0011", "C,10,1A0,s,0010"))
    assert changed_bytes(base, action) == [("1A0", 1, "10", ["11"])]


def test_counter_varying_in_base_is_ignored():
    base = byte_values(frames("C,0,1A0,s,01", "C,10,1A0,s,02"))
    action = byte_values(frames("C,0,1A0,s,03"))
    assert changed_bytes(base, action) == []


def test_new_id_is_reported():
    assert new_ids(frames("C,0,1A0,s,00"), frames("C,0,1A0,s,00", "C,0,2B0,s,FF")) == ["2B0"]


def test_non_can_frames_are_ignored():
    assert byte_values(frames("E,0,3,1", "F,0,0,0")) == {}


def test_parse_candump_standard():
    assert parse_candump("(1730000000.123456) can0 7E8#03410D00") == Frame("C", 1730000000123, ("7E8", "s", "03410D00"))


def test_parse_candump_extended():
    assert parse_candump("(1730000000.5) can0 18daf110#03410d") == Frame("C", 1730000000500, ("18DAF110", "x", "03410D"))


def test_parse_candump_empty_data():
    assert parse_candump("(1730000000.123456) can0 7E8#") == Frame("C", 1730000000123, ("7E8", "s", ""))


def test_parse_candump_remote_is_none():
    assert parse_candump("(1730000000.123456) can0 7E8#R") is None


def test_parse_candump_garbage_is_none():
    for line in ["", "basura total", "(1730000000.123456) can0 7E8#123", "(1730000000.123456) can0 7E8#001122334455667788"]:
        assert parse_candump(line) is None


def test_load_frames_mixes_rdp_and_candump(tmp_path):
    p = tmp_path / "mix.log"
    p.write_text("C,0,1A0,s,00\n(1730000000.123456) can0 7E8#03410D00\nbasura total\n")
    assert load_frames(p) == [
        Frame("C", 0, ("1A0", "s", "00")),
        Frame("C", 1730000000123, ("7E8", "s", "03410D00")),
    ]


def test_changed_bytes_with_candump_frames():
    base = byte_values([parse_candump("(1730000000.1) can0 7E8#03410D00"), parse_candump("(1730000000.2) can0 7E8#03410D00")])
    action = byte_values([parse_candump("(1730000000.3) can0 7E8#03410E00")])
    assert changed_bytes(base, action) == [("7E8", 2, "0D", ["0E"])]


def test_rdp_and_candump_ids_match_without_leading_zeros():
    base = [parse_line("C,0,A0,s,00")]
    action = [parse_candump("(1730000000.000000) can0 0A0#01")]
    assert new_ids(base, action) == []
    assert changed_bytes(byte_values(base), byte_values(action)) == [("0A0", 0, "00", ["01"])]
